package com.stripe.android.paymentelement.embedded.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.StripeTheme

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Immutable
internal data class ExpressCheckoutContent(
    private val interactor: ExpressCheckoutInteractor,
) {
    @Composable
    fun Content() {
        val state by interactor.state.collectAsState()

        StripeTheme {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.walletsState?.let { walletsState ->
                    walletsState.link?.run {
                        LinkButton(
                            email = email,
                            enabled = walletsState.buttonsEnabled,
                            onClick = walletsState.onLinkPressed,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    walletsState.googlePay?.run {
                        state.cardBrandFilter?.let { cardBrandFilter ->
                            GooglePayButton(
                                state = if (
                                    state.isProcessing &&
                                    state.selection == EmbeddedPaymentElement.ExpressCheckoutType.GooglePay
                                ) {
                                    PrimaryButton.State.StartProcessing
                                } else {
                                    PrimaryButton.State.Ready
                                },
                                allowCreditCards = allowCreditCards,
                                buttonType = buttonType,
                                billingAddressParameters = billingAddressParameters,
                                isEnabled = walletsState.buttonsEnabled,
                                cardBrandFilter = cardBrandFilter,
                                onPressed = walletsState.onGooglePayPressed,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}