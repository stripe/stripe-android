package com.stripe.android.paymentelement.confirmation.lpms

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.model.PaymentMethodCreateParams
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CashAppLpmTest : BaseLpmNetworkTest() {
    @Test
    fun withPaymentIntent() = testWithPaymentIntent(
        amount = 5050,
        currency = "USD",
        paymentMethodTypes = listOf("cashapp"),
        createWithSetupFutureUsage = false,
        createParams = PaymentMethodCreateParams.createCashAppPay(),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )

    @Test
    fun withSetupIntent() = testWithSetupIntent(
        paymentMethodTypes = listOf("cashapp"),
        createParams = PaymentMethodCreateParams.createCashAppPay(),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )

    @Test
    fun withDeferredPaymentIntent() = testWithDeferredPaymentIntent(
        amount = 5050,
        currency = "USD",
        paymentMethodTypes = listOf("cashapp"),
        createWithSetupFutureUsage = false,
        createParams = PaymentMethodCreateParams.createCashAppPay(),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )

    @Test
    fun withDeferredSetupIntent() = testWithDeferredSetupIntent(
        paymentMethodTypes = listOf("cashapp"),
        createParams = PaymentMethodCreateParams.createCashAppPay(),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )
}
