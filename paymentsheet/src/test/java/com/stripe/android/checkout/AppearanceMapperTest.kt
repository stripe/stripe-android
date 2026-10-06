package com.stripe.android.checkout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.PaymentElement.Configuration.Appearance
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

@OptIn(
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
)
class AppearanceMapperTest {
    @Test
    fun `default appearance maps to PaymentSheet defaults`() {
        val appearance = PaymentElement.Configuration.Appearance()

        assertThat(appearance.build().asPaymentSheet()).isEqualTo(PaymentSheet.Appearance())
    }

    @Test
    fun `default shipping address appearance maps to PaymentSheet defaults`() {
        val appearance = ShippingAddressElement.Configuration.Appearance()
        val mapped = appearance.build().asPaymentSheet()

        assertThat(mapped).isEqualTo(PaymentSheet.Appearance())
        assertThat(mapped.primaryButton.colorsDark.onSuccessBackgroundColor)
            .isEqualTo(Color.Black.toArgb())
    }

    @Test
    fun `configured appearance maps colors and primary button`() {
        val appearance = PaymentElement.Configuration.Appearance()
            .colorsLight(
                PaymentElement.Configuration.Appearance.Colors.light()
                    .primary(Color.Red)
            )
            .primaryButton(
                PaymentElement.Configuration.Appearance.PrimaryButton()
                    .colorsLight(
                        PaymentElement.Configuration.Appearance.PrimaryButton.Colors.light()
                            .background(Color.Green)
                    )
            )

        val mapped = appearance.build().asPaymentSheet()

        assertThat(mapped.colorsLight.primary).isEqualTo(Color.Red.toArgb())
        assertThat(mapped.primaryButton.colorsLight.background).isEqualTo(Color.Green.toArgb())
    }

    @Test
    fun `integer color setters are preserved`() {
        val appearance = PaymentElement.Configuration.Appearance()
            .colorsDark(
                PaymentElement.Configuration.Appearance.Colors.dark()
                    .primary(0xFF654321.toInt())
            )

        assertThat(appearance.build().asPaymentSheet().colorsDark.primary)
            .isEqualTo(0xFF654321.toInt())
    }

    @Test
    fun `configuration stores its supplied appearance`() {
        val appearance = PaymentElement.Configuration.Appearance()
            .colorsLight(
                PaymentElement.Configuration.Appearance.Colors.light().primary(Color.Red)
            )

        val state = PaymentElement.Configuration().appearance(appearance).build()

        assertThat(state.appearance).isEqualTo(appearance.build())
    }

    @Test
    fun `shipping address appearance maps all supported fields`() {
        val appearance = ShippingAddressElement.Configuration.Appearance()
            .colorsLight(
                ShippingAddressElement.Configuration.Appearance.Colors.light()
                    .primary(Color.Red)
            )
            .colorsDark(
                ShippingAddressElement.Configuration.Appearance.Colors.dark()
                    .primary(Color.Blue)
            )
            .themeMode(ShippingAddressElement.Configuration.Appearance.ThemeMode.AlwaysDark)
            .primaryButton(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton()
                    .colorsLight(
                        ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.light()
                            .background(Color.Green)
                            .onBackground(Color.White)
                            .border(Color.Black)
                    )
                    .shape(
                        ShippingAddressElement.Configuration.Appearance.PrimaryButton.Shape()
                            .cornerRadiusDp(12f)
                            .borderStrokeWidthDp(2f)
                            .heightDp(48f)
                    )
                    .typography(
                        ShippingAddressElement.Configuration.Appearance.PrimaryButton.Typography()
                            .fontSizeSp(18f)
                    )
            )
            .formInsetValues(
                ShippingAddressElement.Configuration.Appearance.Insets(1f, 2f, 3f, 4f)
            )

        val mapped = appearance.build().asPaymentSheet()

        assertThat(mapped.colorsLight.primary).isEqualTo(Color.Red.toArgb())
        assertThat(mapped.colorsDark.primary).isEqualTo(Color.Blue.toArgb())
        assertThat(mapped.themeMode).isEqualTo(PaymentSheet.ThemeMode.AlwaysDark)
        assertThat(mapped.primaryButton.colorsLight.background).isEqualTo(Color.Green.toArgb())
        assertThat(mapped.primaryButton.colorsLight.onBackground).isEqualTo(Color.White.toArgb())
        assertThat(mapped.primaryButton.colorsLight.border).isEqualTo(Color.Black.toArgb())
        assertThat(mapped.primaryButton.shape.cornerRadiusDp).isEqualTo(12f)
        assertThat(mapped.primaryButton.shape.borderStrokeWidthDp).isEqualTo(2f)
        assertThat(mapped.primaryButton.shape.heightDp).isEqualTo(48f)
        assertThat(mapped.primaryButton.typography.fontSizeSp).isEqualTo(18f)
        assertThat(mapped.formInsetValues.startDp).isEqualTo(1f)
        assertThat(mapped.formInsetValues.topDp).isEqualTo(2f)
        assertThat(mapped.formInsetValues.endDp).isEqualTo(3f)
        assertThat(mapped.formInsetValues.bottomDp).isEqualTo(4f)
    }

