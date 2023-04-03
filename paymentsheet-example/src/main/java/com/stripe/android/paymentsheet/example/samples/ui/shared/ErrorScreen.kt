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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.example.R

@Composable
fun ErrorScreen(
    onRetry: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(
            text = stringResource(R.string.unable_to_load_checkout_title),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.unable_to_load_checkout_subtitle),
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(text = stringResource(R.string.unable_to_load_checkout_button))
        }
    }
}
