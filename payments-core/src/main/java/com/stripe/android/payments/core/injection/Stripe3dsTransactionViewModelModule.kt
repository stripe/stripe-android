package com.stripe.android.payments.core.injection

import android.app.Application
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2TransactionContract
import com.stripe.android.stripe3ds2.transaction.InitChallengeRepositoryFactory
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module
internal class Stripe3dsTransactionViewModelModule {
    @Provides
    fun providesInitChallengeRepository(
        application: Application,
        args: Stripe3ds2TransactionContract.Args,
        @IOContext workContext: CoroutineContext
    ) = InitChallengeRepositoryFactory(
        application,
        args.stripeIntent.isLiveMode,
        args.sdkTransactionId,
        args.config.uiCustomization.uiCustomization,
        args.fingerprint.directoryServerEncryption.rootCerts,
        args.enableLogging,
        workContext
    ).create()
}
