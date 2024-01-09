package com.stripe.android.paymentsheet.prototype

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
internal class ParsingMetadata(
    val stripeIntent: StripeIntent,
    val configuration: PaymentSheet.Configuration,
    // TODO: New type for LUXE Spec.
//    val sharedDataSpecs: List<SharedDataSpec>,
    val isDeferred: Boolean,
    val financialConnectionsAvailable: Boolean,
): Parcelable {
    @IgnoredOnParcel
    val merchantName: String = configuration.merchantDisplayName

    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }
}
