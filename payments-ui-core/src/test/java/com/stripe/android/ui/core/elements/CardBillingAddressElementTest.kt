package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.AutocompleteAddressController
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.utils.isInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardBillingAddressElementTest {
    val dropdownFieldController = DropdownFieldController(
        CountryConfig(emptySet())
    )
    val cardBillingElement = CardBillingAddressElement(
        IdentifierSpec.Generic("billing_element"),
        rawValuesMap = emptyMap(),
        emptySet(),
        dropdownFieldController,
        null,
        null,
        null
    )

    @Test
    fun `Verify that when US is selected postal is not hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("US")
            verifyPostalShown(expectMostRecentItem())
        }
    }

    @Test
    fun `Verify that when GB is selected postal is not hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("GB")
            verifyPostalShown(expectMostRecentItem())
        }
    }

    @Test
    fun `Verify that when CA is selected postal is not hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("CA")
            verifyPostalShown(expectMostRecentItem())
        }
    }

    @Test
    fun `Verify that when DE is selected postal IS hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("DE")
            verifyPostalHidden(expectMostRecentItem())
        }
    }

    @Test
    fun `Verify that AutocompleteAddressElement is used when billing details collection is Full`() =
        autocompleteTest(
            collectionMode = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        ) { cardBillingAddressElement ->
            assertThat(cardBillingAddressElement.sectionFieldErrorController())
                .isInstanceOf<AutocompleteAddressController>()
        }

    @Test
    fun `Verify that AddressElement is used when billing details collection is Never`() = autocompleteTest(
        collectionMode = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
    ) { cardBillingAddressElement ->
        assertThat(cardBillingAddressElement.sectionFieldErrorController()).isInstanceOf<AddressController>()
    }

    @Test
    fun `Verify that AddressElement is used when billing details collection is Automatic`() = autocompleteTest(
        collectionMode = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
    ) { cardBillingAddressElement ->
        assertThat(cardBillingAddressElement.sectionFieldErrorController()).isInstanceOf<AddressController>()
    }

    fun verifyPostalShown(hiddenIdentifiers: Set<IdentifierSpec>) {
        Truth.assertThat(hiddenIdentifiers).doesNotContain(IdentifierSpec.PostalCode)
        Truth.assertThat(hiddenIdentifiers).doesNotContain(IdentifierSpec.Country)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line1)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line2)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.State)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.City)
    }

    fun verifyPostalHidden(hiddenIdentifiers: Set<IdentifierSpec>) {
        Truth.assertThat(hiddenIdentifiers).doesNotContain(IdentifierSpec.Country)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.PostalCode)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line1)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line2)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.State)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.City)
    }

    private fun autocompleteTest(
        collectionMode: BillingDetailsCollectionConfiguration.AddressCollectionMode,
        block: (CardBillingAddressElement) -> Unit,
    ) = runTest {
        block(
            CardBillingAddressElement(
                identifier = IdentifierSpec.Generic("billing_element"),
                rawValuesMap = emptyMap(),
                countryCodes = emptySet(),
                countryDropdownFieldController = dropdownFieldController,
                autocompleteAddressInteractorFactory = {
                    object : AutocompleteAddressInteractor {
                        override val interactorScope: CoroutineScope = backgroundScope

                        override val autocompleteConfig: AutocompleteAddressInteractor.Config =
                            AutocompleteAddressInteractor.Config(
                                googlePlacesApiKey = null,
                                autocompleteCountries = emptySet()
                            )

                        override val autocompleteEvent: SharedFlow<AutocompleteAddressInteractor.Event> =
                            MutableSharedFlow()

                        override fun onAutocomplete(country: String) {
                            error("Should not be called!")
                        }
                    }
                },
                sameAsShippingElement = null,
                shippingValuesMap = null,
                collectionMode = collectionMode,
            )
        )
    }
}
