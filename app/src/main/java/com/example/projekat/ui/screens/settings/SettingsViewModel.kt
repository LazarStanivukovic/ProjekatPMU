package com.example.projekat.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.repository.AuthRepository
import com.example.projekat.data.sync.SyncManager
import com.example.projekat.data.sync.SyncState
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val syncState: StateFlow<SyncState> = syncManager.syncState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncState.Idle)

    val lastSyncTime: StateFlow<Long?> = syncManager.lastSyncTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    val isAuthenticated: Boolean
        get() = authRepository.isAuthenticated

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncAll()
        }
    }

    fun showLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun dismissLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

    fun logout() {
        authRepository.logout()
        dismissLogoutDialog()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun formatLastSyncTime(timestamp: Long?): String {
        if (timestamp == null) return "Nikada"
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
