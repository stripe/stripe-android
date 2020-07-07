package com.stripe.android

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * Test class for [StripeTextUtils].
 */
class StripeTextUtilsTest {

    @Test
    fun removeSpacesAndHyphens_withSpacesInInterior_returnsSpacelessNumber() {
        val testCardNumber = "4242 4242 4242 4242"
        assertThat(StripeTextUtils.removeSpacesAndHyphens(testCardNumber))
            .isEqualTo(CardNumberFixtures.VISA_NO_SPACES)
    }

    @Test
    fun removeSpacesAndHyphens_withExcessiveSpacesInInterior_returnsSpacelessNumber() {
        val testCardNumber = "4  242                  4 242 4  242 42 4   2"
        assertThat(StripeTextUtils.removeSpacesAndHyphens(testCardNumber))
            .isEqualTo(CardNumberFixtures.VISA_NO_SPACES)
    }

    @Test
    fun removeSpacesAndHyphens_withSpacesOnExterior_returnsSpacelessNumber() {
        val testCardNumber = "      42424242 4242 4242    "
        assertThat(StripeTextUtils.removeSpacesAndHyphens(testCardNumber))
            .isEqualTo(CardNumberFixtures.VISA_NO_SPACES)
    }

    @Test
    fun removeSpacesAndHyphens_whenEmpty_returnsNull() {
        assertThat(StripeTextUtils.removeSpacesAndHyphens("        ")).isNull()
    }

    @Test
    fun removeSpacesAndHyphens_whenNull_returnsNull() {
        assertThat(StripeTextUtils.removeSpacesAndHyphens(null)).isNull()
    }

    @Test
    fun removeSpacesAndHyphens_withHyphenatedCardNumber_returnsCardNumber() {
        assertThat(StripeTextUtils.removeSpacesAndHyphens("4242-4242-4242-4242")
        ).isEqualTo(CardNumberFixtures.VISA_NO_SPACES)
    }

    @Test
    fun removeSpacesAndHyphens_removesMultipleSpacesAndHyphens() {
        assertThat(StripeTextUtils
            .removeSpacesAndHyphens(" -    1-  --- 2   3- - - -------- "))
            .isEqualTo("123")
    }
}
