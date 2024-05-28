package com.stripe.android.camera.framework.time

import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Clock {
    @JvmStatic
    fun markNow(): ClockMark = PreciseClockMark(System.nanoTime())
}

/**
 * Convert a milliseconds since epoch timestamp to a clock mark.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Long.asEpochMillisecondsClockMark(): ClockMark = AbsoluteClockMark(this)

/**
 * A marked point in time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class ClockMark {
    abstract fun elapsedSince(): Duration

    abstract fun toMillisecondsSinceEpoch(): Long

    abstract fun hasPassed(): Boolean

    abstract fun isInFuture(): Boolean

    abstract operator fun plus(duration: Duration): ClockMark

    abstract operator fun minus(duration: Duration): ClockMark

    abstract operator fun compareTo(other: ClockMark): Int
}

/**
 * A clock mark based on milliseconds since epoch. This is precise to the nearest millisecond.
 */
private class AbsoluteClockMark(private val millisecondsSinceEpoch: Long) : ClockMark() {
    override fun elapsedSince(): Duration =
        (System.currentTimeMillis() - millisecondsSinceEpoch).milliseconds

    override fun toMillisecondsSinceEpoch(): Long = millisecondsSinceEpoch

    override fun hasPassed(): Boolean = elapsedSince() > Duration.ZERO

    override fun isInFuture(): Boolean = elapsedSince() < Duration.ZERO

    override fun plus(duration: Duration): ClockMark =
        AbsoluteClockMark(millisecondsSinceEpoch + duration.inMilliseconds.toLong())

    override fun minus(duration: Duration): ClockMark =
        AbsoluteClockMark(millisecondsSinceEpoch - duration.inMilliseconds.toLong())

    override fun compareTo(other: ClockMark): Int =
        millisecondsSinceEpoch.compareTo(other.toMillisecondsSinceEpoch())

    override fun equals(other: Any?): Boolean =
        this === other || when (other) {
            is AbsoluteClockMark -> millisecondsSinceEpoch == other.millisecondsSinceEpoch
            is ClockMark -> toMillisecondsSinceEpoch() == other.toMillisecondsSinceEpoch()
            else -> false
        }

    override fun hashCode(): Int {
        return millisecondsSinceEpoch.hashCode()
    }

    override fun toString(): String {
        return "AbsoluteClockMark(at $millisecondsSinceEpoch ms since epoch})"
    }
}

/**
 * A precise clock mark that is not bound to epoch seconds. This is precise to the nearest
 * nanosecond.
 */
private class PreciseClockMark(private val originMarkNanoseconds: Long) : ClockMark() {
    override fun elapsedSince(): Duration = (System.nanoTime() - originMarkNanoseconds).nanoseconds

    override fun toMillisecondsSinceEpoch(): Long =
        System.currentTimeMillis() - elapsedSince().inMilliseconds.toLong()

    override fun hasPassed(): Boolean = elapsedSince() > Duration.ZERO

    override fun isInFuture(): Boolean = elapsedSince() < Duration.ZERO

    override fun plus(duration: Duration): ClockMark =
        PreciseClockMark(originMarkNanoseconds + duration.inNanoseconds)

    override fun minus(duration: Duration): ClockMark =
        PreciseClockMark(originMarkNanoseconds + duration.inNanoseconds)

    override fun compareTo(other: ClockMark): Int = elapsedSince().compareTo(other.elapsedSince())

    override fun equals(other: Any?): Boolean =
        this === other || when (other) {
            is PreciseClockMark -> originMarkNanoseconds == other.originMarkNanoseconds
            is ClockMark -> toMillisecondsSinceEpoch() == other.toMillisecondsSinceEpoch()
            else -> false
        }

    override fun hashCode(): Int {
        return originMarkNanoseconds.hashCode()
    }

    override fun toString(): String = elapsedSince().let {
        if (it >= Duration.ZERO) {
            "PreciseClockMark($it ago)"
        } else {
            "PreciseClockMark(${-it} in the future)"
        }
    }
}

/**
 * Measure the amount of time a process takes.
 *
 * TODO: use contracts when they are no longer experimental
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T> measureTime(block: () -> T): Pair<Duration, T> {
    // contract { callsInPlace(block, EXACTLY_ONCE) }
    val mark = Clock.markNow()
    val result = block()
    return mark.elapsedSince() to result
}
