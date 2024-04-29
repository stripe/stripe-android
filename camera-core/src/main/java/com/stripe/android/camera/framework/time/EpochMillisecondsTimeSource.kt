@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.camera.framework.time

import androidx.annotation.RestrictTo
import kotlin.time.AbstractLongTimeSource
import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EpochMillisecondsTimeSource(private val startTimeMs: Long) : AbstractLongTimeSource(DurationUnit.MILLISECONDS) {
    override fun read(): Long = System.currentTimeMillis() - startTimeMs
}

fun Long.asEpochMillisecondsComparableTimeMark(): ComparableTimeMark = EpochMillisecondsTimeSource(this).markNow()
