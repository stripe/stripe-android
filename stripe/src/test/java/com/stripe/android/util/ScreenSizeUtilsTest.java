package com.stripe.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.stripe.android.R;
import com.stripe.android.testharness.ViewTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;


/**
 * Test class for {@link ScreenSizeUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22)
public class ScreenSizeUtilsTest {

    @Mock Context mContext;
    @Mock Resources mResources;

    DisplayMetrics mDisplayMetrics;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources()).thenReturn(mResources);

        mDisplayMetrics = new DisplayMetrics();
        when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);
    }

    @Test
    public void getScreenWidthExtraPixels_whenMdpi_returnsSmallValue() {
        mDisplayMetrics.widthPixels = 380;
        setMockResourcesForDensity(ViewTestUtils.MDPI);
        int extraWidthPixels =
                ScreenSizeUtils.getScreenWidthExtraPixels(mContext, R.dimen.card_widget_min_width);
        assertEquals(extraWidthPixels, 60);
    }

    @Test
    public void getScreenWidthExtraPixels_whenXXHdpi_returnsExpectedValue() {
        mDisplayMetrics.widthPixels = 1080;
        setMockResourcesForDensity(ViewTestUtils.XXHDPI);
        int extraWidthPixels =
                ScreenSizeUtils.getScreenWidthExtraPixels(mContext, R.dimen.card_widget_min_width);
        assertEquals(extraWidthPixels, 120);
    }

    private void setMockResourcesForDensity(@ViewTestUtils.ScreenDensity int density) {
        final int baseValue = 320;
        int resourceReturnValue;
        switch (density) {
            case ViewTestUtils.MDPI:
                resourceReturnValue = (int) (baseValue * 1.0f);
                break;
            case ViewTestUtils.HDPI:
                resourceReturnValue = (int) (baseValue * 1.5f);
                break;
            case ViewTestUtils.XHDPI:
                resourceReturnValue = (int) (baseValue * 2.0f);
                break;
            case ViewTestUtils.XXHDPI:
                resourceReturnValue = (int) (baseValue * 3.0f);
                break;
            case ViewTestUtils.XXXHDPI:
                resourceReturnValue = (int) (baseValue * 4.0f);
                break;
            default:
                resourceReturnValue = (int) (baseValue * 2.0f);
                break;
        }
        when(mResources.getDimensionPixelSize(anyInt())).thenReturn(resourceReturnValue);
    }
}
