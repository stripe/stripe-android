package com.stripe.android.view;

import android.graphics.Color;
import android.support.annotation.ColorInt;

import com.stripe.android.BuildConfig;
import com.stripe.android.model.Card;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ViewUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25, constants = BuildConfig.class)
public class ViewUtilsTest {

    ActivityController<CardInputTestActivity> mActivityController;

    @Before
    public void setup() {
        mActivityController = Robolectric.buildActivity(CardInputTestActivity.class)
                .create().start();
    }

    @Test
    public void getThemeAccentColor_whenOnPostLollipopConfig_getsNonzeroColor() {
        @ColorInt int color = ViewUtils.getThemeAccentColor(mActivityController.get()).data;
        assertTrue(Color.alpha(color) > 0);
    }

    @Test
    @Config(sdk = 16)
    public void getThemeAccentColor_whenOnPreKitKatConfig_getsNonzeroColor() {
        @ColorInt int color = ViewUtils.getThemeAccentColor(mActivityController.get()).data;
        assertTrue(Color.alpha(color) > 0);
    }

    @Test
    public void getThemeColorControlNormal_whenOnPostLollipopConfig_getsNonzeroColor() {
        @ColorInt int color = ViewUtils.getThemeColorControlNormal(mActivityController.get()).data;
        assertTrue(Color.alpha(color) > 0);
    }

    @Test
    @Config(sdk = 16)
    public void getThemeColorControlNormal_whenOnPreKitKatConfig_getsNonzeroColor() {
        @ColorInt int color = ViewUtils.getThemeColorControlNormal(mActivityController.get()).data;
        assertTrue(Color.alpha(color) > 0);
    }

    @Test
    public void isColorTransparent_whenColorIsZero_returnsTrue() {
        assertTrue(ViewUtils.isColorTransparent(0));
    }

    @Test
    public void isColorTransparent_whenColorIsNonzeroButHasLowAlpha_returnsTrue() {
        @ColorInt int invisibleBlue = 0x050000ff;
        @ColorInt int invisibleRed = 0x0bff0000;
        assertTrue(ViewUtils.isColorTransparent(invisibleBlue));
        assertTrue(ViewUtils.isColorTransparent(invisibleRed));
    }

    @Test
    public void isColorTransparent_whenColorIsNotCloseToTransparent_returnsFalse() {
        @ColorInt int brightWhite = 0xffffffff;
        @ColorInt int completelyBlack = 0xff000000;
        assertFalse(ViewUtils.isColorTransparent(brightWhite));
        assertFalse(ViewUtils.isColorTransparent(completelyBlack));
    }

