package com.lextezy.tambiolo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "tambiolo")

/** Small settings + saved combos, stored on the phone. */
class AppPrefs(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val combosKey = stringPreferencesKey("combos")
    private val themeKey = stringPreferencesKey("theme")
    private val adsRemovedKey = booleanPreferencesKey("ads_removed")

    val combos: Flow<List<Combo>> = context.dataStore.data.map { p ->
        p[combosKey]?.let { runCatching { json.decodeFromString<List<Combo>>(it) }.getOrNull() } ?: emptyList()
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { p ->
        p[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val adsRemoved: Flow<Boolean> = context.dataStore.data.map { it[adsRemovedKey] ?: false }

    suspend fun saveCombo(combo: Combo) = context.dataStore.edit { p ->
        val list = p[combosKey]?.let { runCatching { json.decodeFromString<List<Combo>>(it) }.getOrNull() } ?: emptyList()
        p[combosKey] = json.encodeToString(listOf(combo) + list.filter { it.id != combo.id })
    }

    suspend fun deleteCombo(id: Long) = context.dataStore.edit { p ->
        val list = p[combosKey]?.let { runCatching { json.decodeFromString<List<Combo>>(it) }.getOrNull() } ?: emptyList()
        p[combosKey] = json.encodeToString(list.filter { it.id != id })
    }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[themeKey] = mode.name }

    suspend fun setAdsRemoved(removed: Boolean) = context.dataStore.edit { it[adsRemovedKey] = removed }

    private val languageKey = stringPreferencesKey("language")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val simTipKey = booleanPreferencesKey("sim_tip_seen")

    /** Chosen language; until the user picks one, follows the phone (Filipino → Tagalog). */
    val language: Flow<Language> = context.dataStore.data.map { p ->
        p[languageKey]?.let { runCatching { Language.valueOf(it) }.getOrNull() } ?: Language.fromPhone()
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[onboardedKey] ?: false }
    val simTipSeen: Flow<Boolean> = context.dataStore.data.map { it[simTipKey] ?: false }

    suspend fun setLanguage(lang: Language) = context.dataStore.edit { it[languageKey] = lang.name }
    suspend fun setOnboarded() = context.dataStore.edit { it[onboardedKey] = true }
    suspend fun setSimTipSeen() = context.dataStore.edit { it[simTipKey] = true }
}

enum class Language {
    EN, TL;

    companion object {
        fun fromPhone(): Language =
            if (java.util.Locale.getDefault().language in setOf("fil", "tl")) TL else EN
    }
}
