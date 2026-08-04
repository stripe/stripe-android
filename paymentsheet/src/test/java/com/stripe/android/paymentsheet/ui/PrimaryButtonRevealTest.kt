package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.addresselement.ScrollableColumn
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.elements.H4Text
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

internal class PrimaryButtonRevealTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.fillMaxSize(),
    )

    @Test
    fun `moves enabled primary button into payment form viewport`() {
        paparazziRule.gif(end = 1_400L) {
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            var isEnabled by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(500L)
                isEnabled = true
            }

            RevealPrimaryButtonWhenEnabled(
                isEnabled = isEnabled,
                isImeVisible = true,
                bringIntoViewRequester = bringIntoViewRequester,
            )

            PaymentFormPage(
                isPrimaryButtonEnabled = isEnabled,
                primaryButtonBringIntoViewRequester = bringIntoViewRequester,
            )
        }
    }
}

@Composable
private fun PaymentFormPage(
    isPrimaryButtonEnabled: Boolean,
    primaryButtonBringIntoViewRequester: BringIntoViewRequester,
) {
    Box(
        modifier = Modifier
            .height(320.dp)
            .clipToBounds(),
    ) {
        ScrollableColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            H4Text(
                text = "Card details",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(bottom = 8.dp),
            )
            Text(
                text = "Enter your card information to complete your payment.",
                modifier = Modifier.padding(bottom = 24.dp),
            )
            OutlinedTextField(
                value = "4242 4242 4242 4242",
                onValueChange = {},
                label = { Text("Card number") },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = "12 / 34",
                    onValueChange = {},
                    label = { Text("Expiration") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = "123",
                    onValueChange = {},
                    label = { Text("CVC") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Your payment is secured with Stripe.",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            PrimaryButton(
                label = "Pay $10.99",
                locked = false,
                enabled = isPrimaryButtonEnabled,
                modifier = Modifier.bringIntoViewRequester(primaryButtonBringIntoViewRequester),
                onClick = {},
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
