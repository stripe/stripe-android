package com.stripe.android.paymentelement.confirmation.shoppay

import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.WalletConfiguration
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface ShopPayConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsShopPayConfirmationDefinition(
        definition: ShopPayConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun providesWalletHandlers(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): WalletConfiguration.Handlers? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.walletHandlers
                ?: WalletConfiguration.Handlers()
        }
    }
}