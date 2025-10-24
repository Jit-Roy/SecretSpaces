plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.secretspaces32.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.secretspaces32.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read MapTiler API key from local.properties
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }
        val maptilerApiKey = properties.getProperty("MAPTILER_API_KEY") ?: ""
        buildConfigField("String", "MAPTILER_API_KEY", "\"$maptilerApiKey\"")

        // Read Cloudinary credentials from local.properties
        val cloudinaryCloudName = properties.getProperty("CLOUDINARY_CLOUD_NAME") ?: ""
        val cloudinaryApiKey = properties.getProperty("CLOUDINARY_API_KEY") ?: ""
        val cloudinaryApiSecret = properties.getProperty("CLOUDINARY_API_SECRET") ?: ""
        val cloudinaryUnsignedPreset = properties.getProperty("CLOUDINARY_UNSIGNED_PRESET") ?: ""
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"$cloudinaryApiKey\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"$cloudinaryApiSecret\"")
        buildConfigField("String", "CLOUDINARY_UNSIGNED_PRESET", "\"$cloudinaryUnsignedPreset\"")

        // Add native library options for 16 KB page size support
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
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
        }
        debug {
            // Keep debug simple for faster iterations
            isMinifyEnabled = false
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
            // Keep debugSymbols for better crash reporting
            keepDebugSymbols += "**/*.so"
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)

    // Firebase using BoM for consistent versions
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Cloudinary for image storage
    implementation("com.cloudinary:cloudinary-android:3.1.2")
    implementation("com.cloudinary:cloudinary-core:2.3.2")

    // Google Sign-In with Credential Manager (modern approach)
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Material Icons for Compose
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)


    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Coil
    implementation(libs.coil.compose)

    // uCrop for image cropping
    implementation("com.github.yalantis:ucrop:2.2.8")

    // MapLibre for maps (using MapTiler)
    implementation("org.maplibre.gl:android-sdk:11.5.2")
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.2")
    implementation(libs.play.services.location)

    // Coroutines for Play Services (needed for .await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Permissions
    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}