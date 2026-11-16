package com.lextezy.tambiolo.ui.results

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lextezy.tambiolo.TambioloApplication
import com.lextezy.tambiolo.data.DigitDraw
import com.lextezy.tambiolo.data.Draw
import com.lextezy.tambiolo.data.manilaNow
import com.lextezy.tambiolo.data.recentKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultsUiState(
    val draws: List<Draw> = emptyList(),
    val digits: List<DigitDraw> = emptyList(),
    val updated: String = "",
    val loading: Boolean = false,
    val offline: Boolean = false,
) {
    fun latest(max: Int): Draw? = draws.filter { it.max == max }.maxByOrNull { it.date }
    fun history(max: Int): List<Draw> = draws.filter { it.max == max }.sortedByDescending { it.date }

    /** A digit game's draws, newest first (latest day first, and 9 PM before 2 PM within a day). */
    fun digitHistory(code: String): List<DigitDraw> =
        digits.filter { it.game == code }.sortedWith(compareByDescending<DigitDraw> { it.date }.thenByDescending { it.hour })
}

class ResultsViewModel(app: Application) : AndroidViewModel(app) {
    private val tambiolo = app as TambioloApplication
    private val repo = tambiolo.results
    private val _state = MutableStateFlow(ResultsUiState(loading = true))
    val state: StateFlow<ResultsUiState> = _state

    init {
        viewModelScope.launch {
            repo.cached()?.let { f -> _state.update { it.copy(draws = f.draws, updated = f.updated) } }
            repo.cachedDigits()?.let { f -> _state.update { it.copy(digits = f.draws) } }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            // Digit games live in their own file; if it is missing or fails, the lotto results still load.
            runCatching { repo.refreshDigits() }
                .onSuccess { f -> _state.update { it.copy(digits = f.draws) } }
            runCatching { repo.refresh() }
                .onSuccess { f ->
                    _state.update { it.copy(draws = f.draws, updated = f.updated, loading = false, offline = false) }
                    // The user is looking at these results now, so the background check must not announce them again.
                    val s = _state.value
                    runCatching { tambiolo.prefs.setSeenDraws(recentKeys(s.draws, s.digits, manilaNow().toLocalDate())) }
                }
                .onFailure { _state.update { it.copy(loading = false, offline = true) } }
        }
    }
}
