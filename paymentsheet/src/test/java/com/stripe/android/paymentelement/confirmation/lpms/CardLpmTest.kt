package com.stripe.android.paymentelement.confirmation.lpms

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.model.CardParams
import com.stripe.android.model.PaymentMethodCreateParams
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CardLpmTest : BaseLpmNetworkTest() {
    @Test
    fun withPaymentIntent() = testWithPaymentIntent(
        amount = 5050,
        currency = "USD",
        paymentMethodTypes = listOf("card"),
        createWithSetupFutureUsage = false,
        createParams = PaymentMethodCreateParams.createCard(
            cardParams = CardParams(
                number = "4242424242424242",
                expMonth = 12,
                expYear = 2032,
                cvc = "454"
            )
        ),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )

    @Test
    fun withSetupIntent() = testWithSetupIntent(
        paymentMethodTypes = listOf("card"),
        createParams = PaymentMethodCreateParams.createCard(
            cardParams = CardParams(
                number = "4242424242424242",
                expMonth = 12,
                expYear = 2032,
                cvc = "454"
            )
        ),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )

    @Test
    fun withDeferredPaymentIntent() = testWithDeferredPaymentIntent(
        amount = 5050,
        currency = "USD",
        paymentMethodTypes = listOf("card"),
        createWithSetupFutureUsage = false,
        createParams = PaymentMethodCreateParams.createCard(
            cardParams = CardParams(
                number = "4242424242424242",
                expMonth = 12,
                expYear = 2032,
                cvc = "454"
            )
        ),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )

    @Test
    fun withDeferredSetupIntent() = testWithDeferredSetupIntent(
        paymentMethodTypes = listOf("card"),
        createParams = PaymentMethodCreateParams.createCard(
            cardParams = CardParams(
                number = "4242424242424242",
                expMonth = 12,
                expYear = 2032,
                cvc = "454"
            )
        ),
        optionsParams = null,
        extraParams = null,
        shippingValues = null,
        customerRequestedSave = false,
    )
}
