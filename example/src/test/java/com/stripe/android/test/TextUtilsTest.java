package com.stripe.android.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.stripe.android.util.TextUtils;

@RunWith(RobolectricTestRunner.class)
public class TextUtilsTest {
    @Test
    public void hasAnyPrefixShouldFailIfNull() {
        assertFalse(TextUtils.hasAnyPrefix(null));
    }

    @Test
    public void hasAnyPrefixShouldFailIfEmpty() {
        assertFalse(TextUtils.hasAnyPrefix(""));
    }

    @Test
    public void hasAnyPrefixShouldFailWithNullAndEmptyPrefix() {
        assertFalse(TextUtils.hasAnyPrefix(null, ""));
    }

    @Test
    public void hasAnyPrefixShouldFailWithNullAndSomePrefix() {
        assertFalse(TextUtils.hasAnyPrefix(null, "1"));
    }

    @Test
    public void hasAnyPrefixShouldMatch() {
        assertTrue(TextUtils.hasAnyPrefix("1234", "12"));
    }

    @Test
    public void hasAnyPrefixShouldMatchMultiple() {
        assertTrue(TextUtils.hasAnyPrefix("1234", "12", "1"));
    }

    @Test
    public void hasAnyPrefixShouldMatchSome() {
        assertTrue(TextUtils.hasAnyPrefix("abcd", "bc", "ab"));
    }

    @Test
    public void hasAnyPrefixShouldNotMatch() {
        assertFalse(TextUtils.hasAnyPrefix("1234", "23"));
    }

    @Test
    public void hasAnyPrefixShouldNotMatchWithSpace() {
        assertFalse(TextUtils.hasAnyPrefix("xyz", " x"));
    }

    @Test
    public void wholePositiveNumberShouldFailNull() {
        assertFalse(TextUtils.isWholePositiveNumber(null));
    }

    @Test
    public void wholePositiveNumberShouldPassIfEmpty() {
        assertTrue(TextUtils.isWholePositiveNumber(""));
    }

    @Test
    public void wholePositiveNumberShouldPass() {
        assertTrue(TextUtils.isWholePositiveNumber("123"));
    }

    @Test
    public void wholePositiveNumberShouldPassWithLeadingZero() {
        assertTrue(TextUtils.isWholePositiveNumber("000"));
    }

    @Test
    public void wholePositiveNumberShouldFailIfNegative() {
        assertFalse(TextUtils.isWholePositiveNumber("-1"));
    }

    @Test
    public void wholePositiveNumberShouldFailIfLetters() {
        assertFalse(TextUtils.isWholePositiveNumber("1a"));
    }

    @Test
    public void testNullIfBlankNullShouldBeNull() {
        assertEquals(null, TextUtils.nullIfBlank(null));
    }

    @Test
    public void testNullIfBlankEmptyShouldBeNull() {
        assertEquals(null, TextUtils.nullIfBlank(""));
    }

    @Test
    public void testNullIfBlankSpaceShouldBeNull() {
        assertEquals(null, TextUtils.nullIfBlank(" "));
    }

    @Test
    public void testNullIfBlankSpacesShouldBeNull() {
        assertEquals(null, TextUtils.nullIfBlank("     "));
    }

    @Test
    public void testNullIfBlankTabShouldBeNull() {
        assertEquals(null, TextUtils.nullIfBlank("	"));
    }

    @Test
    public void testNullIfBlankNumbersShouldNotBeNull() {
        assertEquals("0", TextUtils.nullIfBlank("0"));
    }

    @Test
    public void testNullIfBlankLettersShouldNotBeNull() {
        assertEquals("abc", TextUtils.nullIfBlank("abc"));
    }

    @Test
    public void isBlankShouldPassIfNull() {
        assertTrue(TextUtils.isBlank(null));
    }

    @Test
    public void isBlankShouldPassIfEmpty() {
        assertTrue(TextUtils.isBlank(""));
    }

    @Test
    public void isBlankShouldPassIfSpace() {
        assertTrue(TextUtils.isBlank(" "));
    }

    @Test
    public void isBlankShouldPassIfSpaces() {
        assertTrue(TextUtils.isBlank("     "));
    }

    @Test
    public void isBlankShouldPassIfTab() {
        assertTrue(TextUtils.isBlank("	"));
    }

    @Test
    public void isBlankShouldFailIfNumber() {
        assertFalse(TextUtils.isBlank("0"));
    }

    @Test
    public void isBlankShouldFailIfLetters() {
        assertFalse(TextUtils.isBlank("abc"));
    }
}