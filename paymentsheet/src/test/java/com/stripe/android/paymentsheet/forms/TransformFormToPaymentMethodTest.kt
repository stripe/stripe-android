package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.specifications.sofortParamKey
import org.junit.Test

class TransformFormToPaymentMethodTest {

    @Test
    fun `transform to payment method params`() {
        val paymentMethodParams = TransformFormToPaymentMethod().transform(
            sofortParamKey,
            FormFieldValues(
                mapOf(
                    Name.identifier to "joe",
                    Email.identifier to "joe@gmail.com",
                    Country.identifier to "United States",
                )
            )
        )

        assertThat(
            paymentMethodParams.toString().replace("\\s".toRegex(), "")
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
}
