package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.ui.inline.InlineSignupViewModel
import com.stripe.android.link.ui.paymentmethod.PaymentMethodViewModel
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Component that holds the dependency graph for the lifecycle of LinkPaymentLauncher and related
 * classes.
 */
@Singleton
@Component(
    modules = [
        LinkPaymentLauncherModule::class,
        LoggingModule::class
    ]
)
internal abstract class LinkPaymentLauncherComponent {
    abstract val linkAccountManager: LinkAccountManager

    abstract fun inject(factory: LinkActivityViewModel.Factory)
    abstract fun inject(factory: SignUpViewModel.Factory)
    abstract fun inject(factory: VerificationViewModel.Factory)
    abstract fun inject(factory: WalletViewModel.Factory)
    abstract fun inject(factory: InlineSignupViewModel.Factory)
    abstract fun inject(factory: PaymentMethodViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun merchantName(@Named(MERCHANT_NAME) merchantName: String): Builder

        @BindsInstance
        fun customerEmail(@Named(CUSTOMER_EMAIL) customerEmail: String?): Builder

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun ioContext(@IOContext workContext: CoroutineContext): Builder

        @BindsInstance
        fun uiContext(@UIContext uiContext: CoroutineContext): Builder

        @BindsInstance
        fun analyticsRequestFactory(paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory): Builder

        @BindsInstance
        fun analyticsRequestExecutor(analyticsRequestExecutor: AnalyticsRequestExecutor): Builder

        @BindsInstance
        fun stripeRepository(stripeRepository: StripeRepository): Builder

        @BindsInstance
        fun resourceRepository(resourceRepository: ResourceRepository): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        @BindsInstance
        fun starterArgs(starterArgs: LinkActivityContract.Args): Builder

        fun build(): LinkPaymentLauncherComponent
    }
}
