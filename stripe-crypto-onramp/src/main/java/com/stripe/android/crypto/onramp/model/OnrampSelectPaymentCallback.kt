package com.stripe.android.crypto.onramp.model

import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampCollectPaymentCallback {
    fun onResult(result: OnrampCollectPaymentResult)
}

/**
 * Result of selecting a payment type in Onramp.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class OnrampCollectPaymentResult {
    /**
     * The user has selected a payment option.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(val displayData: PaymentOptionDisplayData) : OnrampCollectPaymentResult()

    /**
     * The user declined to select a payment option.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampCollectPaymentResult()

    /**
     * Selecting a payment option failed due to an error.
     * @param error The error that caused the failure.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(val error: Throwable) : OnrampCollectPaymentResult()
}

@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentOptionDisplayData internal constructor(
    val icon: Drawable,
    /**
     * User facing strings representing payment method information
     */
    val label: String,
    val sublabel: String?
)
