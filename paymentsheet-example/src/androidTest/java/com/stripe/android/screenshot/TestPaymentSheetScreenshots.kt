package com.stripe.android.screenshot

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.settings.PrimaryButtonLabelSettingsDefinition
import com.stripe.android.test.core.TestParameters
import com.stripe.android.ui.core.forms.resources.LpmRepository
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

    @Test
    fun testPaymentSheetNewCustomer() {
        testDriver.screenshotRegression(
            testParams.copy()
        )
    }

//    @Test
//    fun testPaymentSheetNewCustomerAppearance() {
//        testDriver.screenshotRegression(
//            testParams.copy(
//                forceDarkMode = false,
//                appearance = appearance
//            )
//        )
//    }
//
//    @Test
//    fun testPaymentSheetNewCustomerDark() {
//        testDriver.screenshotRegression(
//            testParams.copy(forceDarkMode = true)
//        )
//    }
//
//    @Test
//    fun testPaymentSheetNewCustomerDarkAppearance() {
//        testDriver.screenshotRegression(
//            testParams.copy(forceDarkMode = true, appearance = appearance)
//        )
//    }
//
//    @Test
//    fun testPaymentSheetReturningCustomerLight() {
//        testDriver.screenshotRegression(
//            testParams.copy(forceDarkMode = false).copyPlaygroundSettings { settings ->
//                settings[CustomerSettingsDefinition] =
//                    CustomerSettingsDefinition.CustomerType.RETURNING
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetReturningCustomerLightAppearance() {
//        testDriver.screenshotRegression(
//            testParams.copy(
//                forceDarkMode = false,
//                appearance = appearance
//            ).copyPlaygroundSettings { settings ->
//                settings[CustomerSettingsDefinition] =
//                    CustomerSettingsDefinition.CustomerType.RETURNING
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetReturningCustomerDark() {
//        testDriver.screenshotRegression(
//            testParams.copy(forceDarkMode = true).copyPlaygroundSettings { settings ->
//                settings[CustomerSettingsDefinition] =
//                    CustomerSettingsDefinition.CustomerType.RETURNING
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetReturningCustomerDarkAppearance() {
//        testDriver.screenshotRegression(
//            testParams.copy(
//                forceDarkMode = true,
//                appearance = appearance
//            ).copyPlaygroundSettings { settings ->
//                settings[CustomerSettingsDefinition] =
//                    CustomerSettingsDefinition.CustomerType.RETURNING
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetEditPaymentMethodsLight() {
//        testDriver.screenshotRegression(
//            testParameters = testParams.copy(forceDarkMode = false)
//                .copyPlaygroundSettings { settings ->
//                    settings[CustomerSettingsDefinition] =
//                        CustomerSettingsDefinition.CustomerType.RETURNING
//                },
//            customOperations = {
//                testDriver.pressEdit()
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetEditPaymentMethodsLightAppearance() {
//        testDriver.screenshotRegression(
//            testParameters = testParams.copy(
//                forceDarkMode = false,
//                appearance = appearance
//            ).copyPlaygroundSettings { settings ->
//                settings[CustomerSettingsDefinition] =
//                    CustomerSettingsDefinition.CustomerType.RETURNING
//            },
//            customOperations = {
//                testDriver.pressEdit()
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetEditPaymentMethodsDark() {
//        testDriver.screenshotRegression(
//            testParameters = testParams.copy(forceDarkMode = true)
//                .copyPlaygroundSettings { settings ->
//                    settings[CustomerSettingsDefinition] =
//                        CustomerSettingsDefinition.CustomerType.RETURNING
//                },
//            customOperations = {
//                testDriver.pressEdit()
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetEditPaymentMethodsDarkAppearance() {
//        testDriver.screenshotRegression(
//            testParameters = testParams.copy(
//                forceDarkMode = true,
//                appearance = appearance
//            ).copyPlaygroundSettings { settings ->
//                settings[CustomerSettingsDefinition] =
//                    CustomerSettingsDefinition.CustomerType.RETURNING
//            },
//            customOperations = {
//                testDriver.pressEdit()
//            }
//        )
//    }
//
//    @Test
//    fun testPaymentSheetPrimaryButtonAppearanceLight() {
//        testDriver.screenshotRegression(
//            testParams.copy(
//                forceDarkMode = false,
//                appearance = PaymentSheet.Appearance(
//                    primaryButton = primaryButton
//                )
//            )
//        )
//    }
//
//    @Test
//    fun testPaymentSheetPrimaryButtonAppearanceDark() {
//        testDriver.screenshotRegression(
//            testParams.copy(
//                forceDarkMode = true,
//                appearance = PaymentSheet.Appearance(
//                    primaryButton = primaryButton
//                )
//            )
//        )
//    }

    @Test
    fun testPaymentSheetCustomPrimaryButtonLabel() {
        testDriver.screenshotRegression(
            testParams.copyPlaygroundSettings { settings ->
                settings[PrimaryButtonLabelSettingsDefinition] =
                    "Buy this now!"
            }
        )
    }
}