    @Test
    fun `shipping address dark primary button maps custom foreground to success state`() {
        val appearance = ShippingAddressElement.Configuration.Appearance()
            .primaryButton(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton()
                    .colorsDark(
                        ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.dark()
                            .onBackground(Color.Yellow)
                    )
            )

        val colorsDark = appearance.build().asPaymentSheet().primaryButton.colorsDark

        assertThat(colorsDark.onBackground).isEqualTo(Color.Yellow.toArgb())
        assertThat(colorsDark.successBackgroundColor)
            .isEqualTo(PaymentSheet.PrimaryButtonColors.defaultDark.successBackgroundColor)
        assertThat(colorsDark.onSuccessBackgroundColor).isEqualTo(Color.Yellow.toArgb())
    }

    @Test
    fun `configuration retains and applies root typography`() {
        val typography = Appearance.Typography()
            .sizeScaleFactor(1.5f)
            .fontResId(123)
            .custom(
                Appearance.Typography.Custom().h1(
                    Appearance.Typography.Font().fontFamily(456).fontSizeSp(22f)
                        .fontWeight(600).letterSpacingSp(0.5f)
                )
            )
        val appearance = Appearance().typography(typography)
        val state = PaymentElement.Configuration().appearance(appearance).build().appearance

        assertThat(state.typography).isEqualTo(typography.build())
        assertThat(state.asPaymentSheet().typography).isEqualTo(
            PaymentSheet.Typography.Builder()
                .sizeScaleFactor(1.5f)
                .fontResId(123)
                .custom(PaymentSheet.Typography.Custom(PaymentSheet.Typography.Font(456, 22f, 600, 0.5f)))
                .build()
        )
    }

    @Test
    fun `configuration retains and applies root shapes`() {
        val shapes = Appearance.Shapes()
            .cornerRadiusDp(12f)
            .borderStrokeWidthDp(2f)
            .bottomSheetCornerRadiusDp(24f)
        val state = PaymentElement.Configuration().appearance(Appearance().shapes(shapes)).build().appearance

        assertThat(state.shapes).isEqualTo(shapes.build())
        assertThat(state.asPaymentSheet().shapes).isEqualTo(
            PaymentSheet.Shapes.Builder()
                .cornerRadiusDp(12f)
                .borderStrokeWidthDp(2f)
                .bottomSheetCornerRadiusDp(24f)
                .build()
        )
    }

    @Test
    fun `bottom sheet shape defaults to configured component corner radius`() {
        val mapped = Appearance().shapes(Appearance.Shapes().cornerRadiusDp(17f))
            .build().asPaymentSheet()

        assertThat(mapped.shapes.cornerRadiusDp).isEqualTo(17f)
        assertThat(mapped.shapes.bottomSheetCornerRadiusDp).isEqualTo(17f)
    }

    @Test
    fun `configuration retains and applies embedded margins and fonts`() {
        val embedded = Appearance.Embedded()
            .paymentMethodIconMargins(Appearance.Insets(1f, 2f, 3f, 4f))
            .titleFont(
                Appearance.Typography.Font().fontFamily(123).fontSizeSp(20f)
                    .fontWeight(700).letterSpacingSp(0.5f)
            )
            .subtitleFont(
                Appearance.Typography.Font().fontFamily(456).fontSizeSp(16f)
                    .fontWeight(400).letterSpacingSp(0.25f)
            )
        val state = PaymentElement.Configuration().appearance(Appearance().embeddedAppearance(embedded))
            .build().appearance

        assertThat(state.embeddedAppearance).isEqualTo(embedded.build())
        assertThat(state.asPaymentSheet().embeddedAppearance).isEqualTo(
            PaymentSheet.Appearance.Embedded.Builder()
                .paymentMethodIconMargins(PaymentSheet.Insets(1f, 2f, 3f, 4f))
                .titleFont(PaymentSheet.Typography.Font(123, 20f, 700, 0.5f))
                .subtitleFont(PaymentSheet.Typography.Font(456, 16f, 400, 0.25f))
                .build()
        )
    }

