package com.stripe.android.financialconnections.lite.repository

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.lite.network.FinancialConnectionsLiteRequestExecutor
import com.stripe.android.financialconnections.lite.repository.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import java.util.Locale

internal interface FinancialConnectionsLiteRepository {
    suspend fun synchronize(
        configuration: FinancialConnectionsSheetConfiguration,
        applicationId: String
    ): Result<SynchronizeSessionResponse>

    suspend fun getFinancialConnectionsSession(
        configuration: FinancialConnectionsSheetConfiguration
    ): Result<FinancialConnectionsSession>
}

internal class FinancialConnectionsLiteRepositoryImpl(
    private val requestExecutor: FinancialConnectionsLiteRequestExecutor,
    private val apiRequestFactory: ApiRequest.Factory,
) : FinancialConnectionsLiteRepository {

    fun FinancialConnectionsSheetConfiguration.apiRequestOptions() = ApiRequest.Options(
        publishableKeyProvider = { publishableKey },
        stripeAccountIdProvider = { stripeAccountId },
    )

    override suspend fun synchronize(
        configuration: FinancialConnectionsSheetConfiguration,
        applicationId: String,
    ): Result<SynchronizeSessionResponse> = requestExecutor.execute(
        apiRequestFactory.createPost(
            url = synchronizeSessionUrl,
            options = configuration.apiRequestOptions(),
            params = mapOf(
                "locale" to Locale.getDefault().toLanguageTag(),
                "mobile" to mapOf(
                    PARAMS_FULLSCREEN to true,
                    PARAMS_HIDE_CLOSE_BUTTON to false,
                    PARAMS_APPLICATION_ID to applicationId,
                    PARAMS_MOBILE_SDK_TYPE to "fc_lite"
                ),
                PARAMS_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret
            )
        ),
        SynchronizeSessionResponse.serializer()
    )

    override suspend fun getFinancialConnectionsSession(
        configuration: FinancialConnectionsSheetConfiguration,
    ): Result<FinancialConnectionsSession> {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = sessionReceiptUrl,
            options = configuration.apiRequestOptions(),
            params = mapOf(
                PARAMS_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSession.serializer()
        )
    }

    companion object {
        internal const val PARAMS_FULLSCREEN = "fullscreen"
        internal const val PARAMS_HIDE_CLOSE_BUTTON = "hide_close_button"
        internal const val PARAMS_APPLICATION_ID = "application_id"
        internal const val PARAMS_MOBILE_SDK_TYPE = "mobile_sdk_type"
        internal const val PARAMS_CLIENT_SECRET = "client_secret"

        internal const val synchronizeSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/financial_connections/sessions/synchronize"

        private const val sessionReceiptUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/session_receipt"
    }
}
