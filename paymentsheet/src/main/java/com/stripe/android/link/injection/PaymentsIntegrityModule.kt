package com.stripe.android.link.injection

import android.app.Application
import com.stripe.android.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ExponentialBackoffRetryDelaySupplier
import com.stripe.attestation.DefaultIntegrityTokenProviderFactory
import com.stripe.attestation.IntegrityRequestManager
import com.stripe.attestation.IntegrityStandardRequestManager
import com.stripe.attestation.IntegrityTokenProviderWarmer
import com.stripe.attestation.RealStandardIntegrityManagerFactory
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.sync.Mutex

@Module
internal object PaymentsIntegrityModule {
    @Provides
    fun provideIntegrityRequestManager(
        context: Application
    ): IntegrityRequestManager = PaymentsIntegrityDependenciesHolder.get(context).integrityRequestManager

    @Provides
    fun provideIntegrityTokenProviderWarmer(
        context: Application
    ): IntegrityTokenProviderWarmer = PaymentsIntegrityDependenciesHolder.get(context).integrityTokenProviderWarmer
}

// Keep one shared provider across Dagger components so warmup only happens once per process.
private object PaymentsIntegrityDependenciesHolder {
    @Volatile
    private var instance: PaymentsIntegrityDependencies? = null

    fun get(application: Application): PaymentsIntegrityDependencies {
        return instance ?: synchronized(this) {
            instance ?: createDependencies(application).also { instance = it }
        }
    }

    private fun createDependencies(application: Application): PaymentsIntegrityDependencies {
        val logError: (String, Throwable) -> Unit = { message, error ->
            Logger.getInstance(BuildConfig.DEBUG).error(message, error)
        }
        val standardIntegrityManagerFactory = RealStandardIntegrityManagerFactory(application)
        val integrityTokenProviderFactory = DefaultIntegrityTokenProviderFactory(
            cloudProjectNumber = STRIPE_PAYMENTS_SDK_CLOUD_PROJECT_NUMBER,
            factory = standardIntegrityManagerFactory,
            logError = logError,
            retryDelaySupplier = ExponentialBackoffRetryDelaySupplier(),
            mutex = Mutex()
        )
        val integrityRequestManager = IntegrityStandardRequestManager(
            integrityTokenProviderFactory = integrityTokenProviderFactory,
            logError = logError
        )

        return PaymentsIntegrityDependencies(
            integrityRequestManager = integrityRequestManager,
            integrityTokenProviderWarmer = integrityTokenProviderFactory
        )
    }
}

private data class PaymentsIntegrityDependencies(
    val integrityRequestManager: IntegrityRequestManager,
    val integrityTokenProviderWarmer: IntegrityTokenProviderWarmer
)

private const val STRIPE_PAYMENTS_SDK_CLOUD_PROJECT_NUMBER = 577365562050L
