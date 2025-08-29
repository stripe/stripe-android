package com.stripe.android.crypto.onramp.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.responseJson
import com.stripe.android.core.networking.toMap
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.CreatePaymentTokenRequest
import com.stripe.android.crypto.onramp.model.CreatePaymentTokenResponse
import com.stripe.android.crypto.onramp.model.CryptoCustomerRequestParams
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.CryptoWalletRequestParams
import com.stripe.android.crypto.onramp.model.GetOnrampSessionResponse
import com.stripe.android.crypto.onramp.model.GetPlatformSettingsResponse
import com.stripe.android.crypto.onramp.model.KycCollectionRequest
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.StartIdentityVerificationRequest
import com.stripe.android.crypto.onramp.model.StartIdentityVerificationResponse
import com.stripe.android.model.PaymentIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.utils.filterNotNullValues
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/*
* Repository interface for crypto-related operations.
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Singleton
internal class CryptoApiRepository @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
    private val stripeRepository: StripeRepository,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    apiVersion: String,
    sdkVersion: String = StripeSdkVersion.VERSION,
    appInfo: AppInfo?
) {
    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    /**
     * Grants the provided session merchant permissions.
     *
     * @param consumerSessionClientSecret The client session secret to attach permissions to.
     */
    suspend fun grantPartnerMerchantPermissions(
        consumerSessionClientSecret: String
    ): Result<CryptoCustomerResponse> {
        val params = CryptoCustomerRequestParams(CryptoCustomerRequestParams.Credentials(consumerSessionClientSecret))

        return execute(
            getGrantPartnerMerchantPermissionsUrl,
            Json.encodeToJsonElement(params).jsonObject,
            CryptoCustomerResponse.serializer()
        )
    }

    /**
     * Collects KYC data to attach it to a link account.
     *
     * @param kycInfo The KycInfo to attach.
     */
    suspend fun collectKycData(
        kycInfo: KycInfo,
        consumerSessionClientSecret: String
    ): Result<Unit> {
        val apiRequest = KycCollectionRequest.fromKycInfo(
            kycInfo = kycInfo,
            credentials = CryptoCustomerRequestParams.Credentials(consumerSessionClientSecret)
        )

        return execute(
            collectKycDataUrl,
            Json.encodeToJsonElement(apiRequest).jsonObject,
            Unit.serializer()
        )
    }

    /**
     * Sets the wallet address for the user.
     *
     * @param walletAddress The wallet address to set.
     * @param network The crypto network for the wallet address.
     * @param consumerSessionClientSecret The client session secret for authentication.
     */
    suspend fun setWalletAddress(
        walletAddress: String,
        network: CryptoNetwork,
        consumerSessionClientSecret: String
    ): Result<Unit> {
        val params = CryptoWalletRequestParams(
            walletAddress = walletAddress,
            network = network,
            credentials = CryptoCustomerRequestParams.Credentials(consumerSessionClientSecret)
        )

        return execute(
            setWalletAddressUrl,
            Json.encodeToJsonElement(params).jsonObject,
            Unit.serializer()
        )
    }

    suspend fun startIdentityVerification(
        consumerSessionClientSecret: String
    ): Result<StartIdentityVerificationResponse> {
        val request = StartIdentityVerificationRequest(
            credentials = CryptoCustomerRequestParams.Credentials(consumerSessionClientSecret),
        )

        val json = Json { encodeDefaults = true }

        return execute(
            startIdentityVerificationUrl,
            json.encodeToJsonElement(request).jsonObject,
            StartIdentityVerificationResponse.serializer()
        )
    }

    suspend fun getPlatformSettings(
        consumerSessionClientSecret: String?,
        countryHint: String?
    ): Result<GetPlatformSettingsResponse> {
        val request = apiRequestFactory.createGet(
            url = platformSettings,
            options = buildRequestOptions(),
            params = mapOf(
                "credentials[consumer_session_client_secret]" to consumerSessionClientSecret,
                "country_hint" to countryHint
            ).filterNotNullValues()
        )

        return execute(
            request = request,
            responseSerializer = GetPlatformSettingsResponse.serializer()
        )
    }

    suspend fun createPaymentToken(
        consumerSessionClientSecret: String,
        paymentMethod: String,
    ): Result<CreatePaymentTokenResponse> {
        val params = CreatePaymentTokenRequest(
            credentials = CryptoCustomerRequestParams.Credentials(consumerSessionClientSecret),
            paymentMethod = paymentMethod,
        )
        return execute(
            url = paymentToken,
            paramsJson = Json.encodeToJsonElement(params).jsonObject,
            responseSerializer = CreatePaymentTokenResponse.serializer()
        )
    }

    /**
     * Retrieves an onramp session.
     *
     * @param sessionId The onramp session identifier.
     * @param sessionClientSecret The onramp session client secret.
     * @return The onramp session details.
     */
    suspend fun getOnrampSession(
        sessionId: String,
        sessionClientSecret: String
    ): Result<GetOnrampSessionResponse> {
        val params = mapOf(
            "crypto_onramp_session" to sessionId,
            "client_secret" to sessionClientSecret
        )

        val request = apiRequestFactory.createGet(
            url = getOnrampSessionUrl,
            options = buildRequestOptions(),
            params = params,
        )

        return execute(
            request = request,
            responseSerializer = GetOnrampSessionResponse.serializer()
        )
    }

    /**
     * Retrieves a PaymentIntent using its client secret.
     *
     * @param clientSecret The PaymentIntent client secret.
     * @param publishableKey The special publishable key from platform settings to use for this request.
     * @return The PaymentIntent.
     */
    suspend fun retrievePaymentIntent(
        clientSecret: String,
        publishableKey: String
    ): Result<PaymentIntent> {
        return stripeRepository.retrievePaymentIntent(
            clientSecret = clientSecret,
            expandFields = listOf("payment_method"),
            options = ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountIdProvider(),
            )
        )
    }

    private fun buildRequestOptions(): ApiRequest.Options {
        return ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )
    }

    private suspend fun <Response> execute(
        url: String,
        paramsJson: JsonObject,
        responseSerializer: KSerializer<Response>,
    ): Result<Response> {
        val request = apiRequestFactory.createPost(
            url = url,
            options = buildRequestOptions(),
            params = paramsJson.toMap(),
        )

        return execute(
            request = request,
            responseSerializer = responseSerializer
        )
    }

    private suspend fun <Response> execute(
        request: StripeRequest,
        responseSerializer: KSerializer<Response>,
    ): Result<Response> {
        return runCatching {
            val response = stripeNetworkClient.executeRequest(request)
            if (response.isError) {
                val error = StripeErrorJsonParser().parse(response.responseJson())
                throw APIException(error)
            }
            try {
                val body = requireNotNull(response.body) { "No response body found" }
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString(responseSerializer, body)
            } catch (e: Exception) {
                throw APIException(
                    message = "Unable to parse response with ${responseSerializer::class.java.simpleName}",
                    cause = e,
                )
            }
        }
    }

    internal companion object {
        /**
         * @return `https://api.stripe.com/v1/crypto/internal/customers`
         */
        internal val getGrantPartnerMerchantPermissionsUrl: String = getApiUrl("crypto/internal/customers")

        /**
         * @return `https://api.stripe.com/v1/crypto/internal/kyc_data_collection`
         */
        internal val collectKycDataUrl: String = getApiUrl("crypto/internal/kyc_data_collection")

        /**
         * @return `https://api.stripe.com/v1/crypto/internal/wallet`
         */
        internal val setWalletAddressUrl: String = getApiUrl("crypto/internal/wallet")

        /**
         * @return `https://api.stripe.com/v1/crypto/internal/start_identity_verification`
         */
        internal val startIdentityVerificationUrl: String = getApiUrl("crypto/internal/start_identity_verification")

        /**
         * @return `https://api.stripe.com/v1/crypto/internal/platform_settings`
         */
        internal val platformSettings: String = getApiUrl("crypto/internal/platform_settings")

        /**
         * @return `https://api.stripe.com/v1/crypto/internal/payment_token`
         */
        internal val paymentToken: String = getApiUrl("crypto/internal/payment_token")

        /**
         * @return `https://api.stripe.com/v1/crypto/internal/onramp_session`
         */
        internal val getOnrampSessionUrl: String = getApiUrl("crypto/internal/onramp_session")

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
