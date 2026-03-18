package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class AddressTest {
    @Test
    fun `build trims whitespace from all fields`() {
        val state = Address()
            .city("  Denver  ")
            .country("  US  ")
            .line1("  123 Main St  ")
            .line2("  Apt 4  ")
            .postalCode("  80202  ")
            .state("  CO  ")
            .build()

        assertThat(state.city).isEqualTo("Denver")
        assertThat(state.country).isEqualTo("US")
        assertThat(state.line1).isEqualTo("123 Main St")
        assertThat(state.line2).isEqualTo("Apt 4")
        assertThat(state.postalCode).isEqualTo("80202")
        assertThat(state.state).isEqualTo("CO")
    }

    @Test
    fun `build requires country`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            Address()
                .city("Denver")
                .build()
        }
        assertThat(error).hasMessageThat().isEqualTo("Country is required.")
    }
}
