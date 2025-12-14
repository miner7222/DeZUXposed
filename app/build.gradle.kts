plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.jjhitel.dezux"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.jjhitel.dezux"
        minSdk = 24
        targetSdk = 36
        versionCode = 102
        versionName = "v0.1.2"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("SIGNING_KEY_STORE_PATH")?.let { file(it) }
                ?: file("release-keystore.jks")

            storeFile = keystoreFile
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: System.getenv("storePassword")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: System.getenv("keyAlias")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: System.getenv("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-Xno-param-assertions",
                "-Xno-call-assertions",
                "-Xno-receiver-assertions"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
        resources.excludes += "**/kotlin/**"
        resources.excludes += "kotlin-tooling-metadata.json"
    }
}

dependencies {
    implementation(libs.androidx.annotation)

    compileOnly(libs.xposed.api)
    compileOnly(libs.xposed.api.sources)
    implementation(libs.yukihook.api)
    ksp(libs.yukihook.ksp.xposed)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)
}