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
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        create("release") {
            storeFile = File(projectDir, "release-keystore.jks")
            storePassword = System.getenv("storePassword")
            keyAlias = System.getenv("keyAlias")
            keyPassword = System.getenv("keyPassword")
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