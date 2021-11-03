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

        return paymentMethodPreference.optJSONObject(stripeIntentFieldName)?.let {
            it.put(FIELD_PAYMENT_METHOD_TYPES, orderedPaymentMethodTypes)
            it.put(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES, unactivatedPaymentMethods)
            parseStripeIntent(it)
        }
    }

    abstract fun parseStripeIntent(stripeIntentJson: JSONObject): StripeIntentType?

    protected companion object {
        private const val OBJECT_TYPE = "payment_method_preference"

        private const val FIELD_OBJECT = "object"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES = "ordered_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
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
