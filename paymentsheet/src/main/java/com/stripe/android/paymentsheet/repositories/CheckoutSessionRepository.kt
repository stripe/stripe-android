package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

internal interface CheckoutSessionRepository {
    suspend fun init(
        sessionId: String,
        options: ApiRequest.Options,
    ): Result<CheckoutSessionResponse>

    suspend fun confirm(
        id: String,
        params: ConfirmCheckoutSessionParams,
        options: ApiRequest.Options,
    ): Result<CheckoutSessionResponse>
}

internal class DefaultCheckoutSessionRepository(
    private val stripeNetworkClient: StripeNetworkClient,
    apiVersion: String,
    sdkVersion: String = StripeSdkVersion.VERSION,
    appInfo: AppInfo?,
) : CheckoutSessionRepository {
    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion,
    )
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    override suspend fun init(
        sessionId: String,
        options: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = initUrl(sessionId),
                options = options,
                params = mapOf(
                    "browser_locale" to Locale.getDefault().toLanguageTag(),
                    "browser_timezone" to TimeZone.getDefault().id,
                    "eid" to UUID.randomUUID().toString(),
                    "redirect_type" to "embedded",
                    "elements_session_client[is_aggregation_expected]" to "true",
                ),
            ),
            responseJsonParser = CheckoutSessionResponseJsonParser(
                isLiveMode = options.apiKeyIsLiveMode,
            ),
        )
    }

    override suspend fun confirm(
        id: String,
        params: ConfirmCheckoutSessionParams,
        options: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = confirmUrl(id),
                options = options,
                params = params.toParamMap(),
            ),
            responseJsonParser = CheckoutSessionResponseJsonParser(
                isLiveMode = options.apiKeyIsLiveMode,
            ),
        )
    }

    private companion object {
        private fun initUrl(sessionId: String): String =
            "${ApiRequest.API_HOST}/v1/payment_pages/$sessionId/init"

        private fun confirmUrl(checkoutSessionId: String): String =
            "${ApiRequest.API_HOST}/v1/payment_pages/$checkoutSessionId/confirm"
    }
}
