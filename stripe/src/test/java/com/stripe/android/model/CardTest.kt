package com.stripe.android.model

import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.parsers.CardJsonParser
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONObject

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
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "123")
        assertTrue(card.validateNumber())
    }

    @Test
    fun testTypeReturnsCorrectlyForAmexCard() {
        val card = Card.create(number = "3412123412341234")
        assertEquals(CardBrand.AmericanExpress, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForDiscoverCard() {
        val card = Card.create(number = "6452123412341234")
        assertEquals(CardBrand.Discover, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForJCBCard() {
        val card = Card.create(number = "3512123412341234")
        assertEquals(CardBrand.JCB, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForDinersClubCard() {
        val card = Card.create(number = "3612123412341234")
        assertEquals(CardBrand.DinersClub, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForVisaCard() {
        val card = Card.create(number = "4112123412341234")
        assertEquals(CardBrand.Visa, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForMasterCard() {
        val card = Card.create(number = "5112123412341234")
        assertEquals(CardBrand.MasterCard, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForUnionPay() {
        val card = Card.create(number = CardNumberFixtures.UNIONPAY_NO_SPACES)
        assertEquals(CardBrand.UnionPay, card.brand)
    }

    @Test
    fun shouldPassValidateNumberIfLuhnNumber() {
        val card = Card.create(number = "4242-4242-4242-4242")
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfNotLuhnNumber() {
        val card = Card.create(number = "4242-4242-4242-4241")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberIfLuhnNumberAmex() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES)
        assertEquals(CardBrand.AmericanExpress, card.brand)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfNull() {
        val card = Card.create(cvc = null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfBlank() {
        val card = Card.create(number = "")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfJustSpaces() {
        val card = Card.create(number = "    ")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfTooShort() {
        val card = Card.create(number = "0")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfContainsLetters() {
        val card = Card.create(number = "424242424242a4242")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfTooLong() {
        val card = Card.create(number = "4242 4242 4242 4242 6")
        assertEquals(CardBrand.Visa, card.brand)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumber() {
        val card = Card.create(CardNumberFixtures.VISA_NO_SPACES)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberSpaces() {
        val card = Card.create(CardNumberFixtures.VISA_WITH_SPACES)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberDashes() {
        val card = Card.create(number = "4242-4242-4242-4242")
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberWithMixedSeparators() {
        val card = Card.create(number = "4242-4   242 424-24 242")
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfWithDot() {
        val card = Card.create(number = "4242.4242.4242.4242")
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateExpiryDateIfNull() {
        val card = Card.create(cvc = null)
        assertFalse(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNullMonth() {
        val card = Card.create(expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfZeroMonth() {
        val card = Card.create(expMonth = 0, expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNegativeMonth() {
        val card = Card.create(expMonth = -1, expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfMonthToLarge() {
        val card = Card.create(expMonth = 13, expYear = YEAR_IN_FUTURE)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNullYear() {
        val card = Card.create(expMonth = 1)
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfZeroYear() {
        val card = Card.create(expMonth = 12, expYear = 0)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNegativeYear() {
        val card = Card.create(expMonth = 12, expYear = -1)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForDecemberOfThisYear() {
        val card = Card.create(expMonth = 12, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfCurrentMonth() {
        val card = Card.create(expMonth = 8, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfCurrentMonthTwoDigitYear() {
        val card = Card.create(expMonth = 8, expYear = 97)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfLastMonth() {
        val card = Card.create(expMonth = 7, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfNextMonth() {
        val card = Card.create(expMonth = 9, expYear = 1997)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForJanuary00() {
        val card = Card.create(expMonth = 1, expYear = 0)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForDecember99() {
        val card = Card.create(expMonth = 12, expYear = 99)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateCVCIfNull() {
        val card = Card.create(cvc = null)
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfBlank() {
        val card = Card.create(cvc = "")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfUnknownTypeAndLength2() {
        val card = Card.create(cvc = "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfUnknownTypeAndLength3() {
        val card = Card.create(cvc = "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfUnknownTypeAndLength4() {
        val card = Card.create(cvc = "1234")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfUnknownTypeAndLength5() {
        val card = Card.create(cvc = "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength2() {
        val card = Card.create(number = "4242 4242 4242 4242", cvc = "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfVisaAndLength3() {
        val card = Card.create(number = "4242 4242 4242 4242", cvc = "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength4() {
        val card = Card.create(number = "4242 4242 4242 4242", cvc = "1234")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength5() {
        val card = Card.create(number = "4242 4242 4242 4242", cvc = "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndNotNumeric() {
        val card = Card.create(number = "4242 4242 4242 4242", cvc = "12a")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength2() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength3() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength4() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "1234")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfAmexAndLength5() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfAmexAndNotNumeric() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, cvc = "123d")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardIfNotLuhnNumber() {
        val card = Card.create(number = "4242-4242-4242-4241", expMonth = 12, expYear = 2050, cvc = "123")
        assertFalse(card.validateCard())
        assertFalse(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardInvalidMonth() {
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 13, expYear = 2050, cvc = "123")
        assertFalse(card.validateCard())
        assertTrue(card.validateNumber())
        assertFalse(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardInvalidYear() {
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 1, expYear = 1990, cvc = "123")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertFalse(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardWithNullCVC() {
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050)
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardVisa() {
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "123")
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithShortCVC() {
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "12")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithLongCVC() {
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "1234")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithBadCVC() {
        val card = Card.create(number = "4242-4242-4242-4242", expMonth = 12, expYear = 2050, cvc = "bad")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardAmex() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "1234")
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardAmexWithNullCVC() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050)
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithShortCVC() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "12")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithLongCVC() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "12345")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithBadCVC() {
        val card = Card.create(number = CardNumberFixtures.AMEX_NO_SPACES, expMonth = 12, expYear = 2050, cvc = "bad")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun testLast4() {
        val card = Card.create(number = "42 42 42 42 42 42 42 42")
        assertEquals("4242", card.last4)
    }

    @Test
    fun last4ShouldBeNullWhenNumberIsNull() {
        val card = Card.create(cvc = null)
        assertNull(card.last4)
    }

    @Test
    fun getLast4_whenNumberIsNullButLast4IsSet_returnsCorrectValue() {
        val card = Card.Builder(null, 2, 2020, "123")
            .name("John Q Public")
            .last4("1234")
            .build()
        assertEquals("1234", card.last4)
    }

    @Test
    fun getBrand_whenNumberIsNullButBrandIsSet_returnsCorrectValue() {
        val card = Card.Builder(null, 2, 2020, "123")
            .name("John Q Public")
            .brand(CardBrand.AmericanExpress)
            .build()
        assertEquals(CardBrand.AmericanExpress, card.brand)
    }

    @Test
    fun fromString_whenStringIsValidJson_returnsExpectedCard() {
        val expectedCard = CARD_USD
        val actualCard = CardJsonParser().parse(JSON_CARD_USD)
        assertEquals(expectedCard, actualCard)
    }

    @Test
    fun fromString_whenStringIsBadJson_returnsNull() {
        assertNull(Card.fromString(BAD_JSON))
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
        val card = requireNotNull(CardJsonParser().parse(JSON_CARD_USD))
        card.toBuilder()
            .loggingTokens(setOf("hello"))

        assertEquals(card, card.toBuilder().build())
    }

    @Test
    fun toPaymentMethodsParams() {
        val actual = CARD_USD.toPaymentMethodsParams()
        val expected = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card(
                expiryMonth = 8,
                expiryYear = 2017
            ),
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Cardholder",
                address = Address.Builder()
                    .setLine1("123 Any Street")
                    .setLine2("456")
                    .setCity("Des Moines")
                    .setState("IA")
                    .setPostalCode("50305")
                    .setCountry("US")
                    .build()
            )
        )
        assertEquals(expected, actual)
    }

    internal companion object {
        private const val YEAR_IN_FUTURE: Int = 2100

        internal val JSON_CARD_USD = JSONObject(
            """
            {
                "id": "card_189fi32eZvKYlo2CHK8NPRME",
                "object": "card",
                "address_city": "Des Moines",
                "address_country": "US",
                "address_line1": "123 Any Street",
                "address_line1_check": "unavailable",
                "address_line2": "456",
                "address_state": "IA",
                "address_zip": "50305",
                "address_zip_check": "unavailable",
                "brand": "Visa",
                "country": "US",
                "currency": "usd",
                "customer": "customer77",
                "cvc_check": "unavailable",
                "exp_month": 8,
                "exp_year": 2017,
                "funding": "credit",
                "fingerprint": "abc123",
                "last4": "4242",
                "name": "John Cardholder",
                "metadata": {
                    "color": "blue",
                    "animal": "dog"
                }
            }
            """.trimIndent()
        )

        private const val BAD_JSON: String = "{ \"id\": "

        internal val CARD_USD = Card.Builder(expMonth = 8, expYear = 2017)
            .brand(CardBrand.Visa)
            .funding(CardFunding.Credit)
            .last4("4242")
            .id("card_189fi32eZvKYlo2CHK8NPRME")
            .country("US")
            .currency("usd")
            .addressCountry("US")
            .addressCity("Des Moines")
            .addressState("IA")
            .addressZip("50305")
            .addressZipCheck("unavailable")
            .addressLine1("123 Any Street")
            .addressLine1Check("unavailable")
            .addressLine2("456")
            .name("John Cardholder")
            .cvcCheck("unavailable")
            .customer("customer77")
            .fingerprint("abc123")
            .metadata(mapOf(
                "color" to "blue",
                "animal" to "dog"
            ))
            .build()
    }
}
