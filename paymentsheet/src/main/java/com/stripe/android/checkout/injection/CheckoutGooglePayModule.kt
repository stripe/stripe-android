package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutGooglePayPaymentDataUpdateAddressBuilder
import com.stripe.android.checkout.CheckoutGooglePayPaymentDataUpdateCallback
import com.stripe.android.checkout.CheckoutGooglePayPaymentDataUpdateMapper
import com.stripe.android.checkout.DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilder
import com.stripe.android.checkout.DefaultCheckoutGooglePayPaymentDataUpdateMapper
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import dagger.Binds
import dagger.Module

@Module
internal interface CheckoutGooglePayModule {
    @Binds
    fun bindsCheckoutGooglePayPaymentDataUpdateAddressBuilder(
        builder: DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilder
    ): CheckoutGooglePayPaymentDataUpdateAddressBuilder

    @Binds
    fun bindsCheckoutGooglePayPaymentDataUpdateMapper(
        mapper: DefaultCheckoutGooglePayPaymentDataUpdateMapper
    ): CheckoutGooglePayPaymentDataUpdateMapper

    @Binds
    fun bindsGooglePayPaymentDataUpdateCallback(
        callback: CheckoutGooglePayPaymentDataUpdateCallback
    ): GooglePayPaymentDataUpdateCallback
}
