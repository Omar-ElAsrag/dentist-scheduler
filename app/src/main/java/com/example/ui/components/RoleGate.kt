package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.SessionManager

@Composable
fun RoleGate(
    allowedRoles: Set<String>,
    fallbackContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val role by SessionManager.role.collectAsState()
    if (role != null && role in allowedRoles) {
        content()
    } else {
        fallbackContent()
    }
}
