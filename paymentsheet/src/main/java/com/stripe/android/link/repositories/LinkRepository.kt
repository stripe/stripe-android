package com.stripe.android.link.repositories

import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.LinkMode
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
     *
     * @param customerId Optional customer ID to associate with the lookup. When provided, enables
     *                   retrieval of displayable payment details.
     */
    suspend fun lookupConsumer(
        email: String?,
        linkAuthIntentId: String?,
        sessionId: String,
        customerId: String?
    ): Result<ConsumerSessionLookup>

    /**
     * Performs a lookup of a consumer session without triggering any
     * back end logging events. This is currently only being used by the
     * Link global holdback to look up consumers in the event Link is disabled.
     */
    suspend fun lookupConsumerWithoutBackendLoggingForExposure(
        email: String,
        sessionId: String,
    ): Result<ConsumerSessionLookup>

    /**
     * Performs a consumer lookup with mobile attestation verification.
     *
     * @param customerId Optional customer ID to associate with the lookup. When provided, enables
     *                   retrieval of displayable payment details.
     */
    suspend fun mobileLookupConsumer(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        verificationToken: String,
        appId: String,
        sessionId: String,
        customerId: String?
    ): Result<ConsumerSessionLookup>

    /**
     * Sign up for a new Link account.
     */
    suspend fun consumerSignUp(
        email: String,
        phone: String?,
        country: String?,
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
    ): Result<LinkPaymentDetails.New>

    suspend fun createBankAccountPaymentDetails(
        bankAccountId: String,
        userEmail: String,
        consumerSessionClientSecret: String,
    ): Result<ConsumerPaymentDetails.PaymentDetails>

    suspend fun shareCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        consumerSessionClientSecret: String,
    ): Result<LinkPaymentDetails.Saved>

    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
        cvc: String?,
        allowRedisplay: String?,
        apiKey: String?
    ): Result<SharePaymentDetails>

    suspend fun createPaymentMethod(
        consumerSessionClientSecret: String,
        paymentMethod: LinkPaymentMethod
    ): Result<PaymentMethod>

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
        consentGranted: Boolean?,
    ): Result<ConsumerSession>

    /**
     * Update consent status for the signed in consumer.
     */
    suspend fun consentUpdate(
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
        consumerPublishableKey: String?
    ): Result<Unit>

    /**
     * Fetch all saved payment methods for the signed in consumer.
     */
    suspend fun listPaymentDetails(
        paymentMethodTypes: Set<String>,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails>

    /**
     * Fetch all shipping addresses for the signed in consumer.
     */
    suspend fun listShippingAddresses(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerShippingAddresses>

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

    suspend fun createLinkAccountSession(
        consumerSessionClientSecret: String,
        stripeIntent: StripeIntent,
        linkMode: LinkMode?,
        consumerPublishableKey: String?
    ): Result<LinkAccountSession>
}
