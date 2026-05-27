pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// SICHERHEITSHINWEIS: Wir laden hier die 'local.properties', um sensible API-Keys (Tokens)
// auszulesen. Diese Datei wird NICHT in Git hochgeladen (siehe .gitignore).
// Team-Mitglieder müssen ihre eigenen Tokens in ihre lokale 'local.properties' eintragen.
val localProperties = java.util.Properties().apply {
    val file = rootProject.projectDir.resolve("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // Mapbox Maven Repository: Wird für den Download der Mapbox-SDKs benötigt.
        // Die Authentifizierung erfolgt über den Secret Download Token (sk.xxx).
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                // Passwort wird sicher aus der local.properties bezogen.
                password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN") 
                    ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
                    ?: ""
            }
        }
    }
}

rootProject.name = "MOCO"
include(":app")
