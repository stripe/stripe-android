package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodOptions
import org.json.JSONObject

internal class PaymentMethodOptionsJsonParser : ModelJsonParser<PaymentMethodOptions> {
    override fun parse(json: JSONObject): PaymentMethodOptions {
        val setupFutureUsage =
            StripeJsonUtils.optString(json, FIELD_SETUP_FUTURE_USAGE)?.let { sfu ->
                PaymentMethodOptions.SetupFutureUsage.values().find { it.value == sfu }
            }
        val verificationMethod =
            StripeJsonUtils.optString(json, FIELD_VERIFICATION_METHOD)?.let { vm ->
                PaymentMethodOptions.VerificationMethod.values().find { it.value == vm }
            }

        return PaymentMethodOptions(
            setupFutureUsage = setupFutureUsage,
            verificationMethod = verificationMethod
        )
    }

    private companion object {
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val FIELD_VERIFICATION_METHOD = "verification_method"
    }
}
