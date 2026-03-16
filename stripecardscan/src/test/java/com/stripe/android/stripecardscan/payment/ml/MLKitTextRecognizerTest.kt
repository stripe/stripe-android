package com.stripe.android.stripecardscan.payment.ml

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MLKitTextRecognizerTest {

    @Test
    @SmallTest
    fun `extractCardNumber with spaced format returns valid PAN`() {
        val result = MLKitTextRecognizer.extractCardNumber("4847 1860 9511 8770")
        assertThat(result).isEqualTo("4847186095118770")
    }

    @Test
    @SmallTest
    fun `extractCardNumber with dashed format returns valid PAN`() {
        val result = MLKitTextRecognizer.extractCardNumber("4847-1860-9511-8770")
        assertThat(result).isEqualTo("4847186095118770")
    }

    @Test
    @SmallTest
    fun `extractCardNumber with contiguous digits returns valid PAN`() {
        val result = MLKitTextRecognizer.extractCardNumber("4847186095118770")
        assertThat(result).isEqualTo("4847186095118770")
    }

    @Test
    @SmallTest
    fun `extractCardNumber with surrounding text returns valid PAN`() {
        val result = MLKitTextRecognizer.extractCardNumber(
            "Card Number: 4847 1860 9511 8770 Exp: 12/25"
        )
        assertThat(result).isEqualTo("4847186095118770")
    }

    @Test
    @SmallTest
    fun `extractCardNumber with invalid Luhn returns null`() {
        val result = MLKitTextRecognizer.extractCardNumber("4847 1860 9511 8771")
        assertThat(result).isNull()
    }

    @Test
    @SmallTest
    fun `extractCardNumber with no card number returns null`() {
        val result = MLKitTextRecognizer.extractCardNumber("Hello World")
        assertThat(result).isNull()
    }

    @Test
    @SmallTest
    fun `extractCardNumber does not match short numbers`() {
        // Dates, CVVs, and other short digit sequences should not match
        val result = MLKitTextRecognizer.extractCardNumber("12/25 CVV 123 Amount 99.99")
        assertThat(result).isNull()
    }

    @Test
    @SmallTest
    fun `extractCardNumber with multiple lines picks first valid PAN`() {
        val result = MLKitTextRecognizer.extractCardNumber(
            "Name: John Doe\n4847 1860 9511 8770\nExpiry: 12/25"
        )
        assertThat(result).isEqualTo("4847186095118770")
    }

    @Test
    @SmallTest
    fun `extractCardNumber with Amex format returns valid PAN`() {
        val result = MLKitTextRecognizer.extractCardNumber("3400 000000 00009")
        assertThat(result).isEqualTo("340000000000009")
    }

    @Test
    @SmallTest
    fun `extractExpiry with MM slash YY format returns expiry`() {
        val result = MLKitTextRecognizer.extractExpiry("12/28")
        assertThat(result).isEqualTo(SSDOcr.ExpiryDate(12, 2028))
    }

    @Test
    @SmallTest
    fun `extractExpiry with MM slash YYYY format returns expiry`() {
        val result = MLKitTextRecognizer.extractExpiry("12/2028")
        assertThat(result).isEqualTo(SSDOcr.ExpiryDate(12, 2028))
    }

    @Test
    @SmallTest
    fun `extractExpiry with dash separator returns expiry`() {
        val result = MLKitTextRecognizer.extractExpiry("03-29")
        assertThat(result).isEqualTo(SSDOcr.ExpiryDate(3, 2029))
    }

    @Test
    @SmallTest
    fun `extractExpiry with dot separator returns expiry`() {
        val result = MLKitTextRecognizer.extractExpiry("06.30")
        assertThat(result).isEqualTo(SSDOcr.ExpiryDate(6, 2030))
    }

    @Test
    @SmallTest
    fun `extractExpiry with surrounding text returns expiry`() {
        val result = MLKitTextRecognizer.extractExpiry("VALID THRU 12/28 CVV 123")
        assertThat(result).isEqualTo(SSDOcr.ExpiryDate(12, 2028))
    }

    @Test
    @SmallTest
    fun `extractExpiry with full card text returns expiry`() {
        val result = MLKitTextRecognizer.extractExpiry(
            "4847 1860 9511 8770\nVALID THRU\n03/29\nJOHN DOE"
        )
        assertThat(result).isEqualTo(SSDOcr.ExpiryDate(3, 2029))
    }

    @Test
    @SmallTest
    fun `extractExpiry with invalid month returns null`() {
        val result = MLKitTextRecognizer.extractExpiry("13/28")
        assertThat(result).isNull()
    }

    @Test
    @SmallTest
    fun `extractExpiry with expired date returns null`() {
        val result = MLKitTextRecognizer.extractExpiry("01/20")
        assertThat(result).isNull()
    }

    @Test
    @SmallTest
    fun `extractExpiry with no date returns null`() {
        val result = MLKitTextRecognizer.extractExpiry("Hello World")
        assertThat(result).isNull()
    }

    @Test
    @SmallTest
    fun `extractExpiry with spaces around separator returns expiry`() {
        val result = MLKitTextRecognizer.extractExpiry("12 / 28")
        assertThat(result).isEqualTo(SSDOcr.ExpiryDate(12, 2028))
    }
}
