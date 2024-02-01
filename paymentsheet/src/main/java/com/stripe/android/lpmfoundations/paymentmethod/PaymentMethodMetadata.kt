package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import kotlinx.parcelize.Parcelize

/**
 * The metadata we need to determine what payment methods are supported, as well as being able to display them.
 * The purpose of this is to be able to easily plumb this information into the locations it’s needed.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentMethodMetadata(
    val stripeIntent: StripeIntent,
    val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
    val allowsDelayedPaymentMethods: Boolean,
    val financialConnectionsAvailable: Boolean = DefaultIsFinancialConnectionsAvailable().invoke(),
) : Parcelable {
    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }
}
