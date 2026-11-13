package com.chargeroute.app.presentation.screens.sidebar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeroute.app.R
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.core.theme.ThemeMode
import com.chargeroute.app.presentation.preview.PreviewData

@Composable
fun SidebarDrawer(
    userName: String,
    userEmail: String,
    savedTripsCount: Int = 1,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToSavedTrips: () -> Unit,
    onNavigateToChargers: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                DrawerHeader(onClose = onClose)

                Spacer(modifier = Modifier.height(24.dp))

                ProfileCard(userName = userName, userEmail = userEmail)

                Spacer(modifier = Modifier.height(24.dp))

                DrawerMenuItems(
                    savedTripsCount = savedTripsCount,
                    onNavigateToPlan = onNavigateToPlan,
                    onNavigateToSavedTrips = onNavigateToSavedTrips,
                    onNavigateToChargers = onNavigateToChargers,
                    onClose = onClose
                )
            }

            // Parte de abajo: selector de tema y cerrar sesion
            DrawerBottom(
                currentThemeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
                onLogout = onLogout
            )
        }
    }
}

// Encabezado con logo y boton de cerrar
@Composable
private fun DrawerHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(44.dp) // RNF02
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.sidebar_close_menu))
        }
    }
}

// Tarjeta de perfil
@Composable
private fun ProfileCard(userName: String, userEmail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            // El invitado no tiene nombre ni mail guardados
            Text(
                text = userName.ifBlank { stringResource(R.string.sidebar_guest_name) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (userEmail.isNotBlank()) {
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Opciones del menu
@Composable
private fun DrawerMenuItems(
    savedTripsCount: Int,
    onNavigateToPlan: () -> Unit,
    onNavigateToSavedTrips: () -> Unit,
    onNavigateToChargers: () -> Unit,
    onClose: () -> Unit
) {
    DrawerItem(
        icon = Icons.Default.Route,
        title = stringResource(R.string.sidebar_plan_trip),
        isSelected = true,
        onClick = {
            onClose()
            onNavigateToPlan()
        }
    )

    DrawerItem(
        icon = Icons.Default.History,
        title = stringResource(R.string.sidebar_my_trips),
        badge = "$savedTripsCount",
        onClick = {
            onClose()
            onNavigateToSavedTrips()
        }
    )

    DrawerItem(
        icon = Icons.Default.EvStation,
        title = stringResource(R.string.sidebar_my_chargers),
        onClick = {
            onClose()
            onNavigateToChargers()
        }
    )
}

// Parte de abajo: selector de tema, cerrar sesion y version
@Composable
private fun DrawerBottom(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit
) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = stringResource(R.string.sidebar_appearance),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Selector de tema: Claro | Oscuro | Auto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ThemeTab(
                title = stringResource(R.string.sidebar_theme_light),
                selected = currentThemeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) }
            )
            ThemeTab(
                title = stringResource(R.string.sidebar_theme_dark),
                selected = currentThemeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) }
            )
            ThemeTab(
                title = stringResource(R.string.sidebar_theme_auto),
                selected = currentThemeMode == ThemeMode.AUTO_SENSOR,
                onClick = { onThemeModeChange(ThemeMode.AUTO_SENSOR) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boton de cerrar sesion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sidebar_logout),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.sidebar_footer, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandPrimary.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp) // RNF02
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BrandPrimary else MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            badge?.let {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ThemeTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) BrandPrimary else Color.Transparent,
        modifier = Modifier
            .height(44.dp) // RNF02
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SidebarDrawerPreview() {
    ChargeRouteTheme(darkTheme = false) {
        SidebarDrawer(
            userName = PreviewData.userName,
            userEmail = PreviewData.userEmail,
            savedTripsCount = 2,
            currentThemeMode = ThemeMode.LIGHT,
            onThemeModeChange = {},
            onNavigateToPlan = {},
            onNavigateToSavedTrips = {},
            onNavigateToChargers = {},
            onLogout = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SidebarDrawerDarkPreview() {
    ChargeRouteTheme(darkTheme = true) {
        SidebarDrawer(
            userName = PreviewData.userName,
            userEmail = PreviewData.userEmail,
            savedTripsCount = 2,
            currentThemeMode = ThemeMode.DARK,
            onThemeModeChange = {},
            onNavigateToPlan = {},
            onNavigateToSavedTrips = {},
            onNavigateToChargers = {},
            onLogout = {},
            onClose = {}
        )
    }
}
