import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.ruidoespontaneo.cassette.data"
        compileSdk = 37
        minSdk = 24

        // Hilt's KSP processor generates Java (factories, hilt_aggregated_deps), which this plugin
        // only compiles when Java is enabled.
        withJava()

        compilerOptions {
            // Match :app's compileOptions (JavaVersion.VERSION_11).
            jvmTarget.set(JvmTarget.JVM_11)
        }

        // GoogleCredentialModule reads google_web_client_id from res/values/strings.xml.
        androidResources { enable = true }

        withHostTest {
            // Firebase's exception constructors call android.text.TextUtils, which is only a stub in
            // local unit tests; default return values let AuthFailureMappingTest build them.
            isReturnDefaultValues = true
        }
    }

    // The framework iosApp links: this module's shared code plus :domain's models and use cases.
    // iosX64 is the simulator on Intel Macs, iosSimulatorArm64 on Apple silicon.
    val xcFramework = XCFramework("Shared")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":domain"))
            // By default, calling a suspend function from Swift anywhere but the main thread
            // crashes the app, and Swift runs nonisolated async code on background threads. The
            // shared code launches its own coroutines, so it doesn't care which thread calls it.
            binaryOption("objcExportSuspendFunctionLaunchThreadRestriction", "none")
            xcFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // `api` so the framework can export :domain to Swift.
            api(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            // Bridges Firestore's Task-based API into suspend functions via `.await()`.
            implementation(libs.kotlinx.coroutines.play.services)

            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)

            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)

            // Credential Manager + Google ID: the current Google-recommended way to offer Google
            // Sign-In, replacing the deprecated GoogleSignInClient API.
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)

            implementation(libs.hilt.android)

            implementation(libs.timber)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }
    }
}

dependencies {
    // Hilt's annotation processor for the Android target's @Module/@Inject classes. The Hilt
    // Gradle plugin isn't needed here: it only rewrites @AndroidEntryPoint classes, and :data has none.
    add("kspAndroid", libs.hilt.compiler)
}
