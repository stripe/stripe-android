package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import kotlinx.parcelize.Parcelize

internal sealed class IntegrationMetadata : Parcelable {
    @Parcelize
    data class IntentFirst(
        val clientSecret: String,
    ) : IntegrationMetadata()

    @Parcelize
    data class DeferredIntentWithPaymentMethod(
        val intentConfiguration: IntentConfiguration,
    ) : IntegrationMetadata()

    @Parcelize
    data class DeferredIntentWithSharedPaymentToken(
        val intentConfiguration: IntentConfiguration,
    ) : IntegrationMetadata()

    @Parcelize
    data class DeferredIntentWithConfirmationToken(
        val intentConfiguration: IntentConfiguration,
    ) : IntegrationMetadata()

    // CustomerSheet doesn't really fit the bill of any of the other integrations, so making it's own, even though it's
    // not ideal.
    @Parcelize
    object CustomerSheet : IntegrationMetadata()
}
