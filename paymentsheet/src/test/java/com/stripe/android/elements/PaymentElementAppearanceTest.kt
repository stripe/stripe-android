package com.stripe.android.elements

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement.Configuration.Appearance
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.StripeThemeDefaults
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class PaymentElementAppearanceTest {
    @Test
    fun `default build uses PaymentSheet appearance defaults`() {
        val state = Appearance().build()

        assertThat(state.shapes.cornerRadiusDp).isEqualTo(StripeThemeDefaults.shapes.cornerRadius)
        assertThat(state.shapes.borderStrokeWidthDp).isEqualTo(StripeThemeDefaults.shapes.borderStrokeWidth)
        assertThat(state.typography.sizeScaleFactor)
            .isEqualTo(StripeThemeDefaults.typography.fontSizeMultiplier)
        assertThat(state.typography.fontResId).isEqualTo(StripeThemeDefaults.typography.fontFamily)
        assertThat(state.embeddedAppearance.rowStyle)
            .isInstanceOf(Appearance.Embedded.RowStyle.FlatWithRadioState::class.java)
    }

    @Test
    fun `shapes fluent setters build immutable state`() {
        val shapes = Appearance.Shapes()
            .cornerRadiusDp(12f)
            .borderStrokeWidthDp(3f)

        val state = shapes.build()
        shapes.cornerRadiusDp(24f).borderStrokeWidthDp(6f)

        assertThat(state.cornerRadiusDp).isEqualTo(12f)
        assertThat(state.borderStrokeWidthDp).isEqualTo(3f)
    }

    @Test
    fun `typography fluent setters build immutable state`() {
        val typography = Appearance.Typography()
            .sizeScaleFactor(1.5f)
            .fontResId(123)

        val state = typography.build()
        typography.sizeScaleFactor(2f).fontResId(null)

        assertThat(state.sizeScaleFactor).isEqualTo(1.5f)
        assertThat(state.fontResId).isEqualTo(123)
    }

    @Test
    fun `flat with radio fluent setters build state`() {
        val state = Appearance.Embedded.RowStyle.FlatWithRadio()
            .separatorThicknessDp(1f)
            .startSeparatorInsetDp(2f)
            .endSeparatorInsetDp(3f)
            .topSeparatorEnabled(false)
            .bottomSeparatorEnabled(true)
            .additionalVerticalInsetsDp(4f)
            .horizontalInsetsDp(5f)
            .colorsLight(
                Appearance.Embedded.RowStyle.FlatWithRadio.Colors.light()
                    .separatorColor(Color.Red)
                    .selectedColor(Color.Green)
                    .unselectedColor(Color.Blue)
            )
            .build()

        assertThat(state.separatorThicknessDp).isEqualTo(1f)
        assertThat(state.startSeparatorInsetDp).isEqualTo(2f)
        assertThat(state.endSeparatorInsetDp).isEqualTo(3f)
        assertThat(state.topSeparatorEnabled).isFalse()
        assertThat(state.bottomSeparatorEnabled).isTrue()
        assertThat(state.additionalVerticalInsetsDp).isEqualTo(4f)
        assertThat(state.horizontalInsetsDp).isEqualTo(5f)
        assertThat(state.colorsLight.separatorColor).isEqualTo(Color.Red.toArgb())
        assertThat(state.colorsLight.selectedColor).isEqualTo(Color.Green.toArgb())
        assertThat(state.colorsLight.unselectedColor).isEqualTo(Color.Blue.toArgb())
    }

    @Test
    fun `all embedded row styles build through appearance`() {
        val checkmark = Appearance.Embedded.RowStyle.FlatWithCheckmark()
            .checkmarkInsetDp(7f)
            .colorsDark(
                Appearance.Embedded.RowStyle.FlatWithCheckmark.Colors.dark()
                    .checkmarkColor(Color.Red)
            )
        val floating = Appearance.Embedded.RowStyle.FloatingButton()
            .spacingDp(8f)
            .additionalInsetsDp(9f)
        val disclosure = Appearance.Embedded.RowStyle.FlatWithDisclosure()
            .horizontalInsetsDp(10f)
            .colorsLight(
                Appearance.Embedded.RowStyle.FlatWithDisclosure.Colors.light()
                    .disclosureColor(Color.Blue)
            )

        val checkmarkState = Appearance().embeddedAppearance(Appearance.Embedded().rowStyle(checkmark))
            .build().embeddedAppearance.rowStyle as Appearance.Embedded.RowStyle.FlatWithCheckmarkState
        val floatingState = Appearance().embeddedAppearance(Appearance.Embedded().rowStyle(floating))
            .build().embeddedAppearance.rowStyle as Appearance.Embedded.RowStyle.FloatingButtonState
        val disclosureState = Appearance().embeddedAppearance(Appearance.Embedded().rowStyle(disclosure))
            .build().embeddedAppearance.rowStyle as Appearance.Embedded.RowStyle.FlatWithDisclosureState

        assertThat(checkmarkState.checkmarkInsetDp).isEqualTo(7f)
        assertThat(checkmarkState.colorsDark.checkmarkColor).isEqualTo(Color.Red.toArgb())
        assertThat(floatingState.spacingDp).isEqualTo(8f)
        assertThat(floatingState.additionalInsetsDp).isEqualTo(9f)
        assertThat(disclosureState.horizontalInsetsDp).isEqualTo(10f)
        assertThat(disclosureState.colorsLight.disclosureColor).isEqualTo(Color.Blue.toArgb())
    }
}
