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
        final PaymentAuthConfig.Stripe3ds2UiCustomization uiCustomization =
                new PaymentAuthConfig.Stripe3ds2UiCustomization.Builder().build();
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
        final PaymentAuthConfig.Stripe3ds2ButtonCustomization buttonCustomization =
                new PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
                        .setBackgroundColor("#000000")
                        .setCornerRadius(16)
                        .setTextColor("#FFFFFF")
                        .setTextFontName("Font Name")
                        .setTextFontSize(24)
                        .build();

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
        final PaymentAuthConfig.Stripe3ds2LabelCustomization labelCustomization =
                new PaymentAuthConfig.Stripe3ds2LabelCustomization.Builder()
                        .setHeadingTextFontName("Header Font Name")
                        .setHeadingTextColor("#FFFFFF")
                        .setHeadingTextFontSize(32)
                        .setTextColor("#000000")
                        .setTextFontName("Font Name")
                        .setTextFontSize(24)
                        .build();

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
        final PaymentAuthConfig.Stripe3ds2TextBoxCustomization textBoxCustomization =
                new PaymentAuthConfig.Stripe3ds2TextBoxCustomization.Builder()
                        .setBorderColor("#000000")
                        .setBorderWidth(8)
                        .setCornerRadius(16)
                        .setTextColor("#FFFFFF")
                        .setTextFontName("Font Name")
                        .setTextFontSize(24)
                        .build();

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
        final PaymentAuthConfig.Stripe3ds2ToolbarCustomization toolbarCustomization =
                new PaymentAuthConfig.Stripe3ds2ToolbarCustomization.Builder()
                        .setBackgroundColor("#000000")
                        .setButtonText("Button Text")
                        .setHeaderText("Header Text")
                        .setTextColor("#FFFFFF")
                        .setTextFontName("Font Name")
                        .setTextFontSize(16)
                        .build();

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
        final PaymentAuthConfig.Stripe3ds2ButtonCustomization nextStripe3ds2ButtonCustomization =
                new PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
                        .setTextColor("#000001").build();

        final PaymentAuthConfig.Stripe3ds2ButtonCustomization cancelStripe3ds2ButtonCustomization =
                new PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
                        .setTextColor("#000011").build();

        final PaymentAuthConfig.Stripe3ds2LabelCustomization stripe3ds2LabelCustomization =
                new PaymentAuthConfig.Stripe3ds2LabelCustomization.Builder()
                        .setTextColor("#000002").build();

        final PaymentAuthConfig.Stripe3ds2TextBoxCustomization stripe3ds2TextBoxCustomization =
                new PaymentAuthConfig.Stripe3ds2TextBoxCustomization.Builder()
                        .setTextColor("#000003").build();

        final PaymentAuthConfig.Stripe3ds2ToolbarCustomization stripe3ds2ToolbarCustomization =
                new PaymentAuthConfig.Stripe3ds2ToolbarCustomization.Builder()
                        .setTextColor("#000004").build();

        final PaymentAuthConfig.Stripe3ds2UiCustomization uiCustomization =
                new PaymentAuthConfig.Stripe3ds2UiCustomization.Builder()
                        .setButtonCustomization(nextStripe3ds2ButtonCustomization,
                                PaymentAuthConfig.Stripe3ds2UiCustomization.ButtonType.NEXT)
                        .setButtonCustomization(cancelStripe3ds2ButtonCustomization,
                                PaymentAuthConfig.Stripe3ds2UiCustomization.ButtonType.CANCEL)
                        .setLabelCustomization(stripe3ds2LabelCustomization)
                        .setTextBoxCustomization(stripe3ds2TextBoxCustomization)
                        .setToolbarCustomization(stripe3ds2ToolbarCustomization)
                        .build();

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
