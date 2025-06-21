package com.stripe.onramp

import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.Stripe
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.onramp.repositories.CryptoApiRepository
import com.stripe.onramp.repositories.CryptoApiService
import com.stripe.onramp.repositories.CryptoApiServiceImpl
import com.stripe.onramp.repositories.CryptoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module
internal object OnRampCoordinatorModule {

    @Provides
    fun provideAppContext(application: Application): Context = application.applicationContext

    @Provides
    fun provideCoroutineScope(lifecycleOwner: LifecycleOwner): CoroutineScope {
        return lifecycleOwner.lifecycleScope
    }

    @Provides
    fun provideViewModelStoreOwner(lifecycleOwner: LifecycleOwner): ViewModelStoreOwner {
        return lifecycleOwner as ViewModelStoreOwner
    }

    @Provides
    @Named(LINK_COORDINATOR_LINK_LAUNCHER)
    fun provideLinkCoordinatorLinkLauncher(
        linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
        linkActivityContract: LinkActivityContract,
        @PaymentElementCallbackIdentifier identifier: String,
        linkStore: LinkStore,
    ) = LinkPaymentLauncher(
        linkAnalyticsComponentBuilder,
        identifier,
        linkActivityContract,
        linkStore,
    )

    @Provides
    fun provideStripeImageLoader(context: Context): StripeImageLoader {
        return StripeImageLoader(context)
    }

    @Provides
    fun provideCryptoApiService(
        logger: Logger,
        @IOContext workContext: CoroutineContext,
    ): CryptoApiService = CryptoApiServiceImpl(
        appInfo = Stripe.appInfo,
        sdkVersion = StripeSdkVersion.VERSION,
        apiVersion = Stripe.API_VERSION,
        stripeNetworkClient = DefaultStripeNetworkClient(
            logger = logger,
            workContext = workContext
        )
    )

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("LinkCoordinator")

    internal const val LINK_COORDINATOR_LINK_LAUNCHER = "LinkPaymentLauncher_LinkCoordinator"
}

@Module
internal abstract class OnRampRepositoryModule {

    @Binds
    abstract fun bindCryptoRepository(
        cryptoApiRepository: CryptoApiRepository
    ): CryptoRepository
}
