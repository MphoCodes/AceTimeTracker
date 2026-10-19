buildscript {
    dependencies {
        // AGP's built-in Kotlin runtime must match the catalog's Compose compiler.
        // The bootstrap classpath is resolved before catalog accessors are available.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.services) apply false
}
