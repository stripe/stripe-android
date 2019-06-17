package com.stripe.android.view.threeds2;

import com.stripe.android.stripe3ds2.init.ui.LabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDS2LabelCustomizationTest {

    @Test
    public void testLabelCustomizationWrapper() {
        final ThreeDS2LabelCustomization labelCustomization = new ThreeDS2LabelCustomization();
        labelCustomization.setHeadingTextFontName("Header Font Name");
        labelCustomization.setHeadingTextColor("#FFFFFF");
        labelCustomization.setHeadingTextFontSize(32);
        labelCustomization.setTextColor("#000000");
        labelCustomization.setTextFontName("Font Name");
        labelCustomization.setTextFontSize(24);

        final LabelCustomization expectedLabelCustomization = new StripeLabelCustomization();
        expectedLabelCustomization.setHeadingTextFontName("Header Font Name");
        expectedLabelCustomization.setHeadingTextColor("#FFFFFF");
        expectedLabelCustomization.setHeadingTextFontSize(32);
        expectedLabelCustomization.setTextColor("#000000");
        expectedLabelCustomization.setTextFontName("Font Name");
        expectedLabelCustomization.setTextFontSize(24);

        assertEquals(expectedLabelCustomization, labelCustomization.mLabelCustomization);
    }
}
