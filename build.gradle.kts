// Plugin dideklarasikan per modul lewat version catalog (lihat gradle/libs.versions.toml).
// Root sengaja kosong agar konfigurasi :shared tidak memaksa resolusi Android Gradle Plugin.
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
