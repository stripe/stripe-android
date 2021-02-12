package com.stripe.android.model.parsers

import android.net.Uri
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class NextActionDataParser : ModelJsonParser<StripeIntent.NextActionData> {
    override fun parse(json: JSONObject): StripeIntent.NextActionData? {
        val nextActionType = StripeIntent.NextActionType.fromCode(json.optString(FIELD_NEXT_ACTION_TYPE))
        val parser = when (nextActionType) {
            StripeIntent.NextActionType.DisplayOxxoDetails -> DisplayOxxoDetailsJsonParser()
            StripeIntent.NextActionType.RedirectToUrl -> RedirectToUrlParser()
            StripeIntent.NextActionType.UseStripeSdk -> SdkDataJsonParser()
            StripeIntent.NextActionType.AlipayRedirect -> AlipayRedirectParser()
            StripeIntent.NextActionType.UpiAppRedirect -> UpiAppRedirectParser()
            else -> return null
        }
        return parser.parse(json.optJSONObject(nextActionType.code) ?: JSONObject())
    }

    private class DisplayOxxoDetailsJsonParser : ModelJsonParser<StripeIntent.NextActionData.DisplayOxxoDetails> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.DisplayOxxoDetails? {
            return StripeIntent.NextActionData.DisplayOxxoDetails(
                expiresAfter = json.optInt(FIELD_EXPIRES_AFTER),
                number = optString(json, FIELD_NUMBER),
                hostedVoucherUrl = optString(json, FIELD_HOSTED_VOUCHER_URL)
            )
        }

        private companion object {
            private const val FIELD_EXPIRES_AFTER = "expires_after"
            private const val FIELD_NUMBER = "number"
            private const val FIELD_HOSTED_VOUCHER_URL = "hosted_voucher_url"
        }
    }

    internal class RedirectToUrlParser : ModelJsonParser<StripeIntent.NextActionData.RedirectToUrl> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.RedirectToUrl? {
            return when {
                json.has(FIELD_URL) ->
                    StripeIntent.NextActionData.RedirectToUrl(
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

    internal class UpiAppRedirectParser : ModelJsonParser<StripeIntent.NextActionData.UpiAppRedirect> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.UpiAppRedirect? {
            return StripeJsonUtils.optString(json, FIELD_NATIVE_DATA)?.let {
                StripeIntent.NextActionData.UpiAppRedirect(it)
            }
        }

        private companion object {
            internal const val FIELD_NATIVE_DATA = "native_data"
        }
    }

    internal class AlipayRedirectParser : ModelJsonParser<StripeIntent.NextActionData.AlipayRedirect> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.AlipayRedirect? {
            return StripeIntent.NextActionData.AlipayRedirect(
                json.getString(FIELD_NATIVE_DATA),
                json.getString(FIELD_URL),
                optString(json, FIELD_RETURN_URL)
            )
        }

        private companion object {
            internal const val FIELD_NATIVE_DATA = "native_data"
            internal const val FIELD_RETURN_URL = "return_url"
            internal const val FIELD_URL = "url"
        }
    }

    private class SdkDataJsonParser : ModelJsonParser<StripeIntent.NextActionData.SdkData> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.SdkData? {
            return when (optString(json, FIELD_TYPE)) {
                TYPE_3DS1 -> StripeIntent.NextActionData.SdkData.Use3DS1(
                    json.optString(FIELD_STRIPE_JS)
                )
                TYPE_3DS2 -> StripeIntent.NextActionData.SdkData.Use3DS2(
                    json.optString(FIELD_THREE_D_SECURE_2_SOURCE),
                    json.optString(FIELD_DIRECTORY_SERVER_NAME),
                    json.optString(FIELD_SERVER_TRANSACTION_ID),
                    parseDirectoryServerEncryption(
                        json.optJSONObject(FIELD_DIRECTORY_SERVER_ENCRYPTION)
                            ?: JSONObject()
                    )
                )
                else -> null
            }
        }

        private fun parseDirectoryServerEncryption(json: JSONObject):
            StripeIntent.NextActionData.SdkData.Use3DS2.DirectoryServerEncryption {
                val rootCert =
                    StripeJsonUtils.jsonArrayToList(json.optJSONArray(FIELD_ROOT_CAS))
                        ?.fold(emptyList<String>()) { acc, entry ->
                            if (entry is String) {
                                acc.plus(entry)
                            } else {
                                acc
                            }
                        } ?: emptyList()

                return StripeIntent.NextActionData.SdkData.Use3DS2.DirectoryServerEncryption(
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
