package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.ui.paymentmethod.PaymentMethodViewModel
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

/**
 * Component to inject the ViewModels Factory when the app process is killed and there is no
 * Injector available.
 * This component will create new instances of all dependencies.
 */
@Singleton
@Component(
    modules = [
        LinkPaymentLauncherModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        LoggingModule::class,
        ResourceRepositoryModule::class
    ]
)
internal interface LinkViewModelFactoryComponent {
    fun inject(factory: LinkActivityViewModel.Factory)
    fun inject(factory: SignUpViewModel.Factory)
    fun inject(factory: VerificationViewModel.Factory)
    fun inject(factory: WalletViewModel.Factory)
    fun inject(factory: PaymentMethodViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

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

        fun build(): LinkViewModelFactoryComponent
    }
}
