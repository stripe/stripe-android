package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.uicore.forms.FormFieldEntry

internal data class BillingDetailsEntry(
    val billingDetailsFormState: BillingDetailsFormState
) {
    fun hasChanged(
        billingDetails: PaymentMethod.BillingDetails?,
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
    ): Boolean {
        val contactInfoChanged = contactInformationChanged(
            configuration = billingDetailsCollectionConfiguration,
            billingDetails = billingDetails
        )

        val addressChanged = when (billingDetailsCollectionConfiguration.address) {
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

        return contactInfoChanged || addressChanged
    }

    private fun contactInformationChanged(
        configuration: BillingDetailsCollectionConfiguration,
        billingDetails: PaymentMethod.BillingDetails?
    ): Boolean {
        val nameChanged = if (configuration.collectsName) {
            billingDetails?.name nullableNeq billingDetailsFormState.name
        } else {
            false
        }

        val emailChanged = if (configuration.collectsEmail) {
            billingDetails?.email nullableNeq billingDetailsFormState.email
        } else {
            false
        }

        val phoneChanged = if (configuration.collectsPhone) {
            billingDetails?.phone nullableNeq billingDetailsFormState.phone
        } else {
            false
        }
        return nameChanged || emailChanged || phoneChanged
    }

    fun isComplete(configuration: BillingDetailsCollectionConfiguration): Boolean {
        val contactInfoComplete = contactInfoComplete(configuration)

        val addressComplete = when (configuration.address) {
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

        return contactInfoComplete && addressComplete
    }

    private fun contactInfoComplete(configuration: BillingDetailsCollectionConfiguration): Boolean {
        val nameComplete = if (configuration.collectsName) {
            billingDetailsFormState.name.isValid
        } else {
            true
        }

        val emailComplete = if (configuration.collectsEmail) {
            billingDetailsFormState.email.isValid
        } else {
            true
        }

        val phoneComplete = if (configuration.collectsPhone) {
            billingDetailsFormState.phone.isValid
        } else {
            true
        }
        return nameComplete && emailComplete && phoneComplete
    }

    private val FormFieldEntry?.isValid
        get() = this?.isComplete ?: true

    private infix fun String?.nullableNeq(other: FormFieldEntry?): Boolean {
        if (other == null) return false
        return this.orEmpty() != other.value.orEmpty()
    }
}
