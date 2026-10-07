# Hanya model yang diserialisasi/dibaca lewat refleksi yang dipertahankan utuh; sisanya disusutkan R8.
-keep class id.asrul.pendaki.shared.model.** { *; }
-keep class id.asrul.pendaki.shared.datalayer.** { *; }
-repackageclasses ''
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable

# kotlinx.serialization (aturan resmi)
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class id.asrul.pendaki.**$$serializer { *; }
-keepclassmembers class id.asrul.pendaki.** { *** Companion; }
-keepclasseswithmembers class id.asrul.pendaki.** { kotlinx.serialization.KSerializer serializer(...); }

# Health Services / Play Services Wearable memakai Parcelable & refleksi ringan
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
# Health Connect
-keep class androidx.health.connect.client.** { *; }
