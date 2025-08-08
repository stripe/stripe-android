package com.stripe.android.hcaptcha

import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaTokenResponse
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnSuccessListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class DefaultHCaptchaService(
    private val hCaptchaProvider: HCaptchaProvider
) : HCaptchaService {

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?
    ): HCaptchaService.Result {
        return suspendCoroutine { coroutine ->
            val hcaptcha = hCaptchaProvider.get(activity).apply {
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
                siteKey = siteKey,
                size = HCaptchaSize.INVISIBLE,
                rqdata = rqData,
                loading = false,
                hideDialog = true,
                disableHardwareAcceleration = true,
                retryPredicate = { _, exception -> exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT }
            )

            hcaptcha.setup(config).verifyWithHCaptcha()
        }
    }
}
