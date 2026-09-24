# Cassette's own R8 rules. Most libraries ship theirs (Retrofit, Moshi codegen, OkHttp, Hilt,
# Firebase, Compose, Media3, Coil), so this only covers what they can't know about.

# Keep file and line numbers in stack traces, so Crashlytics reports stay readable once the
# Crashlytics Gradle plugin uploads the mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Moshi codegen: Moshi finds a model's generated adapter by name at runtime ("<Model>JsonAdapter"),
# so nothing references the adapter directly and R8 would remove it; the model's name must survive
# too. Codegen's own rules don't reach the :data module's models, which failed as
# "Failed to find the generated JsonAdapter class" in release builds.
-keepnames @com.squareup.moshi.JsonClass class *
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*
-keep class <1>_<2>JsonAdapter {
    <init>(...);
    <fields>;
}

# Models with default values are built through Kotlin's synthetic "defaults" constructor, which the
# generated adapter looks up by reflection, including its kotlin.jvm.internal.DefaultConstructorMarker
# parameter. Keep those constructors and the marker's name, or ItunesResultDto fails with
# NoSuchMethodException in release builds.
-if @com.squareup.moshi.JsonClass class *
-keepclassmembers class <1> {
    <init>(...);
}
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
