package com.stripe.android

import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.init.ui.UiCustomization
import com.stripe.android.view.BaseViewTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentAuthConfigTest : BaseViewTest<Fake3ds2ChallengeActivity>(
    Fake3ds2ChallengeActivity::class.java
) {

    @BeforeTest
    fun setup() {
        PaymentAuthConfig.reset()
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun get_whenNotInited_returnsDefault() {
        val paymentAuthConfig = PaymentAuthConfig.get()
        assertEquals(PaymentAuthConfig.Stripe3ds2Config.DEFAULT_TIMEOUT.toLong(),
            paymentAuthConfig.stripe3ds2Config.timeout.toLong())
        assertNotNull(paymentAuthConfig.stripe3ds2Config.uiCustomization)
    }

    @Test
    fun get_whenInit_returnsInstance() {
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(20)
                .build())
            .build())
        assertEquals(20, PaymentAuthConfig.get().stripe3ds2Config.timeout.toLong())
    }

    @Test
    fun testCheckValidTimeout() {
        assertFailsWith<IllegalArgumentException> {
            PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(1)
                .build()
        }

        assertFailsWith<IllegalArgumentException> {
            PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(100)
                .build()
        }
    }

    @Test
    fun testStripe3ds2ConfigBuilder() {
        val uiCustomization = PaymentAuthConfig.Stripe3ds2UiCustomization.Builder().build()
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(20)
                .setUiCustomization(uiCustomization)
                .build())
            .build())
        assertEquals(uiCustomization,
            PaymentAuthConfig.get().stripe3ds2Config.uiCustomization)
    }

    @Test
    fun testButtonCustomizationWrapper() {
        val buttonCustomization = PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
            .setBackgroundColor("#000000")
            .setCornerRadius(16)
            .setTextColor("#FFFFFF")
            .setTextFontName("Font Name")
            .setTextFontSize(24)
            .build()

        val expectedButtonCustomization = StripeButtonCustomization()
        expectedButtonCustomization.setBackgroundColor("#000000")
        expectedButtonCustomization.cornerRadius = 16
        expectedButtonCustomization.setTextColor("#FFFFFF")
        expectedButtonCustomization.setTextFontName("Font Name")
        expectedButtonCustomization.textFontSize = 24

        assertEquals(expectedButtonCustomization, buttonCustomization.mButtonCustomization)
    }

    @Test
    fun testLabelCustomizationWrapper() {
        val labelCustomization = PaymentAuthConfig.Stripe3ds2LabelCustomization.Builder()
            .setHeadingTextFontName("Header Font Name")
            .setHeadingTextColor("#FFFFFF")
            .setHeadingTextFontSize(32)
            .setTextColor("#000000")
            .setTextFontName("Font Name")
            .setTextFontSize(24)
            .build()

        val expectedLabelCustomization = StripeLabelCustomization()
        expectedLabelCustomization.setHeadingTextFontName("Header Font Name")
        expectedLabelCustomization.setHeadingTextColor("#FFFFFF")
        expectedLabelCustomization.headingTextFontSize = 32
        expectedLabelCustomization.setTextColor("#000000")
        expectedLabelCustomization.setTextFontName("Font Name")
        expectedLabelCustomization.textFontSize = 24

        assertEquals(expectedLabelCustomization, labelCustomization.mLabelCustomization)
    }

    @Test
    fun testTextBoxCustomizationWrapper() {
        val textBoxCustomization = PaymentAuthConfig.Stripe3ds2TextBoxCustomization.Builder()
            .setBorderColor("#000000")
            .setBorderWidth(8)
            .setCornerRadius(16)
            .setTextColor("#FFFFFF")
            .setTextFontName("Font Name")
            .setTextFontSize(24)
            .build()

        val expectedTextBoxCustomization = StripeTextBoxCustomization()
        expectedTextBoxCustomization.setBorderColor("#000000")
        expectedTextBoxCustomization.borderWidth = 8
        expectedTextBoxCustomization.cornerRadius = 16
        expectedTextBoxCustomization.setTextColor("#FFFFFF")
        expectedTextBoxCustomization.setTextFontName("Font Name")
        expectedTextBoxCustomization.textFontSize = 24

        assertEquals(expectedTextBoxCustomization, textBoxCustomization.mTextBoxCustomization)
    }

    @Test
    fun testToolbarCustomizationWrapper() {
        val toolbarCustomization = PaymentAuthConfig.Stripe3ds2ToolbarCustomization.Builder()
            .setBackgroundColor("#000000")
            .setStatusBarColor("#000001")
            .setButtonText("Button Text")
            .setHeaderText("Header Text")
            .setTextColor("#FFFFFF")
            .setTextFontName("Font Name")
            .setTextFontSize(16)
            .build()

        val expectedCustomization = StripeToolbarCustomization()
        expectedCustomization.setBackgroundColor("#000000")
        expectedCustomization.setStatusBarColor("#000001")
        expectedCustomization.setButtonText("Button Text")
        expectedCustomization.setHeaderText("Header Text")
        expectedCustomization.setTextColor("#FFFFFF")
        expectedCustomization.setTextFontName("Font Name")
        expectedCustomization.textFontSize = 16

        assertEquals(expectedCustomization, toolbarCustomization.mToolbarCustomization)
    }

    @Test
    fun testUiCustomizationWrapper() {
        val nextStripe3ds2ButtonCustomization = PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
            .setTextColor("#000001").build()

        val cancelStripe3ds2ButtonCustomization = PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
            .setTextColor("#000011").build()

        val selectStripe3ds2ButtonCustomization = PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
            .setBackgroundColor("#000066").build()

        val stripe3ds2LabelCustomization = PaymentAuthConfig.Stripe3ds2LabelCustomization.Builder()
            .setTextColor("#000002").build()

        val stripe3ds2TextBoxCustomization = PaymentAuthConfig.Stripe3ds2TextBoxCustomization.Builder()
            .setTextColor("#000003").build()

        val stripe3ds2ToolbarCustomization = PaymentAuthConfig.Stripe3ds2ToolbarCustomization.Builder()
            .setTextColor("#000004").build()

        val uiCustomization = PaymentAuthConfig.Stripe3ds2UiCustomization.Builder()
            .setButtonCustomization(nextStripe3ds2ButtonCustomization,
                PaymentAuthConfig.Stripe3ds2UiCustomization.ButtonType.NEXT)
            .setButtonCustomization(cancelStripe3ds2ButtonCustomization,
                PaymentAuthConfig.Stripe3ds2UiCustomization.ButtonType.CANCEL)
            .setButtonCustomization(selectStripe3ds2ButtonCustomization,
                PaymentAuthConfig.Stripe3ds2UiCustomization.ButtonType.SELECT)
            .setLabelCustomization(stripe3ds2LabelCustomization)
            .setTextBoxCustomization(stripe3ds2TextBoxCustomization)
            .setToolbarCustomization(stripe3ds2ToolbarCustomization)
            .setAccentColor("#FF00FF")
            .build()

        val nextButtonCustomization = StripeButtonCustomization()
        nextButtonCustomization.setTextColor("#000001")

        val cancelButtonCustomization = StripeButtonCustomization()
        cancelButtonCustomization.setTextColor("#000011")

        val selectButtonCustomization = StripeButtonCustomization()
        selectButtonCustomization.setBackgroundColor("#000066")

        val labelCustomization = StripeLabelCustomization()
        labelCustomization.setTextColor("#000002")

        val textBoxCustomization = StripeTextBoxCustomization()
        textBoxCustomization.setTextColor("#000003")

        val toolbarCustomization = StripeToolbarCustomization()
        toolbarCustomization.setTextColor("#000004")

        val expectedUiCustomization = StripeUiCustomization()
        expectedUiCustomization.setButtonCustomization(nextButtonCustomization,
            UiCustomization.ButtonType.NEXT)
        expectedUiCustomization.setButtonCustomization(cancelButtonCustomization,
            UiCustomization.ButtonType.CANCEL)
        expectedUiCustomization.setButtonCustomization(selectButtonCustomization,
            UiCustomization.ButtonType.SELECT)
        expectedUiCustomization.setLabelCustomization(labelCustomization)
        expectedUiCustomization.setTextBoxCustomization(textBoxCustomization)
        expectedUiCustomization.setToolbarCustomization(toolbarCustomization)
        expectedUiCustomization.setAccentColor("#FF00FF")

        assertEquals(expectedUiCustomization, uiCustomization.uiCustomization)
    }

    @Test
    fun createWithAppTheme_shouldCreateExpectedToolbarCustomization() {
        val uiCustomizationFromTheme = PaymentAuthConfig.Stripe3ds2UiCustomization.Builder
            .createWithAppTheme(createActivity())
            .build()
        val toolbarCustomization = StripeToolbarCustomization()
        toolbarCustomization.setBackgroundColor("#FF222222")
        toolbarCustomization.setStatusBarColor("#FF000000")
        toolbarCustomization.setTextColor("#00000621")
        toolbarCustomization.setStatusBarColor("#FF000000")
        assertEquals(toolbarCustomization,
            uiCustomizationFromTheme.uiCustomization.toolbarCustomization)
    }
}
