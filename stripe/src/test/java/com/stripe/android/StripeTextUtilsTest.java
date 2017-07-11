package com.stripe.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
    public void removeSpacesAndHyphens_withSpacesInInterior_returnsSpacelessNumber() {
        String testCardNumber = "4242 4242 4242 4242";
        assertEquals("4242424242424242", StripeTextUtils.removeSpacesAndHyphens(testCardNumber));
    }

    @Test
    public void removeSpacesAndHyphens_withExcessiveSpacesInInterior_returnsSpacelessNumber() {
        String testCardNumber = "4  242                  4 242 4  242 42 4   2";
        assertEquals("4242424242424242", StripeTextUtils.removeSpacesAndHyphens(testCardNumber));
    }

    @Test
    public void removeSpacesAndHyphens_withSpacesOnExterior_returnsSpacelessNumber() {
        String testCardNumber = "      42424242 4242 4242    ";
        assertEquals("4242424242424242", StripeTextUtils.removeSpacesAndHyphens(testCardNumber));
    }

    @Test
    public void removeSpacesAndHyphens_whenEmpty_returnsNull () {
        assertNull(StripeTextUtils.removeSpacesAndHyphens("        "));
    }

    @Test
    public void removeSpacesAndHyphens_whenNull_returnsNull() {
        assertNull(StripeTextUtils.removeSpacesAndHyphens(null));
    }

    @Test
    public void removeSpacesAndHyphens_withHyphenatedCardNumber_returnsCardNumber() {
        assertEquals("4242424242424242",
                StripeTextUtils.removeSpacesAndHyphens("4242-4242-4242-4242"));
    }

    @Test
    public void removeSpacesAndHyphens_removesMultipleSpacesAndHyphens() {
        assertEquals("123",
                StripeTextUtils.removeSpacesAndHyphens(" -    1-  --- 2   3- - - -------- "));
    }

    @Test
    public void shaHashInput_withNullInput_returnsNull() {
        assertNull(StripeTextUtils.shaHashInput("  "));
    }

    @Test
    public void shaHashInput_withText_returnsDifferentText() {
        String unhashedText = "iamtheverymodelofamodernmajorgeneral";
        String hashedText = StripeTextUtils.shaHashInput(unhashedText);
        assertNotEquals(unhashedText, hashedText);
    }
}
