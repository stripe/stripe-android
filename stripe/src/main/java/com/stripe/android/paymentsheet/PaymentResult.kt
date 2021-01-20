package com.stripe.android.paymentsheet

import android.app.Activity
import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import kotlinx.parcelize.Parcelize

internal sealed class PaymentResult(
    val resultCode: Int
) : Parcelable {

    @Parcelize
    data class Succeeded(
        val paymentIntent: PaymentIntent
    ) : PaymentResult(Activity.RESULT_OK)

    @Parcelize
    data class Failed(
        val error: Throwable,
        val paymentIntent: PaymentIntent?
    ) : PaymentResult(Activity.RESULT_CANCELED)

    @Parcelize
    data class Canceled(
        val mostRecentError: Throwable?,
        val paymentIntent: PaymentIntent?
    ) : PaymentResult(Activity.RESULT_CANCELED)
}
