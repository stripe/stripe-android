package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.viewmodels.LinkCoordinatorViewModel
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object LinkCoordinatorModule {

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
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("LinkCoordinator")

    internal const val LINK_COORDINATOR_LINK_LAUNCHER = "LinkPaymentLauncher_LinkCoordinator"
}
