package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.forms.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.forms.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.forms.SectionSpec.SectionFieldSpec.Name
import org.junit.Test

class FormToPaymentMethodTransformTest {

    @Test
    fun `transform to payment method params`() {
        val paymentMethodParams = FormToPaymentMethodTransform().transform(
            sofortParamKey,
            FormFieldValues(
                mapOf(
                    Name to "joe",
                    Email to "joe@gmail.com",
                    Country to "United States",
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