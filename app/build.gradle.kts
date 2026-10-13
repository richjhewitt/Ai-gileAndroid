plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.smizflark.aigile.messenger"
    compileSdk = 35
    // Pinned to the installed 35: parallel builds auto-installing the default (34) kept corrupting it (AII-9)
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.smizflark.aigile.messenger"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // Sideloaded builds are signed with our own key, kept out of git (keystore/). Falls back to the debug key.
    val keyFile = rootProject.file("keystore/messenger.jks")
    signingConfigs {
        if (keyFile.exists()) create("own") {
            storeFile = keyFile
            storePassword = "aigile-messenger"
            keyAlias = "messenger"
            keyPassword = "aigile-messenger"
        }
    }

    buildTypes {
        debug {
            if (keyFile.exists()) signingConfig = signingConfigs.getByName("own")
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (keyFile.exists()) signingConfigs.getByName("own") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
