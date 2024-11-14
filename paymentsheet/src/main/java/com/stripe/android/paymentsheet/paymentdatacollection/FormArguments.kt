package com.stripe.android.paymentsheet.paymentdatacollection

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility

internal data class FormArguments(
    val paymentMethodCode: PaymentMethodCode,
    val cbcEligibility: CardBrandChoiceEligibility,
    val merchantName: String,
    val amount: Amount? = null,
    val billingDetails: PaymentSheet.BillingDetails? = null,
    val shippingDetails: AddressDetails? = null,
    val paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
    val hasIntentToSetup: Boolean,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
        PaymentSheet.BillingDetailsCollectionConfiguration(),
)
