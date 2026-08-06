plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(providers.gradleProperty("globetraveler.jdk").get().toInt())
    jvm()
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutinesCore)
            api(libs.kotlinx.datetime)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "dev.doug.globetraveler.domain"
    compileSdk = providers.gradleProperty("globetraveler.compileSdk").get().toInt()
    defaultConfig {
        minSdk = providers.gradleProperty("globetraveler.minSdk").get().toInt()
    }
}
