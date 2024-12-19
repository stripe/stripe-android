package com.stripe.android.repository

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithModelJsonParser
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.AttachConsumerToLinkAccountSession
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.model.UpdateAvailableIncentives
import com.stripe.android.model.VerificationType
import com.stripe.android.model.parsers.AttachConsumerToLinkAccountSessionJsonParser
import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import com.stripe.android.model.parsers.ConsumerSessionJsonParser
import com.stripe.android.model.parsers.ConsumerSessionLookupJsonParser
import com.stripe.android.model.parsers.ConsumerSessionSignupJsonParser
import com.stripe.android.model.parsers.SharePaymentDetailsJsonParser
import com.stripe.android.model.parsers.UpdateAvailableIncentivesJsonParser
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface ConsumersApiService {

    suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        locale: Locale?,
        amount: Long?,
        currency: String?,
        incentiveEligibilitySession: IncentiveEligibilitySession?,
        requestSurface: String,
        consentAction: ConsumerSignUpConsentAction,
        requestOptions: ApiRequest.Options,
    ): Result<ConsumerSessionSignup>

    suspend fun lookupConsumerSession(
        email: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup

    suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession

    suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        requestSurface: String,
        type: VerificationType,
        requestOptions: ApiRequest.Options
    ): ConsumerSession

    suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
    ): AttachConsumerToLinkAccountSession

    suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
    ): Result<ConsumerPaymentDetails>

    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
        extraParams: Map<String, Any?>,
    ): Result<SharePaymentDetails>

    suspend fun updateAvailableIncentives(
        sessionId: String,
        consumerSessionClientSecret: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
    ): Result<UpdateAvailableIncentives>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class ConsumersApiServiceImpl(
    private val stripeNetworkClient: StripeNetworkClient,
    apiVersion: String,
    sdkVersion: String = StripeSdkVersion.VERSION,
    appInfo: AppInfo?
) : ConsumersApiService {

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        locale: Locale?,
        amount: Long?,
        currency: String?,
        incentiveEligibilitySession: IncentiveEligibilitySession?,
        requestSurface: String,
        consentAction: ConsumerSignUpConsentAction,
        requestOptions: ApiRequest.Options,
    ): Result<ConsumerSessionSignup> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = consumerAccountsSignUpUrl,
                options = requestOptions,
                params = mapOf(
                    "email_address" to email.lowercase(),
                    "phone_number" to phoneNumber,
                    "country" to country,
                    "country_inferring_method" to "PHONE_NUMBER",
                    "amount" to amount,
                    "currency" to currency,
                    "consent_action" to consentAction.value,
                    "request_surface" to requestSurface,
                ).plus(
                    locale?.let {
                        mapOf("locale" to it.toLanguageTag())
                    } ?: emptyMap()
                ).plus(
                    name?.let {
                        mapOf("legal_name" to it)
                    } ?: emptyMap()
                ).plus(
                    incentiveEligibilitySession?.toParamMap().orEmpty()
                ),
            ),
            responseJsonParser = ConsumerSessionSignupJsonParser,
        )
    }

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun lookupConsumerSession(
        email: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                consumerSessionLookupUrl,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface,
                    "email_address" to email.lowercase()
                )
            ),
            responseJsonParser = ConsumerSessionLookupJsonParser()
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
        requestOptions: ApiRequest.Options,
    ): ConsumerSession {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                startConsumerVerificationUrl,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "type" to type.value,
                    "custom_email_type" to customEmailType?.value,
                    "connections_merchant_name" to connectionsMerchantName,
                    "locale" to locale.toLanguageTag()
                ).filterValues { it != null }
            ),
            responseJsonParser = ConsumerSessionJsonParser()
        )
    }

    /**
     * Confirms an SMS verification for the consumer corresponding to the given client secret.
     */
    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        requestSurface: String,
        type: VerificationType,
        requestOptions: ApiRequest.Options
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
                "code" to verificationCode
            )
        ),
        responseJsonParser = ConsumerSessionJsonParser()
    )

    override suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options
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
                )
            ),
            responseJsonParser = AttachConsumerToLinkAccountSessionJsonParser,
        )
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
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
                )
            ),
            responseJsonParser = ConsumerPaymentDetailsJsonParser,
        )
    }

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
        extraParams: Map<String, Any?>,
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
            ),
            responseJsonParser = SharePaymentDetailsJsonParser,
        )
    }

    override suspend fun updateAvailableIncentives(
        sessionId: String,
        consumerSessionClientSecret: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
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
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                ),
            ),
            responseJsonParser = UpdateAvailableIncentivesJsonParser,
        )
    }

    internal companion object {

        /**
         * @return `https://api.stripe.com/v1/consumers/accounts/sign_up`
         */
        internal val consumerAccountsSignUpUrl: String =
            getApiUrl("consumers/accounts/sign_up")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/lookup`
         */
        internal val consumerSessionLookupUrl: String =
            getApiUrl("consumers/sessions/lookup")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/start_verification`
         */
        internal val startConsumerVerificationUrl: String =
            getApiUrl("consumers/sessions/start_verification")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/confirm_verification`
         */
        internal val confirmConsumerVerificationUrl: String =
            getApiUrl("consumers/sessions/confirm_verification")

        /**
         * @return `https://api.stripe.com/v1/consumers/attach_link_consumer_to_link_account_session`
         */
        internal val attachLinkConsumerToLinkAccountSession: String =
            getApiUrl("consumers/attach_link_consumer_to_link_account_session")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details`
         */
        private val createPaymentDetails: String = getApiUrl("consumers/payment_details")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/share`
         */
        private val sharePaymentDetails: String = getApiUrl("consumers/payment_details/share")

        /**
         * @return `https://api.stripe.com/v1/consumers/incentives/update_available`
         */
        private val updateAvailableIncentivesUrl: String = getApiUrl("consumers/incentives/update_available")

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
