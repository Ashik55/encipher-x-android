/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

@file:Suppress("UnstableApiUsage")

import extension.setupAnvil

plugins {
    id("io.element.android-library")
}

//<?xml version="1.0" encoding="utf-8"?>
//<resources>
//<string name="gcm_defaultSenderId" translatable="false">818394930758</string>
//<string name="google_api_key" translatable="false">AIzaSyDsF38k3jFWQ-q2vrLvgHb72kpr8k7VW1I</string>
//<string name="google_app_id" translatable="false">1:818394930758:android:9368c25c3bad4866ccb447</string>
//<string name="google_crash_reporting_api_key" translatable="false">AIzaSyDsF38k3jFWQ-q2vrLvgHb72kpr8k7VW1I</string>
//<string name="google_storage_bucket" translatable="false">encipherx-c4032.firebasestorage.app</string>
//<string name="project_id" translatable="false">encipherx-c4032</string>
//</resources>



android {
    namespace = "io.element.android.libraries.pushproviders.firebase"

    buildTypes {
        getByName("release") {
            consumerProguardFiles("consumer-proguard-rules.pro")
            resValue(
                type = "string",
                name = "google_app_id",
                value = if (isEnterpriseBuild) {
                    "1:818394930758:android:9368c25c3bad4866ccb447"
                } else {
                    "1:818394930758:android:9368c25c3bad4866ccb447"
                }
            )
        }
        getByName("debug") {
            resValue(
                type = "string",
                name = "google_app_id",
                value = if (isEnterpriseBuild) {
                    "1:818394930758:android:9368c25c3bad4866ccb447"
                } else {
                    "1:818394930758:android:9368c25c3bad4866ccb447"
                }
            )
        }
        register("nightly") {
            consumerProguardFiles("consumer-proguard-rules.pro")
            matchingFallbacks += listOf("release")
            resValue(
                type = "string",
                name = "google_app_id",
                value = if (isEnterpriseBuild) {
                    "1:818394930758:android:9368c25c3bad4866ccb447"
                } else {
                    "1:818394930758:android:9368c25c3bad4866ccb447"
                }
            )
        }
    }
}

setupAnvil()

dependencies {
    implementation(libs.dagger)
    implementation(libs.androidx.corektx)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.core)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.uiStrings)
    implementation(projects.libraries.troubleshoot.api)
    implementation(projects.services.toolbox.api)

    implementation(projects.libraries.pushstore.api)
    implementation(projects.libraries.pushproviders.api)

    api(platform(libs.google.firebase.bom))
    api("com.google.firebase:firebase-messaging-ktx") {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "com.google.firebase", module = "firebase-analytics")
        exclude(group = "com.google.firebase", module = "firebase-measurement-connector")
    }

    testImplementation(libs.coroutines.test)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.truth)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.robolectric)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.push.test)
    testImplementation(projects.libraries.pushstore.test)
    testImplementation(projects.libraries.sessionStorage.implMemory)
    testImplementation(projects.libraries.sessionStorage.test)
    testImplementation(projects.tests.testutils)
    testImplementation(projects.services.toolbox.test)
}
