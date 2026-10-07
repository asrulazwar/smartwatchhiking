// Build mandiri untuk :shared (Kotlin JVM murni). Dipakai di mesin tanpa Android SDK:
//   cd shared && ../gradlew test
// Saat dibangun dari root, file ini diabaikan dan :shared menjadi subproyek biasa.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories { mavenCentral() }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "shared"
