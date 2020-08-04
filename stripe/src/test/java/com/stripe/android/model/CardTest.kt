package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for [Card].
 */
class CardTest {

    private val calendar = Calendar.getInstance()

    @BeforeTest
    fun setup() {
        calendar.set(Calendar.YEAR, 1997)
        calendar.set(Calendar.MONTH, Calendar.AUGUST)
        calendar.set(Calendar.DAY_OF_MONTH, 29)
    }

    @Test
    fun canInitializeWithMinimalArguments() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "123")
        assertTrue(card.validateNumber())
    }

    @Test
    fun testTypeReturnsCorrectlyForAmexCard() {
        val card = createCard(number = "3412123412341234")
        assertEquals(CardBrand.AmericanExpress, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForDiscoverCard() {
        val card = createCard(number = "6452123412341234")
        assertEquals(CardBrand.Discover, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForJCBCard() {
        val card = createCard(number = CardNumberFixtures.JCB_NO_SPACES)
        assertEquals(CardBrand.JCB, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForDinersClubCard() {
        val card = createCard(number = "3612123412341234")
        assertEquals(CardBrand.DinersClub, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForVisaCard() {
        val card = createCard(number = "4112123412341234")
        assertEquals(CardBrand.Visa, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForMasterCard() {
        val card = createCard(number = "5112123412341234")
        assertEquals(CardBrand.MasterCard, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForUnionPay() {
        val card = createCard(number = CardNumberFixtures.UNIONPAY_NO_SPACES)
        assertEquals(CardBrand.UnionPay, card.brand)
    }

    @Test
    fun shouldPassValidateNumberIfLuhnNumber() {
        val card = createCard(number = "4242-4242-4242-4242")
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfNotLuhnNumber() {
        val card = createCard(number = "4242-4242-4242-4241")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberIfLuhnNumberAmex() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES)
        assertEquals(CardBrand.AmericanExpress, card.brand)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfNull() {
        val card = createCard(cvc = null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfBlank() {
        val card = createCard(number = "")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfJustSpaces() {
        val card = createCard(number = "    ")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfTooShort() {
        val card = createCard(number = "0")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfContainsLetters() {
        val card = createCard(number = "424242424242a4242")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfTooLong() {
        val card = createCard(number = "4242 4242 4242 4242 6")
        assertEquals(CardBrand.Visa, card.brand)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumber() {
        val card = createCard(CardNumberFixtures.VISA_NO_SPACES)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberSpaces() {
        val card = createCard(CardNumberFixtures.VISA_WITH_SPACES)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberDashes() {
        val card = createCard(number = "4242-4242-4242-4242")
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberWithMixedSeparators() {
        val card = createCard(number = "4242-4   242 424-24 242")
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfWithDot() {
        val card = createCard(number = "4242.4242.4242.4242")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateExpiryDateIfNull() {
        val card = createCard(cvc = null)
        assertFalse(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNullMonth() {
        val card = createCard(expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfZeroMonth() {
        val card = createCard(expMonth = 0, expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNegativeMonth() {
        val card = createCard(expMonth = -1, expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfMonthToLarge() {
        val card = createCard(expMonth = 13, expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNullYear() {
        val card = createCard(expMonth = 1)
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfZeroYear() {
        val card = createCard(expMonth = 12, expYear = 0)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNegativeYear() {
        val card = createCard(expMonth = 12, expYear = -1)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForDecemberOfThisYear() {
        val card = createCard(expMonth = 12, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfCurrentMonth() {
        val card = createCard(expMonth = 8, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfCurrentMonthTwoDigitYear() {
        val card = createCard(expMonth = 8, expYear = 97)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfLastMonth() {
        val card = createCard(expMonth = 7, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfNextMonth() {
        val card = createCard(expMonth = 9, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForJanuary00() {
        val card = createCard(expMonth = 1, expYear = 0)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForDecember99() {
        val card = createCard(expMonth = 12, expYear = 99)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateCVCIfNull() {
        val card = createCard(cvc = null)
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfBlank() {
        val card = createCard(cvc = "")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfUnknownTypeAndLength2() {
        val card = createCard(cvc = "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfUnknownTypeAndLength3() {
        val card = createCard(cvc = "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfUnknownTypeAndLength4() {
        val card = createCard(cvc = "1234")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfUnknownTypeAndLength5() {
        val card = createCard(cvc = "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength2() {
        val card = createCard(number = "4242 4242 4242 4242", cvc = "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfVisaAndLength3() {
        val card = createCard(number = "4242 4242 4242 4242", cvc = "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength4() {
        val card = createCard(number = "4242 4242 4242 4242", cvc = "1234")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength5() {
        val card = createCard(number = "4242 4242 4242 4242", cvc = "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndNotNumeric() {
        val card = createCard(number = "4242 4242 4242 4242", cvc = "12a")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength2() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength3() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength4() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "1234")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfAmexAndLength5() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfAmexAndNotNumeric() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "123d")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardIfNotLuhnNumber() {
        val card = createCard(number = "4242-4242-4242-4241", expMonth = 12, expYear = 2050, cvc = "123")
        assertFalse(card.validateCard())
        assertFalse(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardInvalidMonth() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 13, expYear = 2050, cvc = "123")
        assertFalse(card.validateCard())
        assertTrue(card.validateNumber())
        assertFalse(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardInvalidYear() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 1, expYear = 1990, cvc = "123")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertFalse(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardWithNullCVC() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050)
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardVisa() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "123")
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithShortCVC() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "12")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithLongCVC() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "1234")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithBadCVC() {
        val card = createCard(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "bad")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardAmex() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "1234")
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardAmexWithNullCVC() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050)
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithShortCVC() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "12")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithLongCVC() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "12345")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithBadCVC() {
        val card = createCard(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "bad")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun testLast4() {
        val card = createCard(number = "42 42 42 42 42 42 42 42")
        assertEquals("4242", card.last4)
    }

    @Test
    fun last4ShouldBeNullWhenNumberIsNull() {
        val card = createCard(cvc = null)
        assertNull(card.last4)
    }

    @Test
    fun getLast4_whenNumberIsNullButLast4IsSet_returnsCorrectValue() {
        val card = Card(
            expMonth = 2,
            expYear = 2020,
            cvc = "123",
            name = "Jenny Rosen",
            last4 = "1234",
            brand = CardBrand.Visa,
            id = "id"
        )
        assertThat(card.last4)
            .isEqualTo("1234")
    }

    @Test
    fun getBrand_whenNumberIsNullButBrandIsSet_returnsCorrectValue() {
        val card = Card(
            expMonth = 2,
            expYear = 2020,
            cvc = "123",
            name = "Jenny Rosen",
            last4 = "1234",
            brand = CardBrand.AmericanExpress,
            id = "id"
        )
        assertThat(card.brand)
            .isEqualTo(CardBrand.AmericanExpress)
    }

    @Test
    fun toBuilder_whenChanged_isNotEquals() {
        assertNotEquals(CardFixtures.CARD, CardFixtures.CARD.toBuilder()
            .name("some other name")
            .build())
    }

    @Test
    fun toBuilder_whenUnchanged_isEquals() {
        assertEquals(CardFixtures.CARD, CardFixtures.CARD.toBuilder().build())
    }

    @Test
    fun toBuilder_withLoggingToken_whenUnchanged_isEquals() {
        val card = CardFixtures.CARD_USD
        card.toBuilder()
            .loggingTokens(setOf("hello"))

        assertEquals(card, card.toBuilder().build())
    }

    @Test
    fun toPaymentMethodsParams() {
        assertThat(CardFixtures.CARD_USD.toPaymentMethodsParams())
            .isEqualTo(
                PaymentMethodCreateParams.create(
                    card = PaymentMethodCreateParams.Card(
                        expiryMonth = 8,
                        expiryYear = 2017
                    ),
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "Jenny Rosen",
                        address = Address.Builder()
                            .setLine1("123 Market St")
                            .setLine2("#345")
                            .setCity("San Francisco")
                            .setState("CA")
                            .setPostalCode("94107")
                            .setCountry("US")
                            .build()
                    )
                )
            )
    }

    @Test
    fun paramsFromCard_mapsCorrectFields() {
        assertThat(CardFixtures.CARD.toParamMap())
            .isEqualTo(
                mapOf(
                    "card" to mapOf(
                        "number" to CardNumberFixtures.VISA_NO_SPACES,
                        "exp_month" to 8,
                        "exp_year" to 2050,
                        "cvc" to "123",
                        "name" to "Jenny Rosen",
                        "currency" to "USD",
                        "address_line1" to "123 Market St",
                        "address_line2" to "#345",
                        "address_city" to "San Francisco",
                        "address_state" to "CA",
                        "address_zip" to "94107",
                        "address_country" to "US"
                    )
                )
            )
    }

    internal companion object {
        private const val YEAR_IN_FUTURE: Int = 2100

        private fun createCard(
            number: String? = null,
            expMonth: Int? = null,
            expYear: Int? = null,
            cvc: String? = null
        ): Card {
            return Card.Builder(number, expMonth, expYear, cvc)
                .build()
        }
    }
}
