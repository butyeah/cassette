plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ruidoespontaneo.cassette"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ruidoespontaneo.cassette"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // MusicBrainz requires every client to send a meaningful User-Agent
        // identifying the application and a way to reach its maintainer.
        // See https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting
        buildConfigField(
            "String",
            "MUSICBRAINZ_CONTACT",
            "\"https://github.com/butyeah/cassette\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (used for the calendar's date ranges) needs desugaring
        // below API 26, and minSdk here is 24.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    // kotlinx-coroutines-play-services: bridges Firestore's Task-based API into
    // suspend functions via `.await()`.
    implementation(libs.kotlinx.coroutines.play.services)
    // Firestore backs GetAlbumsByDayUseCase (dayinhistory package). This compiles
    // and links, but won't initialize at runtime until the app has a real Firebase
    // project: that also needs the com.google.gms.google-services plugin applied
    // here and an app/google-services.json, both deliberately left out until the
    // project exists (see the "This day in history" plan).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}