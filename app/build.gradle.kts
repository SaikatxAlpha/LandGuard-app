import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

/**
 * Release signing material is resolved without ever appearing in the build script:
 *
 *   1. `keystore.properties` at the repository root (git-ignored), or
 *   2. the LANDGUARD_STORE_FILE / _STORE_PASSWORD / _KEY_ALIAS / _KEY_PASSWORD
 *      environment variables, for CI.
 *
 * If neither is present the release build falls back to the debug key so that a
 * fresh clone still compiles. Such an APK is not distributable — see
 * `keystore.properties.example`.
 */
val signingProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    (signingProps.getProperty(key) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "LANDGUARD_STORE_FILE")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }
val releaseStorePassword = signingValue("storePassword", "LANDGUARD_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "LANDGUARD_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "LANDGUARD_KEY_PASSWORD")

val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "com.example.landguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.landguard"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"

        // Production LandGuard backend — the same API the authority control center uses.
        // Every build type uses it; there is no local / LAN / VPN backend and no cleartext HTTP.
        buildConfigField("String", "LANDGUARD_API_BASE_URL", "\"https://api.landguard.online/\"")
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "LandGuard: no release signing material found — falling back to the debug key. " +
                        "This APK is NOT distributable. See keystore.properties.example."
                )
                signingConfigs.getByName("debug")
            }
        }
    }

    // landguard-1.2-release.apk instead of app-release.apk
    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)?.outputFileName =
                "landguard-${variant.versionName}-${variant.buildType.name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)

    // MapLibre Native Android (Replaces Google Maps)
    implementation("org.maplibre.gl:android-sdk:11.5.1")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
