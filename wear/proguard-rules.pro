# Kode aplikasi sendiri dipertahankan utuh (kecil); penyusutan terutama menyasar pustaka.
-keep class id.asrul.pendaki.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable

# kotlinx.serialization (aturan resmi)
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class id.asrul.pendaki.**$$serializer { *; }
-keepclassmembers class id.asrul.pendaki.** { *** Companion; }
-keepclasseswithmembers class id.asrul.pendaki.** { kotlinx.serialization.KSerializer serializer(...); }

# Health Services / Play Services Wearable memakai Parcelable & refleksi ringan
-keep class androidx.health.services.client.** { *; }
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
