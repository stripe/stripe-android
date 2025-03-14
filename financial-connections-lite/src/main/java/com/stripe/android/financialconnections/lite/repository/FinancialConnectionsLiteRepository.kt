package com.stripe.android.financialconnections.lite.repository

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.lite.network.FinancialConnectionsLiteRequestExecutor
import com.stripe.android.financialconnections.lite.repository.model.SynchronizeSessionResponse
import java.util.Locale

internal class FinancialConnectionsLiteRepository(
    private val requestExecutor: FinancialConnectionsLiteRequestExecutor,
    private val apiRequestFactory: ApiRequest.Factory,
) {

    private fun FinancialConnectionsSheetConfiguration.apiRequestOptions() = ApiRequest.Options(
        publishableKeyProvider = { publishableKey },
        stripeAccountIdProvider = { stripeAccountId },
    )

    suspend fun synchronize(
        configuration: FinancialConnectionsSheetConfiguration,
        applicationId: String,
    ): Result<SynchronizeSessionResponse> {
        val request = apiRequestFactory.createPost(
            url = synchronizeSessionUrl,
            options = configuration.apiRequestOptions(),
            params = mapOf(
                "locale" to Locale.getDefault().toLanguageTag(),
                "mobile" to mapOf(
                    PARAMS_FULLSCREEN to true,
                    PARAMS_HIDE_CLOSE_BUTTON to false,
                    PARAMS_APPLICATION_ID to applicationId
                ),
                PARAMS_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret
            )
        )
        return requestExecutor.execute(request, SynchronizeSessionResponse.serializer())
    }

    companion object {
        internal const val PARAMS_FULLSCREEN = "fullscreen"
        internal const val PARAMS_HIDE_CLOSE_BUTTON = "hide_close_button"
        internal const val PARAMS_APPLICATION_ID = "application_id"
        internal const val PARAMS_CLIENT_SECRET = "client_secret"

        internal const val synchronizeSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/financial_connections/sessions/synchronize"
    }
}
