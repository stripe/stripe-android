package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

@Immutable
internal data class CardInputState(
    private val card: PaymentMethod.Card,
    private val cardBrandChoice: CardBrandChoice,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
    val entry: CardDetailsEntry
) {
    val hasChanged: Boolean
        get() {
            val expChanged = card.expiryMonth != entry.expMonth || card.expiryYear != entry.expYear
            val cardBrandChanged = cardBrandChoice != entry.cardBrandChoice
            val addressChanged = when (addressCollectionMode) {
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
                    billingDetails?.address?.postalCode != entry.postalCode ||
                        billingDetails?.address?.country != entry.country
                }
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> false
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    billingDetails?.address?.postalCode != entry.postalCode ||
                        billingDetails?.address?.country != entry.country ||
                        billingDetails?.address?.line1 != entry.line1 ||
                        billingDetails?.address?.line2 != entry.line2 ||
                        billingDetails?.address?.city != entry.city ||
                        billingDetails?.address?.state != entry.state
                }
            }
            return expChanged || cardBrandChanged || addressChanged
        }

//    fun addressValid(): Boolean {
//        when (addressCollectionMode) {
//            AddressCollectionMode.Automatic -> {
//                (entry.country.isNullOrBlank() || entry.postalCode.isNullOrBlank()).not()
//            }
//            AddressCollectionMode.Never -> true
//            AddressCollectionMode.Full -> {
//
//            }
//        }
//    }
}