package com.stripe.android.ui.core.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.stripe.android.model.CardBrand

@Immutable
data class CardNumberInputSpec(
    val value: String = "",
    val onCardNumberUpdated: (CardNumberInputSpec) -> Unit
) : InputSpec {
    val cardBrand: CardBrand = CardBrand.Unknown
    override val valid: Boolean
        get() = true

    @Composable
    override fun Content(modifier: Modifier) {
        SpecBox(
            modifier = modifier,
            spec = InputTextFieldSpec(
                text = value,
                label = TextSpec("Card Number"),
                onValueChanged = { newValue ->
                    onCardNumberUpdated(copy(value = newValue))
                }
            )
        )
    }
}
