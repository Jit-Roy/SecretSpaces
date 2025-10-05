package com.secretspaces32.android.data.repository

// DEPRECATED: This file is replaced by FirebaseUserRepository.kt
// Please use FirebaseUserRepository for Firebase integration
// This file can be safely deleted

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Deprecated("Use FirebaseUserRepository instead")
class UserRepository(private val context: Context) {

    private val USER_ID_KEY = stringPreferencesKey("user_id")

    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY] ?: ""
    }

    suspend fun initializeUserId(): String {
        var id = ""
        context.dataStore.edit { preferences ->
            id = preferences[USER_ID_KEY] ?: run {
                val newId = UUID.randomUUID().toString()
                preferences[USER_ID_KEY] = newId
                newId
            }
        }
        return id
    }

    suspend fun getUserId(): String {
        var id = ""
        context.dataStore.data.collect { preferences ->
            id = preferences[USER_ID_KEY] ?: ""
        }
        return id
    }
}
