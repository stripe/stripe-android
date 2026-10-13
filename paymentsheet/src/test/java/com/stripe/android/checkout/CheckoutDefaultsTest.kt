package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Configuration.Defaults
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutDefaultsTest {
    @Test
    fun `phone is null by default`() {
        assertThat(Defaults().build().phone).isNull()
    }

    @Test
    fun `phone accepts a non-null value and returns the same defaults`() {
        val defaults = Defaults()

        assertThat(defaults.phone("+15555551234")).isSameInstanceAs(defaults)
        assertThat(defaults.build().phone).isEqualTo("+15555551234")
    }

    @Test
    fun `phone accepts null`() {
        assertThat(Defaults().phone(null).build().phone).isNull()
    }

    @Test
    fun `phone can clear a previously configured value`() {
        val defaults = Defaults().phone("+15555551234")
        assertThat(defaults.build().phone).isEqualTo("+15555551234")

        defaults.phone(null)

        assertThat(defaults.build().phone).isNull()
    }
}
