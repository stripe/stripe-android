package com.stripe.android.ui.core

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter.Companion.addPath
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter.Companion.getKeys
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Test

class FieldValuesToParamsMapConverterTest {
    @Test
    fun `transform to payment method with bank`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                mapOf(
                    IdentifierSpec.Generic("ideal[bank]") to FormFieldEntry(
                        "abn_amro",
                        true
                    )
                ),
                PaymentMethod.Type.Ideal.code,
                PaymentMethod.Type.Ideal.requiresMandate
            )

        assertThat(paymentMethodParams.toParamMap().toString().replace("\\s".toRegex(), ""))
            .isEqualTo(
                """
                    {type=ideal,ideal={bank=abn_amro}}
                """.trimIndent()
            )
    }

    @Test
    fun `test function`() {
        val map: MutableMap<String, Any?> = mutableMapOf("type" to "card")
        mapOf(
            IdentifierSpec.Name to FormFieldEntry(
                "joe",
                true
            ),
            IdentifierSpec.Email to FormFieldEntry(
                "joe@gmail.com",
                true
            ),
            IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                "US",
                true
            ),
        ).entries.forEach {
            addPath(map, getKeys(it.key.v1), it.value.value)
        }
        assertThat(map).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_details" to mapOf(
                    "name" to "joe",
                    "email" to "joe@gmail.com",
                    "address" to mapOf(
                        "country" to "US"
                    )
                )
            )
        )
    }

    @Test
    fun `transform to payment method params`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.Email to FormFieldEntry(
                        "joe@gmail.com",
                        true
                    ),
                    IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                        "US",
                        true
                    ),
                    IdentifierSpec.Line1 to FormFieldEntry(
                        "123 Main Street",
                        true
                    ),
                ),
                PaymentMethod.Type.Sofort.code,
                PaymentMethod.Type.Sofort.requiresMandate
            )

        assertThat(
            paymentMethodParams.toParamMap().toString()
        ).isEqualTo(
            "{" +
                "type=sofort, " +
                "billing_details={" +
                "name=joe, " +
                "email=joe@gmail.com, " +
                "address={" +
                "country=US, " +
                "line1=123 Main Street" +
                "}" +
                "}" +
                "}"
        )
    }
}
