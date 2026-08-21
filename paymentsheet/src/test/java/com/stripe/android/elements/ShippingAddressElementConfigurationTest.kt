package com.stripe.android.elements

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ShippingAddressElementConfigurationTest {

    @Test
    fun `defaults leave optional labels unset`() {
        val state = ShippingAddressElement.Configuration().build()

        assertThat(state.title).isNull()
        assertThat(state.buttonTitle).isNull()
        assertThat(state.appearance.colorsLight.primary).isNull()
        assertThat(state.appearance.shapes.cornerRadiusDp).isNull()
    }

    @Test
    fun `custom values are preserved in immutable state`() {
        val state = ShippingAddressElement.Configuration()
            .title("Deliver to")
            .buttonTitle("Use this address")
            .appearance(
                ShippingAddressElement.Appearance()
                    .colorsLight(
                        ShippingAddressElement.Appearance.Colors()
                            .primary(Color.Red)
                    )
                    .shapes(
                        ShippingAddressElement.Appearance.Shapes()
                            .cornerRadiusDp(12f)
                    )
            )
            .build()

        assertThat(state.title).isEqualTo("Deliver to")
        assertThat(state.buttonTitle).isEqualTo("Use this address")
        assertThat(state.appearance.colorsLight.primary).isEqualTo(Color.Red.toArgb())
        assertThat(state.appearance.shapes.cornerRadiusDp).isEqualTo(12f)
    }
}
