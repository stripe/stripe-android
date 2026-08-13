package com.stripe.paymentelementnetwork

import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import org.json.JSONArray

fun NetworkRule.setupV1PaymentMethodsResponse(
    vararg paymentMethodDetails: PaymentMethodDetails,
    type: String = paymentMethodDetails.first().type,
) {
    for (paymentMethodDetail in paymentMethodDetails) {
        // All types must match.
        assertThat(type).isEqualTo(paymentMethodDetail.type)
    }

    enqueue(
        host("api.stripe.com"),
        method("GET"),
        path("/v1/payment_methods"),
        query("type", type),
    ) { response ->
        val paymentMethodsArray = JSONArray()

        for (paymentMethodDetail in paymentMethodDetails) {
            paymentMethodsArray.put(paymentMethodDetail.createJson())
        }

        val paymentMethodsStringified = paymentMethodsArray.toString(2)

        val body = """
            {
              "object": "list",
              "data": $paymentMethodsStringified,
              "has_more": false,
              "url": "/v1/payment_methods"
            }
        """.trimIndent()
        response.setBody(body)
    }
}
