@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CurrencySelectorViewModel
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dagger.Module
import dagger.Provides

@Module
internal object CurrencySelectorElementModule {
    @Provides
    fun provideCurrencySelectorViewModelFactory(
        checkoutController: CheckoutController,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    ): CurrencySelectorViewModel.Factory = CurrencySelectorViewModel.Factory(
        checkoutSession = checkoutController.checkoutSession,
        updateCurrency = checkoutController::updateCurrency,
        analyticsRequestExecutor = analyticsRequestExecutor,
        paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
    )
}
