package com.stripe.android.ui.core.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

@Immutable
data class CardDetailsInputSpec(
    val cardNumber: String = "",
    val cvc: String = "",
    val onValueChanged: (CardDetailsInputSpec) -> Unit
) : InputSpec {
    private val cardNumberInputSpec = CardNumberInputSpec(
        value = cardNumber,
        onCardNumberUpdated = { newValue ->
            onValueChanged(copy(cardNumber = newValue.value))
        }
    )
    private val cvcInputSpec = CvcInputSpec(
        cvc = cvc,
        cardBrand = cardNumberInputSpec.cardBrand,
        onValueChanged = { newValue ->
            onValueChanged(copy(cvc = newValue.cvc))
        }
    )
    override val valid = cardNumberInputSpec.valid && cvcInputSpec.valid

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
        ) {
            SpecBox(
                modifier = Modifier
                    .fillMaxWidth(),
                spec = cardNumberInputSpec
            )
            SpecBox(
                modifier = Modifier
                    .fillMaxWidth(),
                spec = cvcInputSpec
            )
        }
    }
}
