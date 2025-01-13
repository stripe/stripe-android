package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationModule
import dagger.Module

@Module(
    includes = [
        CvcRecollectionConfirmationModule::class,
        PaymentElementConfirmationModule::class,
    ]
)
internal interface ExtendedPaymentElementConfirmationModule
