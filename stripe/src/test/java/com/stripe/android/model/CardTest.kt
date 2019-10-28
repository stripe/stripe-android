package com.stripe.android.model

import com.stripe.android.model.Card.Companion.asCardBrand
import com.stripe.android.model.Card.Companion.asFundingType
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
    fun asCardBrand_whenBlank_returnsNull() {
        assertNull(asCardBrand("   "))
        assertNull(asCardBrand(null))
    }

    @Test
    fun asCardBrand_whenNonemptyButWeird_returnsUnknown() {
        assertEquals(Card.CardBrand.UNKNOWN, asCardBrand("Awesome New CardBrand"))
    }

    @Test
    fun asCardBrand_whenMastercard_returnsMasterCard() {
        assertEquals(Card.CardBrand.MASTERCARD, asCardBrand("MasterCard"))
    }

    @Test
    fun asCardBrand_whenCapitalizedStrangely_stillRecognizesCard() {
        assertEquals(Card.CardBrand.MASTERCARD, asCardBrand("Mastercard"))
    }

    @Test
    fun asCardBrand_whenVisa_returnsVisa() {
        assertEquals(Card.CardBrand.VISA, asCardBrand("visa"))
    }

    @Test
    fun asCardBrand_whenJcb_returnsJcb() {
        assertEquals(Card.CardBrand.JCB, asCardBrand("Jcb"))
    }

    @Test
    fun asCardBrand_whenDiscover_returnsDiscover() {
        assertEquals(Card.CardBrand.DISCOVER, asCardBrand("Discover"))
    }

    @Test
    fun asCardBrand_whenDinersClub_returnsDinersClub() {
        assertEquals(Card.CardBrand.DINERS_CLUB, asCardBrand("Diners Club"))
    }

    @Test
    fun asCardBrand_whenAmericanExpress_returnsAmericanExpress() {
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, asCardBrand("American express"))
    }

    @Test
    fun asCardBrand_whenUnionPay_returnsUnionPay() {
        assertEquals(Card.CardBrand.UNIONPAY, asCardBrand("UnionPay"))
    }

    @Test
    fun asFundingType_whenDebit_returnsDebit() {
        assertEquals(Card.FundingType.DEBIT, asFundingType("debit"))
    }

    @Test
    fun asFundingType_whenCredit_returnsCredit() {
        assertEquals(Card.FundingType.CREDIT, asFundingType("credit"))
    }

    @Test
    fun asFundingType_whenCreditAndCapitalized_returnsCredit() {
        assertEquals(Card.FundingType.CREDIT, asFundingType("Credit"))
    }

    @Test
    fun asFundingType_whenNull_returnsNull() {
        assertNull(asFundingType(null))
    }

    @Test
    fun asFundingType_whenBlank_returnsNull() {
        assertNull(asFundingType("   \t"))
    }

    @Test
    fun asFundingType_whenUnknown_returnsUnknown() {
        assertEquals(Card.FundingType.UNKNOWN, asFundingType("unknown"))
    }

    @Test
    fun asFundingType_whenGobbledegook_returnsUnkown() {
        assertEquals(Card.FundingType.UNKNOWN, asFundingType("personal iou"))
    }

    @Test
    fun canInitializeWithMinimalArguments() {
        val card = Card.create("4242-4242-4242-4242", 12, 2050, "123")
        assertTrue(card.validateNumber())
    }

    @Test
    fun testTypeReturnsCorrectlyForAmexCard() {
        val card = Card.create("3412123412341234", null, null, null)
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForDiscoverCard() {
        val card = Card.create("6452123412341234", null, null, null)
        assertEquals(Card.CardBrand.DISCOVER, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForJCBCard() {
        val card = Card.create("3512123412341234", null, null, null)
        assertEquals(Card.CardBrand.JCB, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForDinersClubCard() {
        val card = Card.create("3612123412341234", null, null, null)
        assertEquals(Card.CardBrand.DINERS_CLUB, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForVisaCard() {
        val card = Card.create("4112123412341234", null, null, null)
        assertEquals(Card.CardBrand.VISA, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForMasterCard() {
        val card = Card.create("5112123412341234", null, null, null)
        assertEquals(Card.CardBrand.MASTERCARD, card.brand)
    }

    @Test
    fun testTypeReturnsCorrectlyForUnionPay() {
        val card = Card.create("6200000000000005", null, null, null)
        assertEquals(Card.CardBrand.UNIONPAY, card.brand)
    }

    @Test
    fun shouldPassValidateNumberIfLuhnNumber() {
        val card = Card.create("4242-4242-4242-4242", null, null, null)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfNotLuhnNumber() {
        val card = Card.create("4242-4242-4242-4241", null, null, null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberIfLuhnNumberAmex() {
        val card = Card.create("378282246310005", null, null, null)
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, card.brand)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfNull() {
        val card = Card.create(null, null, null, null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfBlank() {
        val card = Card.create("", null, null, null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfJustSpaces() {
        val card = Card.create("    ", null, null, null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfTooShort() {
        val card = Card.create("0", null, null, null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfContainsLetters() {
        val card = Card.create("424242424242a4242", null, null, null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfTooLong() {
        val card = Card.create("4242 4242 4242 4242 6", null, null, null)
        assertEquals(Card.CardBrand.VISA, card.brand)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumber() {
        val card = Card.create("4242424242424242", null, null, null)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberSpaces() {
        val card = Card.create("4242 4242 4242 4242", null, null, null)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberDashes() {
        val card = Card.create("4242-4242-4242-4242", null, null, null)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldPassValidateNumberWithMixedSeparators() {
        val card = Card.create("4242-4   242 424-24 242", null, null, null)
        assertTrue(card.validateNumber())
    }

    @Test
    fun shouldFailValidateNumberIfWithDot() {
        val card = Card.create("4242.4242.4242.4242", null, null, null)
        assertFalse(card.validateNumber())
    }

    @Test
    fun shouldFailValidateExpiryDateIfNull() {
        val card = Card.create(null, null, null, null)
        assertFalse(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNullMonth() {
        val card = Card.create(null, null, YEAR_IN_FUTURE, null)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfZeroMonth() {
        val card = Card.create(null, 0, YEAR_IN_FUTURE, null)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNegativeMonth() {
        val card = Card.create(null, -1, YEAR_IN_FUTURE, null)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfMonthToLarge() {
        val card = Card.create(null, 13, YEAR_IN_FUTURE, null)
        assertFalse(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNullYear() {
        val card = Card.create(null, 1, null, null)
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfZeroYear() {
        val card = Card.create(null, 12, 0, null)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfNegativeYear() {
        val card = Card.create(null, 12, -1, null)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForDecemberOfThisYear() {
        val card = Card.create(null, 12, 1997, null)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfCurrentMonth() {
        val card = Card.create(null, 8, 1997, null)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfCurrentMonthTwoDigitYear() {
        val card = Card.create(null, 8, 97, null)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateExpiryDateIfLastMonth() {
        val card = Card.create(null, 7, 1997, null)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateIfNextMonth() {
        val card = Card.create(null, 9, 1997, null)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForJanuary00() {
        val card = Card.create(null, 1, 0, null)
        assertTrue(card.validateExpMonth())
        assertFalse(card.validateExpYear(calendar))
        assertFalse(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldPassValidateExpiryDateForDecember99() {
        val card = Card.create(null, 12, 99, null)
        assertTrue(card.validateExpMonth())
        assertTrue(card.validateExpYear(calendar))
        assertTrue(card.validateExpiryDate(calendar))
    }

    @Test
    fun shouldFailValidateCVCIfNull() {
        val card = Card.create(null, null, null, null)
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfBlank() {
        val card = Card.create(null, null, null, "")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfUnknownTypeAndLength2() {
        val card = Card.create(null, null, null, "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfUnknownTypeAndLength3() {
        val card = Card.create(null, null, null, "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfUnknownTypeAndLength4() {
        val card = Card.create(null, null, null, "1234")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfUnknownTypeAndLength5() {
        val card = Card.create(null, null, null, "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength2() {
        val card = Card.create("4242 4242 4242 4242", null, null, "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfVisaAndLength3() {
        val card = Card.create("4242 4242 4242 4242", null, null, "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength4() {
        val card = Card.create("4242 4242 4242 4242", null, null, "1234")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndLength5() {
        val card = Card.create("4242 4242 4242 4242", null, null, "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfVisaAndNotNumeric() {
        val card = Card.create("4242 4242 4242 4242", null, null, "12a")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength2() {
        val card = Card.create("378282246310005", null, null, "12")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength3() {
        val card = Card.create("378282246310005", null, null, "123")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCVCIfAmexAndLength4() {
        val card = Card.create("378282246310005", null, null, "1234")
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfAmexAndLength5() {
        val card = Card.create("378282246310005", null, null, "12345")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCVCIfAmexAndNotNumeric() {
        val card = Card.create("378282246310005", null, null, "123d")
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardIfNotLuhnNumber() {
        val card = Card.create("4242-4242-4242-4241", 12, 2050, "123")
        assertFalse(card.validateCard())
        assertFalse(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardInvalidMonth() {
        val card = Card.create("4242-4242-4242-4242", 13, 2050, "123")
        assertFalse(card.validateCard())
        assertTrue(card.validateNumber())
        assertFalse(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardInvalidYear() {
        val card = Card.create("4242-4242-4242-4242", 1, 1990, "123")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertFalse(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardWithNullCVC() {
        val card = Card.create("4242-4242-4242-4242", 12, 2050, null)
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardVisa() {
        val card = Card.create("4242-4242-4242-4242", 12, 2050, "123")
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithShortCVC() {
        val card = Card.create("4242-4242-4242-4242", 12, 2050, "12")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithLongCVC() {
        val card = Card.create("4242-4242-4242-4242", 12, 2050, "1234")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardVisaWithBadCVC() {
        val card = Card.create("4242-4242-4242-4242", 12, 2050, "bad")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardAmex() {
        val card = Card.create("378282246310005", 12, 2050, "1234")
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertTrue(card.validateCVC())
    }

    @Test
    fun shouldPassValidateCardAmexWithNullCVC() {
        val card = Card.create("378282246310005", 12, 2050, null)
        assertTrue(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithShortCVC() {
        val card = Card.create("378282246310005", 12, 2050, "12")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithLongCVC() {
        val card = Card.create("378282246310005", 12, 2050, "12345")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun shouldFailValidateCardAmexWithBadCVC() {
        val card = Card.create("378282246310005", 12, 2050, "bad")
        assertFalse(card.validateCard(calendar))
        assertTrue(card.validateNumber())
        assertTrue(card.validateExpiryDate(calendar))
        assertFalse(card.validateCVC())
    }

    @Test
    fun testLast4() {
        val card = Card.create("42 42 42 42 42 42 42 42", null, null, null)
        assertEquals("4242", card.last4)
    }

    @Test
    fun last4ShouldBeNullWhenNumberIsNull() {
        val card = Card.create(null, null, null, null)
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
            .brand(Card.CardBrand.AMERICAN_EXPRESS)
            .build()
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, card.brand)
    }

    @Test
    fun fromString_whenStringIsValidJson_returnsExpectedCard() {
        val expectedCard = CARD_USD
        val actualCard = Card.fromJson(JSON_CARD_USD)
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
        val card = requireNotNull(Card.fromJson(JSON_CARD_USD))
        card.toBuilder()
            .loggingTokens(listOf("hello"))

        assertEquals(card, card.toBuilder().build())
    }

    @Test
    fun toPaymentMethodsParams() {
        val actual = CARD_USD.toPaymentMethodsParams()
        val expected = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.Builder()
                .setExpiryMonth(8)
                .setExpiryYear(2017)
                .build(),
            PaymentMethod.BillingDetails.Builder()
                .setName("John Cardholder")
                .setAddress(Address.Builder()
                    .setLine1("123 Any Street")
                    .setLine2("456")
                    .setCity("Des Moines")
                    .setState("IA")
                    .setPostalCode("50305")
                    .setCountry("US")
                    .build())
                .build()
        )
        assertEquals(expected, actual)
    }

    companion object {
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

        internal val CARD_USD = Card.Builder(null, 8, 2017, null)
            .brand(Card.CardBrand.VISA)
            .funding(Card.FundingType.CREDIT)
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
