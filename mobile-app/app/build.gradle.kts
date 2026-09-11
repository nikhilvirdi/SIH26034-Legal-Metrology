plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.legalmetrology.inspector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.legalmetrology.inspector"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

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
        debug {
            // TODO: Set backend base URL for debug builds here
            // buildConfigField("String", "BASE_URL", "\"https://your-dev-backend.com/api/v1/\"")
            buildConfigField("String", "BASE_URL", "\"https://mock.legalmetrology.dev/api/v1/\"")
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // iText PDF library conflicts
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splash.screen)

    // Compose BOM (manages all compose versions together)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // Room DB (inspection history)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking (Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.kotlin.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // ARCore (physical scale calibration — the heart of the measurement system)
    implementation(libs.arcore)
    // SceneView: Compose-compatible AR rendering with Filament engine
    implementation(libs.sceneview)

    // ML Kit Barcode Scanning (on-device, pre-trained — no retraining needed)
    implementation(libs.mlkit.barcode.scanning)

    // Lottie animations (scan state machine: Searching → Locked → Captured)
    implementation(libs.lottie.compose)

    // Image loading
    implementation(libs.coil.compose)

    // PDF report generation
    implementation(libs.itext.core)

    // Accompanist (runtime permissions in Compose)
    implementation(libs.accompanist.permissions)

    // DataStore (user preferences / session)
    implementation(libs.datastore.preferences)

    // WorkManager (offline inspection sync queue)
    implementation(libs.work.runtime.ktx)

    // Debug tools
    debugImplementation(libs.androidx.ui.tooling)
}
