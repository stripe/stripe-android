package com.stripe.android.payments.samsungpay

import android.os.Bundle
import android.util.Log
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SpaySdk

private const val TAG = "SamsungPayViewModel"
private const val SERVICE_ID = "0915499788d6493aa3a038"

internal object SamsungFactory {
    fun buildPartnerInfo(): PartnerInfo {
        val bundle = Bundle()
        bundle.putString(SpaySdk.PARTNER_SERVICE_TYPE, SpaySdk.ServiceType.INAPP_PAYMENT.toString())
        Log.d(TAG, "buildPartnerInfo: serviceType=${SpaySdk.ServiceType.INAPP_PAYMENT}")
        return PartnerInfo(SERVICE_ID, bundle)
    }
}