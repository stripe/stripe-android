package com.stripe.elements_ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.Card as MaterialCard

@Composable
fun Card(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    MaterialCard(
        modifier = modifier.clickable(
            enabled = isEnabled,
            onClick = onClick,
        )
    ) {
        content()
    }
}
