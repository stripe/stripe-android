package com.stripe.android.hcaptcha

import android.os.SystemClock
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal class DefaultHCaptchaService(
    private val hCaptchaProvider: HCaptchaProvider,
    private val captchaEventsReporter: CaptchaEventsReporter,
    private val webViewStarter: WebViewStarter,
    private val storage: HCaptchaCacheStorage
) : HCaptchaService {
    override suspend fun warmUp(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?
    ) {
        if (storage.value.canWarmUp.not()) return
        storage.emit(CachedResult.Loading)
        val update = when (val result = performPassiveHCaptchaHelper(activity, siteKey, rqData)) {
            is HCaptchaService.Result.Failure -> {
                CachedResult.Failure(result.error)
            }
            is HCaptchaService.Result.Success -> {
                CachedResult.Success(result.token, createdAt = SystemClock.elapsedRealtime())
            }
        }
        storage.emit(update)
    }

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int?
    ): HCaptchaService.Result {
        captchaEventsReporter.attachStart()
        val isReady = storage.value.isReady
        val result = runCatching {
            withTimeout(TIMEOUT) {
                transformCachedResult(
                    activity,
                    siteKey,
                    rqData,
                    tokenTimeoutSeconds = tokenTimeoutSeconds ?: Int.MAX_VALUE
                )
            }
        }.getOrElse { e ->
            HCaptchaService.Result.Failure(e)
        }
        storage.emit(CachedResult.Idle)
        captchaEventsReporter.attachEnd(siteKey, isReady)
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
                host = "stripecdn.com",
                retryPredicate = { _, exception -> exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT }
            )

            hCaptcha.setup(activity, config).verifyWithHCaptcha(activity)
            captchaEventsReporter.execute(siteKey)
        }
    }

    private suspend fun performPassiveHCaptchaHelper(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
    ): HCaptchaService.Result {
        val hCaptcha = hCaptchaProvider.get()
        captchaEventsReporter.init(siteKey)
        val result = runCatching {
            webViewStarter.awaitStartUp(activity)
            startVerification(
                activity = activity,
                siteKey = siteKey,
                rqData = rqData,
                hCaptcha = hCaptcha,
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

    @Suppress("MagicNumber")
    private suspend fun transformCachedResult(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int
    ): HCaptchaService.Result {
        return storage.mapNotNull { cachedResult ->
            when (cachedResult) {
                CachedResult.Idle -> {
                    performPassiveHCaptchaHelper(activity, siteKey, rqData)
                }
                CachedResult.Loading -> {
                    null
                }
                is CachedResult.Success -> {
                    val elapsedSeconds = (SystemClock.elapsedRealtime() - cachedResult.createdAt) / 1000
                    val isExpired = elapsedSeconds >= tokenTimeoutSeconds
                    if (isExpired) {
                        performPassiveHCaptchaHelper(activity, siteKey, rqData)
                    } else {
                        HCaptchaService.Result.Success(cachedResult.token)
                    }
                }
                is CachedResult.Failure -> HCaptchaService.Result.Failure(cachedResult.error)
            }
        }.first()
    }

    companion object {
        internal val TIMEOUT = 6.seconds
    }
}
