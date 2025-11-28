package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.CheckoutSessionResponse
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

internal class CheckoutSessionResponseJsonParser : ModelJsonParser<CheckoutSessionResponse> {
    override fun parse(json: JSONObject): CheckoutSessionResponse {
        val currency = json.getString("currency")
        val amount = json.getJSONObject("total_summary").getLong("total")
        val mode = when (json.get("mode")) {
            "payment" -> CheckoutSessionResponse.Mode.Payment
            "subscription" -> {
                if (json.has("invoice")) {
                    CheckoutSessionResponse.Mode.Payment
                } else {
                    CheckoutSessionResponse.Mode.Subscription
                }
            }
            else -> throw IllegalArgumentException("Invalid mode")
        }
        val setupFutureUsage = StripeIntent.Usage.fromCode(
            optString(json, "setup_future_usage")
        )
        val captureMethod = PaymentIntent.CaptureMethod.fromCode(
            optString(json, "capture_method")
        )
        val paymentMethodOptionsJsonString = optString(json, "payment_method_options")
        val paymentMethodTypes = jsonArrayToList(
            json.optJSONArray("payment_method_types")
        )
        val onBehalfOf = optString(json, "on_behalf_of")

        val intent = when (mode) {
            CheckoutSessionResponse.Mode.Payment -> {
                json.optJSONObject("payment_intent")?.let { PaymentIntentJsonParser().parse(it) }
            }
            CheckoutSessionResponse.Mode.Subscription -> {
                json.optJSONObject("setup_intent")?.let { SetupIntentJsonParser().parse(it) }
            }
        }

        val paymentMethodsJson = json.optJSONObject("customer")?.optJSONArray("payment_methods")
        val paymentMethods = paymentMethodsJson?.let { pmsJson ->
            (0 until pmsJson.length()).mapNotNull { index ->
                PaymentMethodJsonParser().parse(pmsJson.optJSONObject(index))
            }
        } ?: emptyList()

        return CheckoutSessionResponse(
            currency = currency,
            amount = amount,
            mode = mode,
            setupFutureUsage = setupFutureUsage,
            captureMethod = captureMethod,
            paymentMethodOptionsJsonString = null, // TODO:
            paymentMethodTypes = paymentMethodTypes,
            onBehalfOf = onBehalfOf,
            intent = intent,
            paymentMethods = paymentMethods,
        )
    }
}
