package com.stripe.onramp.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.onramp.model.KycInfo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal interface CryptoApiService {

    suspend fun submitKycInfo(
        kycInfo: KycInfo,
        requestOptions: ApiRequest.Options
    ): Result<KycSubmissionResponse>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal class CryptoApiServiceImpl(
    private val stripeNetworkClient: StripeNetworkClient,
    apiVersion: String,
    sdkVersion: String = StripeSdkVersion.VERSION,
    appInfo: AppInfo?
) : CryptoApiService {

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun submitKycInfo(
        kycInfo: KycInfo,
        requestOptions: ApiRequest.Options
    ): kotlin.Result<KycSubmissionResponse> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = submitKycUrl,
                options = requestOptions,
                params = kycInfo.toParamMap()
            ),
            responseJsonParser = KycSubmissionResponse.JsonParser,
        )
    }

    private fun KycInfo.toParamMap(): Map<String, Any?> {
        return mapOf(
            "first_name" to firstName,
            "last_name" to lastName,
            "date_of_birth" to dateOfBirth,
            "address" to address.toParamMap(),
            "ssn" to ssn,
            "government_id" to governmentId?.toParamMap()
        ).filterValues { it != null }
    }

    private fun PaymentSheet.Address.toParamMap(): Map<String, Any?> {
        return mapOf(
            "line1" to line1,
            "line2" to line2,
            "city" to city,
            "state" to state,
            "postal_code" to postalCode,
            "country" to country
        ).filterValues { it != null }
    }

    private fun KycInfo.GovernmentId.toParamMap(): Map<String, Any?> {
        return mapOf(
            "type" to type.name.lowercase(),
            "number" to number,
            "expiration_date" to expirationDate
        ).filterValues { it != null }
    }

    internal companion object {
        /**
         * @return `https://api.stripe.com/v1/crypto/kyc/submit`
         */
        internal val submitKycUrl: String = getApiUrl("crypto/kyc/submit")

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
