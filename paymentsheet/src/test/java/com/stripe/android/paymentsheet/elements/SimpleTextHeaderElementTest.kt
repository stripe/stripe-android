package com.stripe.android.paymentsheet.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class SimpleTextHeaderElementTest {
    private val resources = ApplicationProvider.getApplicationContext<Application>().resources

    @Test
    fun `unsupported payment method returns empty string`() {
        val element = SimpleTextHeaderElement(
            IdentifierSpec.Generic("test"),
            PaymentMethod.Type.Card
        )

        assertThat(element.getLabel(resources)).isEqualTo("")
    }

    @Test
    fun `klarna has correct string`() {
        val testMap = mapOf(
            "AT" to "Buy now or pay later with Klarna.",
            "BE" to "Buy now or pay later with Klarna.",
            "DK" to "Pay later with Klarna.",
            "FI" to "Pay later with Klarna.",
            "FR" to "Pay later with Klarna.",
            "DE" to "Buy now or pay later with Klarna.",
            "IT" to "Buy now or pay later with Klarna.",
            "NL" to "Buy now or pay later with Klarna.",
            "NO" to "Pay later with Klarna.",
            "ES" to "Buy now or pay later with Klarna.",
            "SE" to "Buy now or pay later with Klarna.",
            "GB" to "Pay later with Klarna.",
            "US" to "Pay later with Klarna.",
        )

        val element = SimpleTextHeaderElement(
            IdentifierSpec.Generic("test"),
            PaymentMethod.Type.Klarna
        )

        for (entry in testMap) {
            Locale.setDefault(Locale("", entry.key))
            assertThat(entry.key + element.getLabel(resources)).isEqualTo(entry.key + entry.value)
        }
    }
}
