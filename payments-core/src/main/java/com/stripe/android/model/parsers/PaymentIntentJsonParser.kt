package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentIntentJsonParser : ModelJsonParser<PaymentIntent> {
    override fun parse(json: JSONObject): PaymentIntent? {
        val objectType = optString(json, FIELD_OBJECT)
        if (OBJECT_TYPE != objectType) {
            return null
        }

        val id = optString(json, FIELD_ID)
        val paymentMethodTypes = jsonArrayToList(
            json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
        )
        val amount = StripeJsonUtils.optLong(json, FIELD_AMOUNT)
        val canceledAt = json.optLong(FIELD_CANCELED_AT)
        val cancellationReason = PaymentIntent.CancellationReason.fromCode(
            optString(json, FIELD_CANCELLATION_REASON)
        )
        val captureMethod = PaymentIntent.CaptureMethod.fromCode(
            optString(json, FIELD_CAPTURE_METHOD)
        )
        val clientSecret = optString(json, FIELD_CLIENT_SECRET)
        val confirmationMethod = PaymentIntent.ConfirmationMethod.fromCode(
            optString(json, FIELD_CONFIRMATION_METHOD)
        )
        val created = json.optLong(FIELD_CREATED)
        val currency = StripeJsonUtils.optCurrency(json, FIELD_CURRENCY)
        val description = optString(json, FIELD_DESCRIPTION)
        val livemode = StripeJsonUtils.optBoolean(json, FIELD_LIVEMODE)

        val paymentMethod = json.optJSONObject(FIELD_PAYMENT_METHOD)?.let {
            PaymentMethodJsonParser().parse(it)
        }
        val paymentMethodId =
            optString(json, FIELD_PAYMENT_METHOD).takeIf { paymentMethod == null }
                ?: paymentMethod?.id

        val receiptEmail = optString(json, FIELD_RECEIPT_EMAIL)
        val status = StripeIntent.Status.fromCode(
            optString(json, FIELD_STATUS)
        )

        val paymentMethodOptions = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS)?.toString()

        val setupFutureUsage = StripeIntent.Usage.fromCode(
            optString(json, FIELD_SETUP_FUTURE_USAGE)
        )
        val lastPaymentError =
            json.optJSONObject(FIELD_LAST_PAYMENT_ERROR)?.let {
                ErrorJsonParser().parse(it)
            }

        val shipping = json.optJSONObject(FIELD_SHIPPING)?.let {
            ShippingJsonParser().parse(it)
        }
        val nextActionData = json.optJSONObject(FIELD_NEXT_ACTION)?.let {
            NextActionDataParser().parse(it)
        }

        val unactivatedPaymentMethods = jsonArrayToList(
            json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES)
        ).map { it.lowercase() }

        val linkFundingSources = jsonArrayToList(json.optJSONArray(FIELD_LINK_FUNDING_SOURCES))
            .map { it.lowercase() }

        val countryCode = optString(json, FIELD_COUNTRY_CODE)

        return PaymentIntent(
            id = id,
            paymentMethodTypes = paymentMethodTypes,
            amount = amount,
            canceledAt = canceledAt,
            cancellationReason = cancellationReason,
            captureMethod = captureMethod,
            clientSecret = clientSecret,
            confirmationMethod = confirmationMethod,
            countryCode = countryCode,
            created = created,
            currency = currency,
            description = description,
            isLiveMode = livemode,
            paymentMethod = paymentMethod,
            paymentMethodId = paymentMethodId,
            receiptEmail = receiptEmail,
            status = status,
            setupFutureUsage = setupFutureUsage,
            lastPaymentError = lastPaymentError,
            shipping = shipping,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            linkFundingSources = linkFundingSources,
            nextActionData = nextActionData,
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

        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_CREATED = "created"
        private const val FIELD_CANCELED_AT = "canceled_at"
        private const val FIELD_CANCELLATION_REASON = "cancellation_reason"
        private const val FIELD_CAPTURE_METHOD = "capture_method"
        private const val FIELD_CLIENT_SECRET = "client_secret"
        private const val FIELD_CONFIRMATION_METHOD = "confirmation_method"
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_LAST_PAYMENT_ERROR = "last_payment_error"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_NEXT_ACTION = "next_action"
        private const val FIELD_PAYMENT_METHOD = "payment_method"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_PAYMENT_METHOD_OPTIONS = "payment_method_options"
        private const val FIELD_RECEIPT_EMAIL = "receipt_email"
        private const val FIELD_SHIPPING = "shipping"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES =
            "unactivated_payment_method_types"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
    }
}
