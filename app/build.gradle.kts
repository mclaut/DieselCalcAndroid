import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.google.services)
}

// firebase.properties — appId + tester groups для Firebase App Distribution.
// Файл у gitignore (шаблон у firebase.properties.sample). Якщо відсутній,
// `appDistributionUploadRelease` task падає, але assembleRelease сам по собі
// продовжує працювати.
val firebasePropertiesFile = rootProject.file("firebase.properties")
val firebaseProperties = Properties().apply {
    if (firebasePropertiesFile.exists()) {
        firebasePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.mclaut.dieselcalc"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mclaut.dieselcalc"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firebase App Distribution config — auth береться з Firebase CLI
            // (`firebase login`), serviceCredentialsFile не використовуємо.
            // Release notes — build/last-commit.txt, який scripts/distribute-android.sh
            // генерує з git log перед запуском Gradle.
            firebaseAppDistribution {
                appId = firebaseProperties.getProperty("appId", "")
                groups = firebaseProperties.getProperty("groups", "testers")
                artifactType = "APK"
                releaseNotesFile = "${rootProject.projectDir}/build/last-commit.txt"
            }
        }

        debug {
            // Дозволяє швидко закидати debug-збірку тестерам без підпису keystore'м.
            firebaseAppDistribution {
                appId = firebaseProperties.getProperty("appId", "")
                groups = firebaseProperties.getProperty("groups", "testers")
                artifactType = "APK"
                releaseNotesFile = "${rootProject.projectDir}/build/last-commit.txt"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.material:material-icons-extended")
    // Widget
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
    // WorkManager for periodic widget updates
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
