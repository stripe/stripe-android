package com.stripe.android.network

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal fun NetworkRule.setupPaymentMethodDetachResponse(
    paymentMethodId: String,
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/payment_methods/$paymentMethodId/detach"),
    ) { response ->
        response.setResponseCode(200)
    }
}

internal fun NetworkRule.setupPaymentMethodUpdateResponse(
    paymentMethodDetails: CardPaymentMethodDetails,
    cardBrand: String,
    countDownLatch: CountDownLatch = CountDownLatch(0),
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/payment_methods/${paymentMethodDetails.id}"),
        bodyPart(urlEncode("card[networks][preferred]"), cardBrand)
    ) { response ->
        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
        response.setBody(
            paymentMethodDetails.createJson { originalCard ->
                originalCard.copy(
                    card = originalCard.card!!.copy(
                        displayBrand = cardBrand
                    )
                )
            }.toString(2)
        )
    }
}
