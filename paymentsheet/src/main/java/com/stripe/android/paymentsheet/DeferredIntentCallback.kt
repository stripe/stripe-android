package com.stripe.android.paymentsheet

import com.stripe.android.CreateIntentCallback
import com.stripe.android.StripeDeferredIntentCreationBetaApi

@OptIn(StripeDeferredIntentCreationBetaApi::class)
internal fun interface DeferredIntentCallback {
    suspend fun onIntentCreated(
        paymentMethodId: String,
        customerRequestedSave: Boolean,
    ): CreateIntentCallback.Result
}
