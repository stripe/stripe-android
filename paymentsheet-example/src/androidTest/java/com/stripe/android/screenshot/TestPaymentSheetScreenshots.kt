package com.stripe.android.screenshot

import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.PrimaryButtonLabelSettingsDefinition
import com.stripe.android.test.core.TestParameters
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPaymentSheetScreenshots : BasePlaygroundTest(disableAnimations = false) {

    private val testParams = TestParameters.create(
        paymentMethod = LpmRepository.HardcodedCard,
    ).copy(
        saveForFutureUseCheckboxVisible = true,
        authorizationAction = null,
        snapshotReturningCustomer = true,
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

    @Before
    @After
    fun resetAppearanceStore() {
        AppearanceStore.reset()
        forceLightMode()
    }

    @Test
    fun testPaymentSheetNewCustomer() {
        testDriver.screenshotRegression(
            testParams.copy()
        )
    }

    @Test
    fun testPaymentSheetNewCustomerAppearance() {
        AppearanceStore.state = appearance
        testDriver.screenshotRegression(
            testParams
        )
    }

    @Test
    fun testPaymentSheetNewCustomerDark() {
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams
        )
    }

    @Test
    fun testPaymentSheetNewCustomerDarkAppearance() {
        AppearanceStore.state = appearance
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams
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
            testParams
        )
    }

    @Test
    fun testPaymentSheetPrimaryButtonAppearanceDark() {
        AppearanceStore.state = PaymentSheet.Appearance(
            primaryButton = primaryButton
        )
        forceDarkMode()
        testDriver.screenshotRegression(
            testParams
        )
    }

    @Test
    fun testPaymentSheetCustomPrimaryButtonLabel() {
        testDriver.screenshotRegression(
            testParams.copyPlaygroundSettings { settings ->
                settings[PrimaryButtonLabelSettingsDefinition] =
                    "Buy this now!"
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