    @Test
    public void separateCardNumberGroups_withVisa_returnsCorrectCardGroups() {
        String testCardNumber = "4000056655665556";
        String[] groups = ViewUtils.separateCardNumberGroups(testCardNumber, Card.VISA);
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
                ViewUtils.separateCardNumberGroups(testCardNumber, Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("822463", groups[1]);
        assertEquals("10005", groups[2]);
    }

    @Test
    public void separateCardNumberGroups_withDinersClub_returnsCorrectCardGroups() {
        String testCardNumber = "38520000023237";
        String[] groups =
                ViewUtils.separateCardNumberGroups(testCardNumber, Card.DINERS_CLUB);
        assertEquals(4, groups.length);
        assertEquals("3852", groups[0]);
        assertEquals("0000", groups[1]);
        assertEquals("0232", groups[2]);
        assertEquals("37", groups[3]);
    }

    @Test
    public void separateCardNumberGroups_withInvalid_returnsCorrectCardGroups() {
        String testCardNumber = "1234056655665556";
        String[] groups = ViewUtils.separateCardNumberGroups(testCardNumber, Card.UNKNOWN);
        assertEquals(4, groups.length);
        assertEquals("1234", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5566", groups[2]);
        assertEquals("5556", groups[3]);
    }

    @Test
    public void separateCardNumberGroups_withAmexPrefix_returnsPrefixGroups() {
        String testCardNumber = "378282246310005";
        String[] groups = ViewUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 2), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("37", groups[0]);
        assertNull(groups[1]);
        assertNull(groups[2]);

        groups = ViewUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 5), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("8", groups[1]);
        assertNull(groups[2]);

        groups = ViewUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 11), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("822463", groups[1]);
        assertEquals("1", groups[2]);
    }

    @Test
    public void separateCardNumberGroups_withVisaPrefix_returnsCorrectGroups() {
        String testCardNumber = "4000056655665556";
        String[] groups = ViewUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 2), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("40", groups[0]);
        assertNull(groups[1]);
        assertNull(groups[2]);
        assertNull(groups[3]);

        groups = ViewUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 5), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0", groups[1]);
        assertNull(groups[2]);
        assertNull(groups[3]);

        groups = ViewUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 9), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5", groups[2]);
        assertNull(groups[3]);

        groups = ViewUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 15), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5566", groups[2]);
        assertEquals("555", groups[3]);
    }

    @Test
    public void isCvcMaximalLength_whenThreeDigitsAndNotAmEx_returnsTrue() {
        assertTrue(ViewUtils.isCvcMaximalLength(Card.VISA, "123"));
        assertTrue(ViewUtils.isCvcMaximalLength(Card.MASTERCARD, "345"));
        assertTrue(ViewUtils.isCvcMaximalLength(Card.JCB, "678"));
        assertTrue(ViewUtils.isCvcMaximalLength(Card.DINERS_CLUB, "910"));
        assertTrue(ViewUtils.isCvcMaximalLength(Card.DISCOVER, "234"));
        assertTrue(ViewUtils.isCvcMaximalLength(Card.UNKNOWN, "333"));
    }

    @Test
    public void isCvcMaximalLength_whenThreeDigitsAndIsAmEx_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.AMERICAN_EXPRESS, "123"));
    }

    @Test
    public void isCvcMaximalLength_whenFourDigitsAndIsAmEx_returnsTrue() {
        assertTrue(ViewUtils.isCvcMaximalLength(Card.AMERICAN_EXPRESS, "1234"));
    }

    @Test
    public void isCvcMaximalLength_whenTooManyDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.AMERICAN_EXPRESS, "12345"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.VISA, "1234"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.MASTERCARD, "123456"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.DINERS_CLUB, "1234567"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.DISCOVER, "12345678"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.JCB, "123456789012345"));
    }

    @Test
    public void isCvcMaximalLength_whenNotEnoughDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.AMERICAN_EXPRESS, ""));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.VISA, "1"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.MASTERCARD, "12"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.DINERS_CLUB, ""));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.DISCOVER, "8"));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.JCB, "1"));
    }

    @Test
    public void isCvcMaximalLength_whenWhitespaceAndNotEnoughDigits_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.AMERICAN_EXPRESS, "   "));
        assertFalse(ViewUtils.isCvcMaximalLength(Card.VISA, "  1"));
    }

    @Test
    public void isCvcMaximalLength_whenNull_returnsFalse() {
        assertFalse(ViewUtils.isCvcMaximalLength(Card.AMERICAN_EXPRESS, null));
    }

    @Test
    public void isColorDark_forExampleLightColors_returnsFalse() {
        @ColorInt int middleGray = 0x888888;
        @ColorInt int offWhite = 0xfaebd7;
        @ColorInt int lightCyan = 0x8feffb;
        @ColorInt int lightYellow = 0xfcf4b2;
        @ColorInt int lightBlue = 0x9cdbff;

        assertFalse(ViewUtils.isColorDark(middleGray));
        assertFalse(ViewUtils.isColorDark(offWhite));
        assertFalse(ViewUtils.isColorDark(lightCyan));
        assertFalse(ViewUtils.isColorDark(lightYellow));
        assertFalse(ViewUtils.isColorDark(lightBlue));
        assertFalse(ViewUtils.isColorDark(Color.WHITE));
    }

    @Test
    public void isColorDark_forExampleDarkColors_returnsTrue() {
        @ColorInt int logoBlue = 0x6772e5;
        @ColorInt int slate = 0x525f7f;
        @ColorInt int darkPurple = 0x6b3791;
        @ColorInt int darkishRed = 0x9e2146;

        assertTrue(ViewUtils.isColorDark(logoBlue));
        assertTrue(ViewUtils.isColorDark(slate));
        assertTrue(ViewUtils.isColorDark(darkPurple));
        assertTrue(ViewUtils.isColorDark(darkishRed));
        assertTrue(ViewUtils.isColorDark(Color.BLACK));
    }

    @Test
    public void separateCardNumberGroups_forLongInputs_doesNotCrash() {
        String testCardNumber = "1234567890123456789";
        String[] groups = ViewUtils.separateCardNumberGroups(
                testCardNumber, Card.VISA);
        assertEquals(4, groups.length);
    }
}
