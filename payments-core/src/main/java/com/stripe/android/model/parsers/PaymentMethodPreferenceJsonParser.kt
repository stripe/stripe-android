package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.mapToJsonObject
import com.stripe.android.core.model.StripeJsonUtils.optMap
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodPreference
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

internal sealed class PaymentMethodPreferenceJsonParser<StripeIntentType : StripeIntent> :
    ModelJsonParser<PaymentMethodPreference> {
    abstract val stripeIntentFieldName: String

    override fun parse(json: JSONObject): PaymentMethodPreference? {
        val paymentMethodPreference = mapToJsonObject(optMap(json, OBJECT_TYPE_PREFERENCE))

        val objectType = optString(paymentMethodPreference, FIELD_OBJECT)
        if (paymentMethodPreference == null || OBJECT_TYPE_PREFERENCE != objectType) {
            return null
        }

        val countryCode = paymentMethodPreference.optString(FIELD_COUNTRY_CODE)

        val paymentMethodSpecs = json.optJSONArray(FIELD_TYPE_PAYMENT_METHOD_SCHEMA)

        val linkFundingSources =
            json.optJSONObject(FIELD_LINK_SETTINGS)?.optJSONArray(FIELD_LINK_FUNDING_SOURCES)

        val orderedPaymentMethodTypes =
            paymentMethodPreference.optJSONArray(FIELD_ORDERED_PAYMENT_METHOD_TYPES)

        val unactivatedPaymentMethods =
            json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES)

        return paymentMethodPreference.optJSONObject(stripeIntentFieldName)
            ?.let { stripeIntentJsonObject ->
                orderedPaymentMethodTypes?.let {
                    stripeIntentJsonObject.put(
                        FIELD_PAYMENT_METHOD_TYPES,
                        orderedPaymentMethodTypes
                    )
                }
                stripeIntentJsonObject.put(
                    FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES,
                    unactivatedPaymentMethods
                )
                stripeIntentJsonObject.put(
                    FIELD_LINK_FUNDING_SOURCES,
                    linkFundingSources
                )
                stripeIntentJsonObject.put(
                    FIELD_COUNTRY_CODE,
                    countryCode
                )
                parseStripeIntent(stripeIntentJsonObject)
            }?.let {
                PaymentMethodPreference(
                    it,
                    paymentMethodSpecs?.toString() // formUI string
                )
            }
    }

    abstract fun parseStripeIntent(stripeIntentJson: JSONObject): StripeIntentType?

    protected companion object {
        private const val OBJECT_TYPE_PREFERENCE = "payment_method_preference"

        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES = "ordered_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES =
            "unactivated_payment_method_types"
        private const val FIELD_TYPE_PAYMENT_METHOD_SCHEMA =
            "payment_method_specs"
        private const val FIELD_LINK_SETTINGS = "link_settings"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
    }
}

internal class PaymentMethodPreferenceForPaymentIntentJsonParser :
    PaymentMethodPreferenceJsonParser<PaymentIntent>() {
    override val stripeIntentFieldName = "payment_intent"

    override fun parseStripeIntent(stripeIntentJson: JSONObject) =
        PaymentIntentJsonParser().parse(stripeIntentJson)
}

internal class PaymentMethodPreferenceForSetupIntentJsonParser :
    PaymentMethodPreferenceJsonParser<SetupIntent>() {
    override val stripeIntentFieldName = "setup_intent"

    override fun parseStripeIntent(stripeIntentJson: JSONObject) =
        SetupIntentJsonParser().parse(stripeIntentJson)
}
