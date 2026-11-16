package com.lextezy.tambiolo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        // An edited combo stays where it was; a new one goes on top.
        p[combosKey] = json.encodeToString(
            if (list.any { it.id == combo.id }) list.map { if (it.id == combo.id) combo else it } else listOf(combo) + list
        )
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
    private val drawsDoneKey = androidx.datastore.preferences.core.intPreferencesKey("draws_done")
    private val reviewAskedKey = booleanPreferencesKey("review_asked")

    /** Chosen language; until the user picks one, follows the phone (Filipino → Tagalog). */
    val language: Flow<Language> = context.dataStore.data.map { p ->
        p[languageKey]?.let { runCatching { Language.valueOf(it) }.getOrNull() } ?: Language.fromPhone()
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[onboardedKey] ?: false }
    val simTipSeen: Flow<Boolean> = context.dataStore.data.map { it[simTipKey] ?: false }

    suspend fun setLanguage(lang: Language) = context.dataStore.edit { it[languageKey] = lang.name }
    suspend fun setOnboarded() = context.dataStore.edit { it[onboardedKey] = true }
    suspend fun setSimTipSeen() = context.dataStore.edit { it[simTipKey] = true }

    // ---- Result alerts ----
    private val alertsEnabledKey = booleanPreferencesKey("alerts_enabled")
    private val alertGamesKey = stringSetPreferencesKey("alert_games")
    private val alertPromptDoneKey = booleanPreferencesKey("alert_prompt_done")
    private val seenDrawsKey = stringSetPreferencesKey("seen_draws")

    val alertsEnabled: Flow<Boolean> = context.dataStore.data.map { it[alertsEnabledKey] ?: false }
    val alertGames: Flow<Set<String>> = context.dataStore.data.map { it[alertGamesKey] ?: DEFAULT_ALERT_GAMES }
    /** True once the user answered the "turn on alerts?" card, either way. */
    val alertPromptDone: Flow<Boolean> = context.dataStore.data.map { it[alertPromptDoneKey] ?: false }
    /** Draws this phone already knows about; null until the first results are loaded. */
    val seenDraws: Flow<Set<String>?> = context.dataStore.data.map { it[seenDrawsKey] }

    suspend fun setAlertsEnabled(on: Boolean) = context.dataStore.edit {
        it[alertsEnabledKey] = on
        it[alertPromptDoneKey] = true
    }
    suspend fun setAlertGames(games: Set<String>) = context.dataStore.edit { it[alertGamesKey] = games }
    suspend fun setAlertPromptDone() = context.dataStore.edit { it[alertPromptDoneKey] = true }
    suspend fun setSeenDraws(keys: Set<String>) = context.dataStore.edit { it[seenDrawsKey] = keys }

    /** Counts finished draws. Returns true exactly once, when it's time to ask for a review. */
    suspend fun recordDrawAndCheckReview(askAfter: Int): Boolean {
        var ask = false
        context.dataStore.edit { p ->
            val done = (p[drawsDoneKey] ?: 0) + 1
            p[drawsDoneKey] = done
            if (done >= askAfter && p[reviewAskedKey] != true) {
                p[reviewAskedKey] = true
                ask = true
            }
        }
        return ask
    }
}

