package com.stripe.android.stripecardscan.framework.time

import androidx.test.filters.SmallTest
import com.stripe.android.camera.framework.time.Duration
import com.stripe.android.camera.framework.time.days
import com.stripe.android.camera.framework.time.hours
import com.stripe.android.camera.framework.time.max
import com.stripe.android.camera.framework.time.microseconds
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.camera.framework.time.min
import com.stripe.android.camera.framework.time.minutes
import com.stripe.android.camera.framework.time.months
import com.stripe.android.camera.framework.time.nanoseconds
import com.stripe.android.camera.framework.time.seconds
import com.stripe.android.camera.framework.time.weeks
import com.stripe.android.camera.framework.time.years
import org.junit.Test
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun Float.truncate(digits: Int) =
    ((this * 10.0.pow(digits)).roundToLong() / 10.0.pow(digits)).toFloat()
private fun Double.truncate(digits: Int) =
    (this * 10.0.pow(digits)).roundToLong() / 10.0.pow(digits)

class DurationTest {

    @Test
    @SmallTest
    fun reflective() {
        val randomInt = Random.nextInt(-10, 10)
        val randomLong = Random.nextLong(-10, 10)
        val randomFloat = Random.nextFloat() * 20 - 10
        val randomDouble = Random.nextDouble() * 20 - 10

        assertEquals(randomInt, randomInt.nanoseconds.inNanoseconds.toInt())
        assertEquals(randomInt, randomInt.microseconds.inMicroseconds.roundToInt())
        assertEquals(randomInt, randomInt.milliseconds.inMilliseconds.roundToInt())
        assertEquals(randomInt, randomInt.seconds.inSeconds.roundToInt())
        assertEquals(randomInt, randomInt.minutes.inMinutes.roundToInt())
        assertEquals(randomInt, randomInt.hours.inHours.roundToInt())
        assertEquals(randomInt, randomInt.days.inDays.roundToInt())
        assertEquals(randomInt, randomInt.weeks.inWeeks.roundToInt())
        assertEquals(randomInt, randomInt.months.inMonths.roundToInt())
        assertEquals(randomInt, randomInt.years.inYears.roundToInt())

        assertEquals(randomLong, randomLong.nanoseconds.inNanoseconds)
        assertEquals(randomLong, randomLong.microseconds.inMicroseconds.roundToLong())
        assertEquals(randomLong, randomLong.milliseconds.inMilliseconds.roundToLong())
        assertEquals(randomLong, randomLong.seconds.inSeconds.roundToLong())
        assertEquals(randomLong, randomLong.minutes.inMinutes.roundToLong())
        assertEquals(randomLong, randomLong.hours.inHours.roundToLong())
        assertEquals(randomLong, randomLong.days.inDays.roundToLong())
        assertEquals(randomLong, randomLong.weeks.inWeeks.roundToLong())
        assertEquals(randomLong, randomLong.months.inMonths.roundToLong())
        assertEquals(randomLong, randomLong.years.inYears.roundToLong())

        // These have to be truncated since the limiting factor for accuracy is nanoseconds.
        assertEquals(
            randomFloat.roundToLong(),
            randomFloat.nanoseconds.inNanoseconds,
            randomFloat.toString()
        )
        assertEquals(
            randomFloat.truncate(3),
            randomFloat.microseconds.inMicroseconds.toFloat(),
            randomFloat.toString()
        )
        assertEquals(
            randomFloat.truncate(6),
            randomFloat.milliseconds.inMilliseconds.toFloat(),
            randomFloat.toString()
        )
        assertEquals(
            randomFloat.truncate(9),
            randomFloat.seconds.inSeconds.toFloat(),
            randomFloat.toString()
        )
        assertEquals(randomFloat, randomFloat.minutes.inMinutes.toFloat(), randomFloat.toString())
        assertEquals(randomFloat, randomFloat.hours.inHours.toFloat(), randomFloat.toString())
        assertEquals(randomFloat, randomFloat.days.inDays.toFloat(), randomFloat.toString())
        assertEquals(randomFloat, randomFloat.weeks.inWeeks.toFloat(), randomFloat.toString())
        assertEquals(randomFloat, randomFloat.months.inMonths.toFloat(), randomFloat.toString())
        assertEquals(randomFloat, randomFloat.years.inYears.toFloat(), randomFloat.toString())

        // These have to be truncated since the limiting factor for accuracy is nanoseconds.
        assertEquals(
            randomDouble.roundToLong(),
            randomDouble.nanoseconds.inNanoseconds,
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(3),
            randomDouble.microseconds.inMicroseconds,
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(6),
            randomDouble.milliseconds.inMilliseconds.truncate(6),
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(9),
            randomDouble.seconds.inSeconds.truncate(9),
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(8),
            randomDouble.minutes.inMinutes.truncate(8),
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(10),
            randomDouble.hours.inHours.truncate(10),
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(10),
            randomDouble.days.inDays.truncate(10),
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(11),
            randomDouble.weeks.inWeeks.truncate(11),
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(11),
            randomDouble.months.inMonths.truncate(11),
            randomDouble.toString()
        )
        assertEquals(
            randomDouble.truncate(11),
            randomDouble.years.inYears.truncate(11),
            randomDouble.toString()
        )
    }

