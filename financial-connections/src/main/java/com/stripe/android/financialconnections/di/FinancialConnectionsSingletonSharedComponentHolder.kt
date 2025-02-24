package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.IntegrityVerdictManager
import com.stripe.attestation.BuildConfig
import com.stripe.attestation.IntegrityRequestManager
import com.stripe.attestation.IntegrityStandardRequestManager
import com.stripe.attestation.RealStandardIntegrityManagerFactory
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
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
        context: Application
    ): IntegrityRequestManager = IntegrityStandardRequestManager(
        cloudProjectNumber = 527113280969, // stripe-financial-connections
        logError = { message, error -> Logger.getInstance(BuildConfig.DEBUG).error(message, error) },
        factory = RealStandardIntegrityManagerFactory(context)
    )

    @Provides
    @Singleton
    fun providesIntegrityVerdictManager(): IntegrityVerdictManager = IntegrityVerdictManager()
}
