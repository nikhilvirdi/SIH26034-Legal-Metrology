# Default ProGuard rules for an Android app
# Add project specific ProGuard rules here.

# Keep ARCore classes
-keep class com.google.ar.** { *; }
-keep class com.google.ar.core.** { *; }

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
