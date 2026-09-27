package com.motonavtw.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 建立 DataStore 實例
private val Context.dataStore by preferencesDataStore(name = "settings")

class GatewayManager(private val context: Context) {
    private val GATEWAY_URL_KEY = stringPreferencesKey("gateway_url")

    // 讀取網關位址
    val gatewayUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GATEWAY_URL_KEY] ?: ""
        }

    // 儲存網關位址
    suspend fun saveGatewayUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[GATEWAY_URL_KEY] = url
        }
    }
}