    @Test
    fun `configuration retains and applies FlatWithRadio embedded row style`() {
        val style = Appearance.Embedded.RowStyle.FlatWithRadio()
            .separatorThicknessDp(2f)
            .startSeparatorInsetDp(3f)
            .endSeparatorInsetDp(4f)
            .topSeparatorEnabled(false)
            .bottomSeparatorEnabled(false)
            .additionalVerticalInsetsDp(5f)
            .horizontalInsetsDp(6f)
            .colorsLight(
                Appearance.Embedded.RowStyle.FlatWithRadio.Colors.light()
                    .separatorColor(Color.Red)
                    .selectedColor(Color.Green)
                    .unselectedColor(Color.Blue)
            )
            .colorsDark(
                Appearance.Embedded.RowStyle.FlatWithRadio.Colors.dark()
                    .separatorColor(Color.Cyan)
                    .selectedColor(Color.Magenta)
                    .unselectedColor(Color.Yellow)
            )
        val embedded = Appearance.Embedded().rowStyle(style)
        val state = PaymentElement.Configuration().appearance(Appearance().embeddedAppearance(embedded))
            .build().appearance

        assertThat(state.embeddedAppearance.style).isEqualTo(style.build())
        assertThat(state.asPaymentSheet().embeddedAppearance.style).isEqualTo(
            PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.Builder()
                .separatorThicknessDp(2f)
                .startSeparatorInsetDp(3f)
                .endSeparatorInsetDp(4f)
                .topSeparatorEnabled(false)
                .bottomSeparatorEnabled(false)
                .additionalVerticalInsetsDp(5f)
                .horizontalInsetsDp(6f)
                .colorsLight(
                    PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.Colors.Builder.light()
                        .separatorColor(Color.Red.toArgb())
                        .selectedColor(Color.Green.toArgb())
                        .unselectedColor(Color.Blue.toArgb())
                        .build()
                )
                .colorsDark(
                    PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.Colors.Builder.dark()
                        .separatorColor(Color.Cyan.toArgb())
                        .selectedColor(Color.Magenta.toArgb())
                        .unselectedColor(Color.Yellow.toArgb())
                        .build()
                )
                .build()
        )
    }

    @Test
    fun `configuration retains and applies FlatWithCheckmark embedded row style`() {
        val style = Appearance.Embedded.RowStyle.FlatWithCheckmark()
            .separatorThicknessDp(2f)
            .startSeparatorInsetDp(3f)
            .endSeparatorInsetDp(4f)
            .topSeparatorEnabled(false)
            .bottomSeparatorEnabled(false)
            .additionalVerticalInsetsDp(5f)
            .horizontalInsetsDp(6f)
            .checkmarkInsetDp(8f)
            .colorsLight(
                Appearance.Embedded.RowStyle.FlatWithCheckmark.Colors.light()
                    .separatorColor(Color.Red)
                    .checkmarkColor(Color.Green)
            )
            .colorsDark(
                Appearance.Embedded.RowStyle.FlatWithCheckmark.Colors.dark()
                    .separatorColor(Color.Cyan)
                    .checkmarkColor(Color.Magenta)
            )
        val embedded = Appearance.Embedded().rowStyle(style)
        val state = PaymentElement.Configuration().appearance(Appearance().embeddedAppearance(embedded))
            .build().appearance

        assertThat(state.embeddedAppearance.style).isEqualTo(style.build())
        assertThat(state.asPaymentSheet().embeddedAppearance.style).isEqualTo(
            PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.Builder()
                .separatorThicknessDp(2f)
                .startSeparatorInsetDp(3f)
                .endSeparatorInsetDp(4f)
                .topSeparatorEnabled(false)
                .bottomSeparatorEnabled(false)
                .additionalVerticalInsetsDp(5f)
                .horizontalInsetsDp(6f)
                .checkmarkInsetDp(8f)
                .colorsLight(
                    PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.Colors.Builder.light()
                        .separatorColor(Color.Red.toArgb())
                        .checkmarkColor(Color.Green.toArgb())
                        .build()
                )
                .colorsDark(
                    PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.Colors.Builder.dark()
                        .separatorColor(Color.Cyan.toArgb())
                        .checkmarkColor(Color.Magenta.toArgb())
                        .build()
                )
                .build()
        )
    }

