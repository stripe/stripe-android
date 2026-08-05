package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.toPaymentElementThemeValues
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.FormInsets
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.LocalIconStyle
import com.stripe.android.uicore.LocalSectionSpacing
import com.stripe.android.uicore.LocalTextFieldInsets
import com.stripe.android.uicore.PrimaryButtonStyle
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeShapes
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeTypography
import com.stripe.android.uicore.isSystemDarkTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeFormInsets
import com.stripe.android.uicore.stripePrimaryButtonStyle
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.stripeThemeIsDark
import com.stripe.android.uicore.stripeTypography
import com.stripe.android.uicore.stripeVerticalModeRowPadding
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(AppearanceAPIAdditionsPreview::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentElementThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    @Config(qualifiers = "notnight")
    fun `always dark provides dark appearance independently of system theme`() = runScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysDark,
    ) {
        assertThat(isDark).isTrue()
        assertThat(contextIsDark).isTrue()
        assertThat(colors.materialColors.primary).isEqualTo(DARK_PRIMARY)
        assertThat(shapes.cornerRadius).isEqualTo(12f)
        assertThat(typography.fontSizeMultiplier).isEqualTo(1.5f)
        assertThat(primaryButtonStyle.colorsDark.background).isEqualTo(DARK_BUTTON)
        assertThat(primaryButtonStyle.shape.cornerRadius).isEqualTo(14f)
        assertThat(formInsets).isEqualTo(FormInsets(start = 1f, top = 2f, end = 3f, bottom = 4f))
        assertThat(sectionSpacing).isEqualTo(10f)
        assertThat(textFieldInsets).isEqualTo(FormInsets(start = 5f, top = 6f, end = 7f, bottom = 8f))
        assertThat(iconStyle).isEqualTo(IconStyle.Outlined)
        assertThat(verticalModeRowPadding).isEqualTo(9f)
    }

    @Test
    @Config(qualifiers = "night")
    fun `always light provides light appearance independently of system theme`() = runScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysLight,
    ) {
        assertThat(isDark).isFalse()
        assertThat(contextIsDark).isFalse()
        assertThat(colors.materialColors.primary).isEqualTo(LIGHT_PRIMARY)
        assertThat(primaryButtonStyle.colorsLight.background).isEqualTo(LIGHT_BUTTON)
    }

    @Test
    fun `automatic follows dark system theme`() {
        assertThat(PaymentSheet.ThemeMode.Automatic.isDarkTheme(isSystemDark = true)).isTrue()
    }

    @Test
    fun `automatic follows light system theme`() {
        assertThat(PaymentSheet.ThemeMode.Automatic.isDarkTheme(isSystemDark = false)).isFalse()
    }

    @Test
    fun `parse appearance applies immutable values to legacy theme`() {
        val appearance = createAppearance(PaymentSheet.ThemeMode.Automatic)
        val expected = appearance.toPaymentElementThemeValues()

        try {
            appearance.parseAppearance()

            assertThat(StripeTheme.colorsLightMutable.materialColors.primary)
                .isEqualTo(expected.colorsLight.materialColors.primary)
            assertThat(StripeTheme.colorsDarkMutable.materialColors.primary)
                .isEqualTo(expected.colorsDark.materialColors.primary)
            assertThat(StripeTheme.shapesMutable).isEqualTo(expected.shapes)
            assertThat(StripeTheme.typographyMutable).isEqualTo(expected.typography)
            assertThat(StripeTheme.primaryButtonStyle).isEqualTo(expected.primaryButtonStyle)
            assertThat(StripeTheme.formInsets).isEqualTo(expected.formInsets)
            assertThat(StripeTheme.customSectionSpacing).isEqualTo(expected.sectionSpacing)
            assertThat(StripeTheme.textFieldInsets).isEqualTo(expected.textFieldInsets)
            assertThat(StripeTheme.iconStyle).isEqualTo(expected.iconStyle)
            assertThat(StripeTheme.verticalModeRowPadding).isEqualTo(expected.verticalModeRowPadding)
        } finally {
            PaymentSheet.Appearance().parseAppearance()
        }
    }

    private fun runScenario(
        themeMode: PaymentSheet.ThemeMode,
        block: ThemeSnapshot.() -> Unit,
    ) {
        var snapshot: ThemeSnapshot? = null

        composeRule.setContent {
            PaymentElementTheme(appearance = createAppearance(themeMode)) {
                snapshot = ThemeSnapshot(
                    isDark = MaterialTheme.stripeThemeIsDark,
                    contextIsDark = androidx.compose.ui.platform.LocalContext.current.isSystemDarkTheme(),
                    colors = MaterialTheme.stripeColors,
                    shapes = MaterialTheme.stripeShapes,
                    typography = MaterialTheme.stripeTypography,
                    primaryButtonStyle = MaterialTheme.stripePrimaryButtonStyle,
                    formInsets = MaterialTheme.stripeFormInsets,
                    sectionSpacing = LocalSectionSpacing.current,
                    textFieldInsets = LocalTextFieldInsets.current,
                    iconStyle = LocalIconStyle.current,
                    verticalModeRowPadding = MaterialTheme.stripeVerticalModeRowPadding,
                )
            }
        }
        composeRule.waitForIdle()

        requireNotNull(snapshot).apply(block)
    }

    private fun createAppearance(themeMode: PaymentSheet.ThemeMode): PaymentSheet.Appearance {
        return PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = LIGHT_PRIMARY),
            colorsDark = PaymentSheet.Colors.configureDefaultDark(primary = DARK_PRIMARY),
            themeMode = themeMode,
            shapes = PaymentSheet.Shapes(
                cornerRadiusDp = 12f,
                borderStrokeWidthDp = 2f,
                bottomSheetCornerRadiusDp = 16f,
            ),
            typography = PaymentSheet.Typography(
                sizeScaleFactor = 1.5f,
                fontResId = null,
            ),
            primaryButton = PaymentSheet.PrimaryButton(
                colorsLight = PaymentSheet.PrimaryButtonColors(
                    background = LIGHT_BUTTON.toArgb(),
                    onBackground = Color.Black.toArgb(),
                    border = Color.Blue.toArgb(),
                ),
                colorsDark = PaymentSheet.PrimaryButtonColors(
                    background = DARK_BUTTON.toArgb(),
                    onBackground = Color.White.toArgb(),
                    border = Color.Cyan.toArgb(),
                ),
                shape = PaymentSheet.PrimaryButtonShape(
                    cornerRadiusDp = 14f,
                    borderStrokeWidthDp = 3f,
                    heightDp = 50f,
                ),
                typography = PaymentSheet.PrimaryButtonTypography(
                    fontResId = null,
                    fontSizeSp = 18f,
                ),
            ),
            embeddedAppearance = PaymentSheet.Appearance.Embedded.default,
            formInsetValues = PaymentSheet.Insets(startDp = 1f, topDp = 2f, endDp = 3f, bottomDp = 4f),
            sectionSpacing = PaymentSheet.Spacing(spacingDp = 10f),
            textFieldInsets = PaymentSheet.Insets(startDp = 5f, topDp = 6f, endDp = 7f, bottomDp = 8f),
            iconStyle = PaymentSheet.IconStyle.Outlined,
            verticalModeRowPadding = 9f,
        )
    }

    private data class ThemeSnapshot(
        val isDark: Boolean,
        val contextIsDark: Boolean,
        val colors: StripeColors,
        val shapes: StripeShapes,
        val typography: StripeTypography,
        val primaryButtonStyle: PrimaryButtonStyle,
        val formInsets: FormInsets,
        val sectionSpacing: Float?,
        val textFieldInsets: FormInsets,
        val iconStyle: IconStyle,
        val verticalModeRowPadding: Float,
    )

    private companion object {
        val LIGHT_PRIMARY = Color(0xFF123456)
        val DARK_PRIMARY = Color(0xFF654321)
        val LIGHT_BUTTON = Color(0xFFABCDEF)
        val DARK_BUTTON = Color(0xFFFEDCBA)
    }
}
