package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.DeferredIntent
import com.stripe.android.model.DeferredIntentParams
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeferredIntentJsonParser(
    private val elementsSessionId: String?,
    private val params: DeferredIntentParams,
    private val apiKey: String,
    private val timeProvider: () -> Long
) : ModelJsonParser<DeferredIntent> {
    override fun parse(json: JSONObject): DeferredIntent {
        val paymentMethodTypes = jsonArrayToList(
            json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
        )

        val unactivatedPaymentMethods = jsonArrayToList(
            json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES)
        )

        val linkFundingSources = jsonArrayToList(json.optJSONArray(FIELD_LINK_FUNDING_SOURCES))
            .map { it.lowercase() }

        val countryCode = optString(json, FIELD_COUNTRY_CODE)

        return DeferredIntent(
            id = elementsSessionId,
            mode = params.mode,
            paymentMethodTypes = paymentMethodTypes,
            captureMethod = params.captureMethod,
            countryCode = countryCode,
            linkFundingSources = linkFundingSources,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            isLiveMode = apiKey.contains("live"),
            created = timeProvider(),
            setupFutureUsage = params.setupFutureUsage
        )
    }

    private companion object {
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
    }
}
