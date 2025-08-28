package com.stripe.android.crypto.onramp.model

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampCollectPaymentMethodCallback {
    fun onResult(result: OnrampCollectPaymentMethodResult)
}

/**
 * Result of selecting a payment type in Onramp.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class OnrampCollectPaymentMethodResult {
    /**
     * The user has selected a payment option.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(val displayData: PaymentMethodDisplayData) : OnrampCollectPaymentMethodResult()

    /**
     * The user declined to select a payment option.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampCollectPaymentMethodResult()

    /**
     * Selecting a payment option failed due to an error.
     * @param error The error that caused the failure.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(val error: Throwable) : OnrampCollectPaymentMethodResult()
}

@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentMethodDisplayData internal constructor(

    /**
     * User facing icon represented payment method.
     */
    @DrawableRes
    val iconRes: Int,

    /**
     * User facing strings representing payment method information
     */
    val label: String,

    val sublabel: String?
)
