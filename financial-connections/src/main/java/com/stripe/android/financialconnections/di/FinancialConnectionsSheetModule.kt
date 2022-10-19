package com.stripe.android.financialconnections.di

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal object FinancialConnectionsSheetModule {

    @Singleton
    @Provides
    fun providesFinancialConnectionsManifestRepository(
        requestExecutor: FinancialConnectionsRequestExecutor,
        apiRequestFactory: ApiRequest.Factory,
        apiOptions: ApiRequest.Options,
        logger: Logger
    ) = FinancialConnectionsManifestRepository(
        requestExecutor = requestExecutor,
        apiRequestFactory = apiRequestFactory,
        apiOptions = apiOptions,
        logger = logger,
        initialSync = null
    )
}
