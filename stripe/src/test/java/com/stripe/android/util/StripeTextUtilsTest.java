package com.stripe.android.util;

import com.stripe.android.model.Card;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link StripeTextUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StripeTextUtilsTest {

    @Test
    public void hasAnyPrefixShouldFailIfNull() {
        assertFalse(StripeTextUtils.hasAnyPrefix(null));
    }

    @Test
    public void hasAnyPrefixShouldFailIfEmpty() {
        assertFalse(StripeTextUtils.hasAnyPrefix(""));
    }

    @Test
    public void hasAnyPrefixShouldFailWithNullAndEmptyPrefix() {
        assertFalse(StripeTextUtils.hasAnyPrefix(null, ""));
    }

    @Test
    public void hasAnyPrefixShouldFailWithNullAndSomePrefix() {
        assertFalse(StripeTextUtils.hasAnyPrefix(null, "1"));
    }

    @Test
    public void hasAnyPrefixShouldMatch() {
        assertTrue(StripeTextUtils.hasAnyPrefix("1234", "12"));
    }

    @Test
    public void hasAnyPrefixShouldMatchMultiple() {
        assertTrue(StripeTextUtils.hasAnyPrefix("1234", "12", "1"));
    }

    @Test
    public void hasAnyPrefixShouldMatchSome() {
        assertTrue(StripeTextUtils.hasAnyPrefix("abcd", "bc", "ab"));
    }

    @Test
    public void hasAnyPrefixShouldNotMatch() {
        assertFalse(StripeTextUtils.hasAnyPrefix("1234", "23"));
    }

    @Test
    public void hasAnyPrefixShouldNotMatchWithSpace() {
        assertFalse(StripeTextUtils.hasAnyPrefix("xyz", " x"));
    }

    @Test
    public void wholePositiveNumberShouldFailNull() {
        assertFalse(StripeTextUtils.isWholePositiveNumber(null));
    }

    @Test
    public void wholePositiveNumberShouldPassIfEmpty() {
        assertTrue(StripeTextUtils.isWholePositiveNumber(""));
    }

    @Test
    public void wholePositiveNumberShouldPass() {
        assertTrue(StripeTextUtils.isWholePositiveNumber("123"));
    }

    @Test
    public void wholePositiveNumberShouldPassWithLeadingZero() {
        assertTrue(StripeTextUtils.isWholePositiveNumber("000"));
    }

    @Test
    public void wholePositiveNumberShouldFailIfNegative() {
        assertFalse(StripeTextUtils.isWholePositiveNumber("-1"));
    }

    @Test
    public void wholePositiveNumberShouldFailIfLetters() {
        assertFalse(StripeTextUtils.isWholePositiveNumber("1a"));
    }

    @Test
    public void testNullIfBlankNullShouldBeNull() {
        assertEquals(null, StripeTextUtils.nullIfBlank(null));
    }

    @Test
    public void testNullIfBlankEmptyShouldBeNull() {
        assertEquals(null, StripeTextUtils.nullIfBlank(""));
    }

    @Test
    public void testNullIfBlankSpaceShouldBeNull() {
        assertEquals(null, StripeTextUtils.nullIfBlank(" "));
    }

    @Test
    public void testNullIfBlankSpacesShouldBeNull() {
        assertEquals(null, StripeTextUtils.nullIfBlank("     "));
    }

    @Test
    public void testNullIfBlankTabShouldBeNull() {
        assertEquals(null, StripeTextUtils.nullIfBlank("	"));
    }

    @Test
    public void testNullIfBlankNumbersShouldNotBeNull() {
        assertEquals("0", StripeTextUtils.nullIfBlank("0"));
    }

    @Test
    public void testNullIfBlankLettersShouldNotBeNull() {
        assertEquals("abc", StripeTextUtils.nullIfBlank("abc"));
    }

    @Test
    public void isBlankShouldPassIfNull() {
        assertTrue(StripeTextUtils.isBlank(null));
    }

    @Test
    public void isBlankShouldPassIfEmpty() {
        assertTrue(StripeTextUtils.isBlank(""));
    }

    @Test
    public void isBlankShouldPassIfSpace() {
        assertTrue(StripeTextUtils.isBlank(" "));
    }

    @Test
    public void isBlankShouldPassIfSpaces() {
        assertTrue(StripeTextUtils.isBlank("     "));
    }

    @Test
    public void isBlankShouldPassIfTab() {
        assertTrue(StripeTextUtils.isBlank("	"));
    }

    @Test
    public void isBlankShouldFailIfNumber() {
        assertFalse(StripeTextUtils.isBlank("0"));
    }

    @Test
    public void isBlankShouldFailIfLetters() {
        assertFalse(StripeTextUtils.isBlank("abc"));
    }

    @Test
    public void asCardBrand_whenBlank_returnsNull() {
        assertNull(StripeTextUtils.asCardBrand("   "));
        assertNull(StripeTextUtils.asCardBrand(null));
    }

    @Test
    public void asCardBrand_whenNonemptyButWeird_returnsUnknown() {
        assertEquals(Card.UNKNOWN, StripeTextUtils.asCardBrand("Awesome New Brand"));
    }

    @Test
    public void asCardBrand_whenMastercard_returnsMasterCard() {
        assertEquals(Card.MASTERCARD, StripeTextUtils.asCardBrand("MasterCard"));
    }

    @Test
    public void asCardBrand_whenCapitalizedStrangely_stillRecognizesCard() {
        assertEquals(Card.MASTERCARD, StripeTextUtils.asCardBrand("Mastercard"));
    }

    @Test
    public void asCardBrand_whenVisa_returnsVisa() {
        assertEquals(Card.VISA, StripeTextUtils.asCardBrand("visa"));
    }

    @Test
    public void asCardBrand_whenJcb_returnsJcb() {
        assertEquals(Card.JCB, StripeTextUtils.asCardBrand("Jcb"));
    }

    @Test
    public void asCardBrand_whenDiscover_returnsDiscover() {
        assertEquals(Card.DISCOVER, StripeTextUtils.asCardBrand("Discover"));
    }

    @Test
    public void asCardBrand_whenDinersClub_returnsDinersClub() {
        assertEquals(Card.DINERS_CLUB, StripeTextUtils.asCardBrand("Diners Club"));
    }

    @Test
    public void asCardBrand_whenAmericanExpress_returnsAmericanExpress() {
        assertEquals(Card.AMERICAN_EXPRESS, StripeTextUtils.asCardBrand("American express"));
    }

    @Test
    public void asFundingType_whenDebit_returnsDebit() {
        assertEquals(Card.FUNDING_DEBIT, StripeTextUtils.asFundingType("debit"));
    }

    @Test
    public void asFundingType_whenCredit_returnsCredit() {
        assertEquals(Card.FUNDING_CREDIT, StripeTextUtils.asFundingType("credit"));
    }

    @Test
    public void asFundingType_whenCreditAndCapitalized_returnsCredit() {
        assertEquals(Card.FUNDING_CREDIT, StripeTextUtils.asFundingType("Credit"));
    }

    @Test
    public void asFundingType_whenNull_returnsNull() {
        assertNull(StripeTextUtils.asFundingType(null));
    }

    @Test
    public void asFundingType_whenBlank_returnsNull() {
        assertNull(StripeTextUtils.asFundingType("   \t"));
    }

    @Test
    public void asFundingType_whenUnknown_returnsUnknown() {
        assertEquals(Card.FUNDING_UNKNOWN, StripeTextUtils.asFundingType("unknown"));
    }

    @Test
    public void asFundingType_whenGobbledegook_returnsUnkown() {
        assertEquals(Card.FUNDING_UNKNOWN, StripeTextUtils.asFundingType("personal iou"));
    }

    @Test
    public void separateCardNumberGroups_withVisa_returnsCorrectCardGroups() {
        String testCardNumber = "4000056655665556";
        String[] groups = StripeTextUtils.separateCardNumberGroups(testCardNumber, Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5566", groups[2]);
        assertEquals("5556", groups[3]);
    }

    @Test
    public void separateCardNumberGroups_withAmex_returnsCorrectCardGroups() {
        String testCardNumber = "378282246310005";
        String[] groups =
                StripeTextUtils.separateCardNumberGroups(testCardNumber, Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("822463", groups[1]);
        assertEquals("10005", groups[2]);
    }

    @Test
    public void separateCardNumberGroups_withDinersClub_returnsCorrectCardGroups() {
        String testCardNumber = "38520000023237";
        String[] groups =
                StripeTextUtils.separateCardNumberGroups(testCardNumber, Card.DINERS_CLUB);
        assertEquals(4, groups.length);
        assertEquals("3852", groups[0]);
        assertEquals("0000", groups[1]);
        assertEquals("0232", groups[2]);
        assertEquals("37", groups[3]);
    }

    @Test
    public void separateCardNumberGroups_withAmexPrefix_returnsPrefixGroups() {
        String testCardNumber = "378282246310005";
        String[] groups = StripeTextUtils.separateCardNumberGroups(
                        testCardNumber.substring(0, 2), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("37", groups[0]);
        assertNull(groups[1]);
        assertNull(groups[2]);

        groups = StripeTextUtils.separateCardNumberGroups(
                        testCardNumber.substring(0, 5), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("8", groups[1]);
        assertNull(groups[2]);

        groups = StripeTextUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 11), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("822463", groups[1]);
        assertEquals("1", groups[2]);
    }

    @Test
    public void separateCardNumberGroups_withVisaPrefix_returnsCorrectGroups() {
        String testCardNumber = "4000056655665556";
        String[] groups = StripeTextUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 2), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("40", groups[0]);
        assertNull(groups[1]);
        assertNull(groups[2]);
        assertNull(groups[3]);

        groups = StripeTextUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 5), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0", groups[1]);
        assertNull(groups[2]);
        assertNull(groups[3]);

        groups = StripeTextUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 9), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5", groups[2]);
        assertNull(groups[3]);

        groups = StripeTextUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 15), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5566", groups[2]);
        assertEquals("555", groups[3]);
    }
}
