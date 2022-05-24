package com.stripe.android.screenshot

import android.graphics.Color
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.INDIVIDUAL_TEST_TIMEOUT_SECONDS
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.MyScreenCaptureProcessor
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.TestWatcher
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestPaymentSheetScreenshots {
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testWatcher = TestWatcher()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver
    private val screenshotProcessor = MyScreenCaptureProcessor()

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, composeTestRule, screenshotProcessor)
    }

    private val testParams = TestParameters(
        SupportedPaymentMethod.Card,
        Customer.New,
        GooglePayState.On,
        Currency.EUR,
        IntentType.Pay,
        Billing.On,
        shipping = Shipping.Off,
        delayed = DelayedPMs.Off,
        automatic = Automatic.On,
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = true,
        useBrowser = null,
        authorizationAction = null,
        takeScreenshotOnLpmLoad = true,
        snapshotReturningCustomer = true
    )

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

    @Test
    fun testPaymentSheetNewCustomer() {
        testDriver.screenshotRegression(
            testParams.copy(forceDarkMode = false)
        )
    }

    @Test
    fun testPaymentSheetNewCustomerAppearance() {
        testDriver.screenshotRegression(
            testParams.copy(
                forceDarkMode = false,
                appearance = appearance
            )
        )
    }

    @Test
    fun testPaymentSheetNewCustomerDark() {
        testDriver.screenshotRegression(
            testParams.copy(forceDarkMode = true)
        )
    }

    @Test
    fun testPaymentSheetNewCustomerDarkAppearance() {
        testDriver.screenshotRegression(
            testParams.copy(forceDarkMode = true, appearance = appearance)
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerLight() {
        testDriver.screenshotRegression(
            testParams.copy(forceDarkMode = false, customer = Customer.Returning)
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerLightAppearance() {
        testDriver.screenshotRegression(
            testParams.copy(
                forceDarkMode = false,
                customer = Customer.Returning,
                appearance = appearance
            )
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerDark() {
        testDriver.screenshotRegression(
            testParams.copy(forceDarkMode = true, customer = Customer.Returning)
        )
    }

    @Test
    fun testPaymentSheetReturningCustomerDarkAppearance() {
        testDriver.screenshotRegression(
            testParams.copy(
                forceDarkMode = true,
                customer = Customer.Returning,
                appearance = appearance
            )
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsLight() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(forceDarkMode = false, customer = Customer.Returning),
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsLightAppearance() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(
                forceDarkMode = false,
                customer = Customer.Returning,
                appearance = appearance
            ),
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsDark() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(forceDarkMode = true, customer = Customer.Returning),
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetEditPaymentMethodsDarkAppearance() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(
                forceDarkMode = true,
                customer = Customer.Returning,
                appearance = appearance
            ),
            customOperations = {
                testDriver.pressEdit()
            }
        )
    }

    @Test
    fun testPaymentSheetPrimaryButtonAppearanceLight() {
        testDriver.screenshotRegression(
            testParams.copy(
                forceDarkMode = false,
                appearance = PaymentSheet.Appearance(
                    primaryButton = primaryButton
                )
            )
        )
    }

    @Test
    fun testPaymentSheetPrimaryButtonAppearanceDark() {
        testDriver.screenshotRegression(
            testParams.copy(
                forceDarkMode = true,
                appearance = PaymentSheet.Appearance(
                    primaryButton = primaryButton
                )
            )
        )
    }
}
