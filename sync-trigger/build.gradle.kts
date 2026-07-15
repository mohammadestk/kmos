import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.dokka)
    `maven-publish`
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
        namespace = "dev.esteki.kmos.sync.trigger"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.syncCore)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    description = "Packages Dokka HTML documentation as a JAR for publication"
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

afterEvaluate {
    publishing {
        publications.withType<MavenPublication>().configureEach {
            artifact(javadocJar)
            pom {
                name.set("KMOS - Sync Trigger")
                description.set("Kotlin Multiplatform Offline-First Sync SDK - Lifecycle hooks and trigger management")
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
