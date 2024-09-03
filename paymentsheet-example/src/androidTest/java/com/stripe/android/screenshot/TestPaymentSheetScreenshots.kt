package com.stripe.android.screenshot

import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.BuildConfig
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectPhoneSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSaveSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodOrderSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PrimaryButtonLabelSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPaymentSheetScreenshots : BasePlaygroundTest(disableAnimations = false) {

    private val testParams = TestParameters.create(
        paymentMethodCode = "card",
        saveForFutureUseCheckboxVisible = true,
        authorizationAction = null,
    ) { settings ->
        settings[PaymentMethodOrderSettingsDefinition] = "card,klarna,p24,eps"
        settings[AutomaticPaymentMethodsSettingsDefinition] = false
        settings[SupportedPaymentMethodsSettingsDefinition] = "card,klarna,p24,eps"
    }

    private val colors = PaymentSheet.Colors(
        primary = Color.MAGENTA,
        surface = Color.CYAN,
        component = Color.YELLOW,
        componentBorder = Color.RED,
        componentDivider = Color.BLACK,
        onComponent = Color.BLUE,
        onSurface = Color.GRAY,
        subtitle = Color.WHITE,
        placeholderText = Color.DKGRAY,
        appBarIcon = Color.GREEN,
        error = Color.GREEN
    )

    private val appearance = PaymentSheet.Appearance(
        colorsLight = colors,
        colorsDark = colors,
        shapes = PaymentSheet.Shapes(
            cornerRadiusDp = 0.0f,
            borderStrokeWidthDp = 4.0f
        ),
        typography = PaymentSheet.Typography.default.copy(
            sizeScaleFactor = 1.1f,
            fontResId = R.font.cursive
        )
    )
    private val primaryButtonColors = PaymentSheet.PrimaryButtonColors(
        background = Color.MAGENTA,
        onBackground = Color.BLUE,
        border = Color.YELLOW,
    )
    private val primaryButton = PaymentSheet.PrimaryButton(
        colorsLight = primaryButtonColors,
        colorsDark = primaryButtonColors,
        shape = PaymentSheet.PrimaryButtonShape(
            cornerRadiusDp = 20.0f,
            borderStrokeWidthDp = 3.0f
        ),
        typography = PaymentSheet.PrimaryButtonTypography(
            fontResId = R.font.cursive,
            fontSizeSp = 12.0f
        )
    )

    @Before
    fun skipTestsIfNeeded() {
        assumeFalse(BuildConfig.IS_BROWSERSTACK_BUILD)
    }

    @Before
    @After
    fun resetAppearanceStore() {
        AppearanceStore.reset()
        forceLightMode()
    }

    @Test
    fun testPaymentSheetNewCustomer() {
        testDriver.screenshotRegression(
            testParams,
            numExpectedPaymentMethodIcons = 4,
        )
    }

    @Test
    fun testPaymentSheetNewCustomerAppearance() {
        AppearanceStore.state = appearance
        testDriver.screenshotRegression(
            testParams,
            numExpectedPaymentMethodIcons = 4,
        )
    }

    @Test
    fun testPaymentSheetNewCustomerDark() {
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams,
            numExpectedPaymentMethodIcons = 4,
        )
    }

    @Test
    fun testPaymentSheetNewCustomerDarkAppearance() {
        AppearanceStore.state = appearance
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams,
            numExpectedPaymentMethodIcons = 4,
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerLight() {
        testDriver.screenshotRegression(
            testParams.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.RETURNING
            }
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerLightAppearance() {
        AppearanceStore.state = appearance
        testDriver.screenshotRegression(
            testParams.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.RETURNING
            }
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerDark() {
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.RETURNING
            }
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerDarkAppearance() {
        AppearanceStore.state = appearance
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.RETURNING
            }
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsLight() {
        testDriver.screenshotRegression(
            testParameters = testParams
                .copyPlaygroundSettings { settings ->
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                },
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsLightAppearance() {
        AppearanceStore.state = appearance
        testDriver.screenshotRegression(
            testParameters = testParams.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.RETURNING
            },
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsDark() {
        forceDarkMode()
        testDriver.screenshotRegression(
            testParameters = testParams
                .copyPlaygroundSettings { settings ->
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                },
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsDarkAppearance() {
        forceDarkMode()
        testDriver.screenshotRegression(
            testParameters = testParams.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.RETURNING
            },
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetPrimaryButtonAppearanceLight() {
        AppearanceStore.state = PaymentSheet.Appearance(
            primaryButton = primaryButton
        )
        testDriver.screenshotRegression(
            testParams,
            numExpectedPaymentMethodIcons = 4,
        )
    }

    @Test
    fun testPaymentSheetPrimaryButtonAppearanceDark() {
        AppearanceStore.state = PaymentSheet.Appearance(
            primaryButton = primaryButton
        )
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams,
            numExpectedPaymentMethodIcons = 4,
        )
    }

    @Test
    fun testPaymentSheetCustomPrimaryButtonLabel() {
        testDriver.screenshotRegression(
            testParams.copyPlaygroundSettings { settings ->
                settings[PrimaryButtonLabelSettingsDefinition] = "Buy this now!"
            },
            numExpectedPaymentMethodIcons = 4,
        )
    }

    @Test
    fun testDefaultBillingAndPhoneDetails() {
        testDriver.screenshotRegression(
            testParameters = testParams.copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.US
                settings[CollectPhoneSettingsDefinition] = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[SupportedPaymentMethodsSettingsDefinition] = "card,amazon_pay,klarna"
                settings[PaymentMethodOrderSettingsDefinition] = "card,amazon_pay,klarna"
            },
            numExpectedPaymentMethodIcons = 3,
            customOperations = {
                testDriver.pressSelection()
                testDriver.scrollToBottom()
            }
        )
    }

    @Test
    fun testPaymentSheetAlongsideSfuLinkSignUp() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(
                resetCustomer = true,
            ).copyPlaygroundSettings { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSessionSaveSettingsDefinition] = true

                settings[SupportedPaymentMethodsSettingsDefinition] = "card,amazon_pay,klarna"
                settings[PaymentMethodOrderSettingsDefinition] = "card,amazon_pay,klarna"
            },
            numExpectedPaymentMethodIcons = 3,
            customOperations = {
                testDriver.pressSelection()
                testDriver.scrollToBottom()
            }
        )
    }

    @Test
    fun testPaymentSheetInsteadOfSfuLinkSignUp() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(
                resetCustomer = true,
            ).copyPlaygroundSettings { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSessionSaveSettingsDefinition] = false

                settings[SupportedPaymentMethodsSettingsDefinition] = "card,amazon_pay,klarna"
                settings[PaymentMethodOrderSettingsDefinition] = "card,amazon_pay,klarna"
            },
            numExpectedPaymentMethodIcons = 3,
            customOperations = {
                testDriver.pressSelection()
                testDriver.scrollToBottom()
            }
        )
    }

    private fun forceDarkMode() {
        AppearanceStore.forceDarkMode = true
        rules.compose.runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun forceLightMode() {
        AppearanceStore.forceDarkMode = false
        rules.compose.runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
