package com.stripe.android.crypto.onramp.samsungpay

import android.os.Bundle

internal object FakeSamsungPaySdkState {
    var statusResult: StatusResult = StatusResult.Success(SpaySdk.SPAY_READY, Bundle())
    var statusPartnerInfo: PartnerInfo? = null
    var paymentPartnerInfo: PartnerInfo? = null
    var paymentInfo: CustomSheetPaymentInfo? = null
    var paymentListener: PaymentManager.CustomSheetTransactionInfoListener? = null
    var updatedSheet: CustomSheet? = null

    fun reset() {
        statusResult = StatusResult.Success(SpaySdk.SPAY_READY, Bundle())
        statusPartnerInfo = null
        paymentPartnerInfo = null
        paymentInfo = null
        paymentListener = null
        updatedSheet = null
    }

    fun deliverStatus(listener: StatusListener) {
        when (val result = statusResult) {
            is StatusResult.Success -> listener.onSuccess(result.status, result.data)
            is StatusResult.Failure -> listener.onFail(result.errorCode, result.data)
        }
    }

    sealed interface StatusResult {
        data class Success(val status: Int, val data: Bundle?) : StatusResult

        data class Failure(val errorCode: Int, val data: Bundle?) : StatusResult
    }
}
