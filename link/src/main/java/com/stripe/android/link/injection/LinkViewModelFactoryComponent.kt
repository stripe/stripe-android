package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.ConfirmCallback
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.ui.cardedit.CardEditViewModel
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
        LinkActivityContractArgsModule::class,
        LinkCommonModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class
    ]
)
internal abstract class LinkViewModelFactoryComponent : NonFallbackInjector {
    abstract fun inject(factory: LinkActivityViewModel.Factory)
    abstract fun inject(factory: SignUpViewModel.Factory)
    abstract fun inject(factory: VerificationViewModel.Factory)
    abstract fun inject(factory: WalletViewModel.Factory)
    abstract fun inject(factory: PaymentMethodViewModel.Factory)
    abstract fun inject(factory: CardEditViewModel.Factory)

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is LinkActivityViewModel.Factory -> inject(injectable)
            is SignUpViewModel.Factory -> inject(injectable)
            is VerificationViewModel.Factory -> inject(injectable)
            is WalletViewModel.Factory -> inject(injectable)
            is PaymentMethodViewModel.Factory -> inject(injectable)
            is CardEditViewModel.Factory -> inject(injectable)
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }

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

        @BindsInstance
        fun confirmCallback(confirmCallback: ConfirmCallback?): Builder

        fun build(): LinkViewModelFactoryComponent
    }
}
