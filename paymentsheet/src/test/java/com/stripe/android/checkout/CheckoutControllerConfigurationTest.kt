package com.stripe.android.checkout

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutControllerConfigurationTest {
    @Test
    fun `payment element appearance uses the default appearance`() {
        val state = CheckoutController.Configuration().build()

        assertThat(state.paymentElementConfiguration.appearance)
            .isEqualTo(ConfigurationDefaults.appearance)
    }

    @Test
    fun `payment element appearance uses the configured appearance`() {
        val appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = Color.Red),
        )
        val state = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration().appearance(appearance))
            .build()

        assertThat(state.paymentElementConfiguration.appearance).isEqualTo(appearance)
    }

    @Test
    fun `api configuration is null by default`() {
        val state = CheckoutController.Configuration().build()

        assertThat(state.apiConfiguration).isNull()
    }

    @Test
    fun `api configuration builds requested credentials`() {
        val state = CheckoutController.Configuration()
            .apiConfiguration(
                ApiConfiguration("pk_test_123")
                    .stripeAccountId("acct_123")
            )
            .build()

        assertThat(state.apiConfiguration?.publishableKey).isEqualTo("pk_test_123")
        assertThat(state.apiConfiguration?.stripeAccountId).isEqualTo("acct_123")
    }
}
