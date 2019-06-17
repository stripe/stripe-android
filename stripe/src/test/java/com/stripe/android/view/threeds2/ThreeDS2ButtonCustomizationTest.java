package com.stripe.android.view.threeds2;

import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDS2ButtonCustomizationTest {

    @Test
    public void testButtonCustomizationWrapper() {
        final ThreeDS2ButtonCustomization buttonCustomization = new ThreeDS2ButtonCustomization();
        buttonCustomization.setBackgroundColor("#000000");
        buttonCustomization.setCornerRadius(16);
        buttonCustomization.setTextColor("#FFFFFF");
        buttonCustomization.setTextFontName("Font Name");
        buttonCustomization.setTextFontSize(24);

        final ButtonCustomization expectedButtonCustomization = new StripeButtonCustomization();
        expectedButtonCustomization.setBackgroundColor("#000000");
        expectedButtonCustomization.setCornerRadius(16);
        expectedButtonCustomization.setTextColor("#FFFFFF");
        expectedButtonCustomization.setTextFontName("Font Name");
        expectedButtonCustomization.setTextFontSize(24);

        assertEquals(expectedButtonCustomization, buttonCustomization.mButtonCustomization);
    }
}
