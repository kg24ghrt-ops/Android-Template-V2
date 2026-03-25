plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.moweapp.antonio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moweapp.antonio"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.6-gecko"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }
    }

    buildFeatures {
        // 🔥 ENABLE THIS for XML layout support
        viewBinding = true
        // You can keep compose = true if you have other compose screens, 
        // but for the browser XML, viewBinding is the priority.
        compose = false 
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 🔥 GECKOVIEW ENGINE
    implementation("org.mozilla.geckoview:geckoview-131.0.3")

    // UI Support (Material 3 & AndroidX)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.core:core-ktx:1.15.0")
    
    // Kotlin Coroutines (For your VPN & Ad-blocker)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
}
