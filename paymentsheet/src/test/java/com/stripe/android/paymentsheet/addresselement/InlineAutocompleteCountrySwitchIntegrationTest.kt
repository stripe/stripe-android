package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.AddressFieldConfiguration
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.AutocompleteAddressController
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// End-to-end: drives the real AutocompleteAddressController + BillingInlineAutocompleteAddressInteractor
// (which uses the real InlineAutocompleteController) through a country-dropdown change, to verify the
// form's input mode toggles in BOTH directions even across the focus loss that expansion triggers.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InlineAutocompleteCountrySwitchIntegrationTest {

    @Test
    fun `switching country unsupported then back to supported reverts to inline mode`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakePlaces = FakePlacesClientProxy(
                findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
                fetchPlaceResult = Result.success(Address()),
            )
            val config = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = null,
                autocompleteCountries = setOf("US"),
                isPlacesAvailable = false,
                isInlineAutocompleteEnabled = true,
                shouldUseStripeHostedAutocomplete = true,
            )
            val interactor = BillingInlineAutocompleteAddressInteractor(
                placesClient = fakePlaces,
                autocompleteConfig = config,
                coroutineScope = backgroundScope,
            )
            val countryController = DropdownFieldController(CountryConfig(setOf("US", "JP")), "US")
            val controller = AutocompleteAddressController(
                identifier = IdentifierSpec.Generic("address"),
                initialValues = mapOf(IdentifierSpec.Country to "US"),
                countryDropdownFieldController = countryController,
                phoneNumberConfig = AddressFieldConfiguration.HIDDEN,
                nameConfig = AddressFieldConfiguration.HIDDEN,
                emailConfig = AddressFieldConfiguration.HIDDEN,
                sameAsShippingElement = null,
                shippingValuesMap = null,
                interactorFactory = { interactor },
            )

            controller.addressElementFlow.test {
                // Supported country -> inline ("condensed") autocomplete field.
                assertThat(awaitItem().addressInputMode)
                    .isInstanceOf<AddressInputMode.AutocompleteInline>()

                // Unsupported country -> form expands to manual entry.
                countryController.onRawValueChange("JP")
                advanceTimeBy(600)
                assertThat(awaitItem().addressInputMode)
                    .isInstanceOf<AddressInputMode.NoAutocomplete>()

                // Expansion removes the inline field, so it loses focus.
                interactor.onFocusLost()

                // Back to a supported country -> must revert to inline mode.
                countryController.onRawValueChange("US")
                advanceTimeBy(600)
                assertThat(awaitItem().addressInputMode)
                    .isInstanceOf<AddressInputMode.AutocompleteInline>()

                cancelAndIgnoreRemainingEvents()
            }
        }
}
