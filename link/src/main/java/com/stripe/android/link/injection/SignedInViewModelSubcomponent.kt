package com.stripe.android.link.injection

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.cardedit.CardEditViewModel
import com.stripe.android.link.ui.paymentmethod.PaymentMethodViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Subcomponent used by ViewModels that require a [LinkAccount].
 */
@Subcomponent
internal interface SignedInViewModelSubcomponent {
    val verificationViewModel: VerificationViewModel
    val walletViewModel: WalletViewModel
    val paymentMethodViewModel: PaymentMethodViewModel
    val cardEditViewModel: CardEditViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun linkAccount(linkAccount: LinkAccount): Builder

        fun build(): SignedInViewModelSubcomponent
    }
}
