package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodOptionsMap
import org.json.JSONObject

internal class PaymentMethodOptionsJsonParser : ModelJsonParser<PaymentMethodOptionsMap> {
    override fun parse(json: JSONObject): PaymentMethodOptionsMap {
        val configurations = json.keys()
            .asSequence()
            .associateWith { key ->
                parseConfiguration(json.getJSONObject(key))
            }

        return PaymentMethodOptionsMap(
            options = configurations
        )
    }

    private fun parseConfiguration(json: JSONObject): PaymentMethodOptionsMap.Options {
        val setupFutureUsage =
            StripeJsonUtils.optString(json, FIELD_SETUP_FUTURE_USAGE)?.let { sfu ->
                PaymentMethodOptionsMap.SetupFutureUsage.values().find { it.value == sfu }
            }
        val verificationMethod =
            StripeJsonUtils.optString(json, FIELD_VERIFICATION_METHOD)?.let { vm ->
                PaymentMethodOptionsMap.VerificationMethod.values().find { it.value == vm }
            }

        return PaymentMethodOptionsMap.Options(
            setupFutureUsage = setupFutureUsage,
            verificationMethod = verificationMethod
        )
    }

    private companion object {
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val FIELD_VERIFICATION_METHOD = "verification_method"
    }
}
