# Shamba AI for Android

Shamba AI is an offline crop-health assistant for smallholder farmers in East Africa.
The farmer takes a photo of one leaf. The phone runs the vision model and shows the
disease name, how sure the model is, how urgent the problem is, and what to do.

- No sign-in. No internet permission. The model and the advice are in the APK.
- Photos never leave the phone. Cloud backup is off (`allowBackup=false` and
  `data_extraction_rules.xml`).
- Crops: beans, cassava, maize, banana. 14 classes.
- Languages: English, Kiswahili, Français.
- Runtime: PyTorch ExecuTorch with the XNNPACK backend (int8 model, about 2.7 MB).

## Screens

1. **Language** (first run only): English, Kiswahili or Français.
2. **Scan**: one big yellow "Check a leaf" button, the tip "one leaf, fill the frame,
   daylight", and "Pick from gallery".
3. **Camera**: CameraX preview with a square framing guide. The square is the same
   center square that the model sees.
4. **Result**: photo, disease name, certainty in words (Very likely, Likely, Not sure)
   with a bar, urgency badge with icon and text (Act today, Act this week,
   Watch closely, Healthy), numbered steps, prevention, when to call the extension
   officer, safety note, disclaimer, the 2 runner-up classes, Share (Android share
   sheet, for example WhatsApp) and Read aloud (Android TextToSpeech).
   When the result is "Not sure", the screen shows the retake tips instead of advice.
5. **History**: past scans on the phone (thumbnail, date, result), with delete.
6. **Library**: diseases by crop, from the knowledge base.
7. **About**: language, model version, file SHA-256 and a check against the manifest,
   backend "ExecuTorch · XNNPACK", last inference time, privacy statement.

## Build

Requirements:

- Java 25 runtime. The build gets a JDK 25 compiler toolchain automatically (see below).
- Android SDK. Write its path in `local.properties` (this file is not in git):

  ```properties
  sdk.dir=/home/you/Android/Sdk
  ```

Commands:

```bash
cd apps/android
./gradlew assembleDebug testDebugUnitTest
# APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug          # install on a connected phone
```

