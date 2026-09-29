package com.stripe.android.crypto.onramp.samsungpay

import android.content.Context
import android.os.Bundle

abstract class SpaySdk {
    enum class ServiceType {
        INAPP_PAYMENT,
    }

    enum class SdkApiLevel(
        val level: String,
    ) {
        LEVEL_2_22("2.22"),
    }

    enum class Brand {
        VISA,
        MASTERCARD,
        AMERICANEXPRESS,
        DISCOVER,
    }

    companion object {
        const val PARTNER_SERVICE_TYPE = "PartnerServiceType"
        const val PARTNER_SDK_API_LEVEL = "PartnerSdkApiLevel"

        const val ERROR_USER_CANCELED = -7
        const val ERROR_SPAY_SETUP_NOT_COMPLETED = -356
        const val ERROR_SPAY_APP_NEED_TO_UPDATE = -357

        const val SPAY_NOT_SUPPORTED = 0
        const val SPAY_NOT_READY = 1
        const val SPAY_READY = 2
        const val SPAY_NOT_ALLOWED_TEMPORALLY = 3
    }
}

class PartnerInfo(
    val serviceId: String,
    val data: Bundle,
)

interface StatusListener {
    fun onSuccess(status: Int, data: Bundle?)

    fun onFail(errorCode: Int, data: Bundle?)
}

class SamsungPay(
    context: Context,
    partnerInfo: PartnerInfo,
) : SpaySdk() {
    init {
        check(context.applicationContext != null)
        FakeSamsungPaySdkState.statusPartnerInfo = partnerInfo
    }

    fun getSamsungPayStatus(listener: StatusListener) {
        FakeSamsungPaySdkState.deliverStatus(listener)
    }

    companion object {
        const val EXTRA_ERROR_REASON = "errorReason"
    }
}
