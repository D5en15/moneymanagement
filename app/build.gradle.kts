plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.jetbrainsKotlinCompose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.moneymanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.moneymanager"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register("exportDebugApkToRoot") {
    dependsOn(":app:assembleDebug")

    val sourceApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    val outputApk = rootProject.layout.projectDirectory.file("MoneyManager-debug.apk")

    inputs.file(sourceApk)
    outputs.file(outputApk)

    doLast {
        val sourceFile = sourceApk.get().asFile
        val outputFile = outputApk.asFile
        sourceFile.copyTo(outputFile, overwrite = true)
        println("DEBUG APK exported to: ${outputFile.absolutePath}")
    }
}

tasks.register("exportReleaseApkToRoot") {
    dependsOn(":app:assembleRelease")

    val releaseApkDir = layout.buildDirectory.dir("outputs/apk/release")
    val outputApk = rootProject.layout.projectDirectory.file("MoneyManager-release.apk")

    outputs.file(outputApk)

    doLast {
        val releaseDirFile = releaseApkDir.get().asFile
        val sourceFile = releaseDirFile
            .listFiles()
            ?.filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }

        if (sourceFile == null) {
            println("No release APK found in: ${releaseDirFile.absolutePath}")
            println("If :app:assembleRelease failed, configure signingConfigs for release in app/build.gradle.kts.")
            return@doLast
        }

        val outputFile = outputApk.asFile
        sourceFile.copyTo(outputFile, overwrite = true)
        println("RELEASE APK exported to: ${outputFile.absolutePath}")
    }
}
