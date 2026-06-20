package com.exemplo.fluidez.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.fluidez.data.AppInfo
import com.exemplo.fluidez.data.AppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppFilter { TODOS, USUARIO, SISTEMA }

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val filter: AppFilter = AppFilter.USUARIO,
    val query: String = "",
    val hasUsageAccess: Boolean = false,
    val loading: Boolean = true
) {
    val visibleApps: List<AppInfo>
        get() = apps
            .filter {
                when (filter) {
                    AppFilter.TODOS -> true
                    AppFilter.USUARIO -> !it.isSystemApp
                    AppFilter.SISTEMA -> it.isSystemApp
                }
            }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
}

class AppsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppsRepository(app)
    private val _state = MutableStateFlow(AppsUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { repo.loadApps() }
            _state.update {
                it.copy(apps = apps, hasUsageAccess = repo.hasUsageAccess(), loading = false)
            }
        }
    }

    fun setFilter(filter: AppFilter) = _state.update { it.copy(filter = filter) }
    fun setQuery(text: String) = _state.update { it.copy(query = text) }
}
