package com.byan.securekitwp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.byan.securekitwp.data.repository.NetworkSecurityState
import com.byan.securekitwp.data.repository.SecurityRepository
import com.byan.securekitwp.data.repository.SecurityStatusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityUiState(
    val isLoading: Boolean = true,
    val securityStatus: SecurityStatusState? = null,
    val networkStatus: NetworkSecurityState? = null,
    val isAppSafe: Boolean = false
)

class SecurityViewModel(private val repository: SecurityRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        checkSecurity()
    }

    fun checkSecurity() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val envStatus = repository.checkSecurityEnvironment()
            val netStatus = repository.checkNetworkSecurity()

            val isSafe = !envStatus.isRooted && 
                         !envStatus.isHooked && 
                         !envStatus.isEmulator && 
                         !envStatus.isDebuggerAttached &&
                         !netStatus.isProxyEnabled &&
                         !netStatus.isVpnEnabled

            _uiState.update {
                it.copy(
                    isLoading = false,
                    securityStatus = envStatus,
                    networkStatus = netStatus,
                    isAppSafe = isSafe
                )
            }
        }
    }
}

class SecurityViewModelFactory(private val repository: SecurityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
