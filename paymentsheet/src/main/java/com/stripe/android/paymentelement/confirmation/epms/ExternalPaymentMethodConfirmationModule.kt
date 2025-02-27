package com.stripe.android.paymentelement.confirmation.epms

import com.stripe.android.paymentelement.callbacks.PAYMENT_ELEMENT_CALLBACK_INSTANCE_ID
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackStorage
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Named

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
            @Named(PAYMENT_ELEMENT_CALLBACK_INSTANCE_ID) instanceId: String,
        ): ExternalPaymentMethodConfirmHandler? {
            return PaymentElementCallbackStorage[instanceId]?.externalPaymentMethodConfirmHandler
        }
    }
}
