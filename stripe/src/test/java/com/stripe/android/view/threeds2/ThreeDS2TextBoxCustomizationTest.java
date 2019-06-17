package com.stripe.android.view.threeds2;

import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDS2TextBoxCustomizationTest {

    @Test
    public void testTextBoxCustomizationWrapper() {
        final ThreeDS2TextBoxCustomization textBoxCustomization =
                new ThreeDS2TextBoxCustomization();
        textBoxCustomization.setBorderColor("#000000");
        textBoxCustomization.setBorderWidth(8);
        textBoxCustomization.setCornerRadius(16);
        textBoxCustomization.setTextColor("#FFFFFF");
        textBoxCustomization.setTextFontName("Font Name");
        textBoxCustomization.setTextFontSize(24);

        final TextBoxCustomization expectedTextBoxCustomization = new StripeTextBoxCustomization();
        expectedTextBoxCustomization.setBorderColor("#000000");
        expectedTextBoxCustomization.setBorderWidth(8);
        expectedTextBoxCustomization.setCornerRadius(16);
        expectedTextBoxCustomization.setTextColor("#FFFFFF");
        expectedTextBoxCustomization.setTextFontName("Font Name");
        expectedTextBoxCustomization.setTextFontSize(24);

        assertEquals(expectedTextBoxCustomization, textBoxCustomization.mTextBoxCustomization);
    }
}
