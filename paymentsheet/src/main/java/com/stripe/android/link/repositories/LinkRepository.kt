package com.stripe.android.link.repositories

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionRefresh
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
        apiConfiguration: ApiConfiguration.State,
        email: String?,
        linkAuthIntentId: String?,
        sessionId: String,
        customerId: String?,
        supportedVerificationTypes: List<String>?,
    ): Result<ConsumerSessionLookup>

    /**
     * Performs a lookup of a consumer session without triggering any
     * back end logging events. This is currently only being used by the
     * Link global holdback to look up consumers in the event Link is disabled.
     */
    suspend fun lookupConsumerWithoutBackendLoggingForExposure(
        apiConfiguration: ApiConfiguration.State,
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
        apiConfiguration: ApiConfiguration.State,
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        verificationToken: String,
        appId: String,
        sessionId: String,
        customerId: String?,
        supportedVerificationTypes: List<String>?,
        linkAuthTokenClientSecret: String?,
    ): Result<ConsumerSessionLookup>

    /**
     * Refresh the mobile consumer session.
     */
    suspend fun refreshConsumer(
        apiConfiguration: ApiConfiguration.State,
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
    ): Result<ConsumerSessionRefresh>

    /**
     * Sign up for a new Link account.
     */
    suspend fun consumerSignUp(
        apiConfiguration: ApiConfiguration.State,
        email: String,
        phone: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<ConsumerSessionSignup>

    suspend fun mobileSignUp(
        apiConfiguration: ApiConfiguration.State,
        name: String?,
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
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
        apiConfiguration: ApiConfiguration.State,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<LinkPaymentDetails.New>

    suspend fun createPaymentDetailsFromPaymentMethod(
        apiConfiguration: ApiConfiguration.State,
        paymentMethod: PaymentMethod,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
        customerEphemeralKey: String,
    ): Result<LinkPaymentDetails.Saved>

    suspend fun createBankAccountPaymentDetails(
        apiConfiguration: ApiConfiguration.State,
        bankAccountId: String,
        userEmail: String,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<ConsumerPaymentDetails.PaymentDetails>

    suspend fun shareCardPaymentDetails(
        apiConfiguration: ApiConfiguration.State,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<LinkPaymentDetails.Passthrough>

    suspend fun sharePaymentDetails(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String?,
        billingPhone: String?,
        cvc: String?,
        allowRedisplay: String?,
        apiKey: String?,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<SharePaymentDetails>

    suspend fun createPaymentMethod(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
        paymentMethod: LinkPaymentMethod,
        clientAttributionMetadata: ClientAttributionMetadata
    ): Result<PaymentMethod>

    suspend fun logOut(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
    ): Result<ConsumerSession>

    /**
     * Start an SMS verification.
     */
    suspend fun startVerification(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
        isResendSmsCode: Boolean = false
    ): Result<ConsumerSession>

    /**
     * Confirm an SMS verification code.
     */
    suspend fun confirmVerification(
        apiConfiguration: ApiConfiguration.State,
        verificationCode: String,
        consumerSessionClientSecret: String,
        consentGranted: Boolean?,
    ): Result<ConsumerSession>

    /**
     * Update consent status for the signed in consumer.
     */
    suspend fun postConsentUpdate(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
    ): Result<Unit>

    /**
     * Fetch all saved payment methods for the signed in consumer.
     */
    suspend fun listPaymentDetails(
        apiConfiguration: ApiConfiguration.State,
        paymentMethodTypes: Set<String>,
        consumerSessionClientSecret: String,
    ): Result<ConsumerPaymentDetails>

    /**
     * Fetch all shipping addresses for the signed in consumer.
     */
    suspend fun listShippingAddresses(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
    ): Result<ConsumerShippingAddresses>

    /**
     * Delete the payment method from the consumer account.
     */
    suspend fun deletePaymentDetails(
        apiConfiguration: ApiConfiguration.State,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
    ): Result<Unit>

    /**
     * Update an existing payment method in the consumer account.
     */
    suspend fun updatePaymentDetails(
        apiConfiguration: ApiConfiguration.State,
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String,
    ): Result<ConsumerPaymentDetails>

    suspend fun createLinkAccountSession(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
    ): Result<LinkAccountSession>

    /**
     * Update the phone number for the signed in consumer.
     */
    suspend fun updatePhoneNumber(
        apiConfiguration: ApiConfiguration.State,
        consumerSessionClientSecret: String,
        phoneNumber: String,
    ): Result<ConsumerSession>
}
