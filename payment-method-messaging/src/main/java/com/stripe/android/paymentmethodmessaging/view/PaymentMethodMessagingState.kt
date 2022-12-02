package com.stripe.android.paymentmethodmessaging.view

/**
 * Result of the Payment Method Messaging state transaction.
 */
internal sealed class PaymentMethodMessagingState {
    /**
     * Represents an ongoing transaction.
     */
    object Loading : PaymentMethodMessagingState()

    /**
     * Represents a successful transaction of the Payment Method Messaging state.
     *
     * @param data the [PaymentMethodMessagingData] backing the composable view in
     * [PaymentMethodMessaging]
     */
    class Success(
        val data: PaymentMethodMessagingData
    ) : PaymentMethodMessagingState()

    /**
     * Represents a failed transaction.
     *
     * @param error the failure reason.
     */
    class Failure(
        val error: Throwable
    ) : PaymentMethodMessagingState()
}
