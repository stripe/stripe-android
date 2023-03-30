package com.stripe.android.paymentsheet.example.samples.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ErrorScreen(
    onRetry: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        Text(
            text = "Unable to load checkout",
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Maybe wait a few seconds, have some tea, read a book… then try again.",
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(text = "Try again")
        }
    }
}
