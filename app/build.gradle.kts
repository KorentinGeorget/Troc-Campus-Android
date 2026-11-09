import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val pwaHost: String = localProps.getProperty("PWA_HOST") ?: "troc-campus.korentingeorget.com"
val syncBaseUrl: String = localProps.getProperty("SYNC_BASE_URL") ?: "https://troc-campus.korentingeorget.com/"

android {
    namespace = "com.korentingeorget.troccampus"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.korentingeorget.troccampus"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "PWA_HOST", "\"$pwaHost\"")
        buildConfigField("String", "SYNC_BASE_URL", "\"$syncBaseUrl\"")
        manifestPlaceholders["pwaHost"] = pwaHost
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose + Material 3
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Activity, Lifecycle, Navigation 3
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    // Core, splash
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    // Room, WorkManager, DataStore
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.datastore.preferences)

    // Réseau (Retrofit, OkHttp, Serialization)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.serialization.json)

    // Firebase Cloud Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // CameraX et localisation
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.compose)
    implementation(libs.play.services.location)

    // ML Kit On-Device (Text Recognition & Image Labeling)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.image.labeling)

    testImplementation(libs.junit)
}
