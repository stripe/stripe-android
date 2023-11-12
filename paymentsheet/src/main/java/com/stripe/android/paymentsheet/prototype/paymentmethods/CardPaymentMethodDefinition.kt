package com.stripe.android.paymentsheet.prototype.paymentmethods

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.prototype.AddPaymentMethodRequirement
import com.stripe.android.paymentsheet.prototype.InitialAddPaymentMethodState
import com.stripe.android.paymentsheet.prototype.ParsingMetadata
import com.stripe.android.paymentsheet.prototype.PaymentMethodConfirmParams
import com.stripe.android.paymentsheet.prototype.PaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.paymentsheet.prototype.buildInitialState
import com.stripe.android.ui.core.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

internal object CardPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Card

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = emptySet()

    override suspend fun initialAddState(
        metadata: ParsingMetadata,
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_card)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card
                tintIconOnSelection = true
            }

            element(CardUiElementDefinition)

            // TODO: Add billing address country selector + zip if needed.

            // TODO: Link inline checkbox info

            // TODO: Save checkbox
        }

        state(CardState())
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        // TODO: Add card number information
        // TODO: Do we need things like SFU in options
        // TODO: Card brand choice.
        val cardState = uiState[cardStateKey]
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParams.Card.Builder()
                    .setNumber(cardState.number)
                    .setCvc(cardState.cvc)
                    .setExpiryMonth(cardState.expirationMonth)
                    .setExpiryYear(cardState.expirationYear)
                    .build()
            )
        )
    }
}

private val cardStateKey = PaymentMethodDefinition.UiStateKey.create<CardState>(CardPaymentMethodDefinition)

@Parcelize
internal data class CardState(
    val number: String = "",
    val cvc: String = "",
    val expirationMonth: Int? = null,
    val expirationYear: Int? = null,
    // TODO: Add error to state so it can be displayed in the UI.
) : UiState.Value {
    @IgnoredOnParcel
    override val key: UiState.Key<CardState> = cardStateKey

    fun isComplete(): Boolean {
        return number.isValidCardNumber() && cvc.isNotEmpty() && hasValidExpirationDate()
    }

    private fun hasValidExpirationDate(): Boolean {
        return true // TODO: Check year and month are after now.
    }
}

internal fun String.isValidCardNumber(): Boolean {
    return isNotEmpty() // TODO: Ensure it's actually valid.
}

internal object CardUiElementDefinition : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        return uiState[cardStateKey].isComplete()
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return CardUiRenderer(uiState[cardStateKey])
    }
}

internal class CardUiRenderer(
    private val cardStateFlow: StateFlow<CardState>,
) : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        val cardState by cardStateFlow.collectAsState()
    }
}
