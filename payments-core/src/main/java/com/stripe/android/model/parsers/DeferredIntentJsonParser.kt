package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.Address
import com.stripe.android.model.DeferredIntent
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeferredIntentJsonParser(
    private val mode: DeferredIntent.Mode,
    private val amount: Long?,
    private val captureMethod: DeferredIntent.CaptureMethod?,
) : ModelJsonParser<DeferredIntent> {
    override fun parse(json: JSONObject): DeferredIntent? {
        val objectType = optString(json, FIELD_OBJECT)
        if (OBJECT_TYPE != objectType) {
            return null
        }

        val paymentMethodTypes = jsonArrayToList(
            json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
        )

        val currency = StripeJsonUtils.optCurrency(json, FIELD_CURRENCY)
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
            mode = mode,
            paymentMethodTypes = paymentMethodTypes,
            amount = amount,
            captureMethod = captureMethod,
            countryCode = countryCode,
            currency = currency,
            linkFundingSources = linkFundingSources,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            isLiveMode = false, // TODO
            setupFutureUsage = setupFutureUsage,
            paymentMethodOptionsJsonString = paymentMethodOptions
        )
    }

    internal class ErrorJsonParser : ModelJsonParser<PaymentIntent.Error> {
        override fun parse(json: JSONObject): PaymentIntent.Error {
            return PaymentIntent.Error(
                charge = optString(json, FIELD_CHARGE),
                code = optString(json, FIELD_CODE),
                declineCode = optString(json, FIELD_DECLINE_CODE),
                docUrl = optString(json, FIELD_DOC_URL),
                message = optString(json, FIELD_MESSAGE),
                param = optString(json, FIELD_PARAM),
                paymentMethod = json.optJSONObject(FIELD_PAYMENT_METHOD)?.let {
                    PaymentMethodJsonParser().parse(it)
                },
                type = PaymentIntent.Error.Type.fromCode(
                    optString(json, FIELD_TYPE)
                )
            )
        }

        private companion object {
            private const val FIELD_CHARGE = "charge"
            private const val FIELD_CODE = "code"
            private const val FIELD_DECLINE_CODE = "decline_code"
            private const val FIELD_DOC_URL = "doc_url"
            private const val FIELD_MESSAGE = "message"
            private const val FIELD_PARAM = "param"
            private const val FIELD_PAYMENT_METHOD = "payment_method"
            private const val FIELD_TYPE = "type"
        }
    }

    internal class ShippingJsonParser : ModelJsonParser<PaymentIntent.Shipping> {
        override fun parse(json: JSONObject): PaymentIntent.Shipping {
            return PaymentIntent.Shipping(
                address = json.optJSONObject(FIELD_ADDRESS)?.let {
                    AddressJsonParser().parse(it)
                } ?: Address(),
                carrier = optString(json, FIELD_CARRIER),
                name = optString(json, FIELD_NAME),
                phone = optString(json, FIELD_PHONE),
                trackingNumber = optString(json, FIELD_TRACKING_NUMBER)
            )
        }

        private companion object {
            private const val FIELD_ADDRESS = "address"
            private const val FIELD_CARRIER = "carrier"
            private const val FIELD_NAME = "name"
            private const val FIELD_PHONE = "phone"
            private const val FIELD_TRACKING_NUMBER = "tracking_number"
        }
    }

    private companion object {
        private const val OBJECT_TYPE = "payment_intent"

        private const val FIELD_OBJECT = "object"
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_CURRENCY = "merchant_currency"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_PAYMENT_METHOD_OPTIONS = "payment_method_options"
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
    }
}
