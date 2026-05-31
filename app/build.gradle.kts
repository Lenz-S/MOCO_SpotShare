import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Lädt die local.properties für den Zugriff auf den Access Token
val localProperties = Properties()
val localPropertiesFile = rootProject.projectDir.resolve("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.moco"
    // Erhöht auf 36, da neuere androidx-Abhängigkeiten dies zwingend erfordern
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.moco"
        minSdk = 24
        // Beibehalten auf 35 für bessere Kompatibilität mit dem aktuellen Emulator/Mapbox
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

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
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
    
    // Mapbox Search SDK für Geocoding (Adresse -> Koordinaten)
    implementation("com.mapbox.search:mapbox-search-android:2.1.0") {
        exclude(group = "com.mapbox.common", module = "common")
    }
    
    // Navigation Library für den Wechsel zwischen den Screens
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
