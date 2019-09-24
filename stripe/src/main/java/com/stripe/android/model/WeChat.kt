package com.stripe.android.model

import com.stripe.android.ObjectBuilder
import java.util.Objects
import org.json.JSONObject

/**
 * [WeChat Pay Payments with Sources](https://stripe.com/docs/sources/wechat-pay)
 */
class WeChat private constructor(builder: Builder) : StripeModel() {
    val statementDescriptor: String?
    val appId: String?
    val nonce: String?
    val packageValue: String?
    val partnerId: String?
    val prepayId: String?
    val sign: String?
    val timestamp: String?
    val qrCodeUrl: String?

    init {
        statementDescriptor = builder.statementDescriptor
        appId = builder.appId
        nonce = builder.nonce
        packageValue = builder.packageValue
        partnerId = builder.partnerId
        prepayId = builder.prepayId
        sign = builder.sign
        timestamp = builder.timestamp
        qrCodeUrl = builder.qrCodeUrl
    }

    override fun hashCode(): Int {
        return Objects.hash(statementDescriptor, appId, nonce, packageValue, partnerId,
            prepayId, sign, timestamp)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is WeChat -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(obj: WeChat): Boolean {
        return statementDescriptor == obj.statementDescriptor &&
            appId == obj.appId &&
            nonce == obj.nonce &&
            packageValue == obj.packageValue &&
            partnerId == obj.partnerId &&
            prepayId == obj.prepayId &&
            sign == obj.sign &&
            timestamp == obj.timestamp &&
            qrCodeUrl == obj.qrCodeUrl
    }

    internal class Builder : ObjectBuilder<WeChat> {
        internal var statementDescriptor: String? = null
        internal var appId: String? = null
        internal var nonce: String? = null
        internal var packageValue: String? = null
        internal var partnerId: String? = null
        internal var prepayId: String? = null
        internal var sign: String? = null
        internal var timestamp: String? = null
        internal var qrCodeUrl: String? = null

        fun setStatementDescriptor(statementDescriptor: String?): Builder {
            this.statementDescriptor = statementDescriptor
            return this
        }

        fun setAppId(appId: String?): Builder {
            this.appId = appId
            return this
        }

        fun setNonce(nonce: String?): Builder {
            this.nonce = nonce
            return this
        }

        fun setPackageValue(packageValue: String?): Builder {
            this.packageValue = packageValue
            return this
        }

        fun setPartnerId(partnerId: String?): Builder {
            this.partnerId = partnerId
            return this
        }

        fun setPrepayId(prepayId: String?): Builder {
            this.prepayId = prepayId
            return this
        }

        fun setSign(sign: String?): Builder {
            this.sign = sign
            return this
        }

        fun setTimestamp(timestamp: String?): Builder {
            this.timestamp = timestamp
            return this
        }

        fun setQrCodeUrl(qrCodeUrl: String?): Builder {
            this.qrCodeUrl = qrCodeUrl
            return this
        }

        override fun build(): WeChat {
            return WeChat(this)
        }
    }

    companion object {
        private const val FIELD_APPID = "android_appId"
        private const val FIELD_NONCE = "android_nonceStr"
        private const val FIELD_PACKAGE = "android_package"
        private const val FIELD_PARTNERID = "android_partnerId"
        private const val FIELD_PREPAYID = "android_prepayId"
        private const val FIELD_SIGN = "android_sign"
        private const val FIELD_TIMESTAMP = "android_timeStamp"
        private const val FIELD_STATEMENT_DESCRIPTOR = "statement_descriptor"
        private const val FIELD_QR_CODE_URL = "qr_code_url"

        @JvmStatic
        fun fromJson(json: JSONObject): WeChat {
            return Builder()
                .setAppId(StripeJsonUtils.optString(json, FIELD_APPID))
                .setNonce(StripeJsonUtils.optString(json, FIELD_NONCE))
                .setPackageValue(StripeJsonUtils.optString(json, FIELD_PACKAGE))
                .setPartnerId(StripeJsonUtils.optString(json, FIELD_PARTNERID))
                .setPrepayId(StripeJsonUtils.optString(json, FIELD_PREPAYID))
                .setSign(StripeJsonUtils.optString(json, FIELD_SIGN))
                .setTimestamp(StripeJsonUtils.optString(json, FIELD_TIMESTAMP))
                .setStatementDescriptor(StripeJsonUtils.optString(json, FIELD_STATEMENT_DESCRIPTOR))
                .setQrCodeUrl(StripeJsonUtils.optString(json, FIELD_QR_CODE_URL))
                .build()
        }
    }
}
