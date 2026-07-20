package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class GooglePayConfigurationMapperTest {

    @Test
    fun `default owned config maps to PaymentSheet defaults`() {
        val mapped = requireNotNull(
            GooglePayConfiguration(
                GooglePayConfiguration.Environment.Test,
            ).build().asPaymentSheet(merchantCountry = "US")
        )

        assertThat(mapped.environment).isEqualTo(PaymentSheet.GooglePayConfiguration.Environment.Test)
        assertThat(mapped.countryCode).isEqualTo("US")
        assertThat(mapped.buttonType).isEqualTo(PaymentSheet.GooglePayConfiguration.ButtonType.Pay)
    }

    @Test
    fun `all fields map 1 to 1`() {
        val mapped = requireNotNull(
            GooglePayConfiguration(
                GooglePayConfiguration.Environment.Production,
            )
                .label("Total")
                .buttonType(GooglePayConfiguration.ButtonType.Checkout)
                .additionalEnabledNetworks(listOf("INTERAC"))
                .build()
                .asPaymentSheet(merchantCountry = "CA")
        )

        assertThat(mapped.environment)
            .isEqualTo(PaymentSheet.GooglePayConfiguration.Environment.Production)
        assertThat(mapped.countryCode).isEqualTo("CA")
        assertThat(mapped.label).isEqualTo("Total")
        assertThat(mapped.buttonType)
            .isEqualTo(PaymentSheet.GooglePayConfiguration.ButtonType.Checkout)
        assertThat(mapped.additionalEnabledNetworks).containsExactly("INTERAC")
    }

    @Test
    fun `returns null when merchant country is not available`() {
        val mapped = GooglePayConfiguration(
            GooglePayConfiguration.Environment.Test,
        ).build().asPaymentSheet(merchantCountry = null)

        assertThat(mapped).isNull()
    }

    @Test
    fun `every button type maps to the matching PaymentSheet value`() {
        GooglePayConfiguration.ButtonType.entries.forEach { buttonType ->
            val mapped = requireNotNull(
                GooglePayConfiguration(
                    GooglePayConfiguration.Environment.Test,
                )
                    .buttonType(buttonType)
                    .build()
                    .asPaymentSheet(merchantCountry = "US")
            )

            assertThat(mapped.buttonType.name).isEqualTo(buttonType.name)
        }
    }
}
