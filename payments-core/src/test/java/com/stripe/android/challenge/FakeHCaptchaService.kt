package com.stripe.android.challenge

import androidx.fragment.app.FragmentActivity
import app.cash.turbine.Turbine
import com.stripe.android.hcaptcha.HCaptchaService

internal class FakeHCaptchaService : HCaptchaService {
    var result: HCaptchaService.Result? = null
    private val calls = Turbine<Call>()

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?
    ): HCaptchaService.Result {
        calls.add(Call(activity, siteKey, rqData))
        return result ?: HCaptchaService.Result.Success("default_token")
    }

    suspend fun awaitCall(): Call {
        return calls.awaitItem()
    }

    data class Call(
        val activity: FragmentActivity,
        val siteKey: String,
        val rqData: String?
    )
}
