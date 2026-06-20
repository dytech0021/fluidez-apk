package com.exemplo.fluidez.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.fluidez.data.AppStorage
import com.exemplo.fluidez.data.StorageOverview
import com.exemplo.fluidez.data.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageUiState(
    val overview: StorageOverview = StorageOverview(),
    val apps: List<AppStorage> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val loading: Boolean = true
)

class StorageViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StorageRepository(app)
    private val _state = MutableStateFlow(StorageUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val overview = withContext(Dispatchers.IO) { repo.overview() }
            val apps = withContext(Dispatchers.IO) { repo.appStorage() }
            _state.update {
                it.copy(
                    overview = overview,
                    apps = apps,
                    hasUsageAccess = repo.hasUsageAccess(),
                    loading = false
                )
            }
        }
    }
}
