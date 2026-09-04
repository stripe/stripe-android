package com.stripe.android.hcaptcha

import androidx.fragment.app.FragmentActivity
import app.cash.turbine.Turbine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeHCaptchaService : HCaptchaService {
    var result: HCaptchaService.Result? = null
    var warmUpResult: suspend () -> Unit = {}
    var cacheStateResult: Flow<HCaptchaService.CacheState> =
        MutableStateFlow(HCaptchaService.CacheState.NeedsRefresh)
    private val cacheStateCalls = Turbine<CacheStateCall>()
    private val passiveCaptchaTokenCalls = Turbine<PassiveCaptchaTokenCall>()
    private val performPassiveHCaptchaCalls = Turbine<Call>()
    private val warmUpCalls = Turbine<Call>()

    override fun cacheState(timeoutSeconds: Int?): Flow<HCaptchaService.CacheState> {
        cacheStateCalls.add(CacheStateCall(timeoutSeconds))
        return cacheStateResult
    }

    override suspend fun warmUp(activity: FragmentActivity, siteKey: String, rqData: String?) {
        warmUpCalls.add(Call(activity, siteKey, rqData))
        warmUpResult()
    }

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int?
    ): HCaptchaService.Result {
        performPassiveHCaptchaCalls.add(Call(activity, siteKey, rqData))
        return result ?: HCaptchaService.Result.Success("default_token")
    }

    override suspend fun passiveCaptchaToken(tokenTimeoutSeconds: Int?): HCaptchaService.Result {
        passiveCaptchaTokenCalls.add(PassiveCaptchaTokenCall(tokenTimeoutSeconds))
        return result ?: HCaptchaService.Result.Success("default_token")
    }

    suspend fun awaitCacheStateCall(): CacheStateCall {
        return cacheStateCalls.awaitItem()
    }

    suspend fun awaitPassiveCaptchaTokenCall(): PassiveCaptchaTokenCall {
        return passiveCaptchaTokenCalls.awaitItem()
    }

    suspend fun awaitPerformPassiveHCaptchaCall(): Call {
        return performPassiveHCaptchaCalls.awaitItem()
    }

    suspend fun awaitWarmUpCall(): Call {
        return warmUpCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        cacheStateCalls.ensureAllEventsConsumed()
        passiveCaptchaTokenCalls.ensureAllEventsConsumed()
        performPassiveHCaptchaCalls.ensureAllEventsConsumed()
        warmUpCalls.ensureAllEventsConsumed()
    }

    data class CacheStateCall(val timeoutSeconds: Int?)

    data class PassiveCaptchaTokenCall(val timeoutSeconds: Int?)

    data class Call(
        val activity: FragmentActivity,
        val siteKey: String,
        val rqData: String?
    )
}
