buildscript {
    dependencies {
        // Align AGP's built-in Kotlin runtime without applying kotlin-android.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}
