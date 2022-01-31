package com.stripe.android.camera.framework.time

import androidx.annotation.RestrictTo
import kotlin.math.round
import kotlin.math.roundToLong

/**
 * Round a number to a specified number of digits.
 */
private fun Double.roundTo(numberOfDigits: Int): Double {
    var multiplier = 1.0F
    repeat(numberOfDigits) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

/**
 * Since kotlin time is still experimental, implement our own version for utility.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class Duration : Comparable<Duration> {

    companion object {
        val ZERO: Duration = DurationNanoseconds(0)
        val INFINITE: Duration = DurationInfinitePositive
        val NEGATIVE_INFINITE: Duration = DurationInfiniteNegative
    }

    abstract val inYears: Double

    abstract val inMonths: Double

    abstract val inWeeks: Double

    abstract val inDays: Double

    abstract val inHours: Double

    abstract val inMinutes: Double

    abstract val inSeconds: Double

    abstract val inMilliseconds: Double

    abstract val inMicroseconds: Double

    abstract val inNanoseconds: Long

    override fun equals(other: Any?): Boolean =
        if (other is Duration) inNanoseconds == other.inNanoseconds else false

    override fun hashCode(): Int = inNanoseconds.toInt()

    override fun toString(): String = when {
        inYears > 1 -> "${inYears.roundTo(2)} years"
        inMonths > 1 -> "${inMonths.roundTo(2)} months"
        inWeeks > 1 -> "${inWeeks.roundTo(2)} weeks"
        inDays > 1 -> "${inDays.roundTo(2)} days"
        inHours > 1 -> "${inHours.roundTo(2)} hours"
        inMinutes > 1 -> "${inMinutes.roundTo(2)} minutes"
        inSeconds > 1 -> "${inSeconds.roundTo(2)} seconds"
        inMilliseconds > 1 -> "${inMilliseconds.roundTo(2)} milliseconds"
        inMicroseconds > 1 -> "${inMicroseconds.roundTo(2)} microseconds"
        else -> "$inNanoseconds nanoseconds"
    }

    open operator fun plus(other: Duration): Duration =
        DurationNanoseconds(inNanoseconds + other.inNanoseconds)

    open operator fun minus(other: Duration): Duration =
        DurationNanoseconds(inNanoseconds - other.inNanoseconds)

    open operator fun times(multiplier: Int): Duration =
        DurationNanoseconds(inNanoseconds * multiplier)

    open operator fun times(multiplier: Long): Duration =
        DurationNanoseconds(inNanoseconds * multiplier)

    open operator fun times(multiplier: Float): Duration =
        DurationNanoseconds((inNanoseconds * multiplier.toDouble()).roundToLong())

    open operator fun times(multiplier: Double): Duration =
        DurationNanoseconds((inNanoseconds * multiplier).roundToLong())

    open operator fun div(denominator: Int): Duration =
        DurationNanoseconds(inNanoseconds / denominator)

    open operator fun div(denominator: Long): Duration =
        DurationNanoseconds(inNanoseconds / denominator)

    open operator fun div(denominator: Float): Duration =
        DurationNanoseconds((inNanoseconds / denominator.toDouble()).roundToLong())

    open operator fun div(denominator: Double): Duration =
        DurationNanoseconds((inNanoseconds / denominator).roundToLong())

    open operator fun unaryMinus(): Duration =
        DurationNanoseconds(-inNanoseconds)

    override operator fun compareTo(other: Duration): Int =
        inNanoseconds.compareTo(other.inNanoseconds)
}

private abstract class DurationInfinite : Duration() {
    override operator fun plus(other: Duration): Duration = this
    override operator fun minus(other: Duration): Duration = this
    override operator fun times(multiplier: Int): Duration = this
    override operator fun times(multiplier: Long): Duration = this
    override operator fun times(multiplier: Float): Duration = this
    override operator fun times(multiplier: Double): Duration = this
    override operator fun div(denominator: Int): Duration = this
    override operator fun div(denominator: Long): Duration = this
    override operator fun div(denominator: Float): Duration = this
    override operator fun div(denominator: Double): Duration = this
}

private object DurationInfinitePositive : DurationInfinite() {
    override val inYears: Double = Double.POSITIVE_INFINITY
    override val inMonths: Double = Double.POSITIVE_INFINITY
    override val inWeeks: Double = Double.POSITIVE_INFINITY
    override val inDays: Double = Double.POSITIVE_INFINITY
    override val inHours: Double = Double.POSITIVE_INFINITY
    override val inMinutes: Double = Double.POSITIVE_INFINITY
    override val inSeconds: Double = Double.POSITIVE_INFINITY
    override val inMilliseconds: Double = Double.POSITIVE_INFINITY
    override val inMicroseconds: Double = Double.POSITIVE_INFINITY
    override val inNanoseconds: Long = Long.MAX_VALUE

    override fun toString(): String {
        return "INFINITE"
    }

    override operator fun unaryMinus(): Duration = DurationInfiniteNegative
}

private object DurationInfiniteNegative : DurationInfinite() {
    override val inYears: Double = Double.NEGATIVE_INFINITY
    override val inMonths: Double = Double.NEGATIVE_INFINITY
    override val inWeeks: Double = Double.NEGATIVE_INFINITY
    override val inDays: Double = Double.NEGATIVE_INFINITY
    override val inHours: Double = Double.NEGATIVE_INFINITY
    override val inMinutes: Double = Double.NEGATIVE_INFINITY
    override val inSeconds: Double = Double.NEGATIVE_INFINITY
    override val inMilliseconds: Double = Double.NEGATIVE_INFINITY
    override val inMicroseconds: Double = Double.NEGATIVE_INFINITY
    override val inNanoseconds: Long = Long.MIN_VALUE

    override fun toString(): String {
        return "Duration(NEGATIVE_INFINITE)"
    }

    override operator fun unaryMinus(): Duration = DurationInfiniteNegative
}

private class DurationNanoseconds(nanoseconds: Long) : Duration() {
    override val inYears by lazy { (inDays / 365.25) }
    override val inMonths by lazy { (inYears * 12) }
    override val inWeeks by lazy { inDays / 7 }
    override val inDays by lazy { inHours / 24 }
    override val inHours by lazy { inMinutes / 60 }
    override val inMinutes by lazy { inSeconds / 60 }
    override val inSeconds by lazy { inMilliseconds / 1000 }
    override val inMilliseconds by lazy { inMicroseconds / 1000 }
    override val inMicroseconds by lazy { inNanoseconds / 1000.0 }
    override val inNanoseconds = nanoseconds

    companion object {
        fun fromYears(years: Double) = fromDays(years * 365.25)
        fun fromMonths(months: Double) = fromYears(months / 12)
        fun fromWeeks(weeks: Double) = fromDays(weeks * 7)
        fun fromDays(days: Double) = fromHours(days * 24)
        fun fromHours(hours: Double) = fromMinutes(hours * 60)
        fun fromMinutes(minutes: Double) = fromSeconds(minutes * 60)
        fun fromSeconds(seconds: Double) = fromMilliseconds(seconds * 1000)
        fun fromMilliseconds(milliseconds: Double) = fromMicroseconds(milliseconds * 1000)
        fun fromMicroseconds(microseconds: Double) =
            fromNanoseconds((microseconds * 1000).roundToLong())
        fun fromNanoseconds(nanoseconds: Long) = DurationNanoseconds(nanoseconds)
    }
}

val Int.years get(): Duration = this.toDouble().years
val Int.months get(): Duration = this.toDouble().months
val Int.weeks get(): Duration = this.toDouble().weeks
val Int.days get(): Duration = this.toDouble().days
val Int.hours get(): Duration = this.toDouble().hours
val Int.minutes get(): Duration = this.toDouble().minutes
val Int.seconds get(): Duration = this.toDouble().seconds
val Int.milliseconds get(): Duration = this.toDouble().milliseconds
val Int.microseconds get(): Duration = this.toDouble().microseconds
val Int.nanoseconds get(): Duration = this.toLong().nanoseconds

val Long.years get(): Duration = this.toDouble().years
val Long.months get(): Duration = this.toDouble().months
val Long.weeks get(): Duration = this.toDouble().weeks
val Long.days get(): Duration = this.toDouble().days
val Long.hours get(): Duration = this.toDouble().hours
val Long.minutes get(): Duration = this.toDouble().minutes
val Long.seconds get(): Duration = this.toDouble().seconds
val Long.milliseconds get(): Duration = this.toDouble().milliseconds
val Long.microseconds get(): Duration = this.toDouble().microseconds
val Long.nanoseconds get(): Duration = DurationNanoseconds.fromNanoseconds(this)

val Float.years get(): Duration = this.toDouble().years
val Float.months get(): Duration = this.toDouble().months
val Float.weeks get(): Duration = this.toDouble().weeks
val Float.days get(): Duration = this.toDouble().days
val Float.hours get(): Duration = this.toDouble().hours
val Float.minutes get(): Duration = this.toDouble().minutes
val Float.seconds get(): Duration = this.toDouble().seconds
val Float.milliseconds get(): Duration = this.toDouble().milliseconds
val Float.microseconds get(): Duration = this.toDouble().microseconds
val Float.nanoseconds get(): Duration = this.roundToLong().nanoseconds

val Double.years get(): Duration = DurationNanoseconds.fromYears(this)
val Double.months get(): Duration = DurationNanoseconds.fromMonths(this)
val Double.weeks get(): Duration = DurationNanoseconds.fromWeeks(this)
val Double.days get(): Duration = DurationNanoseconds.fromDays(this)
val Double.hours get(): Duration = DurationNanoseconds.fromHours(this)
val Double.minutes get(): Duration = DurationNanoseconds.fromMinutes(this)
val Double.seconds get(): Duration = DurationNanoseconds.fromSeconds(this)
val Double.milliseconds get(): Duration = DurationNanoseconds.fromMilliseconds(this)
val Double.microseconds get(): Duration = DurationNanoseconds.fromMicroseconds(this)
val Double.nanoseconds get(): Duration = this.roundToLong().nanoseconds

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun min(duration1: Duration, duration2: Duration): Duration =
    when {
        duration1 is DurationInfinitePositive -> duration2
        duration1 is DurationInfiniteNegative -> duration1
        duration2 is DurationInfinitePositive -> duration1
        duration2 is DurationInfiniteNegative -> duration2
        else -> kotlin.math.min(duration1.inNanoseconds, duration2.inNanoseconds).nanoseconds
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun max(duration1: Duration, duration2: Duration): Duration =
    when {
        duration1 is DurationInfinitePositive -> duration1
        duration1 is DurationInfiniteNegative -> duration2
        duration2 is DurationInfinitePositive -> duration2
        duration2 is DurationInfiniteNegative -> duration1
        else -> kotlin.math.max(duration1.inNanoseconds, duration2.inNanoseconds).nanoseconds
    }
