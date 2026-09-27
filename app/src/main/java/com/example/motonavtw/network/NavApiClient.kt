package com.motonavtw.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class NavApiClient(private val httpClient: OkHttpClient = OkHttpClient()) {

    suspend fun searchDestination(gatewayUrl: String, query: String, lat: Double, lng: Double): String? {
        return withContext(Dispatchers.IO) {
            // 依循指南規範，採用後端既有 HTTP 接口與 Schema
            val url = "$gatewayUrl/api/search?q=$query&lat=$lat&lng=$lng"
            val request = Request.Builder().url(url).build()
            try {
                val response: Response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            } catch (e: IOException) {
                null
            }
        }
    }
}