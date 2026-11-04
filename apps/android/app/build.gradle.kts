import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------
// Model and knowledge assets. The repo is the single source of truth:
//   ../../ml/artifacts            (final training output, used when present)
//   ../../ml/runs/smoke/artifacts (smoke run, used until training is done)
//   ../../knowledge/diseases.json
// The files are copied into a generated assets dir. No binary is kept in apps/android.
// ---------------------------------------------------------------------------
val shambaRepoDir: File = rootProject.projectDir.resolve("../..").canonicalFile
val shambaModelFileName = "shamba_crop_health_xnnpack_int8.pte"
val shambaModelDirs: List<File> = listOf(
    shambaRepoDir.resolve("ml/artifacts"),
    shambaRepoDir.resolve("ml/runs/smoke/artifacts"),
)
val shambaKnowledgeFile: File = shambaRepoDir.resolve("knowledge/diseases.json")

/** Copies the .pte, manifest.json and diseases.json into the generated assets dir. */
abstract class SyncShambaAssetsTask : DefaultTask() {
    @get:Input
    abstract val modelFileName: Property<String>

    /** Candidate model dirs, in priority order. */
    @get:Input
    abstract val candidateDirs: ListProperty<String>

    /** All candidate files. A missing file is a valid input state, so a new ml/artifacts dir triggers a re-run. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val candidateFiles: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val knowledgeFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    @Suppress("UNCHECKED_CAST")
    fun sync() {
        val name = modelFileName.get()
        val source = candidateDirs.get().map(::File).firstOrNull {
            it.resolve(name).isFile && it.resolve("manifest.json").isFile
        } ?: throw GradleException(
            "Shamba AI: no model found. Expected $name and manifest.json in one of: " +
                candidateDirs.get().joinToString(),
        )
        val modelFile = source.resolve(name)
        val manifestFile = source.resolve("manifest.json")

        // Check the model against the sha256 in the manifest. A mismatch stops the build.
        val manifest = groovy.json.JsonSlurper().parse(manifestFile) as Map<String, Any?>
        val entry = (manifest["files"] as? List<Map<String, Any?>>)?.firstOrNull { it["file"] == name }
            ?: throw GradleException("Shamba AI: $manifestFile does not list $name")
        val digest = MessageDigest.getInstance("SHA-256")
        modelFile.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        val expected = entry["sha256"] as? String
        if (expected != null && !expected.equals(actual, ignoreCase = true)) {
            throw GradleException("Shamba AI: sha256 of $modelFile is $actual, manifest says $expected")
        }

        val out = outputDir.get().asFile
        out.deleteRecursively()
        val modelOut = out.resolve("model").apply { mkdirs() }
        val knowledgeOut = out.resolve("knowledge").apply { mkdirs() }
        modelFile.copyTo(modelOut.resolve(name), overwrite = true)
        manifestFile.copyTo(modelOut.resolve("manifest.json"), overwrite = true)
        knowledgeFile.get().asFile.copyTo(knowledgeOut.resolve("diseases.json"), overwrite = true)
        logger.lifecycle("Shamba AI: model ${manifest["version"]} from $source (sha256 $actual)")
    }
}

android {
    namespace = "ai.shamba.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "ai.shamba.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "2.0.0"
        // The ExecuTorch AAR ships these two ABIs only.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        resValue("string", "executorch_version", libs.versions.executorch.get())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        resValues = true
    }

    androidResources {
        // The model is copied out of the APK on first run. Do not spend CPU on inflating it.
        noCompress += "pte"
        // Keep library strings only for the 3 app languages. This cuts APK size.
        localeFilters += listOf("en", "sw", "fr")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            // AGP 9.5 Compose Preview tasks need compiled resources in unit tests.
            isIncludeAndroidResources = true
            all { test ->
                val manifestDir = shambaModelDirs.firstOrNull { it.resolve("manifest.json").isFile }
                test.systemProperty("shamba.knowledge", shambaKnowledgeFile.absolutePath)
                if (manifestDir != null) {
                    test.systemProperty("shamba.manifest", manifestDir.resolve("manifest.json").absolutePath)
                }
                test.inputs.file(shambaKnowledgeFile).withPathSensitivity(PathSensitivity.NONE)
                test.maxHeapSize = "384m"
            }
        }
    }
}

java {
    // Compiler JDK for AGP. The bytecode target stays at Java 17 (see compileOptions).
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

androidComponents {
    onVariants { variant ->
        val taskName = "sync${variant.name.replaceFirstChar { it.uppercase() }}ShambaAssets"
        val syncTask = tasks.register<SyncShambaAssetsTask>(taskName) {
            group = "shamba"
            description = "Copies the crop model, its manifest and the knowledge base into the APK assets."
            modelFileName.set(shambaModelFileName)
            candidateDirs.set(shambaModelDirs.map { it.absolutePath })
            candidateFiles.from(
                shambaModelDirs.flatMap { listOf(it.resolve(shambaModelFileName), it.resolve("manifest.json")) },
            )
            knowledgeFile.set(shambaKnowledgeFile)
        }
        // Registers the dir as an asset source. AGP runs the task before merge<Variant>Assets.
        variant.sources.assets?.addGeneratedSourceDirectory(syncTask, SyncShambaAssetsTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.executorch.android)

    testImplementation(libs.junit)
}
