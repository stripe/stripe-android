package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSelectionRepository
import dagger.Binds
import dagger.Module

@Module
internal abstract class PaymentOptionsLinkModule {
    @Binds
    abstract fun providePaymentSelectionRepository(
        paymentOptionsViewModel: PaymentOptionsViewModel
    ): PaymentSelectionRepository
}
