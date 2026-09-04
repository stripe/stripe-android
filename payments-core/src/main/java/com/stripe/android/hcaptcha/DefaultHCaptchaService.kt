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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal class DefaultHCaptchaService(
    private val hCaptchaProvider: HCaptchaProvider,
    private val captchaEventsReporter: CaptchaEventsReporter
) : HCaptchaService {
    private val cachedResult = MutableStateFlow<CachedResult>(CachedResult.Idle)

    override fun cacheState(timeoutSeconds: Int?): Flow<HCaptchaService.CacheState> {
        // A production implementation would observe the cached result, report Cached while its token is valid,
        // and report NeedsRefresh when the token is missing, failed, consumed, or expired using timeoutSeconds.
        return flowOf(HCaptchaService.CacheState.NeedsRefresh)
    }

    override suspend fun warmUp(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?
    ) {
        val currentResult = cachedResult.value
        if (currentResult.canWarmUp.not()) return
        if (cachedResult.compareAndSet(currentResult, CachedResult.Loading).not()) return
        try {
            val update = when (val result = performPassiveHCaptchaHelper(activity, siteKey, rqData)) {
                is HCaptchaService.Result.Failure -> {
                    CachedResult.Failure(result.error)
                }
                is HCaptchaService.Result.Success -> {
                    CachedResult.Success(result.token, createdAt = SystemClock.elapsedRealtime())
                }
            }
            cachedResult.compareAndSet(CachedResult.Loading, update)
        } catch (error: CancellationException) {
            cachedResult.compareAndSet(CachedResult.Loading, CachedResult.Idle)
            throw error
        }
    }

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int?
    ): HCaptchaService.Result {
        captchaEventsReporter.attachStart()
        val isReady = cachedResult.value.isReady
        val result = consumeCachedResult(
            tokenTimeoutSeconds = tokenTimeoutSeconds,
            onCacheMiss = { performPassiveHCaptchaHelper(activity, siteKey, rqData) }
        )
        captchaEventsReporter.attachEnd(siteKey, isReady)
        return result
    }

    override suspend fun passiveCaptchaToken(tokenTimeoutSeconds: Int?): HCaptchaService.Result {
        return consumeCachedResult(tokenTimeoutSeconds, onCacheMiss = null)
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
        return try {
            val result = runCatching {
                startVerification(
                    activity = activity,
                    siteKey = siteKey,
                    rqData = rqData,
                    hCaptcha = hCaptcha,
                )
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                HCaptchaService.Result.Failure(error)
            }
            when (result) {
                is HCaptchaService.Result.Failure -> {
                    captchaEventsReporter.error(result.error, siteKey)
                }
                is HCaptchaService.Result.Success -> {
                    captchaEventsReporter.success(siteKey)
                }
            }
            result
        } finally {
            hCaptcha.reset()
        }
    }

    private suspend fun consumeCachedResult(
        tokenTimeoutSeconds: Int?,
        onCacheMiss: (suspend () -> HCaptchaService.Result)?
    ): HCaptchaService.Result {
        return runCatching {
            withTimeout(TIMEOUT) {
                transformCachedResult(tokenTimeoutSeconds, onCacheMiss)
            }
        }.getOrElse { error ->
            when (error) {
                is TimeoutCancellationException -> HCaptchaService.Result.Failure(error)
                is CancellationException -> throw error
                else -> HCaptchaService.Result.Failure(error)
            }
        }
    }

    private suspend fun transformCachedResult(
        tokenTimeoutSeconds: Int?,
        onCacheMiss: (suspend () -> HCaptchaService.Result)?
    ): HCaptchaService.Result {
        return cachedResult.mapNotNull { cachedResult ->
            when (cachedResult) {
                CachedResult.Idle -> {
                    performCacheMiss(onCacheMiss)
                }
                CachedResult.Loading -> {
                    null
                }
                is CachedResult.Success -> {
                    if (cachedResult.isExpired(tokenTimeoutSeconds)) {
                        this.cachedResult.compareAndSet(cachedResult, CachedResult.Idle)
                        null
                    } else {
                        cachedResult.consume(
                            HCaptchaService.Result.Success(cachedResult.token)
                        )
                    }
                }
                is CachedResult.Failure -> cachedResult.consume(
                    HCaptchaService.Result.Failure(cachedResult.error)
                )
            }
        }.first()
    }

    private suspend fun performCacheMiss(
        onCacheMiss: (suspend () -> HCaptchaService.Result)?
    ): HCaptchaService.Result? {
        if (onCacheMiss == null) return null
        if (cachedResult.compareAndSet(CachedResult.Idle, CachedResult.Loading).not()) return null
        return try {
            onCacheMiss()
        } finally {
            cachedResult.compareAndSet(CachedResult.Loading, CachedResult.Idle)
        }
    }

    private fun CachedResult.consume(result: HCaptchaService.Result): HCaptchaService.Result? {
        return if (cachedResult.compareAndSet(this, CachedResult.Idle)) result else null
    }

    private fun CachedResult.Success.isExpired(tokenTimeoutSeconds: Int?): Boolean {
        return remainingLifetimeMillis(tokenTimeoutSeconds)?.let { it <= 0 } ?: false
    }

    private fun CachedResult.Success.remainingLifetimeMillis(tokenTimeoutSeconds: Int?): Long? {
        val lifetimeMillis = tokenTimeoutSeconds?.seconds?.inWholeMilliseconds ?: return null
        val elapsedMillis = SystemClock.elapsedRealtime() - createdAt
        return lifetimeMillis - elapsedMillis
    }

    sealed interface CachedResult {
        data object Idle : CachedResult
        data object Loading : CachedResult
        data class Success(
            val token: String,
            val createdAt: Long
        ) : CachedResult
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

    companion object {
        internal val TIMEOUT = 6.seconds
    }
}
