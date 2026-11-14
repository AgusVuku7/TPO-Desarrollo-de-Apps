package com.chargeroute.app

import com.chargeroute.app.domain.model.ThemeMode
import com.chargeroute.app.domain.model.UserSession
import com.chargeroute.app.domain.repository.UserRepository
import com.chargeroute.app.presentation.SessionViewModel
import com.chargeroute.app.presentation.screens.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Repositorio de usuario falso: guarda todo en memoria y recuerda lo que se le pidio guardar.
 */
private class FakeUserRepository : UserRepository {
    private val sessionFlow = MutableStateFlow(UserSession())
    private val themeFlow = MutableStateFlow(ThemeMode.LIGHT)

    override val session = sessionFlow
    override val themeMode = themeFlow

    // Cuantas veces se llamo a login (para comprobar que no se guardo nada)
    var loginCalls = 0
        private set

    override suspend fun login(name: String, email: String) {
        loginCalls++
        sessionFlow.update { UserSession(isLoggedIn = true, userName = name, userEmail = email) }
    }

    override suspend fun logout() {
        sessionFlow.update { UserSession() }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeFlow.update { mode }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeUserRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeUserRepository()
        viewModel = LoginViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login valido guarda los datos sin espacios y avisa que termino`() = runTest(testDispatcher) {
        var avisos = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.loginDone.collect { avisos++ } }
        advanceUntilIdle()

        viewModel.onNameChange("  Ana Perez  ")
        viewModel.onEmailChange(" ana@mail.com ")
        viewModel.login()
        advanceUntilIdle()

        val session = fakeRepository.session.value
        assertTrue(session.isLoggedIn)
        assertEquals("Ana Perez", session.userName)
        assertEquals("ana@mail.com", session.userEmail)
        assertEquals(1, avisos)
    }

    @Test
    fun `nombre vacio muestra error y no guarda nada`() = runTest(testDispatcher) {
        viewModel.onNameChange("   ")
        viewModel.onEmailChange("ana@mail.com")
        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.nameError)
        assertFalse(viewModel.uiState.value.emailError)
        assertEquals(0, fakeRepository.loginCalls)
    }

    @Test
    fun `mail invalido muestra error y no guarda nada`() = runTest(testDispatcher) {
        viewModel.onNameChange("Ana")
        viewModel.onEmailChange("ana@mail")
        viewModel.login()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.nameError)
        assertTrue(viewModel.uiState.value.emailError)
        assertEquals(0, fakeRepository.loginCalls)
    }

    @Test
    fun `escribir de nuevo en un campo limpia su error`() = runTest(testDispatcher) {
        viewModel.login()
        assertTrue(viewModel.uiState.value.nameError)
        assertTrue(viewModel.uiState.value.emailError)

        viewModel.onNameChange("A")
        assertFalse(viewModel.uiState.value.nameError)
        assertTrue(viewModel.uiState.value.emailError)

        viewModel.onEmailChange("a")
        assertFalse(viewModel.uiState.value.emailError)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logout deja el repositorio sin sesion`() = runTest(testDispatcher) {
        val fakeRepository = FakeUserRepository()
        fakeRepository.login("Ana", "ana@mail.com")
        val viewModel = SessionViewModel(fakeRepository)

        viewModel.logout()
        advanceUntilIdle()

        assertFalse(fakeRepository.session.value.isLoggedIn)
        assertEquals("", fakeRepository.session.value.userName)
    }
}
