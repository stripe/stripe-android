package com.stripe.android.view

import com.stripe.android.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [ViewUtils]
 */
@RunWith(RobolectricTestRunner::class)
internal class ViewUtilsTest {

    @Test
    fun separateCardNumberGroups_withVisa_returnsCorrectCardGroups() {
        val testCardNumber = "4000056655665556"
        val groups =
            ViewUtils.separateCardNumberGroups(testCardNumber, Card.CardBrand.VISA)
        assertEquals(4, groups.size)
        assertEquals("4000", groups[0])
        assertEquals("0566", groups[1])
        assertEquals("5566", groups[2])
        assertEquals("5556", groups[3])
    }

    @Test
    fun separateCardNumberGroups_withAmex_returnsCorrectCardGroups() {
        val testCardNumber = "378282246310005"
        val groups =
            ViewUtils.separateCardNumberGroups(testCardNumber, Card.CardBrand.AMERICAN_EXPRESS)
        assertEquals(3, groups.size)
        assertEquals("3782", groups[0])
        assertEquals("822463", groups[1])
        assertEquals("10005", groups[2])
    }

    @Test
    fun separateCardNumberGroups_withDinersClub_returnsCorrectCardGroups() {
        val testCardNumber = "38520000023237"
        val groups =
            ViewUtils.separateCardNumberGroups(testCardNumber, Card.CardBrand.DINERS_CLUB)
        assertEquals(4, groups.size)
        assertEquals("3852", groups[0])
        assertEquals("0000", groups[1])
        assertEquals("0232", groups[2])
        assertEquals("37", groups[3])
    }

    @Test
    fun separateCardNumberGroups_withInvalid_returnsCorrectCardGroups() {
        val testCardNumber = "1234056655665556"
        val groups =
            ViewUtils.separateCardNumberGroups(testCardNumber, Card.CardBrand.UNKNOWN)
        assertEquals(4, groups.size)
        assertEquals("1234", groups[0])
        assertEquals("0566", groups[1])
        assertEquals("5566", groups[2])
        assertEquals("5556", groups[3])
    }

    @Test
    fun separateCardNumberGroups_withAmexPrefix_returnsPrefixGroups() {
        val testCardNumber = "378282246310005"
        var groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 2), Card.CardBrand.AMERICAN_EXPRESS)
        assertEquals(3, groups.size)
        assertEquals("37", groups[0])
        assertNull(groups[1])
        assertNull(groups[2])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 5), Card.CardBrand.AMERICAN_EXPRESS)
        assertEquals(3, groups.size)
        assertEquals("3782", groups[0])
        assertEquals("8", groups[1])
        assertNull(groups[2])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 11), Card.CardBrand.AMERICAN_EXPRESS)
        assertEquals(3, groups.size)
        assertEquals("3782", groups[0])
        assertEquals("822463", groups[1])
        assertEquals("1", groups[2])
    }

    @Test
    fun separateCardNumberGroups_withVisaPrefix_returnsCorrectGroups() {
        val testCardNumber = "4000056655665556"
        var groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 2), Card.CardBrand.VISA)
        assertEquals(4, groups.size)
        assertEquals("40", groups[0])
        assertNull(groups[1])
        assertNull(groups[2])
        assertNull(groups[3])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 5), Card.CardBrand.VISA)
        assertEquals(4, groups.size)
        assertEquals("4000", groups[0])
        assertEquals("0", groups[1])
        assertNull(groups[2])
        assertNull(groups[3])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 9), Card.CardBrand.VISA)
        assertEquals(4, groups.size)
        assertEquals("4000", groups[0])
        assertEquals("0566", groups[1])
        assertEquals("5", groups[2])
        assertNull(groups[3])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 15), Card.CardBrand.VISA)
        assertEquals(4, groups.size)
        assertEquals("4000", groups[0])
        assertEquals("0566", groups[1])
        assertEquals("5566", groups[2])
        assertEquals("555", groups[3])
    }

    @Test
    fun isCvcMaximalLength_whenThreeDigitsAndNotAmEx_returnsTrue() {
        assertTrue(ViewUtils.isCvcMaximalLength(Card.CardBrand.VISA, "123"))
        assertTrue(ViewUtils.isCvcMaximalLength(Card.CardBrand.MASTERCARD, "345"))
        assertTrue(ViewUtils.isCvcMaximalLength(Card.CardBrand.JCB, "678"))
        assertTrue(ViewUtils.isCvcMaximalLength(Card.CardBrand.DINERS_CLUB, "910"))
        assertTrue(ViewUtils.isCvcMaximalLength(Card.CardBrand.DISCOVER, "234"))
        assertTrue(ViewUtils.isCvcMaximalLength(Card.CardBrand.UNKNOWN, "333"))
    }

    @Test
    fun isCvcMaximalLength_whenThreeDigitsAndIsAmEx_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.AMERICAN_EXPRESS, "123"))
    }

    @Test
    fun isCvcMaximalLength_whenFourDigitsAndIsAmEx_returnsTrue() {
        assertTrue(ViewUtils.isCvcMaximalLength(Card.CardBrand.AMERICAN_EXPRESS, "1234"))
    }

    @Test
    fun isCvcMaximalLength_whenTooManyDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.AMERICAN_EXPRESS, "12345"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.VISA, "1234"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.MASTERCARD, "123456"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.DINERS_CLUB, "1234567"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.DISCOVER, "12345678"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.JCB, "123456789012345"))
    }

    @Test
    fun isCvcMaximalLength_whenNotEnoughDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.AMERICAN_EXPRESS, ""))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.VISA, "1"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.MASTERCARD, "12"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.DINERS_CLUB, ""))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.DISCOVER, "8"))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.JCB, "1"))
    }

    @Test
    fun isCvcMaximalLength_whenWhitespaceAndNotEnoughDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.AMERICAN_EXPRESS, "   "))
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.VISA, "  1"))
    }

    @Test
    fun isCvcMaximalLength_whenNull_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.CardBrand.AMERICAN_EXPRESS, null))
    }

    @Test
    fun separateCardNumberGroups_forLongInputs_doesNotCrash() {
        val testCardNumber = "1234567890123456789"
        val groups = ViewUtils.separateCardNumberGroups(
            testCardNumber, Card.CardBrand.VISA)
        assertEquals(4, groups.size)
    }
}
