import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Google Services Plugin für die Firebase-Konfiguration
    alias(libs.plugins.google.services)
}

// Lädt die local.properties für den Zugriff auf den Access Token
val localProperties = Properties()
val localPropertiesFile = rootProject.projectDir.resolve("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.moco"
    // CompileSdk muss auf 36 bleiben, damit die Bibliotheken bauen
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.moco"
        minSdk = 24
        // TargetSdk auf 35 begrenzt, um Preview-Bugs der API 36 zu umgehen
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Fügt den Mapbox Access Token als String-Ressource hinzu.
        // So bleibt der Token in der local.properties und landet nicht in Git.
        val mapboxToken = localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""
        resValue("string", "mapbox_access_token", mapboxToken)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Ermöglicht das Hinzufügen von Ressourcen (wie dem Mapbox Token) direkt aus Gradle
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Hinzufügen der Mapbox Compose Library
    implementation(libs.mapbox.compose)
    // maps-android wird zwingend für Kamera-Zustände und Basis-Klassen benötigt
    implementation(libs.mapbox.maps)
    // Mapbox Search SDK für Geocoding
    implementation(libs.mapbox.search)
    // Mapbox Java Services für den klassischen Geocoder
    implementation(libs.mapbox.java.services)
    implementation(libs.mapbox.java.geojson)
    
    // Navigation Library für den Wechsel zwischen den Screens
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Firebase-Abhängigkeiten (BoM sorgt für kompatible Versionen)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
