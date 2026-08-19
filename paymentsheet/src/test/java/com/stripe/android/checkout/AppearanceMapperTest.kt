package com.stripe.android.checkout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
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
    fun `configured appearance maps colors shapes and primary button`() {
        val appearance = PaymentElement.Configuration.Appearance()
            .colorsLight(
                PaymentElement.Configuration.Appearance.Colors.light()
                    .primary(Color.Red)
            )
            .shapes(
                PaymentElement.Configuration.Appearance.Shapes()
                    .cornerRadiusDp(0f)
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
        assertThat(mapped.shapes.cornerRadiusDp).isEqualTo(0f)
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
            .shapes(PaymentElement.Configuration.Appearance.Shapes().cornerRadiusDp(12f))

        val state = PaymentElement.Configuration().appearance(appearance).build()

        assertThat(state.appearance).isEqualTo(appearance.build())
    }
}
