package com.stripe.android.financialconnections.di

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Singleton

@Module
internal object FinancialConnectionsSheetModule {

    @Singleton
    @Provides
    fun providesFinancialConnectionsManifestRepository(
        requestExecutor: FinancialConnectionsRequestExecutor,
        apiRequestFactory: ApiRequest.Factory,
        apiOptions: ApiRequest.Options,
        locale: Locale?,
        logger: Logger
    ) = FinancialConnectionsManifestRepository(
        requestExecutor = requestExecutor,
        apiRequestFactory = apiRequestFactory,
        apiOptions = apiOptions,
        logger = logger,
        locale = locale ?: Locale.getDefault(),
        initialSync = null
    )
}
