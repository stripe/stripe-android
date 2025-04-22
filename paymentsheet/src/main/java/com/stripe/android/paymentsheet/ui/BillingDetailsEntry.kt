package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.uicore.forms.FormFieldEntry

internal data class BillingDetailsEntry(
    val billingDetailsFormState: BillingDetailsFormState
) {
    fun hasChanged(
        billingDetails: PaymentMethod.BillingDetails?,
        addressCollectionMode: AddressCollectionMode,
    ): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                billingDetails?.address?.postalCode nullableNeq billingDetailsFormState.postalCode ||
                    billingDetails?.address?.country nullableNeq billingDetailsFormState.country
            }
            AddressCollectionMode.Never -> false
            AddressCollectionMode.Full -> {
                billingDetails?.address?.postalCode nullableNeq billingDetailsFormState.postalCode ||
                    billingDetails?.address?.country nullableNeq billingDetailsFormState.country ||
                    billingDetails?.address?.line1 nullableNeq billingDetailsFormState.line1 ||
                    billingDetails?.address?.line2 nullableNeq billingDetailsFormState.line2 ||
                    billingDetails?.address?.city nullableNeq billingDetailsFormState.city ||
                    billingDetails?.address?.state nullableNeq billingDetailsFormState.state
            }
        }
    }

    fun isComplete(addressCollectionMode: AddressCollectionMode): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                billingDetailsFormState.country.isValid && billingDetailsFormState.postalCode.isValid
            }
            AddressCollectionMode.Never -> {
                true
            }
            AddressCollectionMode.Full -> {
                billingDetailsFormState.country.isValid && billingDetailsFormState.state.isValid &&
                    billingDetailsFormState.postalCode.isValid && billingDetailsFormState.line1.isValid &&
                    billingDetailsFormState.city.isValid
            }
        }
    }

    private val FormFieldEntry?.isValid
        get() = this?.isComplete ?: true

    private infix fun String?.nullableNeq(other: FormFieldEntry?): Boolean {
        return this.orEmpty() != other?.value.orEmpty()
    }
}
