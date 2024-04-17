/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

import com.datadog.gradle.Dependencies
import com.datadog.gradle.config.AndroidConfig
import com.datadog.gradle.config.java17
import com.datadog.gradle.config.kotlinConfig

plugins {
    id("com.android.application")
    kotlin("android")
}

val nightlyTestsTokenKey = "NIGHTLY_TESTS_TOKEN"
val nightlyTestsRumAppIdKey = "NIGHTLY_TESTS_RUM_APP_ID"

android {

    compileSdk = AndroidConfig.TARGET_SDK
    buildToolsVersion = AndroidConfig.BUILD_TOOLS_VERSION

    defaultConfig {
        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = AndroidConfig.VERSION.code
        versionName = AndroidConfig.VERSION.name

        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildFeatures {
            buildConfig = true
        }

        buildConfigField(
            "String",
            nightlyTestsTokenKey,
            "\"${project.findProperty(nightlyTestsTokenKey)}\""
        )
        buildConfigField(
            "String",
            nightlyTestsRumAppIdKey,
            "\"${project.findProperty(nightlyTestsRumAppIdKey)}\""
        )
        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++14")
            }
        }
    }

    namespace = "com.datadog.android.nightly"

    sourceSets.named("main") {
        java.srcDir("src/main/kotlin")
    }
    sourceSets.named("androidTest") {
        java.srcDir("src/androidTest/kotlin")
    }

    compileOptions {
        java17()
    }

    packaging {
        resources {
            excludes += "META-INF/*"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            testProguardFile("test-proguard-rules.pro")
        }
        getByName("debug") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            testProguardFile("test-proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path = File("$projectDir/src/main/cpp/CMakeLists.txt")
            version = Dependencies.Versions.CMake
        }
    }
    ndkVersion = Dependencies.Versions.Ndk
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":features:dd-sdk-android-ndk"))
    implementation(project(":features:dd-sdk-android-logs"))
    implementation(project(":features:dd-sdk-android-trace"))
    implementation(project(":features:dd-sdk-android-webview"))
    implementation(project(":features:dd-sdk-android-rum"))
    implementation(project(":integrations:dd-sdk-android-okhttp"))

    implementation(libs.bundles.androidXNavigation)
    implementation(libs.gson)
    implementation(libs.kotlin)
    implementation(libs.bundles.androidXSupportBase)
    implementation(libs.androidXMultidex)
    implementation(libs.elmyr)
    implementation(libs.okHttp)

    // Ktor (local server)
    implementation(libs.bundles.ktor)

    androidTestImplementation(project(":tools:unit")) {
        attributes {
            attribute(
                com.android.build.api.attributes.ProductFlavorAttr.of("platform"),
                objects.named("art")
            )
        }
    }
    androidTestImplementation(libs.bundles.integrationTests)
}

kotlinConfig()
