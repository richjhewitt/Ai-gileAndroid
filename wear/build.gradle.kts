plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    // Same package as the phone app: the Data Layer pairs the two by package name + signing key (AII-412)
    namespace = "com.smizflark.aigile.messenger.wear"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.smizflark.aigile.messenger"
        minSdk = 30
        targetSdk = 34
        versionCode = providers.exec { commandLine("git", "rev-list", "--count", "HEAD"); isIgnoreExitValue = true }
            .standardOutput.asText.get().trim().toIntOrNull() ?: 1
        versionName = "0.1.0"
    }

    // Same key as :app (keystore/messenger.jks, else the debug key), or the Data Layer will not pair them
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
        debug { if (keyFile.exists()) signingConfig = signingConfigs.getByName("own") }
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
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.wear:wear-input:1.1.0")
    implementation("androidx.wear.tiles:tiles:1.4.1")
    implementation("androidx.wear.protolayout:protolayout:1.2.1")
    implementation("com.google.guava:guava:33.0.0-android")
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
