plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(providers.gradleProperty("globetraveler.jdk").get().toInt())
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.material3)
        }
    }
}

android {
    namespace = "dev.doug.globetraveler.design"
    compileSdk = providers.gradleProperty("globetraveler.compileSdk").get().toInt()
    defaultConfig {
        minSdk = providers.gradleProperty("globetraveler.minSdk").get().toInt()
    }
}
