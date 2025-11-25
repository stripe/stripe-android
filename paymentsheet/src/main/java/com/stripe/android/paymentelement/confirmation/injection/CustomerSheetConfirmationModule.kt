package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.intent.CustomerSheetIntentConfirmationModule
import dagger.Module

@Module(
    includes = [
        DefaultConfirmationModule::class,
        CustomerSheetIntentConfirmationModule::class,
    ]
)
internal interface CustomerSheetConfirmationModule
