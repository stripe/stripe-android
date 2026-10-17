package com.stripe.android.paymentsheet.paymentdatacollection

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.forms.FormFieldEntry

internal data class FormArguments(
    val paymentMethodCode: PaymentMethodCode,
    val cbcEligibility: CardBrandChoiceEligibility,
    val merchantName: String,
    val amount: Amount? = null,
    val billingDetails: PaymentSheet.BillingDetails? = null,
    val shippingDetails: AddressDetails? = null,
    val paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior?,
    val hasIntentToSetup: Boolean,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
        PaymentSheet.BillingDetailsCollectionConfiguration(),
) {
    val defaultFormValues by lazy {
        mutableMapOf<FormFieldId, String>().apply {
            if (billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod) {
                billingDetails?.let { billingDetails ->
                    billingDetails.name?.let { this[FormFieldId.Name] = it }
                    billingDetails.email?.let { this[FormFieldId.Email] = it }
                    billingDetails.phone?.let { this[FormFieldId.Phone] = it }
                    billingDetails.address?.line1?.let { this[FormFieldId.Companion.Line1] = it }
                    billingDetails.address?.line2?.let { this[FormFieldId.Companion.Line2] = it }
                    billingDetails.address?.city?.let { this[FormFieldId.Companion.City] = it }
                    billingDetails.address?.state?.let { this[FormFieldId.Companion.State] = it }
                    billingDetails.address?.postalCode?.let { this[FormFieldId.Companion.PostalCode] = it }
                    billingDetails.address?.country?.let { this[FormFieldId.Companion.Country] = it }
                }
            }
        }.toMap()
    }

    fun noUserInteractionFormFieldValues(): FormFieldValues {
        return FormFieldValues(
            fieldValuePairs = defaultFormValues.mapValues {
                FormFieldEntry(it.value, isComplete = true)
            },
            // userRequestedReuse only changes based on `SaveForFutureUse`, which won't ever hit this
            // code path.
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest
        )
    }
}
