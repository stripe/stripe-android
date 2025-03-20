package com.stripe.android.financialconnections.lite.di

import com.stripe.android.core.ApiVersion
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.financialconnections.lite.network.FinancialConnectionsLiteRequestExecutor
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

internal object Di {
    private val apiVersion = ApiVersion(
        betas = setOf("financial_connections_client_api_beta=v1")
    )
    private val apiRequestFactory = ApiRequest.Factory(
        apiVersion = apiVersion.code
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val workContext = Dispatchers.IO
    val logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

    fun repository(): FinancialConnectionsLiteRepository = FinancialConnectionsLiteRepositoryImpl(
        requestExecutor = FinancialConnectionsLiteRequestExecutor(
            stripeNetworkClient = DefaultStripeNetworkClient(
                workContext = workContext,
                logger = logger
            ),
            json = json,
            logger = logger
        ),
        apiRequestFactory = apiRequestFactory
    )
}
