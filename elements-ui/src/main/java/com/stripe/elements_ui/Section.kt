package com.stripe.elements_ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.Card as MaterialCard

@Composable
fun Section(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        if (header != null) {
            header()
            Spacer(modifier = Modifier.requiredHeight(8.dp))
        }

        MaterialCard {
            content()
        }

        if (footer != null) {
            Spacer(modifier = Modifier.requiredHeight(8.dp))
            footer()
        }
    }
}
