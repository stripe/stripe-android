package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
class SetupIntentJsonParser : ModelJsonParser<SetupIntent> {
    override fun parse(json: JSONObject): SetupIntent? {
        val objectType = optString(json, FIELD_OBJECT)
        if (VALUE_SETUP_INTENT != objectType) {
            return null
        }

        val paymentMethod = json.optJSONObject(FIELD_PAYMENT_METHOD)?.let {
            PaymentMethodJsonParser().parse(it)
        }
        val paymentMethodId = optString(json, FIELD_PAYMENT_METHOD).takeIf {
            paymentMethod == null
        } ?: paymentMethod?.id

        val unactivatedPaymentMethods = jsonArrayToList(
            json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES)
        ).map { it.lowercase() }

        val linkFundingSources = jsonArrayToList(json.optJSONArray(FIELD_LINK_FUNDING_SOURCES))
            .map { it.lowercase() }

        val paymentMethodOptions = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS)?.toString()

        return SetupIntent(
            id = optString(json, FIELD_ID),
            created = json.optLong(FIELD_CREATED),
            clientSecret = optString(json, FIELD_CLIENT_SECRET),
            cancellationReason = SetupIntent.CancellationReason.fromCode(
                optString(json, FIELD_CANCELLATION_REASON)
            ),
            countryCode = optString(json, FIELD_COUNTRY_CODE),
            description = optString(json, FIELD_DESCRIPTION),
            isLiveMode = json.optBoolean(FIELD_LIVEMODE),
            paymentMethod = paymentMethod,
            paymentMethodId = paymentMethodId,
            paymentMethodTypes = jsonArrayToList(
                json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
            ),
            status = StripeIntent.Status.fromCode(optString(json, FIELD_STATUS)),
            usage = StripeIntent.Usage.fromCode(optString(json, FIELD_USAGE)),
            lastSetupError = json.optJSONObject(FIELD_LAST_SETUP_ERROR)?.let {
                ErrorJsonParser().parse(it)
            },
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            linkFundingSources = linkFundingSources,
            nextActionData = json.optJSONObject(FIELD_NEXT_ACTION)?.let {
                NextActionDataParser().parse(it)
            },
            paymentMethodOptionsJsonString = paymentMethodOptions
        )
    }

    internal class ErrorJsonParser : ModelJsonParser<SetupIntent.Error> {
        override fun parse(json: JSONObject): SetupIntent.Error {
            return SetupIntent.Error(
                code = optString(json, FIELD_CODE),
                declineCode = optString(json, FIELD_DECLINE_CODE),
                docUrl = optString(json, FIELD_DOC_URL),
                message = optString(json, FIELD_MESSAGE),
                param = optString(json, FIELD_PARAM),
                paymentMethod = json.optJSONObject(FIELD_PAYMENT_METHOD)?.let {
                    PaymentMethodJsonParser().parse(it)
                },
                type = SetupIntent.Error.Type.fromCode(optString(json, FIELD_TYPE))
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
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_LAST_SETUP_ERROR = "last_setup_error"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_NEXT_ACTION = "next_action"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_STATUS = "status"
        private const val FIELD_USAGE = "usage"
        private const val FIELD_PAYMENT_METHOD = "payment_method"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES =
            "unactivated_payment_method_types"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
        private const val FIELD_PAYMENT_METHOD_OPTIONS = "payment_method_options"
    }
}
