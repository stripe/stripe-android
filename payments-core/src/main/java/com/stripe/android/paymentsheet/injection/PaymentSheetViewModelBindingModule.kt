package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module

@Module
internal interface PaymentSheetViewModelBindingModule {
    @Binds
    fun bindsApplicationForContext(application: Application): Context
}