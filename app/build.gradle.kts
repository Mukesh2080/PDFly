plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mukesh.pdfly"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mukesh.pdfly"
        minSdk = 24
        targetSdk = 35
        versionCode = 1   // increment each release!
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true       // shrink + obfuscate
            isShrinkResources = true     // remove unused resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // keep debug unsigned
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
