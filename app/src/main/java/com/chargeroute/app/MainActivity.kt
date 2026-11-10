package com.chargeroute.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chargeroute.app.core.sensor.LightThemeHysteresis
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.core.theme.ThemeMode
import com.chargeroute.app.presentation.ThemeViewModel
import com.chargeroute.app.presentation.navigation.AppNavHost
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as ChargeRouteApp).container }

    private val themeViewModel: ThemeViewModel by viewModels {
        viewModelFactory { initializer { ThemeViewModel(container.userPreferences) } }
    }

    private var isSensorDarkTheme by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Escucha el sensor de luz para cambiar solo entre modo claro y oscuro (RNF04, RNF05)
        // Solo escucha el sensor mientras la pantalla esta visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.lightSensorManager.getAmbientLuxFlow().collectLatest { lux ->
                    isSensorDarkTheme = LightThemeHysteresis.isDark(isSensorDarkTheme, lux)
                }
            }
        }

        setContent {
            val currentThemeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

            val isDark = when (currentThemeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO_SENSOR -> isSensorDarkTheme
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            ChargeRouteTheme(darkTheme = isDark) {
                AppNavHost(
                    container = container,
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = themeViewModel::setThemeMode
                )
            }
        }
    }
}
