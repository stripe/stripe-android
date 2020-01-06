package com.stripe.android.model.parsers

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class PaymentIntentJsonParser : ModelJsonParser<PaymentIntent> {
    override fun parse(json: JSONObject): PaymentIntent? {
        if (OBJECT_TYPE != json.optString(FIELD_OBJECT)) {
            return null
        }

        val id = StripeJsonUtils.optString(json, FIELD_ID)
        val objectType = StripeJsonUtils.optString(json, FIELD_OBJECT)
        val paymentMethodTypes = ModelJsonParser.jsonArrayToList(
            json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES))
        val amount = StripeJsonUtils.optLong(json, FIELD_AMOUNT)
        val canceledAt = json.optLong(FIELD_CANCELED_AT)
        val cancellationReason = PaymentIntent.CancellationReason.fromCode(
            StripeJsonUtils.optString(json, FIELD_CANCELLATION_REASON)
        )
        val captureMethod = StripeJsonUtils.optString(json, FIELD_CAPTURE_METHOD)
        val clientSecret = StripeJsonUtils.optString(json, FIELD_CLIENT_SECRET)
        val confirmationMethod = StripeJsonUtils.optString(json, FIELD_CONFIRMATION_METHOD)
        val created = json.optLong(FIELD_CREATED)
        val currency = StripeJsonUtils.optCurrency(json, FIELD_CURRENCY)
        val description = StripeJsonUtils.optString(json, FIELD_DESCRIPTION)
        val livemode = StripeJsonUtils.optBoolean(json, FIELD_LIVEMODE)
        val paymentMethodId = StripeJsonUtils.optString(json, FIELD_PAYMENT_METHOD_ID)
        val receiptEmail = StripeJsonUtils.optString(json, FIELD_RECEIPT_EMAIL)
        val status = StripeIntent.Status.fromCode(
            StripeJsonUtils.optString(json, FIELD_STATUS)
        )
        val setupFutureUsage = StripeIntent.Usage.fromCode(
            StripeJsonUtils.optString(json, FIELD_SETUP_FUTURE_USAGE)
        )
        val nextAction = StripeJsonUtils.optMap(json, FIELD_NEXT_ACTION)
        val lastPaymentError =
            json.optJSONObject(FIELD_LAST_PAYMENT_ERROR)?.let {
                ErrorJsonParser().parse(it)
            }

        return PaymentIntent(
            id = id,
            objectType = objectType,
            paymentMethodTypes = paymentMethodTypes,
            amount = amount,
            canceledAt = canceledAt,
            cancellationReason = cancellationReason,
            captureMethod = captureMethod,
            clientSecret = clientSecret,
            confirmationMethod = confirmationMethod,
            created = created,
            currency = currency,
            description = description,
            isLiveMode = livemode,
            nextAction = nextAction,
            paymentMethodId = paymentMethodId,
            receiptEmail = receiptEmail,
            status = status,
            setupFutureUsage = setupFutureUsage,
            lastPaymentError = lastPaymentError
        )
    }

    internal class ErrorJsonParser : ModelJsonParser<PaymentIntent.Error> {
        override fun parse(json: JSONObject): PaymentIntent.Error {
            return PaymentIntent.Error(
                charge = StripeJsonUtils.optString(json, FIELD_CHARGE),
                code = StripeJsonUtils.optString(json, FIELD_CODE),
                declineCode = StripeJsonUtils.optString(json, FIELD_DECLINE_CODE),
                docUrl = StripeJsonUtils.optString(json, FIELD_DOC_URL),
                message = StripeJsonUtils.optString(json, FIELD_MESSAGE),
                param = StripeJsonUtils.optString(json, FIELD_PARAM),
                paymentMethod = json.optJSONObject(FIELD_PAYMENT_METHOD)?.let {
                    PaymentMethodJsonParser().parse(it)
                },
                type = PaymentIntent.Error.Type.fromCode(
                    StripeJsonUtils.optString(json, FIELD_TYPE)
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
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_LAST_PAYMENT_ERROR = "last_payment_error"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_NEXT_ACTION = "next_action"
        private const val FIELD_PAYMENT_METHOD_ID = "payment_method_id"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_RECEIPT_EMAIL = "receipt_email"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
    }
}
