import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The signing key is stored as Base64 text (so it can live in a web-edited repo); decode once.
val flexKeystore = file("flex.keystore")
if (!flexKeystore.exists()) {
    val b64 = file("flex.keystore.b64")
    if (b64.exists()) flexKeystore.writeBytes(Base64.getMimeDecoder().decode(b64.readText()))
}

android {
    namespace = "sk.flex.console"
    compileSdk = 34

    defaultConfig {
        applicationId = "sk.flex.console"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Fixed key so every new build installs over the previous one (updates keep data).
    signingConfigs {
        create("flex") {
            storeFile = flexKeystore
            storePassword = "flexflex"
            keyAlias = "flex"
            keyPassword = "flexflex"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("flex")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("flex")
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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
