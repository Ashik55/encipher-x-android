///*
// * Copyright 2025 New Vector Ltd.
// *
// * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
// * Please see LICENSE files in the repository root for full details.
// */
//
//package io.element.android.features.call.impl.client
//
//import kotlinx.serialization.json.Json
//import retrofit2.Retrofit
//
//
//
//object RetrofitClient {
//    private const val BASE_URL = "https://dev.enciph-er.com/_matrix/client/v3/"
//    private val json = Json { ignoreUnknownKeys = true }
//
//    val apiService: CallApiService by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .addConverterFactory(GsonC)
//            .build()
//            .create(CallApiService::class.java)
//    }
//}
