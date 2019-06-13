package com.stripe.android.model;

import android.support.annotation.NonNull;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.stripe.android.model.Card.asCardBrand;
import static com.stripe.android.model.Card.asFundingType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link Card}.
 */
public class CardTest {
    private static final int YEAR_IN_FUTURE = 2100;

    static final String JSON_CARD_USD = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": \"Des Moines\",\n" +
            "    \"address_country\": \"US\",\n" +
            "    \"address_line1\": \"123 Any Street\",\n" +
            "    \"address_line1_check\": \"unavailable\",\n" +
            "    \"address_line2\": \"456\",\n" +
            "    \"address_state\": \"IA\",\n" +
            "    \"address_zip\": \"50305\",\n" +
            "    \"address_zip_check\": \"unavailable\",\n" +
            "    \"brand\": \"Visa\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"usd\",\n" +
            "    \"customer\": \"customer77\",\n" +
            "    \"cvc_check\": \"unavailable\",\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"fingerprint\": \"abc123\",\n" +
            "    \"last4\": \"4242\",\n" +
            "    \"name\": \"John Cardholder\",\n" +
            "    \"metadata\": {\n" +
            "      \"color\": \"blue\",\n" +
            "      \"animal\": \"dog\"\n" +
            "    }\n" +
            "  }";

    static final String JSON_CARD_EUR = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": \"Des Moines\",\n" +
            "    \"address_country\": \"US\",\n" +
            "    \"address_line1\": \"123 Any Street\",\n" +
            "    \"address_line1_check\": \"unavailable\",\n" +
            "    \"address_line2\": \"456\",\n" +
            "    \"address_state\": \"IA\",\n" +
            "    \"address_zip\": \"50305\",\n" +
            "    \"address_zip_check\": \"unavailable\",\n" +
            "    \"brand\": \"Visa\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"eur\",\n" +
            "    \"customer\": \"customer77\",\n" +
            "    \"cvc_check\": \"unavailable\",\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"fingerprint\": \"abc123\",\n" +
            "    \"last4\": \"4242\",\n" +
            "    \"name\": \"John Cardholder\",\n" +
            "    \"metadata\": {\n" +
            "      \"color\": \"blue\",\n" +
            "      \"animal\": \"dog\"\n" +
            "    }\n" +
            "  }";

    private static final String BAD_JSON = "{ \"id\": ";

    private final Calendar calendar = Calendar.getInstance();

    @Before
    public void setup() {
        calendar.set(Calendar.YEAR, 1997);
        calendar.set(Calendar.MONTH, Calendar.AUGUST);
        calendar.set(Calendar.DAY_OF_MONTH, 29);
    }

    @Test
    public void asCardBrand_whenBlank_returnsNull() {
        assertNull(asCardBrand("   "));
        assertNull(asCardBrand(null));
    }

    @Test
    public void asCardBrand_whenNonemptyButWeird_returnsUnknown() {
        assertEquals(Card.UNKNOWN, asCardBrand("Awesome New Brand"));
    }

    @Test
    public void asCardBrand_whenMastercard_returnsMasterCard() {
        assertEquals(Card.MASTERCARD, asCardBrand("MasterCard"));
    }

    @Test
    public void asCardBrand_whenCapitalizedStrangely_stillRecognizesCard() {
        assertEquals(Card.MASTERCARD, asCardBrand("Mastercard"));
    }

    @Test
    public void asCardBrand_whenVisa_returnsVisa() {
        assertEquals(Card.VISA, asCardBrand("visa"));
    }

    @Test
    public void asCardBrand_whenJcb_returnsJcb() {
        assertEquals(Card.JCB, asCardBrand("Jcb"));
    }

    @Test
    public void asCardBrand_whenDiscover_returnsDiscover() {
        assertEquals(Card.DISCOVER, asCardBrand("Discover"));
    }

