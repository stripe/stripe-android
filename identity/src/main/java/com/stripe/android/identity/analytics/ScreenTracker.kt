package com.stripe.android.identity.analytics

import android.util.Log
import com.stripe.android.camera.framework.StatTracker
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.identity.injection.IdentityVerificationScope
import com.stripe.android.identity.networking.IdentityRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tracker for screen transition.
 */
@IdentityVerificationScope
internal class ScreenTracker @Inject constructor(
    private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    private val identityRepository: IdentityRepository
) {

    private var screenStatTracker: ScreenTransitionStatTracker? = null

    /**
     * Start screen transition, if there is a pending start transition drop it.
     */
    fun screenTransitionStart(
        fromScreenName: String? = null,
        startedAt: ClockMark = Clock.markNow()
    ) {
        // create a StatTracker with fromScreenName
        // if there is a screen already started, drop it
        screenStatTracker?.let {
            Log.e(
                TAG,
                "screenStateTracker is already set with ${it.fromScreenName}, when another " +
                    "screenTransition starts from $fromScreenName, dropping the old screenStateTracker."
            )
        }

        screenStatTracker =
            ScreenTransitionStatTracker(startedAt, fromScreenName) { toScreenName ->
                identityRepository.sendAnalyticsRequest(
                    identityAnalyticsRequestFactory.timeToScreen(
                        value = startedAt.elapsedSince().inMilliseconds.toLong(),
                        fromScreenName = fromScreenName,
                        toScreenName = requireNotNull(toScreenName)
                    )
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
            Log.e(
                TAG,
                "screenStateTracker is not set when screenTransition ends at $toScreenName"
            )
        }
    }

    private companion object {
        val TAG: String = ScreenTracker::class.java.simpleName
    }
}

private class ScreenTransitionStatTracker(
    override val startedAt: ClockMark,
    val fromScreenName: String?,
    private val onComplete: suspend (String?) -> Unit
) : StatTracker {
    override suspend fun trackResult(result: String?) =
        coroutineScope { launch { onComplete(result) } }.let { }
}
