plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.moweapp.antonio"
    // 🔥 UPDATED TO API 36 (Android 16)
    compileSdk = 36 

    defaultConfig {
        applicationId = "com.moweapp.antonio"
        minSdk = 26 // Modern baseline for GeckoView 149+
        targetSdk = 36
        versionCode = 7
        versionName = "1.7-gecko-modern"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }
    }

    buildFeatures {
        viewBinding = true
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
    // 🔥 LATEST GECKOVIEW (Stable 149.0 - Released March 24, 2026)
    implementation("org.mozilla.geckoview:geckoview-omni:149.0.20260324091245")

    // 🔥 MODERN ANDROIDX STACK
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // 🔥 LATEST LIFECYCLE & COROUTINES
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