    @Test
    fun `configuration retains and applies FloatingButton embedded row style`() {
        val style = Appearance.Embedded.RowStyle.FloatingButton()
            .spacingDp(7f)
            .additionalInsetsDp(9f)
        val embedded = Appearance.Embedded().rowStyle(style)
        val state = PaymentElement.Configuration().appearance(Appearance().embeddedAppearance(embedded))
            .build().appearance

        assertThat(state.embeddedAppearance.style).isEqualTo(style.build())
        assertThat(state.asPaymentSheet().embeddedAppearance.style).isEqualTo(
            PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.Builder()
                .spacingDp(7f)
                .additionalInsetsDp(9f)
                .build()
        )
    }

    @Test
    fun `configuration retains and applies FlatWithDisclosure embedded row style`() {
        val style = Appearance.Embedded.RowStyle.FlatWithDisclosure()
            .separatorThicknessDp(2f)
            .startSeparatorInsetDp(3f)
            .endSeparatorInsetDp(4f)
            .topSeparatorEnabled(false)
            .bottomSeparatorEnabled(false)
            .additionalVerticalInsetsDp(5f)
            .horizontalInsetsDp(6f)
            .disclosureIconRes(123)
            .colorsLight(
                Appearance.Embedded.RowStyle.FlatWithDisclosure.Colors.light()
                    .separatorColor(Color.Red)
                    .disclosureColor(Color.Green)
            )
            .colorsDark(
                Appearance.Embedded.RowStyle.FlatWithDisclosure.Colors.dark()
                    .separatorColor(Color.Cyan)
                    .disclosureColor(Color.Magenta)
            )
        val embedded = Appearance.Embedded().rowStyle(style)
        val state = PaymentElement.Configuration().appearance(Appearance().embeddedAppearance(embedded))
            .build().appearance

        assertThat(state.embeddedAppearance.style).isEqualTo(style.build())
        assertThat(state.asPaymentSheet().embeddedAppearance.style).isEqualTo(
            PaymentSheet.Appearance.Embedded.RowStyle.FlatWithDisclosure.Builder()
                .separatorThicknessDp(2f)
                .startSeparatorInsetDp(3f)
                .endSeparatorInsetDp(4f)
                .topSeparatorEnabled(false)
                .bottomSeparatorEnabled(false)
                .additionalVerticalInsetsDp(5f)
                .horizontalInsetsDp(6f)
                .disclosureIconRes(123)
                .colorsLight(
                    PaymentSheet.Appearance.Embedded.RowStyle.FlatWithDisclosure.Colors.Builder.light()
                        .separatorColor(Color.Red.toArgb())
                        .disclosureColor(Color.Green.toArgb())
                        .build()
                )
                .colorsDark(
                    PaymentSheet.Appearance.Embedded.RowStyle.FlatWithDisclosure.Colors.Builder.dark()
                        .separatorColor(Color.Cyan.toArgb())
                        .disclosureColor(Color.Magenta.toArgb())
                        .build()
                )
                .build()
        )
    }

    @Test
    fun `nested mutations leave built configuration unchanged and appear in subsequent builds`() {
        val colors = Appearance.Embedded.RowStyle.FlatWithRadio.Colors.light().selectedColor(Color.Red)
        val style = Appearance.Embedded.RowStyle.FlatWithRadio().colorsLight(colors)
        val font = Appearance.Typography.Font().fontFamily(123).fontSizeSp(20f)
        val custom = Appearance.Typography.Custom().h1(font)
        val typography = Appearance.Typography().custom(custom)
        val shapes = Appearance.Shapes().cornerRadiusDp(12f)
        val embedded = Appearance.Embedded().rowStyle(style).titleFont(font)
        val configuration = PaymentElement.Configuration().appearance(
            Appearance().typography(typography).shapes(shapes).embeddedAppearance(embedded)
        )
        val first = configuration.build()

        colors.selectedColor(Color.Cyan)
        style.horizontalInsetsDp(30f)
        font.fontSizeSp(40f)
        typography.sizeScaleFactor(2f)
        shapes.cornerRadiusDp(24f)
        embedded.subtitleFont(font)
        val second = configuration.build()

        val firstAppearance = first.appearance.asPaymentSheet()
        val secondAppearance = second.appearance.asPaymentSheet()
        val firstStyle = firstAppearance.embeddedAppearance.style as
            PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
        val secondStyle = secondAppearance.embeddedAppearance.style as
            PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
        assertThat(firstStyle.colorsLight.selectedColor).isEqualTo(Color.Red.toArgb())
        assertThat(secondStyle.colorsLight.selectedColor).isEqualTo(Color.Cyan.toArgb())
        assertThat(secondStyle.horizontalInsetsDp).isEqualTo(30f)
        assertThat(first.appearance.typography.custom.h1?.fontSizeSp).isEqualTo(20f)
        assertThat(second.appearance.typography.custom.h1?.fontSizeSp).isEqualTo(40f)
        assertThat(first.appearance.typography.sizeScaleFactor).isEqualTo(1f)
        assertThat(second.appearance.typography.sizeScaleFactor).isEqualTo(2f)
        assertThat(firstAppearance.shapes.bottomSheetCornerRadiusDp).isEqualTo(12f)
        assertThat(secondAppearance.shapes.bottomSheetCornerRadiusDp).isEqualTo(24f)
        assertThat(first.appearance.embeddedAppearance.subtitleFont).isNull()
        assertThat(second.appearance.embeddedAppearance.subtitleFont?.fontSizeSp).isEqualTo(40f)

        custom.h1(null)
        assertThat(configuration.build().appearance.typography.custom.h1).isNull()
        assertThat(first.appearance.typography.custom.h1?.fontFamily).isEqualTo(123)
    }

