package com.stripe.android.network

import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path

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
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/payment_methods/${paymentMethodDetails.id}"),
        bodyPart(urlEncode("card[networks][preferred]"), cardBrand)
    ) { response ->
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
