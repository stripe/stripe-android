package com.stripe.android.model

import com.stripe.android.model.parsers.WeChatJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * [WeChat Pay Payments with Sources](https://stripe.com/docs/sources/wechat-pay)
 */
@Parcelize
data class WeChat internal constructor(
    val statementDescriptor: String?,
    val appId: String?,
    val nonce: String?,
    val packageValue: String?,
    val partnerId: String?,
    val prepayId: String?,
    val sign: String?,
    val timestamp: String?,
    val qrCodeUrl: String? = null
) : StripeModel {
    internal companion object {
        @JvmSynthetic
        internal fun fromJson(json: JSONObject): WeChat {
            return WeChatJsonParser().parse(json)
        }
    }
}
