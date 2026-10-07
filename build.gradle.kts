// Plugin Kotlin/KSP dideklarasikan di sini (apply false) agar hanya dimuat sekali untuk semua modul.
// Android Gradle Plugin dan Hilt sengaja hanya di modul Android, supaya konfigurasi :shared
// tidak memaksa resolusi artefak Google Maven di mesin tanpa akses dl.google.com.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
