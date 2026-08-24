package com.stripe.android.link.repositories

import com.stripe.android.core.networking.ApiRequest
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
        requestOptions: ApiRequest.Options,
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
        requestOptions: ApiRequest.Options,
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
        requestOptions: ApiRequest.Options,
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
        requestOptions: ApiRequest.Options,
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
    ): Result<ConsumerSessionRefresh>

    /**
     * Sign up for a new Link account.
     */
    suspend fun consumerSignUp(
        requestOptions: ApiRequest.Options,
        email: String,
        phone: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<ConsumerSessionSignup>

    suspend fun mobileSignUp(
        requestOptions: ApiRequest.Options,
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
        requestOptions: ApiRequest.Options,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<LinkPaymentDetails.New>

    suspend fun createPaymentDetailsFromPaymentMethod(
        requestOptions: ApiRequest.Options,
        paymentMethod: PaymentMethod,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
        customerEphemeralKey: String,
    ): Result<LinkPaymentDetails.Saved>

    suspend fun createBankAccountPaymentDetails(
        requestOptions: ApiRequest.Options,
        bankAccountId: String,
        userEmail: String,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<ConsumerPaymentDetails.PaymentDetails>

    suspend fun shareCardPaymentDetails(
        requestOptions: ApiRequest.Options,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<LinkPaymentDetails.Passthrough>

    suspend fun sharePaymentDetails(
        requestOptions: ApiRequest.Options,
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
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        paymentMethod: LinkPaymentMethod,
        clientAttributionMetadata: ClientAttributionMetadata
    ): Result<PaymentMethod>

    suspend fun logOut(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
    ): Result<ConsumerSession>

    /**
     * Start an SMS verification.
     */
    suspend fun startVerification(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        isResendSmsCode: Boolean = false
    ): Result<ConsumerSession>

    /**
     * Confirm an SMS verification code.
     */
    suspend fun confirmVerification(
        requestOptions: ApiRequest.Options,
        verificationCode: String,
        consumerSessionClientSecret: String,
        consentGranted: Boolean?,
    ): Result<ConsumerSession>

    /**
     * Update consent status for the signed in consumer.
     */
    suspend fun postConsentUpdate(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
    ): Result<Unit>

    /**
     * Fetch all saved payment methods for the signed in consumer.
     */
    suspend fun listPaymentDetails(
        requestOptions: ApiRequest.Options,
        paymentMethodTypes: Set<String>,
        consumerSessionClientSecret: String,
    ): Result<ConsumerPaymentDetails>

    /**
     * Fetch all shipping addresses for the signed in consumer.
     */
    suspend fun listShippingAddresses(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
    ): Result<ConsumerShippingAddresses>

    /**
     * Delete the payment method from the consumer account.
     */
    suspend fun deletePaymentDetails(
        requestOptions: ApiRequest.Options,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
    ): Result<Unit>

    /**
     * Update an existing payment method in the consumer account.
     */
    suspend fun updatePaymentDetails(
        requestOptions: ApiRequest.Options,
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String,
    ): Result<ConsumerPaymentDetails>

    suspend fun createLinkAccountSession(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
    ): Result<LinkAccountSession>

    /**
     * Update the phone number for the signed in consumer.
     */
    suspend fun updatePhoneNumber(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        phoneNumber: String,
    ): Result<ConsumerSession>
}
