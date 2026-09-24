plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    // Match :app/:data's compileOptions (JavaVersion.VERSION_11).
    jvmToolchain(11)

    // Android consumes the JVM target; the iOS targets are for the shared framework iosApp links
    // (iosX64 is the simulator on Intel Macs, iosSimulatorArm64 on Apple silicon).
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        // For core/di/Inject.kt's expect/actual annotation.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            // `api` because Album/AlbumDetail expose LocalDate to every consumer.
            api(libs.kotlinx.datetime)
        }
        jvmMain.dependencies {
            // Backs the JVM `actual` of Inject, so Hilt still sees javax.inject.Inject.
            implementation(libs.javax.inject)
        }
        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
