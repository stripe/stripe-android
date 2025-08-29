package com.stripe.android.challenge

import android.content.Context
import app.cash.turbine.Turbine
import com.stripe.android.hcaptcha.HCaptchaService

internal class FakeHCaptchaService : HCaptchaService {
    var result: HCaptchaService.Result? = null
    private val calls = Turbine<Call>()

    override suspend fun performPassiveHCaptcha(
        context: Context,
        siteKey: String,
        rqData: String?
    ): HCaptchaService.Result {
        calls.add(Call(context, siteKey, rqData))
        return result ?: HCaptchaService.Result.Success("default_token")
    }

    suspend fun awaitCall(): Call {
        return calls.awaitItem()
    }

    data class Call(
        val context: Context,
        val siteKey: String,
        val rqData: String?
    )
}
