package com.stripe.android.ui.core.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.elements.CvcConfig

@Immutable
data class CvcInputSpec(
    val cvc: String = "",
    val cardBrand: CardBrand = CardBrand.Unknown,
    private val cvcTextFieldConfig: CvcConfig = CvcConfig(),
    val onValueChanged: (CvcInputSpec) -> Unit
) : InputSpec {
    override val valid = cvcTextFieldConfig.determineState(
        brand = cardBrand,
        number = cvc,
        numberAllowedDigits = cardBrand.maxCvcLength
    ).isValid()

    @Composable
    override fun Content(modifier: Modifier) {
        SpecBox(
            modifier = modifier,
            spec = InputTextFieldSpec(
                text = cvc,
                label = TextSpec(
                    text = "CVC"
                ),
                onValueChanged = { newValue ->
                    if (cvc.length > cardBrand.maxCvcLength) return@InputTextFieldSpec
                    onValueChanged(copy(cvc = newValue))
                }
            )
        )
    }
}
