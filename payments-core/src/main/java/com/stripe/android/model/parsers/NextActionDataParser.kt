package com.stripe.android.model.parsers

import android.net.Uri
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.MicrodepositType
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.WeChat
import org.json.JSONObject

internal class NextActionDataParser : ModelJsonParser<StripeIntent.NextActionData> {
    override fun parse(
        json: JSONObject
    ): StripeIntent.NextActionData? {
        val nextActionType = StripeIntent.NextActionType.fromCode(
            json.optString(FIELD_NEXT_ACTION_TYPE)
        )
        val parser = when (nextActionType) {
            StripeIntent.NextActionType.DisplayOxxoDetails -> DisplayOxxoDetailsJsonParser()
            StripeIntent.NextActionType.DisplayPayNowDetails -> DisplayPayNowDetailsJsonParser()
            StripeIntent.NextActionType.DisplayBoletoDetails -> DisplayBoletoDetailsJsonParser()
            StripeIntent.NextActionType.DisplayKonbiniDetails -> DisplayKonbiniDetailsJsonParser()
            StripeIntent.NextActionType.DisplayMultibancoDetails -> DisplayMultibancoDetailsJsonParser()
            StripeIntent.NextActionType.RedirectToUrl -> RedirectToUrlParser()
            StripeIntent.NextActionType.UseStripeSdk -> SdkDataJsonParser()
            StripeIntent.NextActionType.AlipayRedirect -> AlipayRedirectParser()
            StripeIntent.NextActionType.BlikAuthorize -> BlikAuthorizeParser()
            StripeIntent.NextActionType.WeChatPayRedirect -> WeChatPayRedirectParser()
            StripeIntent.NextActionType.VerifyWithMicrodeposits -> VerifyWithMicrodepositsParser()
            StripeIntent.NextActionType.UpiAwaitNotification -> UpiAwaitNotificationParser()
            StripeIntent.NextActionType.CashAppRedirect -> CashAppRedirectParser()
            StripeIntent.NextActionType.SwishRedirect -> SwishRedirectParser()
            null -> return null
        }
        return parser.parse(json.optJSONObject(nextActionType.code) ?: JSONObject())
    }

    private class DisplayOxxoDetailsJsonParser :
        ModelJsonParser<StripeIntent.NextActionData.DisplayOxxoDetails> {
        override fun parse(
            json: JSONObject
        ): StripeIntent.NextActionData.DisplayOxxoDetails {
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

    private class DisplayPayNowDetailsJsonParser :
        ModelJsonParser<StripeIntent.NextActionData.DisplayPayNowDetails> {
        override fun parse(
            json: JSONObject
        ): StripeIntent.NextActionData.DisplayPayNowDetails {
            return StripeIntent.NextActionData.DisplayPayNowDetails(
                hostedVoucherUrl = optString(json, FIELD_HOSTED_INSTRUCTIONS_URL)
            )
        }

        private companion object {
            private const val FIELD_HOSTED_INSTRUCTIONS_URL = "hosted_instructions_url"
        }
    }

    private class DisplayBoletoDetailsJsonParser :
        ModelJsonParser<StripeIntent.NextActionData.DisplayBoletoDetails> {
        override fun parse(
            json: JSONObject
        ): StripeIntent.NextActionData.DisplayBoletoDetails {
            return StripeIntent.NextActionData.DisplayBoletoDetails(
                hostedVoucherUrl = optString(json, FIELD_HOSTED_VOUCHER_URL)
            )
        }

        private companion object {
            private const val FIELD_HOSTED_VOUCHER_URL = "hosted_voucher_url"
        }
    }

    private class DisplayKonbiniDetailsJsonParser :
        ModelJsonParser<StripeIntent.NextActionData.DisplayKonbiniDetails> {
        override fun parse(
            json: JSONObject
        ): StripeIntent.NextActionData.DisplayKonbiniDetails {
            return StripeIntent.NextActionData.DisplayKonbiniDetails(
                hostedVoucherUrl = optString(json, FIELD_HOSTED_VOUCHER_URL)
            )
        }

        private companion object {
            private const val FIELD_HOSTED_VOUCHER_URL = "hosted_voucher_url"
        }
    }

    private class DisplayMultibancoDetailsJsonParser :
        ModelJsonParser<StripeIntent.NextActionData.DisplayMultibancoDetails> {
        override fun parse(
            json: JSONObject
        ): StripeIntent.NextActionData.DisplayMultibancoDetails {
            return StripeIntent.NextActionData.DisplayMultibancoDetails(
                hostedVoucherUrl = optString(json, FIELD_HOSTED_VOUCHER_URL)
            )
        }

        private companion object {
            private const val FIELD_HOSTED_VOUCHER_URL = "hosted_voucher_url"
        }
    }

    internal class RedirectToUrlParser :
        ModelJsonParser<StripeIntent.NextActionData.RedirectToUrl> {
        override fun parse(
            json: JSONObject
        ): StripeIntent.NextActionData.RedirectToUrl? {
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
            const val FIELD_URL = "url"
            const val FIELD_RETURN_URL = "return_url"
        }
    }

    internal class AlipayRedirectParser :
        ModelJsonParser<StripeIntent.NextActionData.AlipayRedirect> {
        override fun parse(
            json: JSONObject
        ): StripeIntent.NextActionData.AlipayRedirect {
            return StripeIntent.NextActionData.AlipayRedirect(
                json.getString(FIELD_NATIVE_DATA),
                json.getString(FIELD_URL),
                optString(json, FIELD_RETURN_URL)
            )
        }

        private companion object {
            const val FIELD_NATIVE_DATA = "native_data"
            const val FIELD_RETURN_URL = "return_url"
            const val FIELD_URL = "url"
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
                    ),
                    optString(json, FIELD_THREE_D_SECURE_2_INTENT),
                    optString(json, FIELD_PUBLISHABLE_KEY)
                )
                else -> null
            }
        }