    @Test
    public void asCardBrand_whenDinersClub_returnsDinersClub() {
        assertEquals(Card.DINERS_CLUB, asCardBrand("Diners Club"));
    }

    @Test
    public void asCardBrand_whenAmericanExpress_returnsAmericanExpress() {
        assertEquals(Card.AMERICAN_EXPRESS, asCardBrand("American express"));
    }

    @Test
    public void asCardBrand_whenUnionPay_returnsUnionPay() {
        assertEquals(Card.UNIONPAY, asCardBrand("UnionPay"));
    }

    @Test
    public void asFundingType_whenDebit_returnsDebit() {
        assertEquals(Card.FUNDING_DEBIT, asFundingType("debit"));
    }

    @Test
    public void asFundingType_whenCredit_returnsCredit() {
        assertEquals(Card.FUNDING_CREDIT, asFundingType("credit"));
    }

    @Test
    public void asFundingType_whenCreditAndCapitalized_returnsCredit() {
        assertEquals(Card.FUNDING_CREDIT, asFundingType("Credit"));
    }

    @Test
    public void asFundingType_whenNull_returnsNull() {
        assertNull(asFundingType(null));
    }

    @Test
    public void asFundingType_whenBlank_returnsNull() {
        assertNull(asFundingType("   \t"));
    }

    @Test
    public void asFundingType_whenUnknown_returnsUnknown() {
        assertEquals(Card.FUNDING_UNKNOWN, asFundingType("unknown"));
    }

    @Test
    public void asFundingType_whenGobbledegook_returnsUnkown() {
        assertEquals(Card.FUNDING_UNKNOWN, asFundingType("personal iou"));
    }

