package com.stripe.android.link.repositories

import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.model.StripeIntent

/**
 * Interface for a repository that interacts with Link services.
 */
@SuppressWarnings("TooManyFunctions")
internal interface LinkRepository {

    /**
     * Check if the email already has a link account.
     */
    suspend fun lookupConsumer(
        email: String,
    ): Result<ConsumerSessionLookup>

    suspend fun mobileLookupConsumer(
        email: String,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        sessionId: String
    ): Result<ConsumerSessionLookup>

    /**
     * Sign up for a new Link account.
     */
    suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<ConsumerSessionSignup>

    suspend fun mobileSignUp(
        name: String?,
        email: String,
        phoneNumber: String,
        country: String,
        consentAction: ConsumerSignUpConsentAction,
        amount: Long?,
        currency: String?,
        incentiveEligibilitySession: IncentiveEligibilitySession?,
        verificationToken: String,
        appId: String
    ): Result<ConsumerSessionSignup>

    /**
     * Create a new card payment method in the consumer account.
     */
    suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
        active: Boolean,
    ): Result<LinkPaymentDetails.New>

    suspend fun shareCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        last4: String,
        consumerSessionClientSecret: String,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
    ): Result<LinkPaymentDetails>

    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
    ): Result<SharePaymentDetails>

    suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
    ): Result<ConsumerSession>

    /**
     * Start an SMS verification.
     */
    suspend fun startVerification(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
    ): Result<ConsumerSession>

    /**
     * Confirm an SMS verification code.
     */
    suspend fun confirmVerification(
        verificationCode: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
    ): Result<ConsumerSession>

    /**
     * Fetch all saved payment methods for the signed in consumer.
     */
    suspend fun listPaymentDetails(
        paymentMethodTypes: Set<String>,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails>

    /**
     * Delete the payment method from the consumer account.
     */
    suspend fun deletePaymentDetails(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<Unit>

    /**
     * Update an existing payment method in the consumer account.
     */
    suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails>
}
