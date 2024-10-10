package com.stripe.android.stripe3ds2.utils

interface AnalyticsDelegate {
    fun didReceiveChallengeResponseWithTransactionId(transactionId: String, flow: String)

    fun cancelButtonTappedWithTransactionId(transactionId: String)

    fun otpSubmitButtonTappedWithTransactionID(transactionId: String)

    fun oobContinueButtonTappedWithTransactionID(transactionId: String)

    fun oobFlowDidPause(transactionId: String)

    fun oobFlowDidResume(transactionId: String)
}
