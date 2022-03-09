package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.WeChat
import org.json.JSONObject

internal class WeChatJsonParser : ModelJsonParser<WeChat> {
    override fun parse(json: JSONObject): WeChat {
        return WeChat(
            appId = StripeJsonUtils.optString(json, FIELD_APPID),
            statementDescriptor = StripeJsonUtils.optString(json, FIELD_STATEMENT_DESCRIPTOR),
            nonce = StripeJsonUtils.optString(json, FIELD_NONCE),
            packageValue = StripeJsonUtils.optString(json, FIELD_PACKAGE),
            partnerId = StripeJsonUtils.optString(json, FIELD_PARTNERID),
            prepayId = StripeJsonUtils.optString(json, FIELD_PREPAYID),
            sign = StripeJsonUtils.optString(json, FIELD_SIGN),
            timestamp = StripeJsonUtils.optString(json, FIELD_TIMESTAMP),
            qrCodeUrl = StripeJsonUtils.optString(json, FIELD_QR_CODE_URL)
        )
    }

    private companion object {
        private const val FIELD_APPID = "android_appId"
        private const val FIELD_NONCE = "android_nonceStr"
        private const val FIELD_PACKAGE = "android_package"
        private const val FIELD_PARTNERID = "android_partnerId"
        private const val FIELD_PREPAYID = "android_prepayId"
        private const val FIELD_SIGN = "android_sign"
        private const val FIELD_TIMESTAMP = "android_timeStamp"
        private const val FIELD_STATEMENT_DESCRIPTOR = "statement_descriptor"
        private const val FIELD_QR_CODE_URL = "qr_code_url"
    }
}
