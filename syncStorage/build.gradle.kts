import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.dokka)
    `maven-publish`
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "dev.esteki.kmos.sync.storage"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.syncCore)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.room3.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.syncTesting)
        }
        androidMain.dependencies {
            api(libs.androidx.sqlite.bundled)
        }
        jvmMain.dependencies {
            api(libs.androidx.sqlite.bundled)
        }
        iosMain.dependencies {
            api(libs.androidx.sqlite.bundled)
        }
        webMain.dependencies {
            api(libs.androidx.sqlite.web)
            implementation(projects.sqliteWasmWorker)
            implementation(projects.sqlJsWorker)
        }
    }
}

dependencies {
    add("kspJvm", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspAndroid", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspJs", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspWasmJs", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspIosArm64", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
    add("kspIosSimulatorArm64", "androidx.room3:room3-compiler:${libs.versions.room3.get()}")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

afterEvaluate {
    publishing {
        publications.withType<MavenPublication>().configureEach {
            artifact(javadocJar)
            pom {
                name.set("KMOS - Sync Storage")
                description.set("Kotlin Multiplatform Offline-First Sync SDK - Room 3 storage adapter")
                url.set("https://github.com/mohammadestk/Kmos")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}
