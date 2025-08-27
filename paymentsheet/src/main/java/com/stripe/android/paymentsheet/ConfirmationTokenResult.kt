package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.model.ConfirmationToken
import kotlinx.parcelize.Parcelize

/**
 * The result of a ConfirmationToken creation attempt in PaymentSheet.
 *
 * Unlike [PaymentSheetResult] which indicates payment completion status,
 * [ConfirmationTokenResult] returns the generated ConfirmationToken for
 * server-side payment confirmation.
 */
sealed class ConfirmationTokenResult : Parcelable {

    /**
     * ConfirmationToken was successfully created.
     *
     * The merchant should send the [confirmationToken] to their server to complete
     * payment confirmation using the Stripe server-side API.
     *
     * @param confirmationToken The generated ConfirmationToken containing payment method
     * data, shipping information, and other checkout state collected by PaymentSheet.
     */
    @Parcelize
    data class Completed(
        val confirmationToken: ConfirmationToken
    ) : ConfirmationTokenResult()

    /**
     * The customer canceled the ConfirmationToken creation attempt.
     */
    @Parcelize
    data object Canceled : ConfirmationTokenResult()

    /**
     * The ConfirmationToken creation attempt failed.
     * @param error The error encountered during token creation.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : ConfirmationTokenResult()
}
