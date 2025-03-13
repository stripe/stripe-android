package com.stripe.android.financialconnections.lite.di

import com.stripe.android.core.ApiVersion
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.financialconnections.lite.network.FinancialConnectionsLiteRequestExecutor
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import kotlinx.serialization.json.Json

internal object Di {
    val logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

    val apiVersion = ApiVersion(
        betas = setOf("financial_connections_client_api_beta=v1")
    )
    val apiRequestFactory = ApiRequest.Factory(
        apiVersion = apiVersion.code
    )

    val json = Json {
        ignoreUnknownKeys = true
    }

    fun repository(): FinancialConnectionsLiteRepository = FinancialConnectionsLiteRepository(
        requestExecutor = FinancialConnectionsLiteRequestExecutor(
            stripeNetworkClient = DefaultStripeNetworkClient(logger = logger),
            json = json,
            logger = logger
        ),
        apiRequestFactory = apiRequestFactory
    )
}
