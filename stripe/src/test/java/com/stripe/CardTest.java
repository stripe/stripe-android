package com.stripe;

import com.stripe.time.FrozenClock;
import com.stripe.util.URLUtils;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CardTest {

    private HashMap<String, String> validCardMap;
    private String thisYear;
    private String lastYear;

    private String thisMonth;
    private String nextMonth;
    private String lastMonth;
    private String nextYear;

    private String goodNumber;
    private String badNumber;
    private String alphaNumber;
    private final String goodNumberStripped = "4242424242424242";

    @Before
    public void setup() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MONTH, Calendar.DECEMBER);
        FrozenClock.freeze(now);

        Calendar calc = Calendar.getInstance();
        calc.set(Calendar.MONTH, Calendar.DECEMBER);

        thisYear = String.valueOf(calc.get(Calendar.YEAR));
        calc.add(Calendar.YEAR, -1);
        lastYear = String.valueOf(calc.get(Calendar.YEAR));
        calc.add(Calendar.YEAR, 2);
        nextYear = String.valueOf(calc.get(Calendar.YEAR));

        thisMonth = String.valueOf(calc.get(Calendar.MONTH) + 1);
        calc.add(Calendar.MONTH, 1);
        nextMonth = String.valueOf(calc.get(Calendar.MONTH) + 1);
        calc.add(Calendar.MONTH, -2);
        lastMonth = String.valueOf(calc.get(Calendar.MONTH) + 1);

        goodNumber = "4242-4242-4242-4242";
        badNumber = "4242-4242-4242-4241";
        alphaNumber = "4242-4242a-4242-4242";

        validCardMap = new HashMap<String, String>();
        validCardMap.put("number", goodNumber);
        validCardMap.put("cvc", "123");
        validCardMap.put("type", "Smastercard");
        validCardMap.put("exp_month", thisMonth);
        validCardMap.put("exp_year", thisYear);
    }

    @After
    public void tearDown() {
        FrozenClock.unfreeze();
    }

    @Test
    public void fromJSON_should_work_with_minimal_fields() throws JSONException {
        Card card = Card.fromJSON("{number: \"4242-4242-4242-4242\"}");
        assertValidation(card.validateNumber(), true);
    }

    @Test
    public void validateCard_should_work() {
        Card card = new Card(validCardMap);
        Validation validation = card.validateCard();
        assertValidation(validation, true);
    }

    @Test
    public void validateCard_should_ignore_null_cvc() {
        validCardMap.remove("cvc");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCard();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpYear_should_work() {
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpYear_should_fail_if_passed() {
        validCardMap.put("exp_year", lastYear);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpYear_should_work_if_padded() {
        validCardMap.put("exp_year", " \t " + thisYear + "\n\n  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpYear_should_fail_if_not_number() {
        validCardMap.put("exp_year", "hello");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpYear_should_fail_if_decimal() {
        validCardMap.put("exp_year", "21.1");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpYear_should_fail_if_negative() {
        validCardMap.put("exp_year", "-17");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpYear_should_fail_if_null() {
        validCardMap.remove("exp_year");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpYear_should_fail_if_blank() {
        validCardMap.put("exp_year", "  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpYear_should_work_if_two_digits() {
        validCardMap.put("exp_year", thisYear.substring(2, 4));
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpMonth_should_work() {
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpMonth_should_validate_if_passed() {
        validCardMap.put("exp_month", lastMonth);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpMonth_should_work_if_padded() {
        validCardMap.put("exp_month", " \t " + thisMonth + "\n\n  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpMonth_should_fail_if_not_number() {
        validCardMap.put("exp_month", "hello");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpMonth_should_fail_if_decimal() {
        validCardMap.put("exp_month", "21.1");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpMonth_should_fail_if_negative() {
        validCardMap.put("exp_month", "-17");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpMonth_should_fail_if_null() {
        validCardMap.remove("exp_month");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpMonth_should_fail_if_blank() {
        validCardMap.put("exp_month", "  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpMonth_should_fail_if_invalid_month() {
        validCardMap.put("exp_month", "13");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpMonth_should_fail_if_0() {
        validCardMap.put("exp_month", "00");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpMonth_should_work_if_0_padded() {
        validCardMap.put("exp_month", "09");
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpMonth();
        assertValidation(validation, true);
        assertEquals("09", card.expMonth);
    }

    @Test
    public void validateExpiryDate_should_work() {
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpiryDate();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpiryDate_should_work_in_future() {
        validCardMap.put("exp_month", nextMonth);
        validCardMap.put("exp_year", nextYear);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpiryDate();
        assertValidation(validation, true);
    }

    @Test
    public void validateExpiryDate_should_fail_if_month_fails() {
        validCardMap.put("exp_month", null);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpiryDate();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpiryDate_should_fail_if_year_fails() {
        validCardMap.put("exp_year", null);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpiryDate();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpiryDate_should_fail_if_year_is_in_the_past() {
        validCardMap.put("exp_year", lastYear);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpiryDate();
        assertValidation(validation, false, StripeError.INVALID_EXP_YEAR);
    }

    @Test
    public void validateExpiryDate_should_fail_if_month_is_in_the_past() {
        validCardMap.put("exp_month", lastMonth);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpiryDate();
        assertValidation(validation, false, StripeError.INVALID_EXP_MONTH);
    }

    @Test
    public void validateExpiryDate_should_work_if_year_two_digits() {
        validCardMap.put("exp_year", thisYear.substring(2, 4));
        validCardMap.put("exp_month", thisMonth);
        Card card = new Card(validCardMap);
        Validation validation = card.validateExpYear();
        assertValidation(validation, true);
    }

    @Test
    public void validateCVC_should_work() {
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, true);
    }

    @Test
    public void validateCVC_should_fail_if_4_digits() {
        validCardMap.put("cvc", "1234");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_2_digits() {
        validCardMap.put("cvc", "12");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_5_digits() {
        validCardMap.put("cvc", "12345");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_work_if_american_express_and_4_digits() {
        validCardMap.put("type", "American Express");
        validCardMap.put("cvc", "1234");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, true);
    }

    @Test
    public void validateCVC_should_fail_if_american_express_and_3_digits() {
        validCardMap.put("type", "American Express");
        validCardMap.put("cvc", "123");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_american_express_and_5_digits() {
        validCardMap.put("type", "American Express");
        validCardMap.put("cvc", "12345");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_pass_if_3_digits_when_unknown_type() {
        validCardMap.remove("type");
        validCardMap.remove("number");
        validCardMap.put("cvc", "123");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, true);
    }

    @Test
    public void validateCVC_should_pass_if_4_digits_when_unknown_type() {
        validCardMap.remove("type");
        validCardMap.remove("number");
        validCardMap.put("cvc", "1234");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, true);
    }


    @Test
    public void validateCVC_should_fail_if_2_digits_when_unknown_type() {
        validCardMap.remove("type");
        validCardMap.remove("number");
        validCardMap.put("cvc", "12");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_5_digits_when_unknown_type() {
        validCardMap.remove("type");
        validCardMap.put("cvc", "12345");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_work_if_padded() {
        validCardMap.put("cvc", " \t 123\n\n  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, true);
    }

    @Test
    public void validateCVC_should_fail_if_not_number() {
        validCardMap.put("cvc", "hello");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_decimal() {
        validCardMap.put("cvc", "21.1");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_negative() {
        validCardMap.put("cvc", "-17");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_null() {
        validCardMap.remove("cvc");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateCVC_should_fail_if_blank() {
        validCardMap.put("cvc", "  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateCVC();
        assertValidation(validation, false, StripeError.INVALID_CVC);
    }

    @Test
    public void validateNumber_should_work() {
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, true);
    }

    @Test
    public void validateNumber_should_fail_if_not_luhn_number() {
        validCardMap.put("number", badNumber);
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_fail_if_alpha_present() {
        validCardMap.put("number", alphaNumber);
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_fail_if_9_digits() {
        validCardMap.put("number", "123456789");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_fail_if_20_digits() {
        validCardMap.put("number", "12345678901234567890");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_work_if_padded() {
        validCardMap.put("number", " \t " + goodNumber + "\n\n  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, true);
    }

    @Test
    public void validateNumber_should_fail_if_not_number() {
        validCardMap.put("number", "abcdefghijklmnop");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_fail_if_decimal() {
        validCardMap.put("number", "21.1");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_fail_if_negative() {
        validCardMap.put("number", "-17");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_fail_if_null() {
        validCardMap.remove("number");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void validateNumber_should_fail_if_blank() {
        validCardMap.put("number", "  ");
        Card card = new Card(validCardMap);
        Validation validation = card.validateNumber();
        assertValidation(validation, false, StripeError.INVALID_NUMBER);
    }

    @Test
    public void last4_should_be_set_for_number() {
        Card card = new Card(validCardMap);
        assertEquals("4242", card.last4);
    }

    @Test
    public void last4_should_be_null_for_null_number() {
        validCardMap.remove("number");
        Card card = new Card(validCardMap);
        assertEquals(null, card.last4);
    }

    @Test
    public void last4_should_work_if_stray_dashes() {
        validCardMap.put("number", goodNumber + "-66");
        Card card = new Card(validCardMap);
        assertEquals("4266", card.last4);
    }

    @Test
    public void type_should_take_specified() {
        Card card = new Card(validCardMap);
        assertEquals("Smastercard", card.type);
    }

    @Test
    public void type_should_be_null_if_number_null() {
        validCardMap.remove("type");
        validCardMap.remove("number");
        Card card = new Card(validCardMap);
        assertNull(card.type);
    }

    @Test
    public void type_should_know_visa_numbers() {
        assertCardTypePrefixes("Visa", "4");
    }

    @Test
    public void type_should_know_mastercard_numbers() {
        assertCardTypePrefixes("MasterCard", "5");
    }

    @Test
    public void type_should_know_jcb_numbers() {
        assertCardTypePrefixes("JCB", "35");
    }

    @Test
    public void type_should_know_diners_club_numbers() {
        assertCardTypePrefixes("Diners Club", "30", "36", "38", "39");
    }

    @Test
    public void type_should_know_discover_numbers() {
        assertCardTypePrefixes("Discover", "60", "62", "64", "65");
    }

    @Test
    public void type_should_know_american_express_numbers() {
        assertCardTypePrefixes("American Express", "34", "37");
    }

    @Test
    public void type_should_be_unknown_for_other_prefixes() {
        assertCardTypePrefixes("Unknown", "31", "9", "1");
    }

    @Test
    public void encode_should_work() {
        String expected = String.format("card[number]=%s&card[cvc]=%s&card[exp_year]=%s&card[exp_month]=%s",
                goodNumberStripped, "123", thisYear, thisMonth);

        Card card = new Card(validCardMap);
        assertEquals(expected, card.urlEncode());
    }

    @Test
    public void encode_should_add_name_if_known() {
        String expected = String.format("card[number]=%s&card[cvc]=%s&card[exp_year]=%s&card[exp_month]=%s&card[name]=%s",
                goodNumberStripped, "123", thisYear, thisMonth, "Tim");
        validCardMap.put("name", "Tim");

        Card card = new Card(validCardMap);
        assertEquals(expected, card.urlEncode());
    }

    @Test
    public void encode_should_add_address_if_known() {
        String expected = String.format("card[number]=%s" +
                "&card[cvc]=%s" +
                "&card[exp_year]=%s" +
                "&card[exp_month]=%s" +
                "&card[address_line1]=%s" +
                "&card[address_line2]=%s" +
                "&card[address_city]=%s" +
                "&card[address_state]=%s" +
                "&card[address_zip]=%s" +
                "&card[address_country]=%s",
                goodNumberStripped, "123", thisYear, thisMonth,
                URLUtils.urlEncode("123 Seasame St"),
                URLUtils.urlEncode("PO 193"),
                URLUtils.urlEncode("Main Town"),
                URLUtils.urlEncode("AK"),
                URLUtils.urlEncode("99999"),
                URLUtils.urlEncode("USA")
        );
        validCardMap.put("address_line1", "123 Seasame St");
        validCardMap.put("address_line2", "PO 193");
        validCardMap.put("address_city", "Main Town");
        validCardMap.put("address_state", "AK");
        validCardMap.put("address_zip", "99999");
        validCardMap.put("address_country", "USA");

        Card card = new Card(validCardMap);
        assertEquals(expected, card.urlEncode());
    }

    private void assertCardTypePrefixes(String cardType, String... prefixes) {
        validCardMap.remove("type");
        for (String prefix : prefixes) {
            validCardMap.put("number", prefix + goodNumber);
            Card card = new Card(validCardMap);
            assertEquals(cardType, card.type);
        }
    }

    private void assertValidation(Validation validation, boolean validity, StripeError... errors) {
        assertEquals(new HashSet<StripeError>(Arrays.asList(errors)), validation.errors);
        assertEquals(validity, validation.isValid);
    }
}
