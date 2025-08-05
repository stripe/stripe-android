package com.stripe.android.crypto.onramp.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.responseJson
import com.stripe.android.core.networking.toMap
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.CryptoCustomerRequestParams
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import com.stripe.android.crypto.onramp.model.KycInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Named

/*
* Repository interface for crypto-related operations.
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal class CryptoApiRepository @Inject internal constructor(
    private val stripeNetworkClient: StripeNetworkClient,
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
        val kycRequestModel = CollectKycRequestModel(kycInfo, CryptoCustomerRequestParams.Credentials(consumerSessionClientSecret))

        return execute(
            collectKycDataUrl,
            Json.encodeToJsonElement(CollectKycRequestModelSerializer, kycRequestModel).jsonObject,
            Unit.serializer()
        )
    }

    private fun buildRequestOptions(): ApiRequest.Options {
        return ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )
    }

    private suspend fun<Response> execute(
        url: String,
        paramsJson: JsonObject,
        responseSerializer: KSerializer<Response>
    ): Result<Response> {
        val request = apiRequestFactory.createPost(
            url = url,
            options = buildRequestOptions(),
            params = paramsJson.toMap(),
        )

        return runCatching {
            stripeNetworkClient.executeRequest(request)
        }.mapCatching { response ->
            if (response.isError) {
                val error = StripeErrorJsonParser().parse(response.responseJson())
                throw APIConnectionException("Failed to execute $request", cause = APIException(error))
            }

            val body = requireNotNull(response.body) { "No response body found" }
            Json.decodeFromString(responseSerializer, body)
        }.recoverCatching {
            throw APIConnectionException("Failed to execute $request", cause = it)
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

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }

    @Serializable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private data class CollectKycRequestModel(
        val kycInfo: KycInfo,
        val credentials: CryptoCustomerRequestParams.Credentials
    )

    private object CollectKycRequestModelSerializer: JsonTransformingSerializer<CollectKycRequestModel>(
        CollectKycRequestModel.serializer()
    ) {
        override fun transformSerialize(element: JsonElement): JsonElement {
            val json = element.jsonObject
            val kycFields = json["kycInfo"]?.jsonObject ?: JsonObject(emptyMap())
            val creds = json["credentials"]?.jsonObject ?: JsonObject(emptyMap())

            val addressObj = kycFields["address"]?.jsonObject ?: JsonObject(emptyMap())
            val otherKycFields = kycFields.filterKeys { it != "address" }

            return buildJsonObject {
                otherKycFields.forEach { (key, value) -> put(key, value) }
                addressObj.forEach { (key, value) -> put(key, value) }

                put("credentials", creds)
            }
        }
    }
}
