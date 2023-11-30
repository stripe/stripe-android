package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

class BacsMandateDataTest {
    @Test
    fun `when payment selection is Bacs and name & email are provided, 'fromPaymentSelection' should return data`() {
        val selection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "",
            iconResource = 0,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParams.Companion.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "10-88-00"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = "John Doe",
                    email = "johndoe@email.com"
                )
            )
        )

        assertThat(BacsMandateData.fromPaymentSelection(selection)).isEqualTo(
            BacsMandateData(
                name = "John Doe",
                email = "johndoe@email.com",
                accountNumber = "00012345",
                sortCode = "10-88-00"
            )
        )
    }

    @Test
    fun `when payment selection is Bacs but without name or email, 'fromPaymentSelection' should return null`() {
        val selection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "",
            iconResource = 0,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParams.Companion.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "10-88-00"
                ),
                billingDetails = PaymentMethod.BillingDetails()
            )
        )

        assertThat(BacsMandateData.fromPaymentSelection(selection)).isNull()
    }

    @Test
    fun `when payment selection is not Bacs, 'fromPaymentSelection' should return null`() {
        val selection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "",
            iconResource = 0,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParams.Companion.create(
                card = PaymentMethodCreateParams.Card(),
                billingDetails = PaymentMethod.BillingDetails()
            )
        )

        assertThat(BacsMandateData.fromPaymentSelection(selection)).isNull()
    }
}