    @Test
    fun `separate default configurations remain independent`() {
        val first = PaymentElement.Configuration()
        val second = PaymentElement.Configuration()
        val defaults = second.build()
        first.appearance(
            Appearance().shapes(Appearance.Shapes().cornerRadiusDp(50f))
                .typography(
                    Appearance.Typography().custom(
                        Appearance.Typography.Custom().h1(Appearance.Typography.Font().fontSizeSp(30f))
                    )
                )
                .embeddedAppearance(
                    Appearance.Embedded().rowStyle(
                        Appearance.Embedded.RowStyle.FlatWithRadio().colorsLight(
                            Appearance.Embedded.RowStyle.FlatWithRadio.Colors.light().selectedColor(Color.Red)
                        )
                    )
                )
        )

        assertThat(first.build().appearance).isNotEqualTo(defaults.appearance)
        assertThat(second.build()).isEqualTo(defaults)
        assertThat(Appearance().build().asPaymentSheet()).isEqualTo(PaymentSheet.Appearance())
    }

    @Test
    fun `nullable font overrides can be reset`() {
        val font = Appearance.Typography.Font().fontFamily(123).fontSizeSp(20f)
            .fontWeight(700).letterSpacingSp(0.5f)
        val configured = font.build()

        font.fontFamily(null).fontSizeSp(null).fontWeight(null).letterSpacingSp(null)

        assertThat(configured.fontFamily).isEqualTo(123)
        assertThat(configured.fontSizeSp).isEqualTo(20f)
        assertThat(configured.fontWeight).isEqualTo(700)
        assertThat(configured.letterSpacingSp).isEqualTo(0.5f)
        assertThat(font.build()).isEqualTo(Appearance.Typography.Font().build())
    }

    @Test
    fun `nullable embedded and typography overrides can be reset`() {
        val font = Appearance.Typography.Font().fontFamily(123)
        val embedded = Appearance.Embedded().titleFont(font).subtitleFont(font)
            .paymentMethodIconMargins(Appearance.Insets(1f, 2f, 3f, 4f))
        val typography = Appearance.Typography().fontResId(456)
            .custom(Appearance.Typography.Custom().h1(font))
        val configured = Appearance().embeddedAppearance(embedded).typography(typography).build()

        embedded.titleFont(null).subtitleFont(null).paymentMethodIconMargins(null)
        typography.fontResId(null).custom(Appearance.Typography.Custom().h1(null))
        val reset = Appearance().embeddedAppearance(embedded).typography(typography).build()

        assertThat(configured.embeddedAppearance.titleFont?.fontFamily).isEqualTo(123)
        assertThat(configured.embeddedAppearance.subtitleFont?.fontFamily).isEqualTo(123)
        assertThat(configured.embeddedAppearance.paymentMethodIconMargins?.startDp).isEqualTo(1f)
        assertThat(configured.typography.fontResId).isEqualTo(456)
        assertThat(configured.typography.custom.h1?.fontFamily).isEqualTo(123)
        assertThat(reset.embeddedAppearance).isEqualTo(Appearance.Embedded().build())
        assertThat(reset.typography).isEqualTo(Appearance.Typography().build())
    }

    @Test
    fun `explicit bottom sheet radius survives component radius changes`() {
        val shapes = Appearance.Shapes().bottomSheetCornerRadiusDp(24f).cornerRadiusDp(12f)
        val first = shapes.build()

        shapes.cornerRadiusDp(30f)

        assertThat(first.cornerRadiusDp).isEqualTo(12f)
        assertThat(first.bottomSheetCornerRadiusDp).isEqualTo(24f)
        assertThat(shapes.build().cornerRadiusDp).isEqualTo(30f)
        assertThat(shapes.build().bottomSheetCornerRadiusDp).isEqualTo(24f)
    }
}