    @Test
    @SmallTest
    fun minMax() {
        assertEquals(5.seconds, max(1.milliseconds, 5.seconds))
        assertEquals(5.seconds, max(5.seconds, 1.milliseconds))

        assertEquals(1.milliseconds, min(1.milliseconds, 5.seconds))
        assertEquals(1.milliseconds, min(5.seconds, 1.milliseconds))

        assertEquals(Duration.INFINITE, max(1.milliseconds, Duration.INFINITE))
        assertEquals(Duration.INFINITE, max(Duration.INFINITE, 1.milliseconds))
        assertEquals(Duration.INFINITE, max(Duration.INFINITE, Duration.INFINITE))
        assertEquals(Duration.ZERO, max(Duration.NEGATIVE_INFINITE, Duration.ZERO))
        assertEquals(Duration.ZERO, max(Duration.ZERO, Duration.NEGATIVE_INFINITE))
        assertEquals(
            Duration.NEGATIVE_INFINITE,
            max(Duration.NEGATIVE_INFINITE, Duration.NEGATIVE_INFINITE)
        )

        assertEquals(
            Duration.NEGATIVE_INFINITE,
            min(1.milliseconds, Duration.NEGATIVE_INFINITE)
        )
        assertEquals(
            Duration.NEGATIVE_INFINITE,
            min(Duration.NEGATIVE_INFINITE, 1.milliseconds)
        )
        assertEquals(
            Duration.NEGATIVE_INFINITE,
            min(Duration.NEGATIVE_INFINITE, Duration.NEGATIVE_INFINITE)
        )
        assertEquals(Duration.ZERO, min(Duration.ZERO, Duration.INFINITE))
        assertEquals(Duration.ZERO, min(Duration.INFINITE, Duration.ZERO))
        assertEquals(Duration.INFINITE, min(Duration.INFINITE, Duration.INFINITE))
    }

    @Test
    @SmallTest
    fun arithmetic() {
        assertEquals(5005.milliseconds, 5.milliseconds + 5.seconds)
        assertEquals(26.seconds, 6000.milliseconds + 20.seconds)

        assertEquals(4995.milliseconds, 5.seconds - 5.milliseconds)
        assertEquals(14.seconds, 20.seconds - 6000.milliseconds)
        assertEquals((-5).seconds, 55.seconds - 1.minutes)

        assertEquals(6.seconds, 2.seconds * 3)
        assertEquals(1.weeks, 3.5.days * 2)
        assertEquals(9.seconds, 6.seconds * 1.5F)
        assertEquals(9.seconds, 6.seconds * 1.5)

        assertEquals(2.days, (4.0 / 7).weeks / 2)
        assertEquals(6.seconds, 12.seconds / 2)
        assertEquals(4.minutes, 10.minutes / 2.5F)
        assertEquals(4.minutes, 10.minutes / 2.5)

        assertEquals((-5).years, -(5.years))
        assertEquals(2.months, -((-2).months))
    }

    @Test
    @SmallTest
    fun comparison() {
        assertTrue { 5.milliseconds > 5.microseconds }
        assertFalse { 5.milliseconds < 5.microseconds }
        assertTrue { 5.milliseconds >= 5.microseconds }

        assertTrue { 5.milliseconds < 6.milliseconds }
        assertFalse { 5.milliseconds > 6.milliseconds }
        assertTrue { 5.milliseconds <= 6.milliseconds }

        assertTrue { 2.hours == 120.minutes }
        assertTrue { 2.hours >= 120.minutes }
        assertTrue { 2.hours <= 120.minutes }
    }

    @Test
    @SmallTest
    fun absolutes() {
        val duration = 2.years

        assertEquals(
            (2 * 365.25 * 24 * 60 * 60 * 1000 * 1000 * 1000).toLong(),
            duration.inNanoseconds
        )
        assertEquals(2 * 365.25 * 24 * 60 * 60 * 1000 * 1000, duration.inMicroseconds)
        assertEquals(2 * 365.25 * 24 * 60 * 60 * 1000, duration.inMilliseconds)
        assertEquals(2 * 365.25 * 24 * 60 * 60, duration.inSeconds)
        assertEquals(2 * 365.25 * 24 * 60, duration.inMinutes)
        assertEquals(2 * 365.25 * 24, duration.inHours)
        assertEquals(2 * 365.25, duration.inDays)
        assertEquals(2 * 365.25 / 7, duration.inWeeks)
        assertEquals(2 * 12.0, duration.inMonths)
        assertEquals(2.0, duration.inYears)
    }
}
