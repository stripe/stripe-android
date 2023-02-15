package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.DeferredIntent
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeferredIntentJsonParser(
    private val params: DeferredIntentParams,
    private val apiKey: String,
    private val timeProvider: () -> Long = {
        System.currentTimeMillis()
    }
) : ModelJsonParser<DeferredIntent> {
    override fun parse(json: JSONObject): DeferredIntent? {
        val objectType = optString(json, FIELD_OBJECT)
        if (OBJECT_TYPE != objectType) {
            return null
        }

        val paymentMethodTypes = jsonArrayToList(
            json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
        )

        val paymentMethodOptions = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS)?.toString()

        val setupFutureUsage = StripeIntent.Usage.fromCode(
            optString(json, FIELD_SETUP_FUTURE_USAGE)
        )

        val unactivatedPaymentMethods = jsonArrayToList(
            json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES)
        )

        val linkFundingSources = jsonArrayToList(json.optJSONArray(FIELD_LINK_FUNDING_SOURCES))
            .map { it.lowercase() }

        val countryCode = optString(json, FIELD_COUNTRY_CODE)

        return DeferredIntent(
            mode = params.mode,
            paymentMethodTypes = paymentMethodTypes,
            captureMethod = params.captureMethod,
            countryCode = countryCode,
            linkFundingSources = linkFundingSources,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            isLiveMode = apiKey.contains("live"),
            created = timeProvider(),
            setupFutureUsage = setupFutureUsage,
            paymentMethodOptionsJsonString = paymentMethodOptions
        )
    }

    private companion object {
        private const val OBJECT_TYPE = "payment_intent"

        private const val FIELD_OBJECT = "object"
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_PAYMENT_METHOD_OPTIONS = "payment_method_options"
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
    }
}
