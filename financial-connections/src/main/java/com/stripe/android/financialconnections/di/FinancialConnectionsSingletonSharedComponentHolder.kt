package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ExponentialBackoffRetryDelaySupplier
import com.stripe.android.financialconnections.BuildConfig
import com.stripe.android.financialconnections.domain.IntegrityVerdictManager
import com.stripe.attestation.DefaultIntegrityTokenProviderFactory
import com.stripe.attestation.IntegrityRequestManager
import com.stripe.attestation.IntegrityStandardRequestManager
import com.stripe.attestation.IntegrityTokenProviderWarmer
import com.stripe.attestation.RealStandardIntegrityManagerFactory
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.sync.Mutex
import javax.inject.Singleton

/**
 * A singleton holder for the [FinancialConnectionsSingletonSharedComponent], ensuring it is initialized only once and
 * shared across activities.
 */
internal object FinancialConnectionsSingletonSharedComponentHolder {

    @Volatile
    private var component: FinancialConnectionsSingletonSharedComponent? = null

    fun getComponent(application: Application): FinancialConnectionsSingletonSharedComponent {
        return component ?: synchronized(this) {
            component ?: buildComponent(application).also { component = it }
        }
    }

    private fun buildComponent(application: Application): FinancialConnectionsSingletonSharedComponent {
        return DaggerFinancialConnectionsSingletonSharedComponent
            .factory()
            .create(application)
    }
}

@Singleton
@Component(modules = [FinancialConnectionsSingletonSharedModule::class])
internal interface FinancialConnectionsSingletonSharedComponent {

    fun integrityRequestManager(): IntegrityRequestManager

    fun integrityTokenProviderWarmer(): IntegrityTokenProviderWarmer

    fun integrityVerdictManager(): IntegrityVerdictManager

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: Application): FinancialConnectionsSingletonSharedComponent
    }
}

@Module
internal class FinancialConnectionsSingletonSharedModule {

    @Provides
    @Singleton
    fun providesIntegrityStandardRequestManager(
        integrityTokenProviderFactory: DefaultIntegrityTokenProviderFactory
    ): IntegrityRequestManager = IntegrityStandardRequestManager(
        integrityTokenProviderFactory = integrityTokenProviderFactory,
        logError = { message, error -> Logger.getInstance(BuildConfig.DEBUG).error(message, error) }
    )

    @Provides
    @Singleton
    fun providesIntegrityTokenProviderFactory(
        context: Application
    ): DefaultIntegrityTokenProviderFactory = DefaultIntegrityTokenProviderFactory(
        cloudProjectNumber = FINANCIAL_CONNECTIONS_CLOUD_PROJECT_NUMBER,
        factory = RealStandardIntegrityManagerFactory(context),
        logError = { message, error -> Logger.getInstance(BuildConfig.DEBUG).error(message, error) },
        retryDelaySupplier = ExponentialBackoffRetryDelaySupplier(),
        mutex = Mutex()
    )

    @Provides
    @Singleton
    fun providesIntegrityTokenProviderWarmer(
        integrityTokenProviderFactory: DefaultIntegrityTokenProviderFactory
    ): IntegrityTokenProviderWarmer = integrityTokenProviderFactory

    @Provides
    @Singleton
    fun providesIntegrityVerdictManager(): IntegrityVerdictManager = IntegrityVerdictManager()
}

private const val FINANCIAL_CONNECTIONS_CLOUD_PROJECT_NUMBER = 527113280969L
