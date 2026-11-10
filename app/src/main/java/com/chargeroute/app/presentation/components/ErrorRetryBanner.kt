package com.chargeroute.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chargeroute.app.R
import com.chargeroute.app.core.theme.BatteryRed
import com.chargeroute.app.core.theme.ChargeRouteTheme

/**
 * Implements RNF06 - Recuperación ante errores.
 * Muestra el error de red con las opciones "Reintentar" y "Abrir última ruta guardada".
 */
@Composable
fun ErrorRetryBanner(
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenSavedRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BatteryRed.copy(alpha = 0.12f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = BatteryRed
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onOpenSavedRoute) {
                Text(stringResource(R.string.component_retry_open_last_route))
            }
            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BatteryRed)
            ) {
                Text(stringResource(R.string.component_retry))
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ErrorRetryBannerPreview() {
    ChargeRouteTheme(darkTheme = false) {
        ErrorRetryBanner(
            errorMessage = "No se pudo calcular la ruta",
            onRetry = {},
            onOpenSavedRoute = {}
        )
    }
}
