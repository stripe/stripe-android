package com.stripe.android.model.parsers

import android.net.Uri
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionType
import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.model.StripeJsonUtils.jsonArrayToList
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class PaymentIntentJsonParser : ModelJsonParser<PaymentIntent> {
    override fun parse(json: JSONObject): PaymentIntent? {
        val objectType = optString(json, FIELD_OBJECT)
        if (OBJECT_TYPE != objectType) {
            return null
        }

        val id = optString(json, FIELD_ID)
        val paymentMethodTypes = ModelJsonParser.jsonArrayToList(
            json.optJSONArray(FIELD_PAYMENT_METHOD_TYPES))
        val amount = StripeJsonUtils.optLong(json, FIELD_AMOUNT)
        val canceledAt = json.optLong(FIELD_CANCELED_AT)
        val cancellationReason = PaymentIntent.CancellationReason.fromCode(
            optString(json, FIELD_CANCELLATION_REASON)
        )
        val captureMethod = optString(json, FIELD_CAPTURE_METHOD)
        val clientSecret = optString(json, FIELD_CLIENT_SECRET)
        val confirmationMethod = optString(json, FIELD_CONFIRMATION_METHOD)
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
        val setupFutureUsage = StripeIntent.Usage.fromCode(
            optString(json, FIELD_SETUP_FUTURE_USAGE)
        )
        val nextAction = StripeJsonUtils.optMap(json, FIELD_NEXT_ACTION)
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

        return PaymentIntent(
            id = id,
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
            paymentMethod = paymentMethod,
            paymentMethodId = paymentMethodId,
            receiptEmail = receiptEmail,
            status = status,
            setupFutureUsage = setupFutureUsage,
            lastPaymentError = lastPaymentError,
            shipping = shipping,
            nextActionData = nextActionData
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
        override fun parse(json: JSONObject): PaymentIntent.Shipping? {
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

    internal class NextActionDataParser : ModelJsonParser<PaymentIntent.NextActionData> {
        override fun parse(json: JSONObject): PaymentIntent.NextActionData? {
            val nextActionType = NextActionType
                .fromCode(json.optString(FIELD_NEXT_ACTION_TYPE))
            val parser = when (nextActionType) {
                NextActionType.DisplayOxxoDetails -> DisplayOxxoDetailsJsonParser()
                NextActionType.RedirectToUrl -> RedirectToUrlParser()
                NextActionType.UseStripeSdk -> SdkDataJsonParser()
                else -> return null
            }
            return parser.parse(json.optJSONObject(nextActionType.code) ?: JSONObject())
        }

        private class DisplayOxxoDetailsJsonParser : ModelJsonParser<PaymentIntent.NextActionData.DisplayOxxoDetails> {
            override fun parse(json: JSONObject): PaymentIntent.NextActionData.DisplayOxxoDetails? {
                return PaymentIntent.NextActionData.DisplayOxxoDetails(
                    expiresAfter = json.optInt(FIELD_EXPIRES_AFTER),
                    number = optString(json, FIELD_NUMBER)
                )
            }

            private companion object {
                private const val FIELD_EXPIRES_AFTER = "expires_after"
                private const val FIELD_NUMBER = "number"
            }
        }

        internal class RedirectToUrlParser : ModelJsonParser<PaymentIntent.NextActionData.RedirectToUrl> {
            override fun parse(json: JSONObject): PaymentIntent.NextActionData.RedirectToUrl? {
                return when {
                    json.has(FIELD_URL) ->
                        PaymentIntent.NextActionData.RedirectToUrl(
                            Uri.parse(json.getString(FIELD_URL)),
                            json.optString(FIELD_RETURN_URL)
                        )
                    else -> null
                }
            }

            private companion object {
                internal const val FIELD_URL = "url"
                internal const val FIELD_RETURN_URL = "return_url"
            }
        }

        private class SdkDataJsonParser : ModelJsonParser<PaymentIntent.NextActionData.SdkData> {
            override fun parse(json: JSONObject): PaymentIntent.NextActionData.SdkData? {
                return when (optString(json, FIELD_TYPE)) {
                    TYPE_3DS1 -> PaymentIntent.NextActionData.SdkData.Use3D1(
                        json.optString(FIELD_STRIPE_JS)
                    )
                    TYPE_3DS2 -> PaymentIntent.NextActionData.SdkData.Use3DS2(
                        json.optString(FIELD_THREE_D_SECURE_2_SOURCE),
                        json.optString(FIELD_DIRECTORY_SERVER_NAME),
                        json.optString(FIELD_SERVER_TRANSACTION_ID),
                        parseDirectoryServerEncryption(
                            json.optJSONObject(FIELD_DIRECTORY_SERVER_ENCRYPTION)
                                ?: JSONObject())
                    )
                    else -> null
                }
            }

            private fun parseDirectoryServerEncryption(json: JSONObject):
                PaymentIntent.NextActionData.SdkData.Use3DS2.DirectoryServerEncryption {
                val rootCert =
                    jsonArrayToList(json.optJSONArray(FIELD_ROOT_CAS))
                        ?.fold(emptyList<String>()) { acc, entry ->
                            if (entry is String) {
                                acc.plus(entry)
                            } else {
                                acc
                            }
                        } ?: emptyList()

                return PaymentIntent.NextActionData.SdkData.Use3DS2.DirectoryServerEncryption(
                    json.optString(FIELD_DIRECTORY_SERVER_ID),
                    json.optString(FIELD_CERTIFICATE),
                    rootCert,
                    json.optString(FIELD_KEY_ID)
                )
            }

            private companion object {
                private const val FIELD_TYPE = "type"

                private const val TYPE_3DS2 = "stripe_3ds2_fingerprint"
                private const val TYPE_3DS1 = "three_d_secure_redirect"

                private const val FIELD_THREE_D_SECURE_2_SOURCE = "three_d_secure_2_source"
                private const val FIELD_DIRECTORY_SERVER_NAME = "directory_server_name"
                private const val FIELD_SERVER_TRANSACTION_ID = "server_transaction_id"
                private const val FIELD_DIRECTORY_SERVER_ENCRYPTION = "directory_server_encryption"

                private const val FIELD_DIRECTORY_SERVER_ID = "directory_server_id"
                private const val FIELD_CERTIFICATE = "certificate"
                private const val FIELD_KEY_ID = "key_id"
                private const val FIELD_ROOT_CAS = "root_certificate_authorities"

                private const val FIELD_STRIPE_JS = "stripe_js"
            }
        }

        private companion object {
            private const val FIELD_NEXT_ACTION_TYPE = "type"
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
        private const val FIELD_PAYMENT_METHOD = "payment_method"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_RECEIPT_EMAIL = "receipt_email"
        private const val FIELD_SHIPPING = "shipping"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
    }
}
