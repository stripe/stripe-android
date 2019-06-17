package com.stripe.android.view.threeds2;

import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDS2ToolbarCustomizationTest {

    @Test
    public void testToolbarCustomizationWrapper() {
        final ThreeDS2ToolbarCustomization toolbarCustomization =
                new ThreeDS2ToolbarCustomization();
        toolbarCustomization.setBackgroundColor("#000000");
        toolbarCustomization.setButtonText("Button Text");
        toolbarCustomization.setHeaderText("Header Text");
        toolbarCustomization.setTextColor("#FFFFFF");
        toolbarCustomization.setTextFontName("Font Name");
        toolbarCustomization.setTextFontSize(16);

        final ToolbarCustomization expectedCustomization = new StripeToolbarCustomization();
        expectedCustomization.setBackgroundColor("#000000");
        expectedCustomization.setButtonText("Button Text");
        expectedCustomization.setHeaderText("Header Text");
        expectedCustomization.setTextColor("#FFFFFF");
        expectedCustomization.setTextFontName("Font Name");
        expectedCustomization.setTextFontSize(16);

        assertEquals(expectedCustomization, toolbarCustomization.mToolbarCustomization);
    }
}
