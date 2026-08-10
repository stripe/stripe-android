package com.stripe.android.checkout.injection

import com.stripe.android.paymentelement.confirmation.cardart.PaymentOptionCardArtPrefetchConfirmationModule
import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationModule
import dagger.Module

@Module(
    includes = [
        CvcRecollectionConfirmationModule::class,
        PaymentOptionCardArtPrefetchConfirmationModule::class,
        CheckoutPaymentElementConfirmationModule::class,
    ],
)
internal interface CheckoutExtendedPaymentElementConfirmationModule
