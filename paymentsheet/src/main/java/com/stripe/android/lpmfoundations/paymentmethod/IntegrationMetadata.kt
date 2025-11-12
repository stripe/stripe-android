package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed class IntegrationMetadata : Parcelable {
    @Parcelize
    object IntentFirst : IntegrationMetadata()

    @Parcelize
    object DeferredIntentWithPaymentMethod : IntegrationMetadata()

    @Parcelize
    object DeferredIntentWithSharedPaymentToken : IntegrationMetadata()

    @Parcelize
    object DeferredIntentWithConfirmationToken : IntegrationMetadata()

    // CustomerSheet doesn't really fit the bill of any of the other integrations, so making it's own, even though it's
    // not ideal.
    @Parcelize
    object CustomerSheet : IntegrationMetadata()
}
