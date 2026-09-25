# Cassette's own R8 rules. Most libraries ship theirs (kotlinx.serialization, Ktor, OkHttp,
# Hilt, Firebase, Compose, Media3, Coil), so this only covers what they can't know about.

# Keep file and line numbers in stack traces, so Crashlytics reports stay readable once the
# Crashlytics Gradle plugin uploads the mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
