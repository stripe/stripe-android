package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

class BacsMandateDataTest {
    @Test
    fun `when payment option is Bacs and name & email are provided, 'fromConfirmationOption' should return data`() {
        val option = createPaymentConfirmationOption(
            createParams = PaymentMethodCreateParams.Companion.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "10-88-00"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = "John Doe",
                    email = "johndoe@email.com"
                )
            ),
        )

        assertThat(BacsMandateData.fromConfirmationOption(option)).isEqualTo(
            BacsMandateData(
                name = "John Doe",
                email = "johndoe@email.com",
                accountNumber = "00012345",
                sortCode = "10-88-00"
            )
        )
    }

    @Test
    fun `when payment option is Bacs but without name or email, 'fromConfirmationOption' should return null`() {
        val option = createPaymentConfirmationOption(
            createParams = PaymentMethodCreateParams.Companion.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "10-88-00"
                ),
                billingDetails = PaymentMethod.BillingDetails()
            ),
        )

        assertThat(BacsMandateData.fromConfirmationOption(option)).isNull()
    }

    @Test
    fun `when payment option is not Bacs, 'fromConfirmationOption' should return null`() {
        val option = createPaymentConfirmationOption(
            createParams = PaymentMethodCreateParams.Companion.create(
                card = PaymentMethodCreateParams.Card(),
                billingDetails = PaymentMethod.BillingDetails()
            ),
        )

        assertThat(BacsMandateData.fromConfirmationOption(option)).isNull()
    }

    private fun createPaymentConfirmationOption(
        createParams: PaymentMethodCreateParams,
    ): PaymentConfirmationOption.BacsPaymentMethod {
        return PaymentConfirmationOption.BacsPaymentMethod(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123",
            ),
            shippingDetails = null,
            createParams = createParams,
            optionsParams = null,
            appearance = PaymentSheetFixtures.CONFIG_CUSTOMER.appearance,
        )
    }
}
