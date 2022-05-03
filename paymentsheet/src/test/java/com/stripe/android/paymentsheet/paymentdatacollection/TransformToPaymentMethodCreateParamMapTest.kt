package com.stripe.android.paymentsheet.paymentdatacollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.TransformToPaymentMethodCreateParams.Companion.addPath
import com.stripe.android.paymentsheet.paymentdatacollection.TransformToPaymentMethodCreateParams.Companion.getKeys
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Test

class TransformToPaymentMethodCreateParamMapTest {
    @Test
    fun `transform to payment method with bank`() {
        val paymentMethodParams = TransformToPaymentMethodCreateParams()
            .transform(
                FormFieldValues(
                    mapOf(
                        IdentifierSpec.Generic("ideal[bank]") to FormFieldEntry(
                            "abn_amro",
                            true
                        )
                    ),
                    showsMandate = false,
                    userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
                ),
                PaymentMethod.Type.Ideal,
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
        FormFieldValues(
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
            ),
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
        ).fieldValuePairs.entries.forEach {
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
        val formFieldValues = FormFieldValues(
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
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        val paymentMethodParams = TransformToPaymentMethodCreateParams()
            .transform(
                formFieldValues,
                PaymentMethod.Type.Sofort,
            )

        assertThat(
            paymentMethodParams.toParamMap().toString()
        ).isEqualTo(
            "{" +
                "type=sofort, " +
                "billing_details={" +
                "name=joe, " +
                "email=joe@gmail.com, " +
                "address={country=US, " +
                "line1=123 Main Street" +
                "}" +
                "}, " +
                "sofort={" +
                "country=US" +
                "}" +
                "}"
        )
    }
}
