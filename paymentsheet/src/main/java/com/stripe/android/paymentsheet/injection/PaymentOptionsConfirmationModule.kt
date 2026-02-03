package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentelement.confirmation.injection.DefaultConfirmationModule
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationModule
import com.stripe.android.paymentelement.confirmation.taptoadd.TapToAddConfirmationModule
import dagger.Module

@Module(
    includes = [
        DefaultConfirmationModule::class,
        DefaultIntentConfirmationModule::class,
        TapToAddConfirmationModule::class,
    ]
)
internal interface PaymentOptionsConfirmationModule
