package com.stripe.android.paymentsheet.forms

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments

internal object FormArgumentsFactory {

    fun create(
        paymentMethodCode: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = paymentMethodCode,
            merchantName = metadata.merchantName,
            amount = metadata.amount(),
            billingDetails = metadata.defaultBillingDetails,
            shippingDetails = metadata.shippingDetails,
            billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
            cbcEligibility = metadata.cbcEligibility,
            hasIntentToSetup = metadata.hasIntentToSetup(),
            paymentMethodSaveConsentBehavior = metadata.paymentMethodSaveConsentBehavior,
        )
    }
}
