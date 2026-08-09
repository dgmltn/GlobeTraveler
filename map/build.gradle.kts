plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(providers.gradleProperty("globetraveler.jdk").get().toInt())
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(project(":design"))
            implementation(libs.maplibre.compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewmodel)
            implementation(libs.lifecycle.viewmodelCompose)
            implementation(libs.lifecycle.runtimeCompose)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.componentsResources)
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.kotlinx.collectionsImmutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kermit)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "dev.doug.globetraveler.map"
}

android {
    namespace = "dev.doug.globetraveler.map"
    compileSdk = providers.gradleProperty("globetraveler.compileSdk").get().toInt()
    defaultConfig {
        minSdk = providers.gradleProperty("globetraveler.minSdk").get().toInt()
    }
}
