package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationModule
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationModule
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationModule
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationModule
import com.stripe.android.paymentelement.confirmation.linkexpress.LinkExpressConfirmationModule
import dagger.Module

@Module(
    includes = [
        DefaultConfirmationModule::class,
        BacsConfirmationModule::class,
        ExternalPaymentMethodConfirmationModule::class,
        GooglePayConfirmationModule::class,
        LinkConfirmationModule::class,
        LinkExpressConfirmationModule::class
    ]
)
internal interface PaymentElementConfirmationModule
