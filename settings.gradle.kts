pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GlobeTraveler"

include(":domain")
include(":data")
include(":design")
include(":map")
include(":app-android")
include(":app-ios")
