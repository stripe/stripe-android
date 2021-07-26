package com.stripe.android.model.parsers

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal sealed class PaymentMethodPreferenceJsonParser<out StripeIntentType : StripeIntent> :
    ModelJsonParser<StripeIntentType> {
    abstract val stripeIntentFieldName: String

    override fun parse(json: JSONObject): StripeIntentType? {
        val objectType = StripeJsonUtils.optString(json, FIELD_OBJECT)
        if (OBJECT_TYPE != objectType) {
            return null
        }

        val orderedPaymentMethodTypes = json.optJSONArray(FIELD_ORDERED_PAYMENT_METHOD_TYPES)

        return json.optJSONObject(stripeIntentFieldName)?.let {
            it.put(FIELD_PAYMENT_METHOD_TYPES, orderedPaymentMethodTypes)
            parseStripeIntent(it)
        }
    }

    abstract fun parseStripeIntent(stripeIntentJson: JSONObject): StripeIntentType?

    protected companion object {
        private const val OBJECT_TYPE = "payment_method_preference"

        private const val FIELD_OBJECT = "object"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES = "ordered_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
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
