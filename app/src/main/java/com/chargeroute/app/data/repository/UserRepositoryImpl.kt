package com.chargeroute.app.data.repository

import com.chargeroute.app.data.local.datastore.UserPreferencesDataStore
import com.chargeroute.app.domain.model.ThemeMode
import com.chargeroute.app.domain.model.UserSession
import com.chargeroute.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

// Los ViewModels hablan con este repositorio y no con DataStore: donde se guarda se puede cambiar aca
class UserRepositoryImpl(
    private val preferencesDataStore: UserPreferencesDataStore
) : UserRepository {

    override val session: Flow<UserSession> = preferencesDataStore.session
    override val themeMode: Flow<ThemeMode> = preferencesDataStore.themeMode

    override suspend fun login(name: String, email: String) {
        preferencesDataStore.login(name, email)
    }

    override suspend fun logout() {
        preferencesDataStore.logout()
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferencesDataStore.setThemeMode(mode)
    }
}
