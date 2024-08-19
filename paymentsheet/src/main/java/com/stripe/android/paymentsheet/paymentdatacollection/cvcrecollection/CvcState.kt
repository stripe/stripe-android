package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.compose.runtime.Immutable
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.uicore.elements.TextFieldIcon

@Immutable
internal data class CvcState(
    val cvc: String,
    val cardBrand: CardBrand
) {
    private val cvcTextFieldConfig: CvcConfig = CvcConfig()

    val isValid: Boolean = cvcTextFieldConfig.determineState(
        brand = cardBrand,
        number = cvc,
        numberAllowedDigits = cardBrand.maxCvcLength
    ).isValid()

    val label: Int = if (cardBrand == CardBrand.AmericanExpress) {
        R.string.stripe_cvc_amex_hint
    } else {
        R.string.stripe_cvc_number_hint
    }

    val cvcIcon = TextFieldIcon.Trailing(cardBrand.cvcIcon, isTintable = false)

    fun updateCvc(cvc: String): CvcState {
        if (cvc.length > cardBrand.maxCvcLength) return this
        return CvcState(
            cvc = cvc,
            cardBrand = cardBrand
        )
    }
}
