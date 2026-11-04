package com.stripe.android.hcaptcha

internal sealed interface CachedResult {
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
