plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(providers.gradleProperty("globetraveler.jdk").get().toInt())
    jvm()
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(libs.room3.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.kermit)
            implementation(libs.compose.runtime)
            implementation(libs.compose.componentsResources)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.serializationKotlinxJson)
            implementation(libs.ktor.clientLogging)
        }
        androidMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.clientDarwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.clientJava)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.ktor.clientMock)
        }
    }
}

android {
    namespace = "dev.doug.globetraveler.data"
    compileSdk = providers.gradleProperty("globetraveler.compileSdk").get().toInt()
    defaultConfig {
        minSdk = providers.gradleProperty("globetraveler.minSdk").get().toInt()
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "dev.doug.globetraveler.data"
}

dependencies {
    add("kspAndroid", libs.room3.compiler)
    add("kspJvm", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