Versions (in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`):

| Part | Version |
| --- | --- |
| Gradle | 9.8.2 |
| Android Gradle plugin | 9.5.0 (built-in Kotlin) |
| Kotlin and Compose compiler | 2.4.21 |
| Compose BOM | 2026.09.00 (Material 3) |
| CameraX | 1.6.2 |
| ExecuTorch | `org.pytorch:executorch-android:1.5.1` |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |

### Java toolchain

Some machines have only a Java runtime and no `javac`. AGP needs a Java compiler.
`settings.gradle.kts` applies the Foojay toolchain resolver, and `app/build.gradle.kts`
asks for a JDK 25 toolchain. If no local JDK 25 has `javac`, Gradle downloads one
into `~/.gradle/jdks` on the first build. The bytecode target is Java 17.

### Low-memory build

`gradle.properties` keeps the build small for a machine with about 1.5 GB of free RAM:
`-Xmx1536m`, 2 workers, no parallel build, Kotlin compiled inside the Gradle daemon
(no separate Kotlin daemon), and a daemon that stops after 10 idle minutes.
To free the memory at once, run `./gradlew --stop`.

## How the model gets into the APK

The repo is the single source of truth. Nothing binary is kept in `apps/android`.

The Gradle task `sync<Variant>ShambaAssets` (in `app/build.gradle.kts`) runs before
`merge<Variant>Assets`. AGP wires it through
`variant.sources.assets.addGeneratedSourceDirectory`. The task:

1. Takes the model from `../../ml/artifacts/` if that dir has both
   `shamba_crop_health_xnnpack_int8.pte` and `manifest.json`. Otherwise it takes them
   from `../../ml/runs/smoke/artifacts/`. If neither has them, the build stops.
2. Computes the SHA-256 of the `.pte` and compares it with the `files[].sha256` entry
   in `manifest.json`. A mismatch stops the build.
3. Copies `../../knowledge/diseases.json`.
4. Writes everything to `app/build/generated/shamba/<variant>/assets` (AGP picks the
   exact dir):

   ```text
   model/shamba_crop_health_xnnpack_int8.pte
   model/manifest.json
   knowledge/diseases.json
   ```

The task tracks all candidate files as inputs. When training writes `ml/artifacts/`,
the next build uses the new model with no change to the app.

At first start the app copies the `.pte` from the APK to `filesDir/model/` (ExecuTorch
loads from a file path), loads it once, and runs one warm-up pass. It copies the file
again only when the bundled manifest SHA-256 changes.

### Model contract

- Input: float32 `[1, 3, 224, 224]`, NCHW, RGB, values in `[0, 1]`. Normalization is
  inside the model.
- Preprocess (`ml/Preprocess.kt`): take the center square with side
  `shortSide * 224 / 256`, scale it to 224 x 224 with bilinear filtering, and write
  planar R, G, B planes divided by 255. When the square is larger than 224 the
  bilinear filter widens with the scale (antialias), the same as PIL and torchvision.
- Output: softmax probabilities `[1, 14]`. Class ids come from `manifest.json`
  `"classes"`. Thresholds come from `"thresholds"`: confident `>= 0.70`,
  uncertain `< 0.45`, otherwise likely.

## Architecture

Single module `:app`, package `ai.shamba.app`.

| Package | Content |
| --- | --- |
| `ai.shamba.app` | `ShambaApp` (Application), `AppContainer` (loads the manifest, the knowledge base and the model once per process), `MainActivity` (applies the app language in `attachBaseContext`). |
| `ml` | `Preprocess` (pure Kotlin), `PredictionLogic` and `Thresholds` (pure Kotlin), `ModelManifest` (parser), `CropClassifier` (ExecuTorch `Module`, runs on `Dispatchers.Default` behind a mutex, measures latency). |
| `knowledge` | `KnowledgeBase` parser for `diseases.json`, `Language`, `LocalizedText` with fallback to English. |
| `data` | `ScanRecord` and `HistoryCodec` (JSON), `HistoryStore` (one JSON file with atomic writes, plus one photo and one thumbnail per scan), `AppSettings` (SharedPreferences). |
| `image` | `PhotoProcessor`: low-memory decode, EXIF rotation, small JPEG copies. |
| `ui` | `AppViewModel` (back stack, scan pipeline), `ShambaRoot`, `ResultText` (share and read-aloud text). |
| `ui.screens` | Language, Scan, Camera, Result, Disease, History, Library, About. |
| `ui.components`, `ui.theme` | Buttons, urgency badge, certainty meter, design tokens, light and dark theme. |
| `util` | `ReadAloud` (TextToSpeech with fallback), locale and number formats. |

All UI strings are in `res/values` (English), `res/values-sw` and `res/values-fr`.
Disease text comes from `diseases.json` in the app language, with English as the
fallback. The Kiswahili and French UI strings need a native-speaker review.

Read aloud: many phones have no Kiswahili voice. When the voice for the app language
is missing, the app shows a message and offers to read the English text.

### Unit tests

`./gradlew testDebugUnitTest` runs JVM tests with no device:

- `PreprocessTest`: crop size, exact center crop at scale 1, planar RGB / 255,
  antialiased downscale, upscale, filter weights.
- `PredictionTest`: threshold bands and edges, ranking, softmax fallback, NaN.
- `KnowledgeBaseTest`: parsing, language fallback to English, the real
  `diseases.json`, and a check that every manifest class has advice.
- `ModelManifestTest`, `HistoryCodecTest`.

## Known limits

- Until `ml/artifacts/` exists, the APK contains the smoke-run model. Its check-set
  accuracy is 7.5 %, so its results are not useful. Build again after training ends.
- The ExecuTorch AAR ships `arm64-v8a` and `x86_64` libraries only. The app does not
  install on phones with a 32-bit-only Android (`armeabi-v7a`), which includes some
  low-cost Android Go phones.
- The debug APK is about 43 MB: 21 MB of native code for two ABIs and 15 MB of
  unshrunk dex. A release build with R8 (`isMinifyEnabled = true`) and only
  `arm64-v8a` is much smaller. The ExecuTorch AAR brings its own R8 keep rules.
  The release build in this project does not use R8 yet and was not tested.
- Nothing was tested on a phone or an emulator yet. The JVM unit tests cover
  preprocessing, thresholds, parsing and the history file format.

## Swap in a Vulkan model later

ExecuTorch publishes a second AAR with the Vulkan delegate:
`org.pytorch:executorch-android-vulkan:1.5.1`. Its `libexecutorch.so` registers both
`VulkanBackend` and `XnnpackBackend`, so one APK can load either kind of `.pte`.
The AAR is about 4 MB larger.

1. Export a Vulkan `.pte` in `ml/` (VulkanPartitioner) with the same input and output
   contract, and list it in `manifest.json` `"files"` with its sha256.
2. In `gradle/libs.versions.toml`, change the library to
   `executorch-android-vulkan` (same version).
3. In `app/build.gradle.kts`, set `shambaModelFileName` to the Vulkan file name. In
   `ml/ModelManifest.kt`, set `MODEL_FILE` to the same name.
4. Change the backend label `backend_name` in `res/values/strings.xml`.
5. Test on real phones. Vulkan drivers on low-end Android phones differ a lot. Keep the
   XNNPACK file as a fallback: if `Module.load` or the first `forward` fails, load
   the XNNPACK `.pte` instead.
