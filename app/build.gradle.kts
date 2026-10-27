plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "fr.babas.noir"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.babas.noir"
        minSdk = 29
        targetSdk = 35
        versionCode = 8
        versionName = "1.3.0"
    }

    // Release signing comes from Gradle properties kept outside the repo (~/.gradle/gradle.properties):
    // noirKeystore, noirKeystorePassword, noirKeyAlias, noirKeyPassword. Without them the release APK
    // is left unsigned, which is what F-Droid expects (it signs its own builds).
    val keystore = findProperty("noirKeystore") as String?
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = findProperty("noirKeystorePassword") as String?
                keyAlias = findProperty("noirKeyAlias") as String?
                keyPassword = findProperty("noirKeyPassword") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    // No Google "dependency metadata" block in the APK signature: it's encrypted for Google only,
    // and F-Droid rejects APKs that contain it
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
