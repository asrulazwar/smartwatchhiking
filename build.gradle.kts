// Semua plugin dideklarasikan di root (apply false) agar AGP dan Kotlin Gradle Plugin dimuat
// dalam satu classloader. Modul :shared juga punya settings.gradle.kts sendiri supaya bisa
// diuji di mesin tanpa Android SDK: `cd shared && ../gradlew test` (lihat CLAUDE.md).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
