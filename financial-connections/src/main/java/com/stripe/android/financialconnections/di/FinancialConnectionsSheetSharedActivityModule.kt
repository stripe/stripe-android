package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.DefaultFinancialConnectionsEventReporter
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.repository.FinancialConnectionsApiRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named

/**
 * Dependencies shared between activities that can't be app-scoped and need to be recreated
 * across activities.
 *
 * Components that depend on user-provided configuration should be here so that they can be
 * recovered after a process kill. The typical component recreation flow would be:
 *
 * 1. user-provided config field is saved on[android.app.Activity.onSaveInstanceState]
 * 2. field is recovered in [android.app.Activity.onRestoreInstanceState]
 * 3. field is passed as a bound instance to the component using this module.
 * 4. component gets recreated.
 *
 * Each activity implementing this module should be responsible for recovering from process kills
 * saving and restoring user-provided configuration dependencies.
 */
@Module
internal object FinancialConnectionsSheetSharedActivityModule {

    @Provides
    @Named(PUBLISHABLE_KEY)
    @ActivityScoped
    fun providesPublishableKey(
        configuration: FinancialConnectionsSheet.Configuration
    ): String = configuration.publishableKey

    @Provides
    @ActivityScoped
    fun provideConnectionsRepository(
        repository: FinancialConnectionsApiRepository
    ): FinancialConnectionsRepository = repository

    @Provides
    @ActivityScoped
    fun provideEventReporter(
        defaultFinancialConnectionsEventReporter: DefaultFinancialConnectionsEventReporter
    ): FinancialConnectionsEventReporter = defaultFinancialConnectionsEventReporter

    @Provides
    @ActivityScoped
    internal fun providesAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor = executor

    @Provides
    @ActivityScoped
    internal fun provideAnalyticsRequestFactory(
        application: Application,
        @Named(PUBLISHABLE_KEY) publishableKey: String
    ): AnalyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageInfo,
        publishableKeyProvider = { publishableKey }
    )
}
