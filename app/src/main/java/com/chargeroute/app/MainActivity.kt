package com.chargeroute.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.core.theme.ThemeMode
import com.chargeroute.app.presentation.navigation.AppNavHost
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isSensorDarkTheme by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted or denied handled gracefully (RNF05)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = ChargeRouteApp.instance.container

        // Request location permissions if not already granted
        checkAndRequestPermissions()

        // Observe ambient light sensor for automatic day/night theme adaptation (RNF04, RNF05)
        lifecycleScope.launch {
            container.lightSensorManager.getAmbientLuxFlow().collectLatest { lux ->
                // Threshold: under 40 lux is low-light/night driving -> Dark theme
                // Over 40 lux is bright sunlight -> High-contrast Light theme
                isSensorDarkTheme = lux < 40f
            }
        }

        setContent {
            val userPrefs = container.userPreferences
            val currentThemeMode by userPrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()

            val isDark = when (currentThemeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO_SENSOR -> isSensorDarkTheme
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            ChargeRouteTheme(darkTheme = isDark) {
                AppNavHost(
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = { mode ->
                        scope.launch {
                            userPrefs.setThemeMode(mode)
                        }
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val fineLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLoc != PackageManager.PERMISSION_GRANTED || coarseLoc != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
