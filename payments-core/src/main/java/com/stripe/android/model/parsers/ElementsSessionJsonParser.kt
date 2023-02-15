package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.StripeIntent
import org.json.JSONArray
import org.json.JSONObject

internal class ElementsSessionJsonParser(
    private val type: ElementsSessionParams.Type,
) : ModelJsonParser<ElementsSession> {
    override fun parse(json: JSONObject): ElementsSession? {
        val paymentMethodPreference = StripeJsonUtils.mapToJsonObject(
            StripeJsonUtils.optMap(json, FIELD_PAYMENT_METHOD_PREFERENCE)
        )
        val objectType = StripeJsonUtils.optString(paymentMethodPreference, FIELD_OBJECT)

        if (paymentMethodPreference == null || FIELD_PAYMENT_METHOD_PREFERENCE != objectType) {
            return null
        }

        val countryCode = paymentMethodPreference.optString(FIELD_COUNTRY_CODE)
        val unactivatedPaymentMethodTypes =
            jsonArrayToList(json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES))
                .map { it.lowercase() }
        val paymentMethodSpecs = json.optJSONArray(FIELD_PAYMENT_METHOD_SPECS)?.toString()
        val linkFundingSources = json.optJSONObject(FIELD_LINK_SETTINGS)?.optJSONArray(
            FIELD_LINK_FUNDING_SOURCES
        )
        val orderedPaymentMethodTypes =
            paymentMethodPreference.optJSONArray(FIELD_ORDERED_PAYMENT_METHOD_TYPES)

        val stripeIntent = parseStripeIntent(
            paymentMethodPreference = paymentMethodPreference,
            orderedPaymentMethodTypes = orderedPaymentMethodTypes,
            unactivatedPaymentMethodTypes = unactivatedPaymentMethodTypes,
            linkFundingSources = linkFundingSources,
            countryCode = countryCode
        )

        return ElementsSession(
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = jsonArrayToList(linkFundingSources)
            ),
            paymentMethodTypes = jsonArrayToList(orderedPaymentMethodTypes),
            unactivatedPaymentMethodTypes = unactivatedPaymentMethodTypes,
            paymentMethodSpecs = paymentMethodSpecs,
            stripeIntent = stripeIntent
        )
    }

    private fun parseStripeIntent(
        paymentMethodPreference: JSONObject?,
        orderedPaymentMethodTypes: JSONArray?,
        unactivatedPaymentMethodTypes: List<String>,
        linkFundingSources: JSONArray?,
        countryCode: String
    ): StripeIntent? {
        return paymentMethodPreference?.optJSONObject(
            type.value
        )?.let { stripeIntentJsonObject ->
            orderedPaymentMethodTypes?.let {
                stripeIntentJsonObject.put(
                    FIELD_PAYMENT_METHOD_TYPES,
                    orderedPaymentMethodTypes
                )
            }
            stripeIntentJsonObject.put(
                FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES,
                unactivatedPaymentMethodTypes
            )
            stripeIntentJsonObject.put(
                FIELD_LINK_FUNDING_SOURCES,
                linkFundingSources
            )
            stripeIntentJsonObject.put(
                FIELD_COUNTRY_CODE,
                countryCode
            )

            when (type) {
                ElementsSessionParams.Type.PaymentIntent ->
                    PaymentIntentJsonParser().parse(stripeIntentJsonObject)
                ElementsSessionParams.Type.SetupIntent ->
                    SetupIntentJsonParser().parse(stripeIntentJsonObject)
                ElementsSessionParams.Type.DeferredIntent -> null
            }
        }
    }

    private companion object {
        private const val FIELD_OBJECT = "object"
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES = "ordered_payment_method_types"
        private const val FIELD_LINK_SETTINGS = "link_settings"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
        private const val FIELD_PAYMENT_METHOD_PREFERENCE = "payment_method_preference"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_SPECS = "payment_method_specs"
    }
}
