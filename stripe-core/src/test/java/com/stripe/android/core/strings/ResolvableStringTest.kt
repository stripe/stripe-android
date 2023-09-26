package com.stripe.android.core.strings

import com.stripe.android.core.strings.transformations.Replace
import org.junit.Test
import kotlin.test.assertEquals

class ResolvableStringTest {
    @Test
    fun `static resolvable strings should be equal when using same value`() {
        assertEquals(
            resolvableString(value = "1453235"),
            resolvableString(value = "1453235")
        )
    }

    @Test
    fun `static resolvable strings should be equal when using same value & arguments`() {
        assertEquals(
            resolvableString(value = "1453235", "argOne", "argTwo"),
            resolvableString(value = "1453235", "argOne", "argTwo")
        )
    }

    @Test
    fun `static resolvable strings with same value & other static resolvable strings should be equal`() {
        assertEquals(
            resolvableString(value = "1453235", resolvableString(value = "argOne"), "argTwo"),
            resolvableString(value = "1453235", resolvableString(value = "argOne"), "argTwo")
        )
    }

    @Test
    fun `identifier resolvable strings should be equal when using same value`() {
        assertEquals(
            resolvableString(id = 1453235),
            resolvableString(id = 1453235)
        )
    }

    @Test
    fun `identifier resolvable strings should be equal when using same value & arguments`() {
        assertEquals(
            resolvableString(id = 1453235, "argOne", "argTwo"),
            resolvableString(id = 1453235, "argOne", "argTwo")
        )
    }

    @Test
    fun `identifier resolvable strings with same value & other identifier resolvable strings should be equal`() {
        assertEquals(
            resolvableString(id = 1453235, resolvableString(id = 52523525), "argTwo"),
            resolvableString(id = 1453235, resolvableString(id = 52523525), "argTwo")
        )
    }

    @Test
    fun `identifier resolvable strings with comparable 'TransformOperation' objects should be equal`() {
        assertEquals(
            resolvableString(
                id = 1453235,
                resolvableString(value = "1453235"),
                "argTwo",
                transformations = listOf(Replace("one", "two"))
            ).toString(),
            resolvableString(
                id = 1453235,
                resolvableString(value = "1453235"),
                "argTwo",
                transformations = listOf(Replace("one", "two"))
            ).toString()
        )
    }

    @Test
    fun `resolvable strings with same value & other resolvable strings should be equal`() {
        assertEquals(
            resolvableString(id = 1453235, resolvableString(value = "1453235"), "argTwo"),
            resolvableString(id = 1453235, resolvableString(value = "1453235"), "argTwo")
        )

        assertEquals(
            resolvableString(value = "1453235", resolvableString(id = 52523525), "argTwo"),
            resolvableString(value = "1453235", resolvableString(id = 52523525), "argTwo")
        )
    }

    @Test
    fun `resolvable strings with the same values & arguments should produce the same 'toString' value`() {
        assertEquals(
            resolvableString(id = 1453235, resolvableString(value = "1453235"), "argTwo").toString(),
            resolvableString(id = 1453235, resolvableString(value = "1453235"), "argTwo").toString()
        )

        assertEquals(
            resolvableString(value = "1453235", resolvableString(id = 52523525), "argTwo").toString(),
            resolvableString(value = "1453235", resolvableString(id = 52523525), "argTwo").toString()
        )
    }
}
