package com.stripe.android.link.injection

import android.app.Application
import com.stripe.android.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.attestation.IntegrityRequestManager
import com.stripe.attestation.IntegrityStandardRequestManager
import com.stripe.attestation.RealStandardIntegrityManagerFactory
import dagger.Module
import dagger.Provides

@Module
internal object PaymentsIntegrityModule {
    @Provides
    fun provideIntegrityRequestManager(
        context: Application
    ): IntegrityRequestManager = createIntegrityStandardRequestManager(context)
}

// Keep a single instance across the app so prepare() only happens once per process.
private object LinkIntegrityRequestManagerHolder {
    @Volatile
    private var instance: IntegrityRequestManager? = null

    fun get(application: Application): IntegrityRequestManager {
        return instance ?: synchronized(this) {
            instance ?: IntegrityStandardRequestManager(
                cloudProjectNumber = 577365562050, // stripe-payments-sdk-prod
                logError = { message, error ->
                    Logger.getInstance(BuildConfig.DEBUG).error(message, error)
                },
                factory = RealStandardIntegrityManagerFactory(application)
            ).also { instance = it }
        }
    }
}

private fun createIntegrityStandardRequestManager(
    context: Application
): IntegrityRequestManager = LinkIntegrityRequestManagerHolder.get(context)
