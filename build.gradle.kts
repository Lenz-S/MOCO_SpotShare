// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Google Services Plugin für Firebase auf Projektebene hinzufügen
    alias(libs.plugins.google.services) apply false
}