package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SessionManager
import com.example.data.SupabaseClient
import com.example.data.UserSession
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data object Success : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = SupabaseClient.auth

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserSession?>(SessionManager.currentSession.value)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val user = auth.currentUserOrNull()
                if (user != null) {
                    loadProfileAndSetSession(user.id, user.email)
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Login succeeded but no user session")
                }
            } catch (e: AuthRestException) {
                _uiState.value = AuthUiState.Error(e.message ?: "Authentication failed")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try { auth.signOut() } catch (_: Exception) {}
            SessionManager.clearSession()
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
        }
    }

    fun restoreSession() {
        viewModelScope.launch {
            try {
                val user = auth.currentUserOrNull()
                if (user != null) {
                    loadProfileAndSetSession(user.id, user.email)
                    _uiState.value = AuthUiState.Success
                }
            } catch (_: Exception) {
                SessionManager.clearSession()
                _currentUser.value = null
            }
        }
    }

    private suspend fun loadProfileAndSetSession(userId: String, email: String?) {
        try {
            val profiles = SupabaseClient.postgrest
                .from("profiles")
                .select { filter("id", FilterOperator.EQ, userId) }
                .decodeList<ProfileRow>()

            val profile = profiles.firstOrNull()
            SessionManager.setSession(
                userId = userId,
                email = email,
                profileRole = profile?.role,
                profileTenantId = profile?.tenantId,
                profileFullName = profile?.fullName
            )
        } catch (_: Exception) {
            SessionManager.setSession(
                userId = userId,
                email = email,
                profileRole = null,
                profileTenantId = null,
                profileFullName = null
            )
        }
        _currentUser.value = SessionManager.currentSession.value
    }

    @Serializable
    data class ProfileRow(
        val id: String = "",
        @SerialName("tenant_id") val tenantId: String? = null,
        @SerialName("full_name") val fullName: String? = null,
        val role: String? = null
    )
}