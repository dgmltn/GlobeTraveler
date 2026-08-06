plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(providers.gradleProperty("globetraveler.jdk").get().toInt())
}

android {
    namespace = "dev.doug.globetraveler.app"
    compileSdk = providers.gradleProperty("globetraveler.compileSdk").get().toInt()

    defaultConfig {
        applicationId = "dev.doug.globetraveler"
        minSdk = providers.gradleProperty("globetraveler.minSdk").get().toInt()
        targetSdk = providers.gradleProperty("globetraveler.targetSdk").get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":design"))
    implementation(project(":map"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
}
