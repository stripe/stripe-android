package com.stripe.android.paymentsheet.paymentdatacollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import org.junit.Test

class TransformToPaymentMethodCreateParamMapTest {
    @Test
    fun `transform to payment method with bank`() {
        val paymentMethodParams = TransformToPaymentMethodCreateParams()
            .transform(
                FormFieldValues(
                    mapOf(
                        IdentifierSpec.Generic("bank") to FormFieldEntry(
                            "abn_amro",
                            true
                        )
                    ),
                    showsMandate = false
                ),
                mapOf(
                    "type" to "ideal",
                    "billing_details" to billingParams,
                    "ideal" to mapOf("bank" to null)
                ),
            )

        assertThat(paymentMethodParams?.toParamMap().toString().replace("\\s".toRegex(), ""))
            .isEqualTo(
                """
                    {type=ideal,billing_details={address={city=null,country=null,line1=null,line2=null,postal_code=null,state=null},name=null,email=null,phone=null},ideal={bank=abn_amro}}
                """.trimIndent()
            )
    }

    @Test
    fun `transform to payment method params`() {
        val paymentMethodParams = TransformToPaymentMethodCreateParams()
            .transform(
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
                        IdentifierSpec.Generic("country") to FormFieldEntry(
                            "US",
                            true
                        ),
                    ),
                    showsMandate = false
                ),
                mapOf(
                    "type" to "sofort",
                    "billing_details" to billingParams,
                    "sofort" to mapOf("country" to null)
                ),
            )

        assertThat(
            paymentMethodParams?.toParamMap().toString().replace("\\s".toRegex(), "")
        ).isEqualTo(
            """
                {
                  type=sofort,
                  billing_details={
                    address={
                      city=null,
                      country=US,
                      line1=null,
                      line2=null,
                      postal_code=null,
                      state=null
                    },
                    name=joe,
                    email=joe@gmail.com,
                    phone=null
                  },
                  sofort={country=US}
                }
            """.replace("\\s".toRegex(), "")
        )
    }

    companion object {
        val addressParams: MutableMap<String, Any?> = mutableMapOf(
            "city" to null,
            "country" to null,
            "line1" to null,
            "line2" to null,
            "postal_code" to null,
            "state" to null,
        )

        val billingParams: MutableMap<String, Any?> = mutableMapOf(
            "address" to addressParams,
            "name" to null,
            "email" to null,
            "phone" to null,
        )
    }
}
