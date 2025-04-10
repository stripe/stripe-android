package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.uicore.forms.FormFieldEntry

internal data class BillingDetailsFormState(
    val line1: FormFieldEntry?,
    val line2: FormFieldEntry?,
    val city: FormFieldEntry?,
    val postalCode: FormFieldEntry?,
    val state: FormFieldEntry?,
    val country: FormFieldEntry?,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
) {
    fun hasChanged(): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                billingDetails?.address?.postalCode nullableNeq postalCode ||
                    billingDetails?.address?.country nullableNeq country
            }
            AddressCollectionMode.Never -> false
            AddressCollectionMode.Full -> {
                billingDetails?.address?.postalCode nullableNeq postalCode ||
                    billingDetails?.address?.country nullableNeq country ||
                    billingDetails?.address?.line1 nullableNeq line1 ||
                    billingDetails?.address?.line2 nullableNeq line2 ||
                    billingDetails?.address?.city nullableNeq city ||
                    billingDetails?.address?.state nullableNeq state
            }
        }
    }

    fun isComplete(): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                country.isValid && postalCode.isValid
            }
            AddressCollectionMode.Never -> {
                true
            }
            AddressCollectionMode.Full -> {
                country.isValid && state.isValid && postalCode.isValid && line1.isValid && city.isValid
            }
        }
    }

    private val FormFieldEntry?.isValid
        get() = this?.isComplete ?: true

    private infix fun String?.nullableNeq(other: FormFieldEntry?): Boolean {
        return this.orEmpty() != other?.value.orEmpty()
    }
}
