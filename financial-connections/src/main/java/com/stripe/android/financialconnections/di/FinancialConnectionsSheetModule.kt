package com.stripe.android.financialconnections.di

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions
import dagger.Module
import dagger.Provides
import java.util.Locale

@Module
internal object FinancialConnectionsSheetModule {

    @ActivityRetainedScope
    @Provides
    internal fun providesProvideApiRequestOptions(
        apiRequestOptions: ApiRequest.Options,
    ): ProvideApiRequestOptions {
        // We don't need to use any consumer publishable key here
        return ProvideApiRequestOptions { apiRequestOptions }
    }

    @ActivityRetainedScope
    @Provides
    fun providesFinancialConnectionsManifestRepository(
        requestExecutor: FinancialConnectionsRequestExecutor,
        apiRequestFactory: ApiRequest.Factory,
        provideApiRequestOptions: ProvideApiRequestOptions,
        locale: Locale?,
        logger: Logger
    ) = FinancialConnectionsManifestRepository(
        requestExecutor = requestExecutor,
        apiRequestFactory = apiRequestFactory,
        provideApiRequestOptions = provideApiRequestOptions,
        logger = logger,
        locale = locale ?: Locale.getDefault(),
        initialSync = null
    )
}
