package com.stripe.android.payments.samsungpay

import android.os.Bundle
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SpaySdk

internal object SamsungFactory {
    fun buildPartnerInfo(serviceId: String): PartnerInfo {
        val bundle = Bundle()
        bundle.putString(SpaySdk.PARTNER_SERVICE_TYPE, SpaySdk.ServiceType.INAPP_PAYMENT.toString())
        return PartnerInfo(serviceId, bundle)
    }
}
