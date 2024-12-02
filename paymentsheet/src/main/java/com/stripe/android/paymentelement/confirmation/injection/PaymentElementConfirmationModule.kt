package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationModule
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationModule
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationModule
import dagger.Module

@Module(
    includes = [
        DefaultConfirmationModule::class,
        BacsConfirmationModule::class,
        ExternalPaymentMethodConfirmationModule::class,
        GooglePayConfirmationModule::class,
    ]
)
internal interface PaymentElementConfirmationModule
