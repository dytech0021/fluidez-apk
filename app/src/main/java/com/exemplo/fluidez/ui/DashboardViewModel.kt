package com.exemplo.fluidez.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.fluidez.data.SystemInfoRepository
import com.exemplo.fluidez.data.SystemSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SystemInfoRepository(app)

    private val _snapshot = MutableStateFlow(SystemSnapshot())
    val snapshot = _snapshot.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { repository.readSnapshot() }
            _snapshot.value = data
        }
    }
}
