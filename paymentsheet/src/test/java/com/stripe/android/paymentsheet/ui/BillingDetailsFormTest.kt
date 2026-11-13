package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class BillingDetailsFormTest {
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

    @Test
    fun `automatic collection with automatic tax collects US tax fields`() = runScenario(
        addressCollectionMode = AddressCollectionMode.Automatic,
        requiresBillingAddressForAutomaticTax = true,
    ) {
        val address = PaymentMethodFixtures.BILLING_DETAILS.address
        formFieldsState.test {
            val item = awaitItem()
            item.line1.isEqualTo(address?.line1)
            item.line2.isNull()
            item.city.isEqualTo(address?.city)
            item.state.isEqualTo(address?.state)
            item.postalCode.isEqualTo(address?.postalCode)
            item.country.isEqualTo(address?.country)
        }
    }

    @Test
    fun `automatic collection with automatic tax collects US tax fields after changing country`() = runScenario(
        billingDetails = PaymentMethod.BillingDetails(address = Address(country = "FR")),
        addressCollectionMode = AddressCollectionMode.Automatic,
        requiresBillingAddressForAutomaticTax = true,
    ) {
        formFieldsState.test {
            val frenchItem = awaitItem()
            frenchItem.line1.isNull()
            frenchItem.city.isNull()
            frenchItem.state.isNull()
            frenchItem.postalCode.isNull()
            frenchItem.country.isEqualTo("FR")

            val billingAddressElement = addressSectionElement.fields[0] as BillingAddressElement
            billingAddressElement.countryElement.controller.onRawValueChange("US")

            val usItem = expectMostRecentItem()
            assertThat(usItem.line1).isNotNull()
            assertThat(usItem.city).isNotNull()
            assertThat(usItem.state).isNotNull()
            assertThat(usItem.postalCode).isNotNull()
            usItem.country.isEqualTo("US")
        }
    }

    @Test
    fun testAllBillingCountriesUsed() = runScenario(
        allowedCountries = emptySet()
    ) {
        assertThat(addressSectionElement.fields.size).isEqualTo(1)
        assertThat(addressSectionElement.fields.firstOrNull()).isInstanceOf<BillingAddressElement>()

        val cardBillingAddressElement = addressSectionElement.fields[0] as BillingAddressElement

        assertThat(cardBillingAddressElement.countryElement.controller.displayItems)
            .hasSize(CountryUtils.supportedBillingCountries.size)
    }

    @Test
    fun testLimitedBillingCountriesUsed() = runScenario(
        allowedCountries = setOf("US", "CA")
    ) {
        assertThat(addressSectionElement.fields.size).isEqualTo(1)
        assertThat(addressSectionElement.fields.firstOrNull()).isInstanceOf<BillingAddressElement>()

        val cardBillingAddressElement = addressSectionElement.fields[0] as BillingAddressElement

        assertThat(cardBillingAddressElement.countryElement.controller.displayItems).containsExactly(
            "\uD83C\uDDFA\uD83C\uDDF8 United States",
            "\uD83C\uDDE8\uD83C\uDDE6 Canada"
        )
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
        allowedCountries: Set<String> = emptySet(),
        requiresBillingAddressForAutomaticTax: Boolean = false,
        block: suspend BillingDetailsForm.() -> Unit
    ) = runTest(testDispatcher) {
        val form = BillingDetailsForm(
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode,
            nameCollection = NameCollection.Disabled,
            collectEmail = false,
            collectPhone = false,
            allowedBillingCountries = allowedCountries,
            autocompleteAddressInteractorFactory = null,
            requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
        )
        block(form)
    }
}
