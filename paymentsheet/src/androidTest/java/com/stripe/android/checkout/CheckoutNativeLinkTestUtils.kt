package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.enqueueConsumerSessionLookup
import com.stripe.android.networktesting.testBodyFromFile
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject

internal fun NetworkRule.enqueueNativeLinkAccount() {
    enqueueConsumerSessionLookup { response ->
        response.testBodyFromFile("consumer-accounts-signup-success.json") { json ->
            json.put("exists", true)
        }
    }
}

internal fun NetworkRule.enqueueNativeLinkPaymentDetails(modify: (JSONObject) -> Unit = {}) {
    enqueue(
        method("POST"),
        path("/v1/consumers/payment_details/list"),
    ) { response ->
        response.testBodyFromFile("consumer-payment-details-success.json") { json ->
            val paymentDetails = json.getJSONObject("redacted_payment_details")
            modify(paymentDetails)
            json.put("redacted_payment_details", JSONArray().put(paymentDetails))
        }
    }
}

internal fun NetworkRule.checkoutConfirmNativeLinkPayment(vararg requestMatchers: RequestMatcher) {
    checkoutConfirm(
        bodyPart("payment_method_data[type]", "link"),
        bodyPart("payment_method_data[link][payment_details_id]", "QAAAKJ6"),
        *requestMatchers,
    ) { response ->
        response.testBodyFromFile("checkout-session-confirm.json")
    }
}

internal fun createNativeLinkCheckoutInitResponse(
    response: MockResponse,
    modify: (JSONObject) -> Unit = {},
) {
    response.testBodyFromFile("checkout-session-init.json") { json ->
        json.put("customer_email", "test@stripe.com")
        json.getJSONObject("link_settings").put("link_mode", "LINK_PAYMENT_METHOD")
        modify(json)
    }
}

@OptIn(CheckoutSessionPreview::class)
internal fun assertNativeLinkPaymentCompleted(result: CheckoutElementsController.Result?) {
    assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    assertThat((result as CheckoutElementsController.Result.Completed).paymentStatus)
        .isEqualTo(CheckoutElementsController.Session.Status.PaymentStatus.Paid)
}
