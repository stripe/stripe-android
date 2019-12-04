package com.stripe.android.model.parsers

import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class SetupIntentJsonParser : ModelJsonParser<SetupIntent?> {
    override fun parse(json: JSONObject): SetupIntent? {
        val objectType = StripeJsonUtils.optString(json, FIELD_OBJECT)
        if (VALUE_SETUP_INTENT != objectType) {
            return null
        }

        val nextAction = StripeJsonUtils.optMap(json, FIELD_NEXT_ACTION)
        val nextActionType = nextAction?.let {
            StripeIntent.NextActionType.fromCode(it[FIELD_NEXT_ACTION_TYPE] as String?)
        }
        return SetupIntent(
            id = StripeJsonUtils.optString(json, FIELD_ID),
            objectType = objectType,
            created = json.optLong(FIELD_CREATED),
            clientSecret = StripeJsonUtils.optString(json, FIELD_CLIENT_SECRET),
            cancellationReason = SetupIntent.CancellationReason.fromCode(
                StripeJsonUtils.optString(json, FIELD_CANCELLATION_REASON)
            ),
            description = StripeJsonUtils.optString(json, FIELD_DESCRIPTION),
            isLiveMode = json.optBoolean(FIELD_LIVEMODE),
            paymentMethodId = StripeJsonUtils.optString(json, FIELD_PAYMENT_METHOD),
            paymentMethodTypes = ModelJsonParser.jsonArrayToList(
                json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
            ),
            status = StripeIntent.Status.fromCode(StripeJsonUtils.optString(json, FIELD_STATUS)),
            usage = StripeIntent.Usage.fromCode(StripeJsonUtils.optString(json, FIELD_USAGE)),
            nextAction = nextAction,
            nextActionType = nextActionType,
            lastSetupError = json.optJSONObject(FIELD_LAST_SETUP_ERROR)?.let {
                ErrorJsonParser().parse(it)
            }
        )
    }

    internal class ErrorJsonParser : ModelJsonParser<SetupIntent.Error> {
        override fun parse(json: JSONObject): SetupIntent.Error {
            return SetupIntent.Error(
                code = StripeJsonUtils.optString(json, FIELD_CODE),
                declineCode = StripeJsonUtils.optString(json, FIELD_DECLINE_CODE),
                docUrl = StripeJsonUtils.optString(json, FIELD_DOC_URL),
                message = StripeJsonUtils.optString(json, FIELD_MESSAGE),
                param = StripeJsonUtils.optString(json, FIELD_PARAM),
                paymentMethod = json.optJSONObject(FIELD_PAYMENT_METHOD)?.let {
                    PaymentMethodJsonParser().parse(it)
                },
                type = SetupIntent.Error.Type.fromCode(StripeJsonUtils.optString(json, FIELD_TYPE))
            )
        }

        private companion object {
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
        private const val VALUE_SETUP_INTENT = "setup_intent"

        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_CANCELLATION_REASON = "cancellation_reason"
        private const val FIELD_CREATED = "created"
        private const val FIELD_CLIENT_SECRET = "client_secret"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_LAST_SETUP_ERROR = "last_setup_error"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_NEXT_ACTION = "next_action"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_STATUS = "status"
        private const val FIELD_USAGE = "usage"
        private const val FIELD_PAYMENT_METHOD = "payment_method"

        private const val FIELD_NEXT_ACTION_TYPE = "type"
    }
}
