package com.stripe.android;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthConfigTest {

    @Before
    public void setup() {
        PaymentAuthConfig.reset();
    }

    @Test
    public void get_whenNotInited_returnsDefault() {
        final PaymentAuthConfig paymentAuthConfig = PaymentAuthConfig.get();
        assertEquals(PaymentAuthConfig.Stripe3ds2Config.DEFAULT_TIMEOUT,
                paymentAuthConfig.stripe3ds2Config.timeout);
        assertNotNull(paymentAuthConfig.stripe3ds2Config.uiCustomization);
    }

    @Test
    public void get_whenInit_returnsInstance() {
        PaymentAuthConfig.init(new PaymentAuthConfig.Builder()
                .set3ds2Config(new PaymentAuthConfig.Stripe3ds2Config.Builder()
                        .setTimeout(20)
                        .build())
                .build());
        assertEquals(20, PaymentAuthConfig.get().stripe3ds2Config.timeout);
    }

    @Test
    public void testStripe3ds2ConfigBuilder() {
        final PaymentAuthConfig.ThreeDS2UiCustomization uiCustomization =
                new PaymentAuthConfig.ThreeDS2UiCustomization();
        PaymentAuthConfig.init(new PaymentAuthConfig.Builder()
                .set3ds2Config(new PaymentAuthConfig.Stripe3ds2Config.Builder()
                        .setTimeout(20)
                        .setUiCustomization(uiCustomization)
                        .build())
                .build());
        assertEquals(uiCustomization,
                PaymentAuthConfig.get().stripe3ds2Config.uiCustomization);
    }

    @Test
    public void testButtonCustomizationWrapper() {
        final PaymentAuthConfig.ThreeDS2ButtonCustomization buttonCustomization =
                new PaymentAuthConfig.ThreeDS2ButtonCustomization();
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

    @Test
    public void testLabelCustomizationWrapper() {
        final PaymentAuthConfig.ThreeDS2LabelCustomization labelCustomization =
                new PaymentAuthConfig.ThreeDS2LabelCustomization();
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

    @Test
    public void testTextBoxCustomizationWrapper() {
        final PaymentAuthConfig.ThreeDS2TextBoxCustomization textBoxCustomization =
                new PaymentAuthConfig.ThreeDS2TextBoxCustomization();
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

    @Test
    public void testToolbarCustomizationWrapper() {
        final PaymentAuthConfig.ThreeDS2ToolbarCustomization toolbarCustomization =
                new PaymentAuthConfig.ThreeDS2ToolbarCustomization();
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

    @Test
    public void testUiCustomizationWrapper() {
        final PaymentAuthConfig.ThreeDS2ButtonCustomization nextThreeDS2ButtonCustomization =
                new PaymentAuthConfig.ThreeDS2ButtonCustomization();
        nextThreeDS2ButtonCustomization.setTextColor("#000001");

        final PaymentAuthConfig.ThreeDS2ButtonCustomization cancelThreeDS2ButtonCustomization =
                new PaymentAuthConfig.ThreeDS2ButtonCustomization();
        cancelThreeDS2ButtonCustomization.setTextColor("#000011");

        final PaymentAuthConfig.ThreeDS2LabelCustomization threeDS2LabelCustomization =
                new PaymentAuthConfig.ThreeDS2LabelCustomization();
        threeDS2LabelCustomization.setTextColor("#000002");

        final PaymentAuthConfig.ThreeDS2TextBoxCustomization threeDS2TextBoxCustomization =
                new PaymentAuthConfig.ThreeDS2TextBoxCustomization();
        threeDS2TextBoxCustomization.setTextColor("#000003");

        final PaymentAuthConfig.ThreeDS2ToolbarCustomization threeDS2ToolbarCustomization =
                new PaymentAuthConfig.ThreeDS2ToolbarCustomization();
        threeDS2ToolbarCustomization.setTextColor("#000004");

        final PaymentAuthConfig.ThreeDS2UiCustomization uiCustomization =
                new PaymentAuthConfig.ThreeDS2UiCustomization();
        uiCustomization.setButtonCustomization(nextThreeDS2ButtonCustomization,
                PaymentAuthConfig.ThreeDS2UiCustomization.ButtonType.NEXT);
        uiCustomization.setButtonCustomization(cancelThreeDS2ButtonCustomization,
                PaymentAuthConfig.ThreeDS2UiCustomization.ButtonType.CANCEL);
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
