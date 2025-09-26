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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.time.Duration

internal class DefaultHCaptchaService(
    private val hCaptchaProvider: HCaptchaProvider,
    private val captchaEventsReporter: CaptchaEventsReporter
) : HCaptchaService {
    private val cachedResult = MutableStateFlow<CachedResult>(CachedResult.Idle)

    override suspend fun warmUp(activity: FragmentActivity, siteKey: String, rqData: String?) {
        if (cachedResult.value.canWarmUp.not()) return
        cachedResult.emit(CachedResult.Loading)
        val update = when (val result = performPassiveHCaptchaHelper(activity, siteKey, rqData)) {
            is HCaptchaService.Result.Failure -> {
                CachedResult.Failure(result.error)
            }
            is HCaptchaService.Result.Success -> {
                CachedResult.Success(result.token)
            }
        }
        cachedResult.emit(update)
    }

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        timeout: Duration?
    ): HCaptchaService.Result {
        captchaEventsReporter.attachStart()
        val isReady = cachedResult.value.isReady
        val result = runCatching {
            withOptionalTimeout(timeout) {
                transformCachedResult(activity, siteKey, rqData)
            }
        }.getOrElse { e ->
            HCaptchaService.Result.Failure(e)
        }
        cachedResult.emit(CachedResult.Idle)
        captchaEventsReporter.attachEnd(siteKey, isReady)
        return result
    }

    private suspend fun withOptionalTimeout(
        duration: Duration?,
        block: suspend () -> HCaptchaService.Result
    ): HCaptchaService.Result {
        if (duration == null) return block()
        return withTimeout(duration) {
            block()
        }
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

    private suspend fun transformCachedResult(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?
    ): HCaptchaService.Result {
        return cachedResult.mapNotNull { cachedResult ->
            when (cachedResult) {
                CachedResult.Idle -> {
                    performPassiveHCaptchaHelper(activity, siteKey, rqData)
                }
                CachedResult.Loading -> {
                    null
                }
                is CachedResult.Success -> HCaptchaService.Result.Success(cachedResult.token)
                is CachedResult.Failure -> HCaptchaService.Result.Failure(cachedResult.error)
            }
        }.first()
    }

    sealed interface CachedResult {
        data object Idle : CachedResult
        data object Loading : CachedResult
        data class Success(val token: String) : CachedResult
        data class Failure(val error: Throwable) : CachedResult

        val canWarmUp: Boolean
            get() {
                return when (this) {
                    is Failure, Idle -> true
                    Loading, is Success -> false
                }
            }

        val isReady: Boolean
            get() {
                return when (this) {
                    Loading, Idle -> false
                    is Failure, is Success -> true
                }
            }
    }
}
