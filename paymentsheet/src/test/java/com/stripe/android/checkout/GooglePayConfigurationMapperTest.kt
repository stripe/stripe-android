package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class GooglePayConfigurationMapperTest {

    @Test
    fun `null config maps to null`() {
        val mapped = (null as GooglePayConfiguration.State?).asPaymentSheet()

        assertThat(mapped).isNull()
    }

    @Test
    fun `default owned config maps to PaymentSheet defaults`() {
        val mapped = GooglePayConfiguration(
            GooglePayConfiguration.Environment.Test,
            "US",
        ).build().asPaymentSheet()

        assertThat(mapped).isEqualTo(
            PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "US",
            )
        )
    }

    @Test
    fun `all fields map 1 to 1`() {
        val mapped = GooglePayConfiguration(
            GooglePayConfiguration.Environment.Production,
            "CA",
        )
            .label("Total")
            .buttonType(GooglePayConfiguration.ButtonType.Checkout)
            .additionalEnabledNetworks(listOf("INTERAC"))
            .build()
            .asPaymentSheet()

        assertThat(mapped).isEqualTo(
            PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                countryCode = "CA",
                label = "Total",
                buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Checkout,
                additionalEnabledNetworks = listOf("INTERAC"),
            )
        )
    }

    @Test
    fun `every button type maps to the matching PaymentSheet value`() {
        GooglePayConfiguration.ButtonType.entries.forEach { buttonType ->
            val mapped = GooglePayConfiguration(
                GooglePayConfiguration.Environment.Test,
                "US",
            )
                .buttonType(buttonType)
                .build()
                .asPaymentSheet()

            assertThat(mapped?.buttonType?.name).isEqualTo(buttonType.name)
        }
    }
}
