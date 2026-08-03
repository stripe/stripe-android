package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.NameSpec
import org.junit.Test
import com.stripe.android.paymentsheet.forms.generated.FormElementSpecV1 as FormElementSpec
import com.stripe.android.paymentsheet.forms.generated.PaymentMethodFormSpecV1 as PaymentMethodFormSpec
import com.stripe.android.paymentsheet.forms.generated.SelectorIconV1 as SelectorIcon

class ServerDrivenFormSpecAdapterTest {
    @Test
    fun `generated form spec converts to the shared native renderer schema`() {
        val json = listOf(
            PaymentMethodFormSpec(
                type = "sepa_debit",
                fields = listOf(
                    FormElementSpec(
                        type = "name",
                        apiPath = "billing_details[name]",
                    )
                ),
                selectorIcon = SelectorIcon(
                    lightThemePng = "https://js.stripe.com/v3/fingerprinted/img/sepa_debit-light.png",
                )
            )
        ).toLpmSpecJson()

        val spec = LpmSerializer.deserializeList(json).getOrThrow().single()

        assertThat(spec.type).isEqualTo("sepa_debit")
        assertThat(spec.fields.single()).isInstanceOf(NameSpec::class.java)
        assertThat(spec.selectorIcon?.lightThemePng)
            .isEqualTo("https://js.stripe.com/v3/fingerprinted/img/sepa_debit-light.png")
    }

    @Test
    fun `native payment method forms are left to the closed native renderer`() {
        val json = listOf(
            PaymentMethodFormSpec(
                type = "card",
                fields = listOf(
                    FormElementSpec(type = "native_component", component = "card_details")
                ),
            )
        ).toLpmSpecJson()

        assertThat(LpmSerializer.deserializeList(json).getOrThrow()).isEmpty()
    }

    @Test
    fun `mixed forms preserve only declarative fields for shared spec decoding`() {
        val json = listOf(
            PaymentMethodFormSpec(
                type = "card",
                fields = listOf(
                    FormElementSpec(type = "native_component", component = "card_details"),
                    FormElementSpec(type = "name"),
                    FormElementSpec(type = "mandate_text", textKey = "sepa"),
                ),
            )
        ).toLpmSpecJson()

        val spec = LpmSerializer.deserializeList(json).getOrThrow().single()

        assertThat(spec.fields).hasSize(1)
        assertThat(spec.fields.single()).isInstanceOf(NameSpec::class.java)
    }
}
