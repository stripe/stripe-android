package com.stripe.tta.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SummaryScreen(
    orderTotal: String,
    onShopAgain: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Thank you!",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Your demo order is complete.",
                style = MaterialTheme.typography.body1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Charged",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = orderTotal,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.weight(1f, fill = true))
            Button(
                onClick = onShopAgain,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Shop again")
            }
        }
    }
}
