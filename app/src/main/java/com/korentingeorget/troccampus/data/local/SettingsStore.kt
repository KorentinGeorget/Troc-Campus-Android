package com.korentingeorget.troccampus.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "troc_settings")

class SettingsStore(private val context: Context) {
    private val keyFcmToken = stringPreferencesKey("fcm_token")
    private val keySeeded = booleanPreferencesKey("seeded_v1")
    private val keyAskedNotif = booleanPreferencesKey("asked_notif")

    val fcmToken: Flow<String?> = context.dataStore.data.map { it[keyFcmToken] }
    val seeded: Flow<Boolean> = context.dataStore.data.map { it[keySeeded] ?: false }
    val askedNotif: Flow<Boolean> = context.dataStore.data.map { it[keyAskedNotif] ?: false }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { it[keyFcmToken] = token }
    }

    suspend fun markSeeded() {
        context.dataStore.edit { it[keySeeded] = true }
    }

    suspend fun markAskedNotif() {
        context.dataStore.edit { it[keyAskedNotif] = true }
    }
}
