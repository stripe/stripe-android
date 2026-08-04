package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.ScrollableColumn
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.FormUI
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
            var isFormComplete by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(500L)
                isFormComplete = true
            }

            RevealPrimaryButtonWhenEnabled(
                isEnabled = isFormComplete,
                isImeVisible = true,
                bringIntoViewRequester = bringIntoViewRequester,
            )

            PaymentFormPage(
                isPrimaryButtonEnabled = isFormComplete,
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
    val formElements = remember {
        CardDefinition.formElements(metadata = cardFormMetadata)
    }

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
            FormUI(
                hiddenIdentifiers = emptySet(),
                enabled = true,
                elements = formElements,
                lastTextFieldIdentifier = null,
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

private val cardFormMetadata = PaymentMethodMetadataFactory.create(
    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
        paymentMethodTypes = listOf("card"),
    ),
    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
    ),
)
