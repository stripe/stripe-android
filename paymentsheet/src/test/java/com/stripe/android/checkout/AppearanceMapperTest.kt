package com.stripe.android.checkout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
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

        assertThat(appearance.build().asPaymentSheet()).isEqualTo(PaymentSheet.Appearance())
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
}
