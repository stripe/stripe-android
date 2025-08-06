package com.stripe.android.paymentelement.confirmation.epms

import com.stripe.android.elements.payment.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface ExternalPaymentMethodConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsExternalPaymentMethodConfirmationDefinition(
        definition: ExternalPaymentMethodConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun providesExternalPaymentMethodConfirmHandler(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): ExternalPaymentMethodConfirmHandler? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.externalPaymentMethodConfirmHandler
        }
    }
}
