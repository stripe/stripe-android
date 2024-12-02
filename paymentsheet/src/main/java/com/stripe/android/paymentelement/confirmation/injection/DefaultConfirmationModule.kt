package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationModule
import dagger.Module

@Module(
    includes = [
        IntentConfirmationModule::class,
        ConfirmationHandlerModule::class,
    ]
)
internal interface DefaultConfirmationModule
