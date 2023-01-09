package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSelectionRepository
import dagger.Binds
import dagger.Module

@Module
internal interface PaymentOptionsLinkModule {
    @Binds
    fun providePaymentSelectionRepository(
        paymentOptionsViewModel: PaymentOptionsViewModel
    ): PaymentSelectionRepository
}
