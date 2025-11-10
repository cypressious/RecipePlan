rootProject.name = "RecipePlan"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
//        maven("https://repo.sellmair.io") {
//            mavenContent {
//                includeGroupAndSubgroups("org.jetbrains.kotlin")
//            }
//        }
        mavenCentral()
        google()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
//        maven("https://repo.sellmair.io") {
//            mavenContent {
//                includeGroupAndSubgroups("org.jetbrains.kotlin")
//            }
//        }
        google()
        mavenCentral()
        mavenLocal()
    }
}

include(":composeApp")