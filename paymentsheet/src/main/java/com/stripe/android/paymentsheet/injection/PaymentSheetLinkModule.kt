package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.PaymentSelectionRepository
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import dagger.Binds
import dagger.Module

@Module
internal abstract class PaymentSheetLinkModule {
    @Binds
    abstract fun providePaymentSelectionRepository(
        paymentSheetViewModel: PaymentSheetViewModel
    ): PaymentSelectionRepository
}
