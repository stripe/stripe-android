package com.stripe.android.paymentsheet.paymentdatacollection

import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.BillingDetails
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec

internal data class FormArguments(
    val paymentMethodCode: PaymentMethodCode,
    val cbcEligibility: CardBrandChoiceEligibility,
    val merchantName: String,
    val amount: Amount? = null,
    val billingDetails: BillingDetails? = null,
    val shippingDetails: AddressDetails? = null,
    val paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
    val hasIntentToSetup: Boolean,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
        PaymentSheet.BillingDetailsCollectionConfiguration(),
) {
    val defaultFormValues by lazy {
        mutableMapOf<IdentifierSpec, String>().apply {
            if (billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod) {
                billingDetails?.let { billingDetails ->
                    billingDetails.name?.let { this[IdentifierSpec.Name] = it }
                    billingDetails.email?.let { this[IdentifierSpec.Email] = it }
                    billingDetails.phone?.let { this[IdentifierSpec.Phone] = it }
                    billingDetails.address?.line1?.let { this[IdentifierSpec.Companion.Line1] = it }
                    billingDetails.address?.line2?.let { this[IdentifierSpec.Companion.Line2] = it }
                    billingDetails.address?.city?.let { this[IdentifierSpec.Companion.City] = it }
                    billingDetails.address?.state?.let { this[IdentifierSpec.Companion.State] = it }
                    billingDetails.address?.postalCode?.let { this[IdentifierSpec.Companion.PostalCode] = it }
                    billingDetails.address?.country?.let { this[IdentifierSpec.Companion.Country] = it }
                }
            }
        }.toMap()
    }
}
