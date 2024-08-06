package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.StripeIntent

/**
 * Defines the result types that can be returned after completing a payment confirmation process.
 */
internal sealed interface PaymentConfirmationResult {
    /**
     * Indicates that the confirmation process was canceled by the customer.
     */
    data class Canceled(
        val action: PaymentCancellationAction,
    ) : PaymentConfirmationResult

    /**
     * Indicates that the confirmation process has been successfully completed. A [StripeIntent] with an updated
     * state is returned as part of the result as well.
     */
    data class Succeeded(
        val intent: StripeIntent,
        val extras: PaymentConfirmationExtras?,
    ) : PaymentConfirmationResult

    /**
     * Indicates that the confirmation process has failed. A cause and potentially a resolvable message are
     * returned as part of the result.
     */
    data class Failed(
        val cause: Throwable,
        val message: ResolvableString,
        val type: PaymentConfirmationErrorType,
    ) : PaymentConfirmationResult
}
