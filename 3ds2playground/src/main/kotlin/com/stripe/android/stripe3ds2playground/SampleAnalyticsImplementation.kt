package com.stripe.android.stripe3ds2playground

import android.util.Log
import com.stripe.android.stripe3ds2.utils.AnalyticsDelegate

internal class SampleAnalyticsImplementation: AnalyticsDelegate {

    override fun didReceiveChallengeResponseWithTransactionId(transactionId: String, flow: String) {
        Log.d("Playground Analytics", "Did Receive Challenge With Trans ID: $transactionId, $flow")
    }

    override fun cancelButtonTappedWithTransactionId(transactionId: String) {
        Log.d("Playground Analytics", "Cancel Button Tapped With Trans ID: $transactionId")
    }

    override fun otpSubmitButtonTappedWithTransactionID(transactionId: String) {
        Log.d("Playground Analytics", "OTP Submit Tapped With Trans ID: $transactionId")
    }

    override fun oobContinueButtonTappedWithTransactionID(transactionId: String) {
        Log.d("Playground Analytics", "OOB Continue Tapped With Trans ID: $transactionId")
    }

    override fun oobFlowDidPause(transactionId: String) {
        Log.d("Playground Analytics", "OOB Flow Paused: $transactionId")
    }

    override fun oobFlowDidResume(transactionId: String) {
        Log.d("Playground Analytics", "OOB Flow Resumed: $transactionId")
    }
}