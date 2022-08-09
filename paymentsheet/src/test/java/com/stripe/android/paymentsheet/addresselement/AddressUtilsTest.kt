package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressUtilsTest {
    @Test
    fun `test edit distance equal address`() {
        val address = AddressDetails(
            city = "San Francisco",
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        Truth.assertThat(address.editDistance(address)).isEqualTo(0)
    }

    @Test
    fun `test edit distance one char diff`() {
        val address = AddressDetails(
            city = "San Francisco",
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        val otherAddress = AddressDetails(
            city = "Sa Francisco", // One char diff here
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(1)
    }

    @Test
    fun `test edit distance different city`() {
        val address = AddressDetails(
            city = "San Francisco",
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        val otherAddress = AddressDetails(
            city = "Freemont",
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(11)
    }

    @Test
    fun `test edit distance missing city original`() {
        val address = AddressDetails(
            city = null,
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        val otherAddress = AddressDetails(
            city = "San Francisco",
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(13)
    }

    @Test
    fun `test edit distance missing city other`() {
        val address = AddressDetails(
            city = "San Francisco",
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        val otherAddress = AddressDetails(
            city = null,
            country = "AT",
            line1 = "510 Townsend St.",
            postalCode = "94102",
            state = "California"
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(13)
    }
}
