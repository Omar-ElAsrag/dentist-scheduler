package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val userId: String,
    val email: String,
    val role: String,
    val tenantId: String?,
    val fullName: String?
)

object SessionManager {
    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    private val _role = MutableStateFlow<String?>(null)
    val role: StateFlow<String?> = _role.asStateFlow()

    private val _tenantId = MutableStateFlow<String?>(null)
    val tenantId: StateFlow<String?> = _tenantId.asStateFlow()

    fun setSession(userId: String, email: String?, profileRole: String?, profileTenantId: String?, profileFullName: String?) {
        val session = UserSession(
            userId = userId,
            email = email ?: "",
            role = profileRole ?: "dentist",
            tenantId = profileTenantId,
            fullName = profileFullName
        )
        _currentSession.value = session
        _role.value = session.role
        _tenantId.value = session.tenantId
    }

    fun clearSession() {
        _currentSession.value = null
        _role.value = null
        _tenantId.value = null
    }
}