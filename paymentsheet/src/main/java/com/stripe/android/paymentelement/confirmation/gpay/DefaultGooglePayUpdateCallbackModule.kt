package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataChangedCallback
import dagger.Module
import dagger.Provides

@Module
internal object DefaultGooglePayUpdateCallbackModule {
    @Provides
    fun provideGooglePayPaymentDataChangedCallback(): GooglePayPaymentDataChangedCallback? = null
}
