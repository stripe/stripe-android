package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.ui.cardedit.CardEditViewModel
import com.stripe.android.link.ui.paymentmethod.PaymentMethodViewModel
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Component that holds the dependency graph for the Link-owned user experience.
 */
@Subcomponent(
    modules = [
        LinkActivityContractArgsModule::class
    ]
)
internal abstract class LinkComponent {
    abstract fun inject(factory: LinkActivityViewModel.Factory)
    abstract fun inject(factory: SignUpViewModel.Factory)
    abstract fun inject(factory: VerificationViewModel.Factory)
    abstract fun inject(factory: WalletViewModel.Factory)
    abstract fun inject(factory: PaymentMethodViewModel.Factory)
    abstract fun inject(factory: CardEditViewModel.Factory)

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun starterArgs(starterArgs: LinkActivityContract.Args): Builder

        fun build(): LinkComponent
    }
}
