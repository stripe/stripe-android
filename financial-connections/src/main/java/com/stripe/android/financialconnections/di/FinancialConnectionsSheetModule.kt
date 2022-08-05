package com.stripe.android.financialconnections.di

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object FinancialConnectionsSheetModule {

    @Singleton
    @Provides
    fun providesFinancialConnectionsManifestRepository(
        @Named(PUBLISHABLE_KEY) publishableKey: String,
        requestExecutor: FinancialConnectionsRequestExecutor,
        configuration: FinancialConnectionsSheet.Configuration,
        apiRequestFactory: ApiRequest.Factory,
        logger: Logger
    ) = FinancialConnectionsManifestRepository(
        publishableKey = publishableKey,
        requestExecutor = requestExecutor,
        configuration = configuration,
        apiRequestFactory = apiRequestFactory,
        logger = logger,
        initialManifest = null
    )
}
