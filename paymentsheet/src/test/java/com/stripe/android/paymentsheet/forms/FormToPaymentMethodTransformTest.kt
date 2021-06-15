package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormToPaymentMethodTransformTest {

    @Test
    fun `transform to payment method params`() {
        val paymentMethodParams = FormToPaymentMethodTransform().transform(
            sofortParamKey,
            FormFieldValues(
                mapOf(
                    Field.NameInput to "joe",
                    Field.EmailInput to "joe@gmail.com",
                    Field.CountryInput to "United States",
                )
            )
        )

        assertThat(
            paymentMethodParams.toString().replace("\\s".toRegex(), "")
        ).isEqualTo(
            """
                {
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
}