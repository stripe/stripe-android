package com.stripe.android.polling

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntentStatusPoller {
    val state: StateFlow<StripeIntent.Status?>

    fun startPolling(scope: CoroutineScope)
    suspend fun forcePoll(): StripeIntent.Status?
    fun stopPolling()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Config(
        val clientSecret: String,
        val pollingStrategy: PollingStrategy,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    sealed class PollingStrategy : Parcelable {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class ExponentialBackoff(
            val maxAttempts: Int,
        ) : PollingStrategy()

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class FixedIntervals(
            val retryIntervalInSeconds: Int,
        ) : PollingStrategy()
    }
}
