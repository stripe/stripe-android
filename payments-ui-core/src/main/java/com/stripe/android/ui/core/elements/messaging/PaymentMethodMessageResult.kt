package com.stripe.android.ui.core.elements.messaging

/**
 * Result of the Payment Method Messaging state transaction.
 */
internal sealed class PaymentMethodMessageResult {
    /**
     * Represents an ongoing transaction.
     */
    object Loading : PaymentMethodMessageResult()

    /**
     * Represents a successful transaction of the Payment Method Messaging state.
     *
     * @param data the [PaymentMethodMessageData] backing the composable view in
     * [PaymentMethodMessage]
     */
    class Success(
        val data: PaymentMethodMessageData
    ) : PaymentMethodMessageResult()

    /**
     * Represents a failed transaction.
     *
     * @param error the failure reason.
     */
    class Failure(
        val error: Throwable
    ) : PaymentMethodMessageResult()
}