    @Test
    public void canInitializeWithMinimalArguments() {
        Card card = Card.create("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateNumber());
    }

    @Test
    public void testTypeReturnsCorrectlyForAmexCard() {
        Card card = Card.create("3412123412341234", null, null, null);
        assertEquals(Card.AMERICAN_EXPRESS, card.getBrand());
    }

    @Test
    public void testTypeReturnsCorrectlyForDiscoverCard() {
        Card card = Card.create("6452123412341234", null, null, null);
        assertEquals(Card.DISCOVER, card.getBrand());
    }

    @Test
    public void testTypeReturnsCorrectlyForJCBCard() {
        Card card = Card.create("3512123412341234", null, null, null);
        assertEquals(Card.JCB, card.getBrand());
    }

    @Test
    public void testTypeReturnsCorrectlyForDinersClubCard() {
        Card card = Card.create("3612123412341234", null, null, null);
        assertEquals(Card.DINERS_CLUB, card.getBrand());
    }

    @Test
    public void testTypeReturnsCorrectlyForVisaCard() {
        Card card = Card.create("4112123412341234", null, null, null);
        assertEquals(Card.VISA, card.getBrand());
    }

    @Test
    public void testTypeReturnsCorrectlyForMasterCard() {
        Card card = Card.create("5112123412341234", null, null, null);
        assertEquals(Card.MASTERCARD, card.getBrand());
    }

    @Test
    public void testTypeReturnsCorrectlyForUnionPay() {
        Card card = Card.create("6200000000000005", null, null, null);
        assertEquals(Card.UNIONPAY, card.getBrand());
    }

    @Test
    public void shouldPassValidateNumberIfLuhnNumber() {
        Card card = Card.create("4242-4242-4242-4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfNotLuhnNumber() {
        Card card = Card.create("4242-4242-4242-4241", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberIfLuhnNumberAmex() {
        Card card = Card.create("378282246310005", null, null, null);
        assertEquals(Card.AMERICAN_EXPRESS, card.getBrand());
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfNull() {
        Card card = Card.create(null, null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfBlank() {
        Card card = Card.create("", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfJustSpaces() {
        Card card = Card.create("    ", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfTooShort() {
        Card card = Card.create("0", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfContainsLetters() {
        Card card = Card.create("424242424242a4242", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfTooLong() {
        Card card = Card.create("4242 4242 4242 4242 6", null, null, null);
        assertEquals(Card.VISA, card.getBrand());
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumber() {
        Card card = Card.create("4242424242424242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberSpaces() {
        Card card = Card.create("4242 4242 4242 4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberDashes() {
        Card card = Card.create("4242-4242-4242-4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberWithMixedSeparators() {
        Card card = Card.create("4242-4   242 424-24 242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfWithDot() {
        Card card = Card.create("4242.4242.4242.4242", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNull() {
        Card card = Card.create(null, null, null, null);
        assertFalse(card.validateExpMonth());
        assertFalse(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfNullMonth() {
        Card card = Card.create(null, null, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfZeroMonth() {
        Card card = Card.create(null, 0, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfNegativeMonth() {
        Card card = Card.create(null, -1, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfMonthToLarge() {
        Card card = Card.create(null, 13, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfNullYear() {
        Card card = Card.create(null, 1, null, null);
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfZeroYear() {
        Card card = Card.create(null, 12, 0, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfNegativeYear() {
        Card card = Card.create(null, 12, -1, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldPassValidateExpiryDateForDecemberOfThisYear() {
        Card card = Card.create(null, 12, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertTrue(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonth() {
        Card card = Card.create(null, 8, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertTrue(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonthTwoDigitYear() {
        Card card = Card.create(null, 8, 97, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertTrue(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateExpiryDateIfLastMonth() {
        Card card = Card.create(null, 7, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldPassValidateExpiryDateIfNextMonth() {
        Card card = Card.create(null, 9, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertTrue(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldPassValidateExpiryDateForJanuary00() {
        Card card = Card.create(null, 1, 0, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear(calendar));
        assertFalse(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldPassValidateExpiryDateForDecember99() {
        Card card = Card.create(null, 12, 99, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear(calendar));
        assertTrue(card.validateExpiryDate(calendar));
    }

    @Test
    public void shouldFailValidateCVCIfNull() {
        Card card = Card.create(null, null, null, null);
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfBlank() {
        Card card = Card.create(null, null, null, "");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfUnknownTypeAndLength2() {
        Card card = Card.create(null, null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfUnknownTypeAndLength3() {
        Card card = Card.create(null, null, null, "123");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfUnknownTypeAndLength4() {
        Card card = Card.create(null, null, null, "1234");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfUnknownTypeAndLength5() {
        Card card = Card.create(null, null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength2() {
        Card card = Card.create("4242 4242 4242 4242", null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfVisaAndLength3() {
        Card card = Card.create("4242 4242 4242 4242", null, null, "123");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength4() {
        Card card = Card.create("4242 4242 4242 4242", null, null, "1234");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength5() {
        Card card = Card.create("4242 4242 4242 4242", null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndNotNumeric() {
        Card card = Card.create("4242 4242 4242 4242", null, null, "12a");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfAmexAndLength2() {
        Card card = Card.create("378282246310005", null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfAmexAndLength3() {
        Card card = Card.create("378282246310005", null, null, "123");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfAmexAndLength4() {
        Card card = Card.create("378282246310005", null, null, "1234");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndLength5() {
        Card card = Card.create("378282246310005", null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndNotNumeric() {
        Card card = Card.create("378282246310005", null, null, "123d");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardIfNotLuhnNumber() {
        Card card = Card.create("4242-4242-4242-4241", 12, 2050, "123");
        assertFalse(card.validateCard());
        assertFalse(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardInvalidMonth() {
        Card card = Card.create("4242-4242-4242-4242", 13, 2050, "123");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertFalse(card.validateExpiryDate(calendar));
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardInvalidYear() {
        Card card = Card.create("4242-4242-4242-4242", 1, 1990, "123");
        assertFalse(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertFalse(card.validateExpiryDate(calendar));
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardWithNullCVC() {
        Card card = Card.create("4242-4242-4242-4242", 12, 2050, null);
        assertTrue(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardVisa() {
        Card card = Card.create("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithShortCVC() {
        Card card = Card.create("4242-4242-4242-4242", 12, 2050, "12");
        assertFalse(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithLongCVC() {
        Card card = Card.create("4242-4242-4242-4242", 12, 2050, "1234");
        assertFalse(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithBadCVC() {
        Card card = Card.create("4242-4242-4242-4242", 12, 2050, "bad");
        assertFalse(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardAmex() {
        Card card = Card.create("378282246310005", 12, 2050, "1234");
        assertTrue(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardAmexWithNullCVC() {
        Card card = Card.create("378282246310005", 12, 2050, null);
        assertTrue(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithShortCVC() {
        Card card = Card.create("378282246310005", 12, 2050, "12");
        assertFalse(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithLongCVC() {
        Card card = Card.create("378282246310005", 12, 2050, "12345");
        assertFalse(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithBadCVC() {
        Card card = Card.create("378282246310005", 12, 2050, "bad");
        assertFalse(card.validateCard(calendar));
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate(calendar));
        assertFalse(card.validateCVC());
    }

    @Test
    public void testLast4() {
        Card card = Card.create("42 42 42 42 42 42 42 42", null, null, null);
        assertEquals("4242", card.getLast4());
    }

    @Test
    public void last4ShouldBeNullWhenNumberIsNull() {
        Card card = Card.create(null, null, null, null);
        assertNull(card.getLast4());
    }

    @Test
    public void fromString_whenStringIsValidJson_returnsExpectedCard() {
        final Card expectedCard = createUsdCard();
        final Card actualCard = Card.fromString(JSON_CARD_USD);
        assertEquals(expectedCard, actualCard);
    }

    @Test
    public void toMap_catchesAllFields_fromRawJson() throws JSONException {
        final JSONObject rawJsonObject = new JSONObject(JSON_CARD_USD);
        final Map<String, Object> rawMap = StripeJsonUtils.jsonObjectToMap(rawJsonObject);
        final Card expectedCard = createUsdCard();
        JsonTestUtils.assertMapEquals(rawMap, expectedCard.toMap());
    }

    @Test
    public void fromString_whenStringIsBadJson_returnsNull() {
        assertNull(Card.fromString(BAD_JSON));
    }

    @Test
    public void toBuilder_whenChanged_isNotEquals() {
        assertNotEquals(CardFixtures.CARD, CardFixtures.CARD.toBuilder()
                .name("some other name")
                .build());
    }

    @Test
    public void toBuilder_whenUnchanged_isEquals() {
        assertEquals(CardFixtures.CARD, CardFixtures.CARD.toBuilder().build());
    }

    @Test
    public void toBuilder_withLoggingToken_whenUnchanged_isEquals() {
        final Card card = Objects.requireNonNull(Card.fromString(JSON_CARD_USD));
        card.addLoggingToken("hello");

        assertEquals(card, card.toBuilder().build());
    }

    @NonNull
    private static Card createUsdCard() {
        final Map<String, String> metadata = new HashMap<>(2);
        metadata.put("color", "blue");
        metadata.put("animal", "dog");

        return new Card.Builder(null, 8, 2017, null)
                .brand(Card.VISA)
                .funding(Card.FUNDING_CREDIT)
                .last4("4242")
                .id("card_189fi32eZvKYlo2CHK8NPRME")
                .country("US")
                .currency("usd")
                .addressCountry("US")
                .addressCity("Des Moines")
                .addressState("IA").addressZip("50305").addressZipCheck("unavailable")
                .addressLine1("123 Any Street").addressLine1Check("unavailable").addressLine2("456")
                .name("John Cardholder")
                .cvcCheck("unavailable")
                .customer("customer77")
                .fingerprint("abc123")
                .metadata(metadata)
                .build();
    }
}

