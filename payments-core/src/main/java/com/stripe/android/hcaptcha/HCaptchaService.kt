package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface HCaptchaService {
    fun cacheState(timeoutSeconds: Int?): Flow<CacheState>

    suspend fun warmUp(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
    )

    suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int?
    ): Result

    suspend fun passiveCaptchaToken(tokenTimeoutSeconds: Int?): Result

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val token: String) : Result

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val error: Throwable) : Result
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface CacheState {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object NeedsRefresh : CacheState

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Cached : CacheState
    }
}