        private fun parseDirectoryServerEncryption(
            json: JSONObject
        ): StripeIntent.NextActionData.SdkData.Use3DS2.DirectoryServerEncryption {
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

            private const val FIELD_THREE_D_SECURE_2_INTENT = "three_d_secure_2_intent"
            private const val FIELD_PUBLISHABLE_KEY = "publishable_key"

            private const val FIELD_STRIPE_JS = "stripe_js"
        }
    }

    internal class BlikAuthorizeParser :
        ModelJsonParser<StripeIntent.NextActionData.BlikAuthorize> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.BlikAuthorize {
            return StripeIntent.NextActionData.BlikAuthorize
        }
    }

    internal class WeChatPayRedirectParser :
        ModelJsonParser<StripeIntent.NextActionData.WeChatPayRedirect> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.WeChatPayRedirect {
            return StripeIntent.NextActionData.WeChatPayRedirect(
                WeChat(
                    appId = json.optString(APP_ID),
                    nonce = json.optString(NONCE_STR),
                    packageValue = json.optString(
                        PACKAGE
                    ),
                    partnerId = json.optString(PARTNER_ID),
                    prepayId = json.optString(
                        PREPAY_ID
                    ),
                    timestamp = json.optString(TIMESTAMP),
                    sign = json.optString(SIGN)
                )
            )
        }

        private companion object {
            private const val APP_ID = "app_id"
            private const val NONCE_STR = "nonce_str"
            private const val PACKAGE = "package"
            private const val PARTNER_ID = "partner_id"
            private const val PREPAY_ID = "prepay_id"
            private const val TIMESTAMP = "timestamp"
            private const val SIGN = "sign"
        }
    }

    internal class VerifyWithMicrodepositsParser :
        ModelJsonParser<StripeIntent.NextActionData.VerifyWithMicrodeposits> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.VerifyWithMicrodeposits {
            return StripeIntent.NextActionData.VerifyWithMicrodeposits(
                arrivalDate = json.optLong(ARRIVAL_DATE),
                hostedVerificationUrl = json.optString(HOSTED_VERIFICATION_URL),
                microdepositType = parseMicrodepositType(json)
            )
        }

        private fun parseMicrodepositType(json: JSONObject): MicrodepositType {
            return MicrodepositType.entries.find {
                it.value == json.optString(MICRODEPOSIT_TYPE)
            } ?: MicrodepositType.UNKNOWN
        }

        private companion object {
            private const val ARRIVAL_DATE = "arrival_date"
            private const val HOSTED_VERIFICATION_URL = "hosted_verification_url"
            private const val MICRODEPOSIT_TYPE = "microdeposit_type"
        }
    }

    internal class UpiAwaitNotificationParser :
        ModelJsonParser<StripeIntent.NextActionData.UpiAwaitNotification> {
        override fun parse(json: JSONObject): StripeIntent.NextActionData.UpiAwaitNotification {
            return StripeIntent.NextActionData.UpiAwaitNotification
        }
    }

    internal class CashAppRedirectParser :
        ModelJsonParser<StripeIntent.NextActionData.CashAppRedirect> {

        override fun parse(json: JSONObject): StripeIntent.NextActionData.CashAppRedirect {
            return StripeIntent.NextActionData.CashAppRedirect(
                mobileAuthUrl = json.optString("mobile_auth_url"),
            )
        }
    }

    internal class SwishRedirectParser :
        ModelJsonParser<StripeIntent.NextActionData.SwishRedirect> {

        override fun parse(json: JSONObject): StripeIntent.NextActionData.SwishRedirect {
            return StripeIntent.NextActionData.SwishRedirect(
                mobileAuthUrl = json.optString("mobile_auth_url"),
            )
        }
    }

    private companion object {
        private const val FIELD_NEXT_ACTION_TYPE = "type"
    }
}
