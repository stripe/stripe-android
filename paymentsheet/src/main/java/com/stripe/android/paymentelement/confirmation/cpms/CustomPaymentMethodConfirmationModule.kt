package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.Identifiable
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface CustomPaymentMethodConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsCustomPaymentMethodConfirmationDefinition(
        customPaymentMethodConfirmationDefinition: CustomPaymentMethodConfirmationDefinition,
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun provideConfirmCustomPaymentMethodCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: Identifiable,
        ): ConfirmCustomPaymentMethodCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.confirmCustomPaymentMethodCallback
        }
    }
}
