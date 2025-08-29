package com.stripe.android.hcaptcha

import androidx.fragment.app.FragmentActivity
import com.stripe.android.hcaptcha.analytics.CaptchaEventsReporter
import com.stripe.hcaptcha.HCaptcha
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaTokenResponse
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnSuccessListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class DefaultHCaptchaService(
    private val hCaptchaProvider: HCaptchaProvider,
    private val captchaEventsReporter: CaptchaEventsReporter
) : HCaptchaService {
    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?
    ): HCaptchaService.Result {
        val hCaptcha = hCaptchaProvider.get()
        captchaEventsReporter.init(siteKey)
        val result = runCatching {
            startVerification(
                activity = activity,
                siteKey = siteKey,
                rqData = rqData,
                hCaptcha = hCaptcha
            )
        }.getOrElse { e ->
            HCaptchaService.Result.Failure(e)
        }
        when (result) {
            is HCaptchaService.Result.Failure -> {
                captchaEventsReporter.error(result.error, siteKey)
            }
            is HCaptchaService.Result.Success -> {
                captchaEventsReporter.success(siteKey)
            }
        }
        hCaptcha.reset()
        return result
    }

    private suspend fun startVerification(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        hCaptcha: HCaptcha
    ): HCaptchaService.Result {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                hCaptcha.reset()
            }

            hCaptcha.addOnSuccessListener(object : OnSuccessListener<HCaptchaTokenResponse> {
                override fun onSuccess(result: HCaptchaTokenResponse) {
                    continuation.resume(HCaptchaService.Result.Success(result.tokenResult))
                }
            }).addOnFailureListener(object : OnFailureListener {
                override fun onFailure(exception: HCaptchaException) {
                    continuation.resume(HCaptchaService.Result.Failure(exception))
                }
            })

            val config = HCaptchaConfig(
                siteKey = siteKey,
                size = HCaptchaSize.INVISIBLE,
                rqdata = rqData,
                loading = false,
                hideDialog = true,
                disableHardwareAcceleration = true,
                retryPredicate = { _, exception -> exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT }
            )

            hCaptcha.setup(activity, config).verifyWithHCaptcha(activity)
            captchaEventsReporter.execute(siteKey)
        }
    }
}
