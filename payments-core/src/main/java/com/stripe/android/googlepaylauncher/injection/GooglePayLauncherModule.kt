package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.Logger
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Module
import dagger.Provides

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayLauncherModule {

    // At the point at which the DefaultFlowControllerInitializer is created the configuration
    // is not known.
    @Provides
    fun provideGooglePayRepositoryFactory(
        appContext: Context,
        logger: Logger,
        errorReporter: ErrorReporter,
    ): (GooglePayEnvironment) -> GooglePayRepository = { environment ->
        DefaultGooglePayRepository(
            appContext,
            environment,
            GooglePayJsonFactory.BillingAddressParameters(),
            existingPaymentMethodRequired = true,
            allowCreditCards = true,
            errorReporter = errorReporter,
            logger = logger
        )
    }
}
