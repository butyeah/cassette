plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ruidoespontaneo.cassette.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        // NetworkModule's MusicBrainz User-Agent needs a contact + version string. A library
        // module's BuildConfig has no versionName (that's app-only in the AGP library DSL), and
        // it can't read :app's BuildConfig either, so both are duplicated here — keep in sync
        // with app/build.gradle.kts's defaultConfig.versionName/MUSICBRAINZ_CONTACT.
        buildConfigField("String", "APP_VERSION_NAME", "\"1.0\"")
        buildConfigField(
            "String",
            "MUSICBRAINZ_CONTACT",
            "\"https://github.com/butyeah/cassette\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (DayInHistoryRepositoryImpl, MusicBrainzRepositoryImpl) needs desugaring
        // below API 26, and minSdk here is 24 — matches app/build.gradle.kts's compileOptions.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":domain"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Bridges Firestore's Task-based API into suspend functions via `.await()`.
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    // Credential Manager + Google ID: the current Google-recommended way to offer Google
    // Sign-In, replacing the deprecated GoogleSignInClient API.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
