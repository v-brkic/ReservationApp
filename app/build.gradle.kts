plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Dodaj plugin za google-services
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.reservationapp" // your package
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.reservationapp"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Jetpack Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui) // If using Compose UI library from your version catalog
    implementation(platform("androidx.compose:compose-bom:2023.05.01"))
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.tooling)

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    // Firebase Analytics, Auth, Firestore
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.6.0")

    // ZXing (if needed for scanning/generating QR codes)
    implementation("com.google.zxing:core:3.5.2")
    // Za skeniranje na Androidu (embedded library):
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// At the very end:
apply(plugin = "com.google.gms.google-services")
