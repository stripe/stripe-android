package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
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
        val paymentMethodPreference =
            StripeJsonUtils.mapToJsonObject(StripeJsonUtils.optMap(json, OBJECT_TYPE))

        val objectType = StripeJsonUtils.optString(paymentMethodPreference, FIELD_OBJECT)
        if (paymentMethodPreference == null || OBJECT_TYPE != objectType) {
            return null
        }

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
                parseStripeIntent(stripeIntentJsonObject)
            }?.let {
                PaymentMethodPreference(
                    it,
                    paymentMethodPreference.optString(FIELD_PAYMENT_METHOD_SCHEMA)// formUI string
                )
            }
    }

    abstract fun parseStripeIntent(stripeIntentJson: JSONObject): StripeIntentType?

    protected companion object {
        private const val OBJECT_TYPE = "payment_method_preference"

        private const val FIELD_OBJECT = "object"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES = "ordered_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES =
            "unactivated_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_SCHEMA =
            "payment_method_specs"
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
