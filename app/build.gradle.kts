plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mukesh.pdfly"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mukesh.pdfly"
        minSdk = 24
        targetSdk = 34
        versionCode = 1   // increment each release!
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.property("PDFLY_STORE_FILE") as String)
            storePassword = project.property("PDFLY_STORE_PASSWORD") as String
            keyAlias = project.property("PDFLY_KEY_ALIAS") as String
            keyPassword = project.property("PDFLY_KEY_PASSWORD") as String
        }
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
            signingConfig = signingConfigs.getByName("release")
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
