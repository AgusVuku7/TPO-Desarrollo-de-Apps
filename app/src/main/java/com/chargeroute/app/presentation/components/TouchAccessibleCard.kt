package com.chargeroute.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Card complying with RNF02 (Ley de Fitts: active touch targets >= 44x44 dp)
 */
@Composable
fun TouchAccessibleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    content: @Composable () -> Unit
) {
    val baseModifier = modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = baseModifier,
            shape = RoundedCornerShape(cornerRadius),
            border = border,
            colors = colors
        ) {
            content()
        }
    } else {
        Card(
            modifier = baseModifier,
            shape = RoundedCornerShape(cornerRadius),
            border = border,
            colors = colors
        ) {
            content()
        }
    }
}
