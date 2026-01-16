package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.googlepaylauncher.GooglePayDynamicUpdateHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface GooglePayConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsGooglePayConfirmationDefinition(
        definition: GooglePayConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun providesGooglePayDynamicUpdateHandler(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): GooglePayDynamicUpdateHandler? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.googlePayDynamicUpdateHandler
        }
    }
}
