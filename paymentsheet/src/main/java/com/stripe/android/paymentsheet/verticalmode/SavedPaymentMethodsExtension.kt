package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility

internal fun PaymentMethod.toDisplayableSavedPaymentMethod(
    providePaymentMethodName: (PaymentMethodCode?) -> String,
    paymentMethodMetadata: PaymentMethodMetadata?,
): DisplayableSavedPaymentMethod {
    return DisplayableSavedPaymentMethod(
        displayName = providePaymentMethodName(type?.code),
        paymentMethod = this,
        isCbcEligible = paymentMethodMetadata?.cbcEligibility is CardBrandChoiceEligibility.Eligible,
    )
}
