package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.uicore.forms.FormFieldEntry

internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
    val expMonth: Int? = null,
    val expYear: Int? = null,
    val city: FormFieldEntry? = null,
    val country: FormFieldEntry? = null, // two-character country code
    val line1: FormFieldEntry? = null,
    val line2: FormFieldEntry? = null,
    val postalCode: FormFieldEntry? = null,
    val state: FormFieldEntry? = null
) {
    fun hasChanged(
        card: PaymentMethod.Card,
        cardBrandChoice: CardBrandChoice,
        billingDetails: PaymentMethod.BillingDetails?,
        addressCollectionMode: AddressCollectionMode,
    ): Boolean {
        val expChanged = card.expiryMonth != expMonth || card.expiryYear != expYear
        val cardBrandChanged = cardBrandChoice != this.cardBrandChoice
        val addressChanged = when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                billingDetails?.address?.postalCode != postalCode?.value ||
                    billingDetails?.address?.country != country?.value
            }
            AddressCollectionMode.Never -> false
            AddressCollectionMode.Full -> {
                billingDetails?.address?.postalCode != postalCode?.value ||
                    billingDetails?.address?.country != country?.value ||
                    billingDetails?.address?.line1 != line1?.value ||
                    billingDetails?.address?.line2 != line2?.value ||
                    billingDetails?.address?.city != city?.value ||
                    billingDetails?.address?.state != state?.value
            }
        }
        return expChanged || cardBrandChanged || addressChanged
    }

    fun valid(addressCollectionMode: AddressCollectionMode): Boolean {
        return addressValid(addressCollectionMode) && expDateValid()
    }

    private fun addressValid(addressCollectionMode: AddressCollectionMode): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                country.isValid && postalCode.isValid
            }
            AddressCollectionMode.Never -> {
                true
            }
            AddressCollectionMode.Full -> {
                country.isValid && state.isValid && postalCode.isValid && line1.isValid && line2.isValid
            }
        }
    }

    private fun expDateValid(): Boolean = expMonth != null && expYear != null
}

private val FormFieldEntry?.isValid
    get() = this?.isComplete ?: false