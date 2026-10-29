package com.lextezy.tambiolo.ui.results

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lextezy.tambiolo.TambioloApplication
import com.lextezy.tambiolo.data.Draw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultsUiState(
    val draws: List<Draw> = emptyList(),
    val updated: String = "",
    val loading: Boolean = false,
    val offline: Boolean = false,
) {
    fun latest(max: Int): Draw? = draws.filter { it.max == max }.maxByOrNull { it.date }
    fun history(max: Int): List<Draw> = draws.filter { it.max == max }.sortedByDescending { it.date }
}

class ResultsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TambioloApplication).results
    private val _state = MutableStateFlow(ResultsUiState(loading = true))
    val state: StateFlow<ResultsUiState> = _state

    init {
        viewModelScope.launch {
            repo.cached()?.let { f -> _state.update { it.copy(draws = f.draws, updated = f.updated) } }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repo.refresh() }
                .onSuccess { f -> _state.update { it.copy(draws = f.draws, updated = f.updated, loading = false, offline = false) } }
                .onFailure { _state.update { it.copy(loading = false, offline = true) } }
        }
    }
}
