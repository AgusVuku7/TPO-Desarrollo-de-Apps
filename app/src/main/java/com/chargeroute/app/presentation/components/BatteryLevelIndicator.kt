package com.chargeroute.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeroute.app.core.theme.BatteryGreen
import com.chargeroute.app.core.theme.BatteryOrange
import com.chargeroute.app.core.theme.BatteryRed
import com.chargeroute.app.domain.model.BatteryLevels

@Composable
fun BatteryLevelBadge(
    percent: Int,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val color = when {
        percent > 40 -> BatteryGreen
        percent >= BatteryLevels.LOW_PERCENT -> BatteryOrange
        else -> BatteryRed
    }

    val containerColor = color.copy(alpha = 0.15f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            Icon(
                imageVector = if (percent < BatteryLevels.LOW_PERCENT) Icons.Default.BatteryAlert else Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = "$percent%",
            color = color,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold
        )
    }
}
