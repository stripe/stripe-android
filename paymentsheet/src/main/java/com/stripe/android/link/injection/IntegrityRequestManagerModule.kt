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
object IntegrityRequestManagerModule {
    @Provides
    fun providesIntegrityStandardRequestManager(
        context: Application
    ): IntegrityRequestManager = IntegrityStandardRequestManager(
        cloudProjectNumber = 577365562050, // stripe-payments-sdk-prod
        logError = { message, error ->
            Logger.getInstance(BuildConfig.DEBUG).error(message, error)
        },
        factory = RealStandardIntegrityManagerFactory(context)
    )
}
