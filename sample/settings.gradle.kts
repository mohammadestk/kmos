rootProject.name = "sample"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Reference the parent SDK project via composite build
includeBuild("..") {
    dependencySubstitution {
        substitute(module("dev.esteki.kmos:sync-core")).using(project(":sync-core"))
        substitute(module("dev.esteki.kmos:sync-storage")).using(project(":sync-storage"))
        substitute(module("dev.esteki.kmos:sync-network")).using(project(":sync-network"))
        substitute(module("dev.esteki.kmos:sync-testing")).using(project(":sync-testing"))
        substitute(module("dev.esteki.kmos:sql-js-worker")).using(project(":sql-js-worker"))
        substitute(module("dev.esteki.kmos:sqlite-wasm-worker")).using(project(":sqlite-wasm-worker"))
    }
}

include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")
