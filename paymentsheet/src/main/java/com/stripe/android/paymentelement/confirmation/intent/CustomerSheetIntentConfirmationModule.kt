package com.stripe.android.paymentelement.confirmation.intent

import dagger.Binds
import dagger.Module

@Module(includes = [IntentConfirmationModule::class])
internal interface CustomerSheetIntentConfirmationModule {
    @Binds
    fun bindsSetupIntentInterceptorFactory(
        defaultCustomerSheetSetupIntentInterceptorFactory: DefaultCustomerSheetSetupIntentInterceptorFactory
    ): CustomerSheetSetupIntentInterceptor.Factory

    @Binds
    fun bindsAttachPaymentMethodInterceptorFactory(
        defaultCustomerSheetAttachPaymentMethodInterceptorFactory:
        DefaultCustomerSheetAttachPaymentMethodInterceptorFactory
    ): CustomerSheetAttachPaymentMethodInterceptor.Factory

    @Binds
    fun bindsIntentConfirmationInterceptorFactory(
        customerSheetConfirmationInterceptorFactory: CustomerSheetIntentConfirmationInterceptorFactory
    ): IntentConfirmationInterceptor.Factory
}
