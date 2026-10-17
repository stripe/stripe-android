package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.cardart.PaymentOptionCardArtPrefetchConfirmationModule
import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationModule
import dagger.Module

@Module(
    includes = [
        CvcRecollectionConfirmationModule::class,
        PaymentOptionCardArtPrefetchConfirmationModule::class,
        PaymentElementConfirmationModule::class,
    ]
)
internal interface ExtendedPaymentElementConfirmationModule
