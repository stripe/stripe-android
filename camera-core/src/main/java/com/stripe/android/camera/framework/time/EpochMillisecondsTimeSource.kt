package com.stripe.android.camera.framework.time

import kotlin.time.AbstractLongTimeSource
import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit

class EpochMillisecondsTimeSource(private val startTimeMs: Long) : AbstractLongTimeSource(DurationUnit.MILLISECONDS) {
    override fun read(): Long = System.currentTimeMillis() - startTimeMs
}

fun Long.asEpochMillisecondsComparableTimeMark(): ComparableTimeMark = EpochMillisecondsTimeSource(this).markNow()
