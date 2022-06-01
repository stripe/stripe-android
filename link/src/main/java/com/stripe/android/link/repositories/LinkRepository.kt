package com.stripe.android.link.repositories

import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.StripeIntent

/**
 * Interface for a repository that interacts with Link services.
 */
internal interface LinkRepository {

    /**
     * Check if the email already has a link account.
     */
    suspend fun lookupConsumer(
        email: String?,
        authSessionCookie: String?
    ): Result<ConsumerSessionLookup>

    /**
     * Sign up for a new Link account.
     */
    suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String,
        authSessionCookie: String?
    ): Result<ConsumerSession>

    /**
     * Start an SMS verification.
     */
    suspend fun startVerification(
        consumerSessionClientSecret: String,
        authSessionCookie: String?
    ): Result<ConsumerSession>

    /**
     * Confirm an SMS verification code.
     */
    suspend fun confirmVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        authSessionCookie: String?
    ): Result<ConsumerSession>

    /**
     * Logs out the current consumer.
     */
    suspend fun logout(
        consumerSessionClientSecret: String,
        authSessionCookie: String?
    ): Result<ConsumerSession>

    /**
     * Fetch all saved payment methods for the consumer.
     */
    suspend fun listPaymentDetails(
        consumerSessionClientSecret: String
    ): Result<ConsumerPaymentDetails>

    /**
     * Create a new payment method in the consumer account.
     */
    suspend fun createPaymentDetails(
        paymentDetails: ConsumerPaymentDetailsCreateParams,
        consumerSessionClientSecret: String,
        stripeIntent: StripeIntent,
        extraConfirmationParams: Map<String, Any>? = null
    ): Result<LinkPaymentDetails>

    /**
     * Update an existing payment method in the consumer account.
     */
    suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String
    ): Result<ConsumerPaymentDetails>

    /**
     * Delete the payment method from the consumer account.
     */
    suspend fun deletePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String
    ): Result<Unit>
}
