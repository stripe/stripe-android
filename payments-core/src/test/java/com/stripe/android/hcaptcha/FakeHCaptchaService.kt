package com.stripe.android.hcaptcha

import androidx.fragment.app.FragmentActivity
import app.cash.turbine.Turbine

internal class FakeHCaptchaService : HCaptchaService {
    var result: HCaptchaService.Result? = null
    var warmUpResult: suspend () -> Unit = {}
    private val performPassiveHCaptchaCalls = Turbine<Call>()
    private val warmUpCalls = Turbine<Call>()

    override suspend fun warmUp(activity: FragmentActivity, siteKey: String, rqData: String?) {
        warmUpCalls.add(Call(activity, siteKey, rqData))
        warmUpResult()
    }

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?
    ): HCaptchaService.Result {
        performPassiveHCaptchaCalls.add(Call(activity, siteKey, rqData))
        return result ?: HCaptchaService.Result.Success("default_token")
    }

    suspend fun awaitPerformPassiveHCaptchaCall(): Call {
        return performPassiveHCaptchaCalls.awaitItem()
    }

    suspend fun awaitWarmUpCall(): Call {
        return warmUpCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        performPassiveHCaptchaCalls.ensureAllEventsConsumed()
        warmUpCalls.ensureAllEventsConsumed()
    }

    data class Call(
        val activity: FragmentActivity,
        val siteKey: String,
        val rqData: String?
    )
}
