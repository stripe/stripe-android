@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import com.stripe.android.checkout.gpay.CheckoutGooglePayPaymentDataChangedCallback
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataChangedCallback
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dagger.Module
import dagger.Provides

@Module
internal object CheckoutGooglePayUpdateCallbackModule {
    @Provides
    fun provideGooglePayPaymentDataChangedCallback(
        callback: CheckoutGooglePayPaymentDataChangedCallback,
    ): GooglePayPaymentDataChangedCallback = callback
}
