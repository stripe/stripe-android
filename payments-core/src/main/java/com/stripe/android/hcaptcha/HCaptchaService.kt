package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.HCaptcha
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaTokenResponse
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnSuccessListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface HCaptchaService {
    suspend fun performPassiveHCaptcha(activity: FragmentActivity): Result

    sealed interface Result {
        data class Success(val token: String) : Result
        data class Failure(val error: Throwable) : Result
    }
}

internal class DefaultHCaptchaService: HCaptchaService {
    override suspend fun performPassiveHCaptcha(activity: FragmentActivity): HCaptchaService.Result {
        return suspendCoroutine { coroutine ->
            val hcaptcha = HCaptcha.getClient(activity).apply {
                addOnSuccessListener(object : OnSuccessListener<HCaptchaTokenResponse> {
                    override fun onSuccess(result: HCaptchaTokenResponse) {

                        coroutine.resume(HCaptchaService.Result.Success(result.tokenResult))
                    }
                })
                addOnFailureListener(object : OnFailureListener {
                    override fun onFailure(exception: HCaptchaException) {
                        coroutine.resume(HCaptchaService.Result.Failure(exception))
                    }
                })
            }

            val config = HCaptchaConfig(
                siteKey = "143aadb6-fb60-4ab6-b128-f7fe53426d4a",
                size = HCaptchaSize.INVISIBLE,
                rqdata = null,
                loading = false,
                hideDialog = true,
                disableHardwareAcceleration = true, // defaults to true in the example app, and seems more stable?
                retryPredicate = { _, exception -> exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT }
            )

            hcaptcha.setup(config).verifyWithHCaptcha()
        }
    }

}
