package com.stripe.android.repository

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithErrorParsing
import com.stripe.android.core.networking.executeRequestWithModelJsonParser
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.AttachConsumerToLinkAccountSession
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.EmailSource
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.model.SignUpParams
import com.stripe.android.model.UpdateAvailableIncentives
import com.stripe.android.model.VerificationType
import com.stripe.android.model.parsers.AttachConsumerToLinkAccountSessionJsonParser
import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import com.stripe.android.model.parsers.ConsumerSessionJsonParser
import com.stripe.android.model.parsers.ConsumerSessionLookupJsonParser
import com.stripe.android.model.parsers.ConsumerSessionRefreshJsonParser
import com.stripe.android.model.parsers.ConsumerSessionSignupJsonParser
import com.stripe.android.model.parsers.LinkAccountSessionJsonParser
import com.stripe.android.model.parsers.SharePaymentDetailsJsonParser
import com.stripe.android.model.parsers.UpdateAvailableIncentivesJsonParser
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface ConsumersApiService {
    fun withApiConfiguration(apiConfiguration: ApiConfiguration.State): ConsumersApiService = this

    suspend fun signUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup> {
        return withApiConfiguration(apiConfiguration).signUp(
            params = params,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun mobileSignUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup> {
        return withApiConfiguration(apiConfiguration).mobileSignUp(
            params = params,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun lookupConsumerSession(
        email: String?,
        linkAuthIntentId: String?,
        requestSurface: String,
        sessionId: String,
        doNotLogConsumerFunnelEvent: Boolean,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        customerId: String?,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): ConsumerSessionLookup {
        return withApiConfiguration(apiConfiguration).lookupConsumerSession(
            email = email,
            linkAuthIntentId = linkAuthIntentId,
            requestSurface = requestSurface,
            sessionId = sessionId,
            doNotLogConsumerFunnelEvent = doNotLogConsumerFunnelEvent,
            supportedVerificationTypes = supportedVerificationTypes,
            requestOptions = requestOptions,
            customerId = customerId,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun mobileLookupConsumerSession(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        requestSurface: String,
        verificationToken: String,
        appId: String,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        sessionId: String,
        customerId: String?,
        linkAuthTokenClientSecret: String?,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): ConsumerSessionLookup {
        return withApiConfiguration(apiConfiguration).mobileLookupConsumerSession(
            email = email,
            emailSource = emailSource,
            linkAuthIntentId = linkAuthIntentId,
            requestSurface = requestSurface,
            verificationToken = verificationToken,
            appId = appId,
            supportedVerificationTypes = supportedVerificationTypes,
            requestOptions = requestOptions,
            sessionId = sessionId,
            customerId = customerId,
            linkAuthTokenClientSecret = linkAuthTokenClientSecret,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun refreshConsumerSession(
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): ConsumerSessionRefresh {
        return withApiConfiguration(apiConfiguration).refreshConsumerSession(
            appId = appId,
            consumerSessionClientSecret = consumerSessionClientSecret,
            supportedVerificationTypes = supportedVerificationTypes,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
        accountPhoneNumber: String?,
        emailAddress: String?,
        requestOptions: RequestCredentials,
        isResendSmsCode: Boolean = false,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): ConsumerSession {
        return withApiConfiguration(apiConfiguration).startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            locale = locale,
            requestSurface = requestSurface,
            type = type,
            customEmailType = customEmailType,
            connectionsMerchantName = connectionsMerchantName,
            accountPhoneNumber = accountPhoneNumber,
            emailAddress = emailAddress,
            requestOptions = requestOptions,
            isResendSmsCode = isResendSmsCode,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        requestSurface: String,
        type: VerificationType,
        consentGranted: Boolean?,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): ConsumerSession {
        return withApiConfiguration(apiConfiguration).confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode,
            requestSurface = requestSurface,
            type = type,
            consentGranted = consentGranted,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun postConsentUpdate(
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
        requestSurface: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Unit> {
        return withApiConfiguration(apiConfiguration).postConsentUpdate(
            consumerSessionClientSecret = consumerSessionClientSecret,
            consentGranted = consentGranted,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): AttachConsumerToLinkAccountSession {
        return withApiConfiguration(apiConfiguration).attachLinkConsumerToLinkAccountSession(
            consumerSessionClientSecret = consumerSessionClientSecret,
            clientSecret = clientSecret,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestSurface: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return withApiConfiguration(apiConfiguration).createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsCreateParams = paymentDetailsCreateParams,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentMethodId: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        customerEphemeralKey: String,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return withApiConfiguration(apiConfiguration).createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentMethodId = paymentMethodId,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            customerEphemeralKey = customerEphemeralKey,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String?,
        billingPhone: String?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        extraParams: Map<String, Any?>,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SharePaymentDetails> {
        return withApiConfiguration(apiConfiguration).sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsId = paymentDetailsId,
            expectedPaymentMethodType = expectedPaymentMethodType,
            billingPhone = billingPhone,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            extraParams = extraParams,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun updateAvailableIncentives(
        sessionId: String,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<UpdateAvailableIncentives> {
        return withApiConfiguration(apiConfiguration).updateAvailableIncentives(
            sessionId = sessionId,
            paymentDetailsId = paymentDetailsId,
            consumerSessionClientSecret = consumerSessionClientSecret,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createLinkAccountSession(
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<LinkAccountSession> {
        return withApiConfiguration(apiConfiguration).createLinkAccountSession(
            consumerSessionClientSecret = consumerSessionClientSecret,
            intentToken = intentToken,
            linkMode = linkMode,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun updatePhoneNumber(
        consumerSessionClientSecret: String,
        phoneNumber: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerSession> {
        return withApiConfiguration(apiConfiguration).updatePhoneNumber(
            consumerSessionClientSecret = consumerSessionClientSecret,
            phoneNumber = phoneNumber,
            requestSurface = requestSurface,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun signUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup>

    suspend fun mobileSignUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup>

    suspend fun lookupConsumerSession(
        email: String?,
        linkAuthIntentId: String?,
        requestSurface: String,
        sessionId: String,
        doNotLogConsumerFunnelEvent: Boolean,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        customerId: String?,
        idempotencyKey: String?,
    ): ConsumerSessionLookup

    suspend fun mobileLookupConsumerSession(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        requestSurface: String,
        verificationToken: String,
        appId: String,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        sessionId: String,
        customerId: String?,
        linkAuthTokenClientSecret: String?,
        idempotencyKey: String?,
    ): ConsumerSessionLookup

    suspend fun refreshConsumerSession(
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): ConsumerSessionRefresh

    suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
        accountPhoneNumber: String?,
        emailAddress: String?,
        requestOptions: RequestCredentials,
        isResendSmsCode: Boolean = false,
        idempotencyKey: String?,
    ): ConsumerSession

    suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        requestSurface: String,
        type: VerificationType,
        consentGranted: Boolean?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): ConsumerSession

    suspend fun postConsentUpdate(
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Unit>

    suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): AttachConsumerToLinkAccountSession

    suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails>

    suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentMethodId: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        customerEphemeralKey: String,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails>

    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String?,
        billingPhone: String?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        extraParams: Map<String, Any?>,
        idempotencyKey: String?,
    ): Result<SharePaymentDetails>

    suspend fun updateAvailableIncentives(
        sessionId: String,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<UpdateAvailableIncentives>

    suspend fun createLinkAccountSession(
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<LinkAccountSession>

    suspend fun updatePhoneNumber(
        consumerSessionClientSecret: String,
        phoneNumber: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSession>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class ConsumersApiServiceImpl(
    private val stripeNetworkClient: StripeNetworkClient,
    private val apiVersion: String,
    private val sdkVersion: String = StripeSdkVersion.VERSION,
    private val appInfo: AppInfo?,
    private val requestApiConfiguration: ApiConfiguration.State?
) : ConsumersApiService {

    override fun withApiConfiguration(apiConfiguration: ApiConfiguration.State): ConsumersApiService {
        return ConsumersApiServiceImpl(
            stripeNetworkClient = stripeNetworkClient,
            apiVersion = apiVersion,
            sdkVersion = sdkVersion,
            appInfo = appInfo,
            requestApiConfiguration = apiConfiguration.copy(betas = apiConfiguration.betas.toSet()),
        )
    }

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun signUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = consumerAccountsSignUpUrl,
                options = requestOptions,
                params = params.toParamMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerSessionSignupJsonParser,
        )
    }

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun mobileSignUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = consumerMobileSignUpUrl,
                options = requestOptions,
                params = params.toParamMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerSessionSignupJsonParser,
        )
    }

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun lookupConsumerSession(
        email: String?,
        linkAuthIntentId: String?,
        requestSurface: String,
        sessionId: String,
        doNotLogConsumerFunnelEvent: Boolean,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        customerId: String?,
        idempotencyKey: String?,
    ): ConsumerSessionLookup {
        val avoidConsumerLoggingParams: Map<String, Boolean> = if (doNotLogConsumerFunnelEvent) {
            mapOf("do_not_log_consumer_funnel_event" to true)
        } else {
            emptyMap()
        }
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                consumerSessionLookupUrl,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface,
                    "session_id" to sessionId,
                    "email_address" to email?.lowercase(),
                    "link_auth_intent_id" to linkAuthIntentId,
                    "customer_id" to customerId
                ).filterValues { it != null } + avoidConsumerLoggingParams,
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerSessionLookupJsonParser()
        )
    }

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun mobileLookupConsumerSession(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        requestSurface: String,
        verificationToken: String,
        appId: String,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        sessionId: String,
        customerId: String?,
        linkAuthTokenClientSecret: String?,
        idempotencyKey: String?,
    ): ConsumerSessionLookup {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                mobileConsumerSessionLookupUrl,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface,
                    "email_address" to email?.lowercase(),
                    "link_auth_intent_id" to linkAuthIntentId,
                    "android_verification_token" to verificationToken,
                    "session_id" to sessionId,
                    "email_source" to emailSource?.backendValue,
                    "app_id" to appId,
                    "customer_id" to customerId,
                    "supported_verification_types" to supportedVerificationTypes,
                    "link_auth_token_client_secret" to linkAuthTokenClientSecret
                ).filterValues { it != null },
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerSessionLookupJsonParser()
        )
    }

    override suspend fun refreshConsumerSession(
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): ConsumerSessionRefresh {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = consumerSessionRefreshUrl,
                options = requestOptions,
                params = mapOf(
                    "app_id" to appId,
                    "request_surface" to requestSurface,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "supported_verification_types" to supportedVerificationTypes
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerSessionRefreshJsonParser()
        )
    }

    /**
     * Triggers a verification for the consumer corresponding to the given client secret.
     */
    override suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
        accountPhoneNumber: String?,
        emailAddress: String?,
        requestOptions: RequestCredentials,
        isResendSmsCode: Boolean,
        idempotencyKey: String?,
    ): ConsumerSession {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                startConsumerVerificationUrl,
                requestOptions,
                buildMap {
                    put("request_surface", requestSurface)
                    put("credentials", mapOf("consumer_session_client_secret" to consumerSessionClientSecret))
                    put("type", type.value)
                    customEmailType?.value?.let { put("custom_email_type", it) }
                    connectionsMerchantName?.let { put("connections_merchant_name", it) }
                    accountPhoneNumber?.let { put("account_phone_number", it) }
                    emailAddress?.let { put("email_address", it.lowercase()) }
                    put("locale", locale.toLanguageTag())
                    if (isResendSmsCode) {
                        put("is_resend_sms_code", true)
                    }
                },
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerSessionJsonParser()
        )
    }

    /**
     * Confirms a verification for the consumer corresponding to the given client secret.
     */
    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        requestSurface: String,
        type: VerificationType,
        consentGranted: Boolean?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): ConsumerSession = executeRequestWithModelJsonParser(
        stripeErrorJsonParser = stripeErrorJsonParser,
        stripeNetworkClient = stripeNetworkClient,
        request = apiRequestFactory.createPost(
            confirmConsumerVerificationUrl,
            requestOptions,
            mapOf(
                "request_surface" to requestSurface,
                "credentials" to mapOf(
                    "consumer_session_client_secret" to consumerSessionClientSecret
                ),
                "type" to type.value,
                "code" to verificationCode,
                "consent_granted" to consentGranted
            ).filterValues { it != null },
            apiConfiguration = requestApiConfiguration,
            idempotencyKey = idempotencyKey,
        ),
        responseJsonParser = ConsumerSessionJsonParser()
    )

    override suspend fun postConsentUpdate(
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Unit> {
        return executeRequestWithErrorParsing(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = consentUpdateUrl,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to requestSurface,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "consent_granted" to consentGranted
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            )
        )
    }

    override suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): AttachConsumerToLinkAccountSession {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                attachLinkConsumerToLinkAccountSession,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret,
                    ),
                    "link_account_session" to clientSecret,
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = AttachConsumerToLinkAccountSessionJsonParser,
        )
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = createPaymentDetails,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to requestSurface,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                ).plus(
                    paymentDetailsCreateParams.toParamMap()
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerPaymentDetailsJsonParser,
        )
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentMethodId: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        customerEphemeralKey: String,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = createPaymentDetailsFromPaymentMethod,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to requestSurface,
                    "payment_method_id" to paymentMethodId,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "customer_ephemeral_key_secret" to customerEphemeralKey
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerPaymentDetailsJsonParser,
        )
    }

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String?,
        billingPhone: String?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        extraParams: Map<String, Any?>,
        idempotencyKey: String?,
    ): Result<SharePaymentDetails> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = sharePaymentDetails,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to requestSurface,
                    "id" to paymentDetailsId,
                    "expected_payment_method_type" to expectedPaymentMethodType,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "billing_phone" to billingPhone,
                ) + extraParams,
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = SharePaymentDetailsJsonParser,
        )
    }

    override suspend fun updateAvailableIncentives(
        sessionId: String,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<UpdateAvailableIncentives> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = updateAvailableIncentivesUrl,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to requestSurface,
                    "session_id" to sessionId,
                    "payment_details_id" to paymentDetailsId,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = UpdateAvailableIncentivesJsonParser,
        )
    }

    override suspend fun createLinkAccountSession(
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<LinkAccountSession> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = createLinkAccountSession,
                options = requestOptions,
                params = mapOf(
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "intent_token" to intentToken,
                    "link_mode" to linkMode?.value,
                    "request_surface" to requestSurface,
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = LinkAccountSessionJsonParser,
        )
    }

    override suspend fun updatePhoneNumber(
        consumerSessionClientSecret: String,
        phoneNumber: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSession> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = updatePhoneNumberUrl,
                options = requestOptions,
                params = mapOf(
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "phone_number" to phoneNumber,
                    "request_surface" to requestSurface,
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            responseJsonParser = ConsumerSessionJsonParser(),
        )
    }

    internal companion object {
        /**
         * @return `https://api.stripe.com/v1/consumers/accounts/sign_up`
         */
        internal val consumerAccountsSignUpUrl: String
            get() = getApiUrl("consumers/accounts/sign_up")

        /**
         * @return `https://api.stripe.com/v1/consumers/mobile/sign_up`
         */
        internal val consumerMobileSignUpUrl: String
            get() = getApiUrl("consumers/mobile/sign_up")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/lookup`
         */
        internal val consumerSessionLookupUrl: String
            get() = getApiUrl("consumers/sessions/lookup")

        /**
         * @return `https://api.stripe.com/v1/consumers/mobile/sessions/lookup`
         */
        internal val mobileConsumerSessionLookupUrl: String
            get() = getApiUrl("consumers/mobile/sessions/lookup")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/refresh`
         */
        internal val consumerSessionRefreshUrl: String
            get() = getApiUrl("consumers/sessions/refresh")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/start_verification`
         */
        internal val startConsumerVerificationUrl: String
            get() = getApiUrl("consumers/sessions/start_verification")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/confirm_verification`
         */
        internal val confirmConsumerVerificationUrl: String
            get() = getApiUrl("consumers/sessions/confirm_verification")

        /**
         * @return `https://api.stripe.com/v1/consumers/attach_link_consumer_to_link_account_session`
         */
        internal val attachLinkConsumerToLinkAccountSession: String
            get() = getApiUrl("consumers/attach_link_consumer_to_link_account_session")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details`
         */
        private val createPaymentDetails: String
            get() = getApiUrl("consumers/payment_details")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/from_payment_method`
         */
        private val createPaymentDetailsFromPaymentMethod: String
            get() = getApiUrl("consumers/payment_details/from_payment_method")

        /**
         * @return `https://api.stripe.com/v1/consumers/link_account_sessions`
         */
        private val createLinkAccountSession: String
            get() = getApiUrl("consumers/link_account_sessions")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/share`
         */
        private val sharePaymentDetails: String
            get() = getApiUrl("consumers/payment_details/share")

        /**
         * @return `https://api.stripe.com/v1/consumers/incentives/update_available`
         */
        private val updateAvailableIncentivesUrl: String
            get() = getApiUrl("consumers/incentives/update_available")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/consent_update`
         */
        internal val consentUpdateUrl: String
            get() = getApiUrl("consumers/sessions/consent_update")

        /**
         * @return `https://api.stripe.com/v1/consumers/accounts/update_phone`
         */
        internal val updatePhoneNumberUrl: String
            get() = getApiUrl("consumers/accounts/update_phone")

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
