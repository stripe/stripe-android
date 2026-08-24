package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import dagger.Module
import dagger.Provides

@Module
internal class GooglePayPaymentDataUpdateNoOpModule {
    @Provides
    fun providesGooglePayPaymentDataUpdateCallback() = CALLBACK

    private companion object {
        val CALLBACK: GooglePayPaymentDataUpdateCallback? = null
    }
}
