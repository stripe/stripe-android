package com.stripe.android.paymentsheet

/**
 * Types of errors that can occur when confirming a payment.
 */
internal sealed interface PaymentConfirmationErrorType {
    /**
     * Fatal confirmation error that occurred while confirming a payment. This should never happen.
     */
    data object Fatal : PaymentConfirmationErrorType

    /**
     * Indicates an error when processing a payment during the confirmation process.
     */
    data object Payment : PaymentConfirmationErrorType

    /**
     * Indicates an internal process error occurred during the confirmation process.
     */
    data object Internal : PaymentConfirmationErrorType

    /**
     * Indicates a merchant integration error occurred during the confirmation process.
     */
    data object MerchantIntegration : PaymentConfirmationErrorType

    /**
     * Indicates an error occurred when confirming with external payment methods
     */
    data object ExternalPaymentMethod : PaymentConfirmationErrorType

    /**
     * Indicates an error occurred when confirming with Google Pay
     */
    data class GooglePay(val errorCode: Int) : PaymentConfirmationErrorType
}
