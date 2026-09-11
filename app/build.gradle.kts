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
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    lint {
        // A single `:app:lint` run should cover the whole app, including :data/:domain — not
        // just :app's own sources.
        checkDependencies = true
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
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Album art loading (AlbumDetailScreen, OneDayLikeTodayScreen).
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    // Crashlytics stays app-side — it's app infra (CrashlyticsTree/CassetteApplication), not a
    // repository, so it doesn't belong in :data.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    // Logging facade — CrashlyticsTree (core/logging) forwards warnings/errors to Firebase.
    implementation(libs.timber)
    implementation(libs.androidx.navigation.compose)
    // LoginViewModel references CredentialManager/GetCredentialRequest/CustomCredential/
    // GetCredentialException/GoogleIdTokenCredential directly; the concrete Google ID Credential
    // Manager wiring lives in :data's GoogleCredentialModule.
    implementation(libs.androidx.credentials)
    implementation(libs.googleid)
    // Daily reminder: notifications/data + notifications/di stay in :app (see the module-split
    // plan — DailyReminderWorker needs MainActivity/app resources, and
    // NotificationScheduleRepositoryImpl enqueues it by class reference). WorkManager schedules
    // the notification, DataStore persists the chosen time.
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