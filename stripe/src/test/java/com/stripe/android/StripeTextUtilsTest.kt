package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test class for [StripeTextUtils].
 */
class StripeTextUtilsTest {
    @Test
    fun removeSpacesAndHyphens_withSpacesInInterior_returnsSpacelessNumber() {
        val testCardNumber = "4242 4242 4242 4242"
        assertEquals(
            CardNumberFixtures.VALID_VISA_NO_SPACES,
            StripeTextUtils.removeSpacesAndHyphens(testCardNumber)
        )
    }

    @Test
    fun removeSpacesAndHyphens_withExcessiveSpacesInInterior_returnsSpacelessNumber() {
        val testCardNumber = "4  242                  4 242 4  242 42 4   2"
        assertEquals(
            CardNumberFixtures.VALID_VISA_NO_SPACES,
            StripeTextUtils.removeSpacesAndHyphens(testCardNumber)
        )
    }

    @Test
    fun removeSpacesAndHyphens_withSpacesOnExterior_returnsSpacelessNumber() {
        val testCardNumber = "      42424242 4242 4242    "
        assertEquals(
            CardNumberFixtures.VALID_VISA_NO_SPACES,
            StripeTextUtils.removeSpacesAndHyphens(testCardNumber)
        )
    }

    @Test
    fun removeSpacesAndHyphens_whenEmpty_returnsNull() {
        assertNull(StripeTextUtils.removeSpacesAndHyphens("        "))
    }

    @Test
    fun removeSpacesAndHyphens_whenNull_returnsNull() {
        assertNull(StripeTextUtils.removeSpacesAndHyphens(null))
    }

    @Test
    fun removeSpacesAndHyphens_withHyphenatedCardNumber_returnsCardNumber() {
        assertEquals(
            CardNumberFixtures.VALID_VISA_NO_SPACES,
            StripeTextUtils.removeSpacesAndHyphens("4242-4242-4242-4242")
        )
    }

    @Test
    fun removeSpacesAndHyphens_removesMultipleSpacesAndHyphens() {
        assertEquals("123",
            StripeTextUtils.removeSpacesAndHyphens(
                " -    1-  --- 2   3- - - -------- "
            )
        )
    }

    @Test
    fun shaHashInput_withNullInput_returnsNull() {
        assertNull(StripeTextUtils.shaHashInput("  "))
    }

    @Test
    fun shaHashInput_withText_returnsDifferentText() {
        assertEquals(
            "9D3B38F100AF0DBD0D0CB11EDF15E40BBF0820C8",
            StripeTextUtils.shaHashInput("iamtheverymodelofamodernmajorgeneral")
        )
    }
}
