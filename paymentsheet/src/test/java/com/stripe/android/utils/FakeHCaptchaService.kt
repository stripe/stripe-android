package com.stripe.android.utils

import androidx.fragment.app.FragmentActivity
import app.cash.turbine.Turbine
import com.stripe.android.hcaptcha.HCaptchaService

internal class FakeHCaptchaService : HCaptchaService {
    var warmUpResult: suspend () -> Unit = {}
    private val warmUpCalls = Turbine<WarmUpCall>()
    private val verificationCalls = Turbine<VerificationCall>()

    override suspend fun warmUp(activity: FragmentActivity, siteKey: String, rqData: String?) {
        warmUpCalls.add(WarmUpCall(activity, siteKey, rqData))
        warmUpResult()
    }

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int?,
    ): HCaptchaService.Result {
        verificationCalls.add(VerificationCall(activity, siteKey, rqData, tokenTimeoutSeconds))
        return HCaptchaService.Result.Success("test_token")
    }

    suspend fun awaitWarmUpCall(): WarmUpCall = warmUpCalls.awaitItem()

    fun ensureAllEventsConsumed() {
        warmUpCalls.ensureAllEventsConsumed()
        verificationCalls.ensureAllEventsConsumed()
    }

    data class WarmUpCall(val activity: FragmentActivity, val siteKey: String, val rqData: String?)

    data class VerificationCall(
        val activity: FragmentActivity,
        val siteKey: String,
        val rqData: String?,
        val tokenTimeoutSeconds: Int?,
    )
}
