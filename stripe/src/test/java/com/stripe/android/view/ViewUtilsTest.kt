package com.stripe.android.view

import com.stripe.android.model.CardBrand
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
            ViewUtils.separateCardNumberGroups(testCardNumber, CardBrand.Visa)
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
            ViewUtils.separateCardNumberGroups(testCardNumber, CardBrand.AmericanExpress)
        assertEquals(3, groups.size)
        assertEquals("3782", groups[0])
        assertEquals("822463", groups[1])
        assertEquals("10005", groups[2])
    }

    @Test
    fun separateCardNumberGroups_withDinersClub_returnsCorrectCardGroups() {
        val testCardNumber = "38520000023237"
        val groups =
            ViewUtils.separateCardNumberGroups(testCardNumber, CardBrand.DinersClub)
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
            ViewUtils.separateCardNumberGroups(testCardNumber, CardBrand.Unknown)
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
            testCardNumber.substring(0, 2), CardBrand.AmericanExpress)
        assertEquals(3, groups.size)
        assertEquals("37", groups[0])
        assertNull(groups[1])
        assertNull(groups[2])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 5), CardBrand.AmericanExpress)
        assertEquals(3, groups.size)
        assertEquals("3782", groups[0])
        assertEquals("8", groups[1])
        assertNull(groups[2])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 11), CardBrand.AmericanExpress)
        assertEquals(3, groups.size)
        assertEquals("3782", groups[0])
        assertEquals("822463", groups[1])
        assertEquals("1", groups[2])
    }

    @Test
    fun separateCardNumberGroups_withVisaPrefix_returnsCorrectGroups() {
        val testCardNumber = "4000056655665556"
        var groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 2), CardBrand.Visa)
        assertEquals(4, groups.size)
        assertEquals("40", groups[0])
        assertNull(groups[1])
        assertNull(groups[2])
        assertNull(groups[3])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 5), CardBrand.Visa)
        assertEquals(4, groups.size)
        assertEquals("4000", groups[0])
        assertEquals("0", groups[1])
        assertNull(groups[2])
        assertNull(groups[3])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 9), CardBrand.Visa)
        assertEquals(4, groups.size)
        assertEquals("4000", groups[0])
        assertEquals("0566", groups[1])
        assertEquals("5", groups[2])
        assertNull(groups[3])

        groups = ViewUtils.separateCardNumberGroups(
            testCardNumber.substring(0, 15), CardBrand.Visa)
        assertEquals(4, groups.size)
        assertEquals("4000", groups[0])
        assertEquals("0566", groups[1])
        assertEquals("5566", groups[2])
        assertEquals("555", groups[3])
    }

    @Test
    fun isCvcMaximalLength_whenThreeDigitsAndNotAmEx_returnsTrue() {
        assertTrue(ViewUtils.isCvcMaximalLength(CardBrand.Visa, "123"))
        assertTrue(ViewUtils.isCvcMaximalLength(CardBrand.MasterCard, "345"))
        assertTrue(ViewUtils.isCvcMaximalLength(CardBrand.JCB, "678"))
        assertTrue(ViewUtils.isCvcMaximalLength(CardBrand.DinersClub, "910"))
        assertTrue(ViewUtils.isCvcMaximalLength(CardBrand.Discover, "234"))
        assertTrue(ViewUtils.isCvcMaximalLength(CardBrand.Unknown, "333"))
    }

    @Test
    fun isCvcMaximalLength_whenThreeDigitsAndIsAmEx_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.AmericanExpress, "123"))
    }

    @Test
    fun isCvcMaximalLength_whenFourDigitsAndIsAmEx_returnsTrue() {
        assertTrue(ViewUtils.isCvcMaximalLength(CardBrand.AmericanExpress, "1234"))
    }

    @Test
    fun isCvcMaximalLength_whenTooManyDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.AmericanExpress, "12345"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.Visa, "1234"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.MasterCard, "123456"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.DinersClub, "1234567"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.Discover, "12345678"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.JCB, "123456789012345"))
    }

    @Test
    fun isCvcMaximalLength_whenNotEnoughDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.AmericanExpress, ""))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.Visa, "1"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.MasterCard, "12"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.DinersClub, ""))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.Discover, "8"))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.JCB, "1"))
    }

    @Test
    fun isCvcMaximalLength_whenWhitespaceAndNotEnoughDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.AmericanExpress, "   "))
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.Visa, "  1"))
    }

    @Test
    fun isCvcMaximalLength_whenNull_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(CardBrand.AmericanExpress, null))
    }

    @Test
    fun separateCardNumberGroups_forLongInputs_doesNotCrash() {
        val testCardNumber = "1234567890123456789"
        val groups = ViewUtils.separateCardNumberGroups(
            testCardNumber, CardBrand.Visa)
        assertEquals(4, groups.size)
    }
}
