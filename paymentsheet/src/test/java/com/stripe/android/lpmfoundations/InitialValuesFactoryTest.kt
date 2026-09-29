package com.stripe.android.lpmfoundations

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId
import org.junit.Test

class InitialValuesFactoryTest {
    private val billingDetails = PaymentSheet.BillingDetails(
        PaymentSheet.Address(
            line1 = "123 Main Street",
            line2 = "APt 1",
            city = "Dublin",
            state = "Co. Dublin",
            postalCode = "T37 F8HK",
            country = "IE"
        ),
        "email.email.com",
        "Jenny Smith"
    )

    private val parameterMapBillingDetails = "billing_details" to mapOf(
        "address" to mapOf(
            "city" to "Berlin",
            "country" to "DE",
            "line1" to "1234 Main St",
            "line2" to null,
            "state" to "Capital",
            "postal_code" to "10787"
        ),
        "email" to "jenny.rosen@example.com",
        "name" to "Jenny Rosen",
        "phone" to "1-800-555-1234"
    )

    private val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Card.code,
        billingDetails = null,
        requiresMandate = PaymentMethod.Type.Card.requiresMandate,
        overrideParamMap = mapOf(
            "type" to "card",
            parameterMapBillingDetails,
            "card" to mapOf(
                "number" to "4242424242424242",
                "exp_month" to "1",
                "exp_year" to "2024",
                "cvc" to "111"
            )
        ),
        productUsage = emptySet(),
        clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
    )

    @Test
    fun `Verify payment method parameters overrides any billing address values`() {
        val initialValues = InitialValuesFactory.create(
            defaultBillingDetails = billingDetails,
            paymentMethodCreateParams = paymentMethodCreateParams,
            paymentMethodExtraParams = null,
        )

        assertThat(initialValues).containsEntry(FormFieldId.Name, "Jenny Rosen")
        assertThat(initialValues).containsEntry(FormFieldId.Email, "jenny.rosen@example.com")
        assertThat(initialValues).containsEntry(FormFieldId.Phone, "1-800-555-1234")
        assertThat(initialValues).containsEntry(FormFieldId.Line1, "1234 Main St")
        assertThat(initialValues).containsEntry(FormFieldId.Line2, null)
        assertThat(initialValues).containsEntry(FormFieldId.City, "Berlin")
        assertThat(initialValues).containsEntry(FormFieldId.State, "Capital")
        assertThat(initialValues).containsEntry(FormFieldId.PostalCode, "10787")
        assertThat(initialValues).containsEntry(FormFieldId.Country, "DE")
        assertThat(initialValues).containsEntry(FormFieldId.Generic("type"), "card")
        assertThat(initialValues).containsEntry(FormFieldId.CardNumber, "4242424242424242")
        assertThat(initialValues).containsEntry(FormFieldId.CardExpMonth, "1")
        assertThat(initialValues).containsEntry(FormFieldId.CardExpYear, "2024")
        assertThat(initialValues).containsEntry(FormFieldId.CardCvc, "111")
    }

    @Test
    fun `Verify if only default billing address they appear in the initial values`() {
        assertThat(
            InitialValuesFactory.create(
                defaultBillingDetails = billingDetails,
                paymentMethodCreateParams = null,
                paymentMethodExtraParams = null,
            )
        ).isEqualTo(
            mapOf(
                FormFieldId.Name to "Jenny Smith",
                FormFieldId.Email to "email.email.com",
                FormFieldId.Phone to null,
                FormFieldId.Line1 to "123 Main Street",
                FormFieldId.Line2 to "APt 1",
                FormFieldId.City to "Dublin",
                FormFieldId.State to "Co. Dublin",
                FormFieldId.PostalCode to "T37 F8HK",
                FormFieldId.Country to "IE"
            )
        )
    }

    @Test
    fun `Verify extra parameters are included if passed in`() {
        assertThat(
            InitialValuesFactory.create(
                defaultBillingDetails = null,
                paymentMethodCreateParams = PaymentMethodCreateParams.create(
                    bacsDebit = PaymentMethodCreateParams.BacsDebit(
                        accountNumber = "00012345",
                        sortCode = "10-88-00"
                    ),
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "Jenny Rosen",
                        email = "jenny.rosen@example.com",
                        address = Address(
                            line1 = "123 Main Street",
                            line2 = "APt 1",
                            city = "Dublin",
                            state = "Co. Dublin",
                            postalCode = "T37 F8HK",
                            country = "IE"
                        )
                    )
                ),
                paymentMethodExtraParams = PaymentMethodExtraParams.BacsDebit(
                    confirmed = true
                ),
            )
        ).isEqualTo(
            mapOf(
                FormFieldId.Name to "Jenny Rosen",
                FormFieldId.Email to "jenny.rosen@example.com",
                FormFieldId.Phone to null,
                FormFieldId.Line1 to "123 Main Street",
                FormFieldId.Line2 to "APt 1",
                FormFieldId.City to "Dublin",
                FormFieldId.State to "Co. Dublin",
                FormFieldId.PostalCode to "T37 F8HK",
                FormFieldId.Country to "IE",
                FormFieldId.Generic("type") to "bacs_debit",
                FormFieldId.Generic("bacs_debit[account_number]") to "00012345",
                FormFieldId.Generic("bacs_debit[sort_code]") to "10-88-00",
                FormFieldId.BacsDebitConfirmed to "true"
            )
        )
    }
}
