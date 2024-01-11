package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

/**
 * The metadata we need to determine what payment methods are supported, as well as being able to display them.
 * The purpose of this is to be able to easily plumb this information into the locations it’s needed.
 */
@Parcelize
internal class PaymentMethodMetadata(
    val stripeIntent: StripeIntent,
    // TODO(jaynewstrom): New type for LUXE Spec.
    val merchantName: String,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val defaultBillingDetails: PaymentSheet.BillingDetails,
    val preferredNetworks: List<CardBrand>,
    val allowsDelayedPaymentMethods: Boolean,
    val financialConnectionsAvailable: Boolean,
) : Parcelable {
    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }
}
