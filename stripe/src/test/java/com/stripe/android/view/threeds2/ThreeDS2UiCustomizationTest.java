package com.stripe.android.view.threeds2;

import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization;
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.UiCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDS2UiCustomizationTest {

    @Test
    public void testUiCustomizationWrapper() {
        final ThreeDS2ButtonCustomization nextThreeDS2ButtonCustomization =
                new ThreeDS2ButtonCustomization();
        nextThreeDS2ButtonCustomization.setTextColor("#000001");

        final ThreeDS2ButtonCustomization cancelThreeDS2ButtonCustomization =
                new ThreeDS2ButtonCustomization();
        cancelThreeDS2ButtonCustomization.setTextColor("#000011");

        final ThreeDS2LabelCustomization threeDS2LabelCustomization =
                new ThreeDS2LabelCustomization();
        threeDS2LabelCustomization.setTextColor("#000002");

        final ThreeDS2TextBoxCustomization threeDS2TextBoxCustomization =
                new ThreeDS2TextBoxCustomization();
        threeDS2TextBoxCustomization.setTextColor("#000003");

        final ThreeDS2ToolbarCustomization threeDS2ToolbarCustomization =
                new ThreeDS2ToolbarCustomization();
        threeDS2ToolbarCustomization.setTextColor("#000004");

        final ThreeDS2UiCustomization uiCustomization = new ThreeDS2UiCustomization();
        uiCustomization.setButtonCustomization(nextThreeDS2ButtonCustomization,
                ThreeDS2UiCustomization.ButtonType.NEXT);
        uiCustomization.setButtonCustomization(cancelThreeDS2ButtonCustomization,
                ThreeDS2UiCustomization.ButtonType.CANCEL);
        uiCustomization.setLabelCustomization(threeDS2LabelCustomization);
        uiCustomization.setTextBoxCustomization(threeDS2TextBoxCustomization);
        uiCustomization.setToolbarCustomization(threeDS2ToolbarCustomization);

        final ButtonCustomization nextButtonCustomization = new StripeButtonCustomization();
        nextButtonCustomization.setTextColor("#000001");

        final ButtonCustomization cancelButtonCustomization = new StripeButtonCustomization();
        cancelButtonCustomization.setTextColor("#000011");

        final LabelCustomization labelCustomization = new StripeLabelCustomization();
        labelCustomization.setTextColor("#000002");

        final TextBoxCustomization textBoxCustomization = new StripeTextBoxCustomization();
        textBoxCustomization.setTextColor("#000003");

        final ToolbarCustomization toolbarCustomization = new StripeToolbarCustomization();
        toolbarCustomization.setTextColor("#000004");

        final UiCustomization expectedUiCustomization = new StripeUiCustomization();
        expectedUiCustomization.setButtonCustomization(nextButtonCustomization,
                UiCustomization.ButtonType.NEXT);
        expectedUiCustomization.setButtonCustomization(cancelButtonCustomization,
                UiCustomization.ButtonType.CANCEL);
        expectedUiCustomization.setLabelCustomization(labelCustomization);
        expectedUiCustomization.setTextBoxCustomization(textBoxCustomization);
        expectedUiCustomization.setToolbarCustomization(toolbarCustomization);

        assertEquals(expectedUiCustomization, uiCustomization.getUiCustomization());
    }
}
