import com.datadog.gradle.config.AndroidConfig
import com.datadog.gradle.config.configureFlavorForBenchmark
import com.datadog.gradle.config.dependencyUpdateConfig
import com.datadog.gradle.config.java17
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

@Suppress("StringLiteralDuplication")
android {
    namespace = "com.datadog.sample.benchmark"
    compileSdk = AndroidConfig.TARGET_SDK
    buildToolsVersion = AndroidConfig.BUILD_TOOLS_VERSION

    defaultConfig {
        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = AndroidConfig.VERSION.code
        versionName = AndroidConfig.VERSION.name
        multiDexEnabled = true

        buildFeatures {
            buildConfig = true
        }
        vectorDrawables.useSupportLibrary = true
        configureFlavorForBenchmark(project.rootDir)
    }
    compileOptions {
        java17()
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidXComposeRuntime.get()
    }
    val bmPassword = System.getenv("BM_STORE_PASSWD")
    signingConfigs {
        if (bmPassword != null) {
            create("release") {
                storeFile = File(project.rootDir, "sample-benchmark.keystore")
                storePassword = bmPassword
                keyAlias = "dd-sdk-android"
                keyPassword = bmPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }

        getByName("release") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isMinifyEnabled = true
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            } ?: kotlin.run {
                signingConfig = signingConfigs.findByName("debug")
            }
        }
    }
}

dependencies {

    implementation(libs.kotlin)

    // Android dependencies
    implementation(libs.androidXMultidex)
    implementation(libs.bundles.androidXNavigation)
    implementation(libs.androidXAppCompat)
    implementation(libs.androidXConstraintLayout)
    implementation(libs.googleMaterial)
    implementation(libs.glideCore)
    implementation(libs.timber)
    implementation(platform(libs.androidXComposeBom))
    implementation(libs.bundles.androidXCompose)
    implementation(libs.coilCompose)
    implementation(project(":features:dd-sdk-android-logs"))
    implementation(project(":features:dd-sdk-android-rum"))
    implementation(project(":features:dd-sdk-android-trace"))
    implementation(project(":features:dd-sdk-android-trace-otel"))
    implementation(project(":features:dd-sdk-android-ndk"))
    implementation(project(":features:dd-sdk-android-webview"))
    implementation(project(":features:dd-sdk-android-session-replay"))
    implementation(project(":features:dd-sdk-android-session-replay-material"))
    implementation(project(":features:dd-sdk-android-session-replay-compose"))
    implementation(project(":integrations:dd-sdk-android-compose"))
    implementation(project(":tools:benchmark"))

    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.systemStubsJupiter)
}

kotlinConfig()
junitConfig()
dependencyUpdateConfig()
