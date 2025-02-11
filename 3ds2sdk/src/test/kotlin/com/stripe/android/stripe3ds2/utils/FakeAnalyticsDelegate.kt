package com.stripe.android.stripe3ds2.utils

class FakeAnalyticsDelegate : AnalyticsDelegate {
    override fun didReceiveChallengeResponseWithTransactionId(transactionId: String, flow: String) {
        // NO-OP
    }

    override fun cancelButtonTappedWithTransactionId(transactionId: String) {
        // NO-OP
    }

    override fun otpSubmitButtonTappedWithTransactionID(transactionId: String) {
        // NO-OP
    }

    override fun oobContinueButtonTappedWithTransactionID(transactionId: String) {
        // NO-OP
    }

    override fun oobFlowDidPause(transactionId: String) {
        // NO-OP
    }

    override fun oobFlowDidResume(transactionId: String) {
        // NO-OP
    }
}
