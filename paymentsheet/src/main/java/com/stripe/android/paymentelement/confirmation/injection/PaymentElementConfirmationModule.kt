package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationModule
import com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodConfirmationModule
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationModule
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationModule
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationModule
import com.stripe.android.paymentelement.confirmation.linkinline.LinkInlineSignupConfirmationModule
import dagger.Module

@Module(
    includes = [
        DefaultConfirmationModule::class,
        BacsConfirmationModule::class,
        ExternalPaymentMethodConfirmationModule::class,
        CustomPaymentMethodConfirmationModule::class,
        GooglePayConfirmationModule::class,
        LinkConfirmationModule::class,
        LinkInlineSignupConfirmationModule::class,
    ]
)
internal interface PaymentElementConfirmationModule
