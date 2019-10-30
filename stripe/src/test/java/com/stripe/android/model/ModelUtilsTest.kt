package com.stripe.android.model

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Test class for [ModelUtils].
 */
class ModelUtilsTest {

    @Test
    fun wholePositiveNumberShouldFailNull() {
        assertFalse(ModelUtils.isWholePositiveNumber(null))
    }

    @Test
    fun wholePositiveNumberShouldPassIfEmpty() {
        assertTrue(ModelUtils.isWholePositiveNumber(""))
    }

    @Test
    fun wholePositiveNumberShouldPass() {
        assertTrue(ModelUtils.isWholePositiveNumber("123"))
    }

    @Test
    fun wholePositiveNumberShouldPassWithLeadingZero() {
        assertTrue(ModelUtils.isWholePositiveNumber("000"))
    }

    @Test
    fun wholePositiveNumberShouldFailIfNegative() {
        assertFalse(ModelUtils.isWholePositiveNumber("-1"))
    }

    @Test
    fun wholePositiveNumberShouldFailIfLetters() {
        assertFalse(ModelUtils.isWholePositiveNumber("1a"))
    }

    @Test
    fun normalizeSameCenturyShouldPass() {
        val now = Calendar.getInstance()
        val year = 1997
        now.set(Calendar.YEAR, year)
        assertEquals(year, ModelUtils.normalizeYear(97, now))
    }

    @Test
    fun normalizeDifferentCenturyShouldFail() {
        val now = Calendar.getInstance()
        val year = 1997
        now.set(Calendar.YEAR, year)
        assertNotEquals(2097, ModelUtils.normalizeYear(97, now))
    }
}
