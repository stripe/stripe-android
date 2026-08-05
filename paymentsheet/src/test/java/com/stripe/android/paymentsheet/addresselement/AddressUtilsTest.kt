package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressUtilsTest {
    @Test
    fun `test edit distance equal address`() {
        val address = AddressDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        Truth.assertThat(address.editDistance(address)).isEqualTo(0)
    }

    @Test
    fun `test edit distance one char diff`() {
        val address = AddressDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        val otherAddress = AddressDetails(
            address = PaymentSheet.Address(
                city = "Sa Francisco", // One char diff here
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(1)
    }

    @Test
    fun `test edit distance different city`() {
        val address = AddressDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        val otherAddress = AddressDetails(
            address = PaymentSheet.Address(
                city = "Freemont",
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(11)
    }

    @Test
    fun `test edit distance missing city original`() {
        val address = AddressDetails(
            address = PaymentSheet.Address(
                city = null,
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        val otherAddress = AddressDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(13)
    }

    @Test
    fun `test edit distance missing city other`() {
        val address = AddressDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        val otherAddress = AddressDetails(
            address = PaymentSheet.Address(
                city = null,
                country = "AT",
                line1 = "510 Townsend St.",
                postalCode = "94102",
                state = "California"
            )
        )

        Truth.assertThat(address.editDistance(otherAddress)).isEqualTo(13)
    }

    @Test
    fun `computeBillingEditDistance returns zero for identical addresses`() {
        val address = Address(
            line1 = "510 Townsend St",
            line2 = "Suite 200",
            city = "San Francisco",
            state = "CA",
            postalCode = "94102",
            country = "US",
        )
        assertThat(computeBillingEditDistance(address, address)).isEqualTo(0)
    }

    @Test
    fun `computeBillingEditDistance sums edits across fields`() {
        val autocomplete = Address(
            line1 = "510 Townsend St",
            city = "San Francisco",
            state = "CA",
            postalCode = "94102",
            country = "US",
        )
        val billing = Address(
            line1 = "510 Townsend Street",
            city = "San Francisco",
            state = "CA",
            postalCode = "94103",
            country = "US",
        )
        // "St" -> "Street" = 4, "94102" -> "94103" = 1
        assertThat(computeBillingEditDistance(autocomplete, billing)).isEqualTo(5)
    }

    @Test
    fun `computeBillingEditDistance excludes country from calculation`() {
        val autocomplete = Address(
            line1 = "123 Main St",
            city = "Toronto",
            state = "ON",
            postalCode = "M5V",
            country = "CA",
        )
        val billing = Address(
            line1 = "123 Main St",
            city = "Toronto",
            state = "ON",
            postalCode = "M5V",
            country = "US",
        )
        assertThat(computeBillingEditDistance(autocomplete, billing)).isEqualTo(0)
    }

    @Test
    fun `computeBillingEditDistance treats null fields as empty strings`() {
        val autocomplete = Address(
            line1 = "123 Main St",
            line2 = null,
            city = "SF",
            state = null,
            postalCode = null,
            country = "US",
        )
        val billing = Address(
            line1 = "123 Main St",
            line2 = "Apt 4",
            city = "SF",
            state = "CA",
            postalCode = "94102",
            country = "US",
        )
        // line2: "" -> "Apt 4" = 5, state: "" -> "CA" = 2, postalCode: "" -> "94102" = 5
        assertThat(computeBillingEditDistance(autocomplete, billing)).isEqualTo(12)
    }
}
