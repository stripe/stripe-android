package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.Stripe
import com.stripe.android.common.analytics.experiment.DefaultLogLinkGlobalHoldbackExposure
import com.stripe.android.common.analytics.experiment.LogLinkGlobalHoldbackExposure
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.repository.ConsumersApiServiceImpl
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Named
import javax.inject.Qualifier
import kotlin.coroutines.CoroutineContext

/**
 * Currently, Link related dependencies are provided via [com.stripe.android.link.injection.LinkModule].
 * Its associated [com.stripe.android.link.injection.LinkComponent] is retrieved
 * via [com.stripe.android.link.LinkConfigurationCoordinator].
 *
 * This coordinator requires a [com.stripe.android.link.LinkConfiguration] object to be passed in.
 * For scenarios where Link is disabled and no configuration is available but we still need to make Link
 * requests (mainly for the Link Global Holdback) we need to provide a configuration-less [LinkApiRepository].
 *
 *  Qualifier to distinguish between the [LinkApiRepository] provided by
 *  the [com.stripe.android.link.LinkConfigurationCoordinator] and the one provided by the [LinkHoldbackExposureModule].
 */
@Qualifier
internal annotation class LinkDisabledApiRepository

@Module
internal class LinkHoldbackExposureModule {

    @Provides
    fun providesLogLinkGlobalHoldbackExposure(
        default: DefaultLogLinkGlobalHoldbackExposure
    ): LogLinkGlobalHoldbackExposure {
        return default
    }

    @Provides
    @LinkDisabledApiRepository
    fun providesLinkRepository(
        application: Application,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
        stripeRepository: StripeRepository,
        @IOContext workContext: CoroutineContext,
        logger: Logger,
        locale: Locale?,
        errorReporter: ErrorReporter,
    ): LinkRepository {
        val consumersApiService = ConsumersApiServiceImpl(
            appInfo = Stripe.appInfo,
            sdkVersion = StripeSdkVersion.VERSION,
            apiVersion = Stripe.API_VERSION,
            stripeNetworkClient = DefaultStripeNetworkClient(
                logger = logger,
                workContext = workContext
            )
        )
        return LinkApiRepository(
            application,
            publishableKeyProvider,
            stripeAccountIdProvider,
            stripeRepository,
            consumersApiService,
            workContext,
            locale,
            errorReporter,
        )
    }
}
