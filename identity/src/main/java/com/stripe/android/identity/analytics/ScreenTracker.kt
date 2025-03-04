package com.stripe.android.identity.analytics

import android.util.Log
import com.stripe.android.camera.framework.StatTracker
import com.stripe.android.identity.injection.IdentityVerificationScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

private class ScreenTransitionStatTracker(
    override val startedAt: ComparableTimeMark,
    val fromScreenName: String?,
    private val onComplete: suspend (String?) -> Unit
) : StatTracker {
    override suspend fun trackResult(result: String?) =
        coroutineScope { launch { onComplete(result) } }.let { }
}
