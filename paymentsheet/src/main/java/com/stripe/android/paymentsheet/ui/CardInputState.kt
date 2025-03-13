package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.uicore.forms.FormFieldEntry

@Immutable
internal data class CardInputState(
    val card: PaymentMethod.Card,
    private val cardBrandChoice: CardBrandChoice,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
    val entry: CardDetailsEntry
) {
    val hasChanged: Boolean
        get() {
            val expChanged = card.expiryMonth != entry.expMonth || card.expiryYear != entry.expYear
            val cardBrandChanged = cardBrandChoice != entry.cardBrandChoice
            val addressChanged = when (addressCollectionMode) {
                AddressCollectionMode.Automatic -> {
                    billingDetails?.address?.postalCode != entry.postalCode?.value ||
                        billingDetails?.address?.country != entry.country?.value
                }
                AddressCollectionMode.Never -> false
                AddressCollectionMode.Full -> {
                    billingDetails?.address?.postalCode != entry.postalCode?.value ||
                        billingDetails?.address?.country != entry.country?.value ||
                        billingDetails?.address?.line1 != entry.line1?.value ||
                        billingDetails?.address?.line2 != entry.line2?.value ||
                        billingDetails?.address?.city != entry.city?.value ||
                        billingDetails?.address?.state != entry.state?.value
                }
            }
            return expChanged || cardBrandChanged || addressChanged
        }

    val valid: Boolean = addressValid() && expDateValid()

    fun addressValid(): Boolean {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                entry.country.isValid && entry.postalCode.isValid
            }
            AddressCollectionMode.Never -> {
                true
            }
            AddressCollectionMode.Full -> {
                entry.country.isValid && entry.state.isValid && entry.postalCode.isValid && entry.line1.isValid
                    && entry.line2.isValid
            }
        }
    }

   fun expDateValid(): Boolean = entry.expMonth != null && entry.expYear != null
}

private val FormFieldEntry?.isValid
    get() = this?.isComplete ?: false