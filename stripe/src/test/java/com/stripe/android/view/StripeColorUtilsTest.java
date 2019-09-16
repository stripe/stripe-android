package com.stripe.android.view;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class StripeColorUtilsTest extends BaseViewTest<CardInputTestActivity> {

    public StripeColorUtilsTest() {
        super(CardInputTestActivity.class);
    }

    @Before
    public void setup() {
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void getThemeAccentColor_whenOnPostLollipopConfig_getsNonzeroColor() {
        @ColorInt int color = create().getThemeAccentColor().data;
        assertTrue(Color.alpha(color) > 0);
    }

    @Test
    public void getThemeColorControlNormal_whenOnPostLollipopConfig_getsNonzeroColor() {
        @ColorInt int color = create().getThemeColorControlNormal().data;
        assertTrue(Color.alpha(color) > 0);
    }

    @Test
    public void isColorTransparent_whenColorIsZero_returnsTrue() {
        assertTrue(StripeColorUtils.isColorTransparent(0));
    }

    @Test
    public void isColorTransparent_whenColorIsNonzeroButHasLowAlpha_returnsTrue() {
        @ColorInt int invisibleBlue = 0x050000ff;
        @ColorInt int invisibleRed = 0x0bff0000;

        final StripeColorUtils colorUtils = create();
        assertTrue(StripeColorUtils.isColorTransparent(invisibleBlue));
        assertTrue(StripeColorUtils.isColorTransparent(invisibleRed));
    }

    @Test
    public void isColorTransparent_whenColorIsNotCloseToTransparent_returnsFalse() {
        @ColorInt int brightWhite = 0xffffffff;
        @ColorInt int completelyBlack = 0xff000000;

        final StripeColorUtils colorUtils = create();
        assertFalse(StripeColorUtils.isColorTransparent(brightWhite));
        assertFalse(StripeColorUtils.isColorTransparent(completelyBlack));
    }

    @Test
    public void isColorDark_forExampleLightColors_returnsFalse() {
        @ColorInt int middleGray = 0x888888;
        @ColorInt int offWhite = 0xfaebd7;
        @ColorInt int lightCyan = 0x8feffb;
        @ColorInt int lightYellow = 0xfcf4b2;
        @ColorInt int lightBlue = 0x9cdbff;

        final StripeColorUtils colorUtils = create();
        assertFalse(StripeColorUtils.isColorDark(middleGray));
        assertFalse(StripeColorUtils.isColorDark(offWhite));
        assertFalse(StripeColorUtils.isColorDark(lightCyan));
        assertFalse(StripeColorUtils.isColorDark(lightYellow));
        assertFalse(StripeColorUtils.isColorDark(lightBlue));
        assertFalse(StripeColorUtils.isColorDark(Color.WHITE));
    }

    @Test
    public void isColorDark_forExampleDarkColors_returnsTrue() {
        @ColorInt int logoBlue = 0x6772e5;
        @ColorInt int slate = 0x525f7f;
        @ColorInt int darkPurple = 0x6b3791;
        @ColorInt int darkishRed = 0x9e2146;

        final StripeColorUtils colorUtils = create();
        assertTrue(StripeColorUtils.isColorDark(logoBlue));
        assertTrue(StripeColorUtils.isColorDark(slate));
        assertTrue(StripeColorUtils.isColorDark(darkPurple));
        assertTrue(StripeColorUtils.isColorDark(darkishRed));
        assertTrue(StripeColorUtils.isColorDark(Color.BLACK));
    }

    @NonNull
    private StripeColorUtils create() {
        return new StripeColorUtils(createActivity());
    }
}
