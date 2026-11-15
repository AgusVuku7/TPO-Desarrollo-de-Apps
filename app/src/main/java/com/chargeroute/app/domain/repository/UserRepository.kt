package com.chargeroute.app.domain.repository

import com.chargeroute.app.domain.model.ThemeMode
import com.chargeroute.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val session: Flow<UserSession>
    val themeMode: Flow<ThemeMode>
    suspend fun login(name: String, email: String)
    suspend fun logout()
    suspend fun setThemeMode(mode: ThemeMode)
}
