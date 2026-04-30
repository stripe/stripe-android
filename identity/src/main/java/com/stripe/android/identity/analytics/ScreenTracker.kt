package com.stripe.android.identity.analytics

import android.util.Log
import com.stripe.android.camera.framework.StatTracker
import com.stripe.android.identity.injection.IdentityVerificationScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * Tracker for screen transition.
 */
@IdentityVerificationScope
internal class ScreenTracker @Inject constructor(
    private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
) {

    private var screenStatTracker: ScreenTransitionStatTracker? = null

    /**
     * Start screen transition, if there is a pending start transition drop it.
     */
    fun screenTransitionStart(
        fromScreenName: String? = null,
        startedAt: ComparableTimeMark = TimeSource.Monotonic.markNow()
    ) {
        // create a StatTracker with fromScreenName
        // if there is a screen already started, drop it
        screenStatTracker?.let {
            val error = IllegalStateException(
                "screenStateTracker is already set with ${it.fromScreenName}, when another " +
                    "screenTransition starts from $fromScreenName, dropping the old screenStateTracker."
            )
            Log.e(TAG, error.message, error)
            identityAnalyticsRequestFactory.genericError(
                throwable = error,
                additionalMetadata = mapOf(
                    IdentityAnalyticsRequestFactory.PARAM_ERROR_CONTEXT to
                        IdentityAnalyticsRequestFactory.ERROR_CONTEXT_SCREEN_TRANSITION,
                    IdentityAnalyticsRequestFactory.PARAM_FROM_SCREEN_NAME to fromScreenName
                )
            )
        }

        screenStatTracker =
            ScreenTransitionStatTracker(startedAt, fromScreenName) { toScreenName ->
                identityAnalyticsRequestFactory.timeToScreen(
                    value = startedAt.elapsedNow().inWholeMilliseconds,
                    fromScreenName = fromScreenName,
                    toScreenName = requireNotNull(toScreenName)
                )
            }
    }

    /**
     * Finish screen transition and send analytics event for the transition.
     */
    suspend fun screenTransitionFinish(toScreenName: String) {
        screenStatTracker?.let {
            it.trackResult(toScreenName)
            screenStatTracker = null
        } ?: run {
            val error = IllegalStateException(
                "screenStateTracker is not set when screenTransition ends at $toScreenName"
            )
            Log.e(TAG, error.message, error)
            identityAnalyticsRequestFactory.genericError(
                throwable = error,
                additionalMetadata = mapOf(
                    IdentityAnalyticsRequestFactory.PARAM_ERROR_CONTEXT to
                        IdentityAnalyticsRequestFactory.ERROR_CONTEXT_SCREEN_TRANSITION,
                    IdentityAnalyticsRequestFactory.PARAM_TO_SCREEN_NAME to toScreenName
                )
            )
        }
    }

    private companion object {
        val TAG: String = ScreenTracker::class.java.simpleName
    }
}

private class ScreenTransitionStatTracker(
    override val startedAt: ComparableTimeMark,
    val fromScreenName: String?,
    private val onComplete: suspend (String?) -> Unit
) : StatTracker {
    override suspend fun trackResult(result: String?) =
        coroutineScope { launch { onComplete(result) } }.let { }
}
