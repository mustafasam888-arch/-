package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bandPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "band7_prefs")

data class BandUserPrefs(
    val pairedMacAddress: String = "",
    val pairedDeviceName: String = "",
    val authKeyHex: String = "",
    val stepGoal: Int = 8000,
    val darkTheme: Boolean = true
)

class BandPreferencesRepository(private val context: Context) {

    private object Keys {
        val PAIRED_MAC = stringPreferencesKey("paired_mac")
        val PAIRED_NAME = stringPreferencesKey("paired_name")
        val AUTH_KEY_HEX = stringPreferencesKey("auth_key_hex")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val prefsFlow: Flow<BandUserPrefs> = context.bandPrefsDataStore.data.map { prefs ->
        BandUserPrefs(
            pairedMacAddress = prefs[Keys.PAIRED_MAC] ?: "",
            pairedDeviceName = prefs[Keys.PAIRED_NAME] ?: "",
            authKeyHex = prefs[Keys.AUTH_KEY_HEX] ?: "",
            stepGoal = prefs[Keys.STEP_GOAL] ?: 8000,
            darkTheme = prefs[Keys.DARK_THEME] ?: true
        )
    }

    suspend fun savePairedDevice(macAddress: String, name: String) {
        context.bandPrefsDataStore.edit { prefs ->
            prefs[Keys.PAIRED_MAC] = macAddress
            prefs[Keys.PAIRED_NAME] = name
        }
    }

    suspend fun saveAuthKeyHex(hex: String) {
        context.bandPrefsDataStore.edit { prefs ->
            prefs[Keys.AUTH_KEY_HEX] = hex.trim().lowercase()
        }
    }

    suspend fun saveStepGoal(goal: Int) {
        context.bandPrefsDataStore.edit { prefs ->
            prefs[Keys.STEP_GOAL] = goal.coerceIn(1000, 50000)
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.bandPrefsDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }
}
