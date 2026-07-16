package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class GooglePayConfigurationTest {

    @Test
    fun `configuration defaults optional values`() {
        val state = GooglePayConfiguration(
            GooglePayConfiguration.Environment.Test,
            "US"
        ).build()

        assertThat(state.environment).isEqualTo(GooglePayConfiguration.Environment.Test)
        assertThat(state.countryCode).isEqualTo("US")
        assertThat(state.label).isNull()
        assertThat(state.buttonType).isEqualTo(GooglePayConfiguration.ButtonType.Pay)
        assertThat(state.additionalEnabledNetworks).isEmpty()
    }

    @Test
    fun `configuration builds requested values`() {
        val state = GooglePayConfiguration(
            GooglePayConfiguration.Environment.Production,
            "CA",
        )
            .label("Total")
            .buttonType(GooglePayConfiguration.ButtonType.Checkout)
            .additionalEnabledNetworks(listOf("INTERAC"))
            .build()

        assertThat(state.environment).isEqualTo(GooglePayConfiguration.Environment.Production)
        assertThat(state.countryCode).isEqualTo("CA")
        assertThat(state.label).isEqualTo("Total")
        assertThat(state.buttonType).isEqualTo(GooglePayConfiguration.ButtonType.Checkout)
        assertThat(state.additionalEnabledNetworks).containsExactly("INTERAC")
    }
}
