package com.stripe.android.core.strings

import android.os.Bundle
import android.os.Parcel
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
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

    @Test
    fun `resolvable strings should parcelize and un-parcelize properly with raw value types`() {
        val identifierResolvable = IdentifierResolvableString(
            id = 1453235,
            args = listOf(
                resolvableString(value = "1453235"),
                "argTwo",
                123,
                1267L,
                12.4,
                listOf(0, "any", 12.1)
            )
        )

        val unparcelizedIdentifierResolvable = writeThenRead(identifierResolvable)

        assertEquals(
            identifierResolvable,
            unparcelizedIdentifierResolvable,
        )

        val staticResolvable = StaticResolvableString(
            value = "This is a value!",
            args = listOf(
                resolvableString(value = "1453235"),
                "argTwo",
                123,
                1267L,
                12.4,
                listOf(0, "any", 12.1)
            )
        )

        val unparcelizedStaticResolvable = writeThenRead(staticResolvable)

        assertEquals(
            staticResolvable,
            unparcelizedStaticResolvable,
        )
    }

    private inline fun <reified T : ResolvableString> writeThenRead(resolvable: T): T? {
        val bytes = Parcel.obtain().run {
            val bundle = Bundle().apply { putParcelable(T::class.java.name, resolvable) }

            writeBundle(bundle)

            val bytes = marshall()

            recycle()

            bytes
        }

        return Parcel.obtain().run {
            unmarshall(bytes, 0, bytes.size)
            setDataPosition(0)

            val unparcelizedResolvable = this.readBundle()?.getParcelable<T>(T::class.java.name)

            recycle()

            unparcelizedResolvable
        }
    }
}
