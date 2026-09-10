plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    // Must come after google-services, which is what provisions the Firebase app this
    // plugin uploads mapping/symbol files for.
    alias(libs.plugins.firebase.crashlytics)
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
    testOptions {
        unitTests {
            // Building a real androidx.credentials.CredentialOption (see LoginViewModelTest's
            // fakes for LoginViewModel's Credential Manager deps) touches android.os.Bundle,
            // which isn't mocked in plain JUnit tests by default — return defaults instead of
            // throwing rather than pulling in Robolectric for this alone.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
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
    // Firestore backs GetAlbumsByDayUseCase (dayinhistory package), reading the
    // albumsByDay collection populated offline by scripts/build_day_index.py +
    // the upload script (see the "This day in history" plan).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    // Logging facade — CrashlyticsTree (core/logging) forwards warnings/errors to Firebase.
    implementation(libs.timber)
    implementation(libs.androidx.navigation.compose)
    // Credential Manager + Google ID: the current Google-recommended way to offer Google
    // Sign-In, replacing the deprecated GoogleSignInClient API.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    // Daily reminder: WorkManager schedules the notification, DataStore persists the chosen time.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
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