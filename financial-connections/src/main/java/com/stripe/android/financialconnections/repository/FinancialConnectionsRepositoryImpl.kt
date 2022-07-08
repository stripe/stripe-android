package com.stripe.android.financialconnections.repository

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.ApiRequest.Companion.API_HOST
import com.stripe.android.financialconnections.di.PUBLISHABLE_KEY
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_APPLICATION_ID
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CLIENT_SECRET
import javax.inject.Inject
import javax.inject.Named

internal class FinancialConnectionsRepositoryImpl @Inject constructor(
    @Named(PUBLISHABLE_KEY) publishableKey: String,
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsRepository {

    private val options = ApiRequest.Options(
        apiKey = publishableKey
    )

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = listAccountsUrl,
            options = options,
            params = getFinancialConnectionsAcccountsParams.toParamMap()
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsAccountList.serializer()
        )
    }

    override suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = sessionReceiptUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSession.serializer()
        )
    }

    override suspend fun generateFinancialConnectionsSessionManifest(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = generateHostedUrl,
            options = options,
            params = mapOf(
                PARAMS_FULLSCREEN to true,
                PARAMS_HIDE_CLOSE_BUTTON to true,
                PARAMS_CLIENT_SECRET to clientSecret,
                PARAMS_APPLICATION_ID to applicationId
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        )
    }

    override suspend fun getFinancialConnectionsSessionManifest(
        clientSecret: String
    ): FinancialConnectionsSessionManifest {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = getManifestUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        )
    }

    override suspend fun markConsentAcquired(
        clientSecret: String
    ): FinancialConnectionsSessionManifest {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = consentAcquiredUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        )
    }

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        institutionId: String
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = authorizationSessionUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                "use_mobile_handoff" to false,
                "institution" to institutionId
            )
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsAuthorizationSession.serializer()
        )
    }

    internal companion object {
        internal const val PARAMS_FULLSCREEN = "fullscreen"
        internal const val PARAMS_HIDE_CLOSE_BUTTON = "hide_close_button"

        internal const val listAccountsUrl: String =
            "$API_HOST/v1/link_account_sessions/list_accounts"

        internal const val consentAcquiredUrl: String =
            "$API_HOST/v1/link_account_sessions/consent_acquired"

        internal const val sessionReceiptUrl: String =
            "$API_HOST/v1/link_account_sessions/session_receipt"

        internal const val generateHostedUrl: String =
            "$API_HOST/v1/link_account_sessions/generate_hosted_url"

        internal const val getManifestUrl: String =
            "$API_HOST/v1/link_account_sessions/manifest"

        internal const val institutionsUrl: String =
            "$API_HOST/v1/connections/institutions"

        internal const val authorizationSessionUrl: String =
            "$API_HOST/v1/connections/auth_sessions"

        internal const val featuredInstitutionsUrl: String =
            "$API_HOST/v1/connections/featured_institutions"
    }
}
