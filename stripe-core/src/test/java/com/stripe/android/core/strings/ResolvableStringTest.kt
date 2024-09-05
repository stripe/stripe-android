package com.stripe.android.core.strings

import android.os.Bundle
import android.os.Parcel
import androidx.test.runner.AndroidJUnit4
import com.stripe.android.core.strings.transformations.Replace
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ResolvableStringTest {
    @Test
    fun `static resolvable strings should be equal when using same value`() {
        assertEquals(
            "1453235".resolvableString,
            "1453235".resolvableString
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
            resolvableString(value = "1453235", "argOne".resolvableString, "argTwo"),
            resolvableString(value = "1453235", "argOne".resolvableString, "argTwo")
        )
    }

    @Test
    fun `identifier resolvable strings should be equal when using same value`() {
        assertEquals(
            1453235.resolvableString,
            1453235.resolvableString
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
            resolvableString(id = 1453235, 52523525.resolvableString, "argTwo"),
            resolvableString(id = 1453235, 52523525.resolvableString, "argTwo")
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
            resolvableString(id = 1453235, "1453235".resolvableString, "argTwo"),
            resolvableString(id = 1453235, "1453235".resolvableString, "argTwo")
        )

        assertEquals(
            resolvableString(value = "1453235", 52523525.resolvableString, "argTwo"),
            resolvableString(value = "1453235", 52523525.resolvableString, "argTwo")
        )
    }

    @Test
    fun `resolvable strings with the same values & arguments should produce the same 'toString' value`() {
        assertEquals(
            resolvableString(id = 1453235, "1453235".resolvableString, "argTwo").toString(),
            resolvableString(id = 1453235, "1453235".resolvableString, "argTwo").toString()
        )

        assertEquals(
            resolvableString(value = "1453235", 52523525.resolvableString, "argTwo").toString(),
            resolvableString(value = "1453235", 52523525.resolvableString, "argTwo").toString()
        )
    }

    @Test
    fun `resolvable strings should parcelize and un-parcelize properly with raw value types`() {
        val identifierResolvable = IdentifierResolvableString(
            id = 1453235,
            args = listOf(
                "1453235".resolvableString,
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
                "1453235".resolvableString,
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

    @Test
    fun `resolvable strings should parcelize and un-parcelize properly with transforms`() {
        val identifierResolvable = IdentifierResolvableString(
            id = 1453235,
            args = emptyList(),
            transformations = listOf(
                Replace("old", "new")
            )
        )

        val unparcelizedIdentifierResolvable = writeThenRead(identifierResolvable)

        assertEquals(
            identifierResolvable,
            unparcelizedIdentifierResolvable,
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
