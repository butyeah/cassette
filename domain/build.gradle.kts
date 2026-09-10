plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // Match :app/:data's compileOptions (JavaVersion.VERSION_11).
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
