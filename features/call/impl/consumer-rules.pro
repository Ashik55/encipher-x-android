# Keep Retrofit service methods
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep models used in API responses
-keep class io.element.android.libraries.matrix.impl.call.model.** { *; }

# Keep Kotlin Serialization types
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Retrofit
-keepattributes Signature
-keepattributes Exceptions

# Keep Gson related classes
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
