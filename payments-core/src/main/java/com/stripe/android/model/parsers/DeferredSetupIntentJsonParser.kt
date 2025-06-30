package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.SetupIntent
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeferredSetupIntentJsonParser(
    private val elementsSessionId: String,
    private val setupMode: DeferredIntentParams.Mode.Setup,
    private val isLiveMode: Boolean,
    private val timeProvider: () -> Long
) : ModelJsonParser<SetupIntent> {
    override fun parse(json: JSONObject): SetupIntent {
        val paymentMethodTypes = jsonArrayToList(
            json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
        )

        val unactivatedPaymentMethods = jsonArrayToList(
            json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES)
        ).map { it.lowercase() }

        val linkFundingSources = jsonArrayToList(json.optJSONArray(FIELD_LINK_FUNDING_SOURCES))
            .map { it.lowercase() }

        val countryCode = optString(json, FIELD_COUNTRY_CODE)

        return SetupIntent(
            id = elementsSessionId,
            cancellationReason = null,
            description = null,
            clientSecret = null,
            paymentMethodTypes = paymentMethodTypes,
            countryCode = countryCode,
            linkFundingSources = linkFundingSources,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            isLiveMode = isLiveMode,
            nextActionData = null,
            paymentMethodId = null,
            created = timeProvider(),
            status = null,
            usage = setupMode.setupFutureUsage,
        )
    }

    private companion object {
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
    }
}
