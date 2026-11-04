plugins {
    alias(libs.plugins.android.application) apply false
    // Puts the pinned Kotlin Gradle plugin on the classpath for AGP built-in Kotlin.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
