import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
       namespace = "dev.esteki.kmos.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            api(libs.compose.uiToolingPreview)
            api(libs.androidx.lifecycle.viewmodelCompose)
            api(libs.androidx.lifecycle.runtimeCompose)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
            api(libs.koin.core)
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.json)
            api(libs.room3.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
        webMain.dependencies {
            implementation(libs.androidx.sqlite.web)
            implementation(projects.sqliteWasmWorker)
            implementation(projects.sqlJsWorker)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        iosMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspJvm", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspAndroid", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspJvm", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspJs", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspWasmJs", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspIosArm64", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspIosSimulatorArm64", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
}
