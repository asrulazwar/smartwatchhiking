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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pendaki"

include(":shared")

// Modul Android hanya dikonfigurasi jika Android SDK tersedia. Dengan begitu
// `./gradlew :shared:test` tetap bisa dijalankan di mesin tanpa SDK (mis. agen
// yang tidak punya akses ke dl.google.com). Lihat CLAUDE.md.
val localProps = java.util.Properties().apply {
    val f = rootDir.resolve("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val sdkAvailable = localProps.getProperty("sdk.dir") != null ||
    System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null
if (sdkAvailable) {
    include(":wear")
    include(":mobile")
} else {
    logger.warn("Android SDK tidak ditemukan: modul :wear dan :mobile dilewati (hanya :shared).")
}
