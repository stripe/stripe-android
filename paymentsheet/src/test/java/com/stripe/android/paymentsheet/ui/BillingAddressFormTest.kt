package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class BillingAddressFormTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun test() = runScenario {
        val address = PaymentMethodFixtures.BILLING_DETAILS.address
        formFieldsState.test {
            val item = awaitItem()
            item.line1.isEqualTo(address?.line1)
            item.line2.isEqualTo(address?.line2)
            item.city.isEqualTo(address?.city)
            item.state.isEqualTo(address?.state)
            item.postalCode.isEqualTo(address?.postalCode)
            item.country.isEqualTo(address?.country)
        }
    }

    @Test
    fun test2() = runScenario(
        addressCollectionMode = AddressCollectionMode.Never
    ) {
        val address = PaymentMethodFixtures.BILLING_DETAILS.address
        formFieldsState.test {
            val item = awaitItem()
            item.line1.isNull()
            item.line2.isNull()
            item.city.isNull()
            item.state.isNull()
            item.postalCode.isNull()
            item.country.isEqualTo(address?.country)
        }
    }

    @Test
    fun test3() = runScenario(
        addressCollectionMode = AddressCollectionMode.Automatic
    ) {
        val address = PaymentMethodFixtures.BILLING_DETAILS.address
        formFieldsState.test {
            val item = awaitItem()
            item.line1.isNull()
            item.line2.isNull()
            item.city.isNull()
            item.state.isNull()
            item.postalCode.isEqualTo(address?.postalCode)
            item.country.isEqualTo(address?.country)
        }
    }

    private fun FormFieldEntry?.isEqualTo(other: Any?) {
        assertThat(this?.value?.takeIf { it.isNotBlank() }).isEqualTo(other)
    }

    private fun FormFieldEntry?.isNull() {
        assertThat(this).isNull()
    }

    private fun runScenario(
        billingDetails: PaymentMethod.BillingDetails? = PaymentMethodFixtures.BILLING_DETAILS,
        addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Full,
        block: suspend BillingAddressForm.() -> Unit
    ) = runTest(testDispatcher) {
        val form = BillingAddressForm(
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode
        )
        block(form)
    }
}