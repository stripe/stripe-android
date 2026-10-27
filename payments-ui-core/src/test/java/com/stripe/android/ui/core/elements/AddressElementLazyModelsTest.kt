package com.stripe.android.ui.core.elements

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressFieldConfiguration
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.AddressTextFieldElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.InlineAutocompleteHandler
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
class AddressElementLazyModelsTest {
    @Test
    fun `default card billing does not initialize hidden auxiliary models`() {
        val billing = BillingAddressElement(
            identifier = FormFieldId.BillingAddress,
            rawValuesMap = mapOf(FormFieldId.Country to "US"),
            autocompleteAddressInteractorFactory = null,
            sameAsShippingElement = null,
            shippingValuesMap = null,
            addressCollectionMode = cardBillingAddressCollectionMode(
                addressCollectionMode = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                requiresBillingAddressForAutomaticTax = false,
            ),
        )
        val address = billing.addressElement as AddressElement
        billing.getFormFieldValueFlow().value
        billing.onValidationStateChanged(true)
        billing.sectionFieldErrorController().validationMessage.value

        listOf("nameElement", "emailElement", "addressAutoCompleteElement", "phoneNumberElement")
            .forEach { assertThat(address.modelIsInitialized(it)).isFalse() }
    }

    @Test
    fun `hidden auxiliary models remain uninitialized across country changes`() {
        val element = createElement()

        assertThat(element.fields.value.map { it.identifier })
            .containsNoneOf(FormFieldId.Name, FormFieldId.Email, FormFieldId.Phone, FormFieldId.OneLineAddress)
        element.countryElement.controller.onRawValueChange("CA")
        element.getFormFieldValueFlow().value
        element.onValidationStateChanged(true)
        element.fields.value

        listOf("nameElement", "emailElement", "addressAutoCompleteElement", "phoneNumberElement")
            .forEach { assertThat(element.modelIsInitialized(it)).isFalse() }
    }

    @Test
    fun `visible contact fields retain initial values and configuration`(
        @TestParameter("OPTIONAL", "REQUIRED") configuration: AddressFieldConfiguration,
    ) {
        val element = createElement(
            AddressInputMode.NoAutocomplete(
                nameConfig = configuration,
                emailConfig = configuration,
                phoneNumberConfig = configuration,
            ),
            mapOf(
                FormFieldId.Country to "US",
                FormFieldId.Name to "Jane Doe",
                FormFieldId.Email to "jane@example.com",
                FormFieldId.Phone to "+16505551234",
            ),
        )

        val values = element.getFormFieldValueFlow().value.toMap()
        assertThat(values[FormFieldId.Name]?.value).isEqualTo("Jane Doe")
        assertThat(values[FormFieldId.Email]?.value).isEqualTo("jane@example.com")
        assertThat(values[FormFieldId.Phone]?.value).isEqualTo("+16505551234")
        assertThat(element.phoneNumberElement.controller.showOptionalLabel)
            .isEqualTo(configuration == AddressFieldConfiguration.OPTIONAL)
        assertThat(element.modelIsInitialized("addressAutoCompleteElement")).isFalse()
    }

    @Test
    fun `phone getter preserves initial international number parsing before country synchronization`() {
        val element = createElement(values = mapOf(FormFieldId.Country to "US", FormFieldId.Phone to "+447911123456"))

        val controller = element.phoneNumberElement.controller

        assertThat(controller.getCountryCode()).isEqualTo("GB")
        assertThat(controller.rawFieldValue.value).isEqualTo("+447911123456")
    }

    @Test
    fun `lazy contact models retain constructor values after raw address values change`() {
        val element = createElement(
            AddressInputMode.NoAutocomplete(nameConfig = AddressFieldConfiguration.OPTIONAL),
            mapOf(FormFieldId.Country to "US", FormFieldId.Name to "Initial Name"),
        )
        element.setRawValue(mapOf(FormFieldId.Country to "US", FormFieldId.Name to "Updated Name"))

        assertThat(element.getFormFieldValueFlow().value.toMap()[FormFieldId.Name]?.value).isEqualTo("Initial Name")
    }

    @Test
    fun `visible phone follows country changes`(
        @TestParameter("OPTIONAL", "REQUIRED") configuration: AddressFieldConfiguration,
    ) {
        val element = createElement(AddressInputMode.NoAutocomplete(phoneNumberConfig = configuration))
        element.fields.value
        assertThat(element.phoneNumberElement.controller.getCountryCode()).isEqualTo("US")

        element.countryElement.controller.onRawValueChange("GB")
        element.fields.value

        assertThat(element.phoneNumberElement.controller.getCountryCode()).isEqualTo("GB")
    }

    @Test
    fun `hidden phone getter uses current country and continues synchronizing`() {
        val element = createElement()
        element.countryElement.controller.onRawValueChange("CA")
        element.fields.value
        assertThat(element.modelIsInitialized("phoneNumberElement")).isFalse()

        val phone = element.phoneNumberElement
        element.fields.value
        assertThat(phone.controller.getCountryCode()).isEqualTo("CA")
        element.countryElement.controller.onRawValueChange("GB")
        element.fields.value

        assertThat(element.phoneNumberElement).isSameInstanceAs(phone)
        assertThat(phone.controller.getCountryCode()).isEqualTo("GB")
    }

    @Test
    fun `condensed autocomplete is created only in modes that display it`(
        @TestParameter mode: AutocompleteMode,
        @TestParameter placesAvailable: Boolean,
    ) {
        val element = createElement(mode.inputMode, placesAvailable = placesAvailable)
        val fields = element.fields.value
        val condensed = mode == AutocompleteMode.Inline || mode.condensed && placesAvailable
        assertThat(fields.any { it.identifier == FormFieldId.OneLineAddress }).isEqualTo(condensed)
        assertThat(element.modelIsInitialized("addressAutoCompleteElement")).isEqualTo(condensed)
    }

    @Test
    fun `inline query getter still materializes condensed autocomplete on demand`() = runTest {
        val element = createElement()
        element.fields.value
        assertThat(element.modelIsInitialized("addressAutoCompleteElement")).isFalse()

        assertThat(element.inlineQuery.value).isEmpty()
        assertThat(element.modelIsInitialized("addressAutoCompleteElement")).isTrue()
        assertThat(element.inlineQuery).isSameInstanceAs(element.inlineQuery)
    }

    @Test
    fun `expanded inline autocomplete retains query values and shipping synchronization`() {
        val handler = FakeInlineAutocompleteHandler()
        val sameAsShipping = SameAsShippingElement(FormFieldId.SameAsShipping, SameAsShippingController(false))
        val element = AddressElement(
            _identifier = FormFieldId.BillingAddress,
            rawValuesMap = mapOf(FormFieldId.Country to "US", FormFieldId.Line1 to "Original street"),
            addressInputMode = AddressInputMode.NoAutocomplete(),
            countryCodes = setOf("US", "CA"),
            sameAsShippingElement = sameAsShipping,
            shippingValuesMap = mapOf(FormFieldId.Country to "CA", FormFieldId.Line1 to "Shipping street"),
            inlineAutocompleteHandler = handler,
        )
        assertThat(element.inlineQuery.value).isEqualTo("Original street")
        val line1 = element.fields.value.single { it.identifier == FormFieldId.Line1 } as AddressTextFieldElement
        assertThat(element.inlineQuery).isSameInstanceAs(line1.inlineQuery)
        assertThat(element.modelIsInitialized("addressAutoCompleteElement")).isFalse()

        sameAsShipping.setRawValue(mapOf(FormFieldId.SameAsShipping to "true"))
        element.fields.value

        assertThat(element.countryElement.controller.rawFieldValue.value).isEqualTo("CA")
        assertThat(element.inlineQuery.value).isEqualTo("Shipping street")
        assertThat(element.modelIsInitialized("addressAutoCompleteElement")).isFalse()
        handler.calls.ensureAllEventsConsumed()
    }

    private fun createElement(
        mode: AddressInputMode = AddressInputMode.NoAutocomplete(),
        values: Map<FormFieldId, String?> = mapOf(FormFieldId.Country to "US"),
        placesAvailable: Boolean = true,
    ) = AddressElement(
        _identifier = FormFieldId.BillingAddress,
        rawValuesMap = values,
        addressInputMode = mode,
        countryCodes = setOf("US", "CA", "GB"),
        sameAsShippingElement = null,
        shippingValuesMap = null,
        isPlacesAvailable = placesAvailable,
    )

    // Inspect lazy state without adding production hooks or initializing the model through its getter.
    private fun AddressElement.modelIsInitialized(model: String): Boolean {
        val fieldName = if (model == "phoneNumberElement") "phoneNumberElementDelegate" else "$model\$delegate"
        val field = AddressElement::class.java.getDeclaredField(fieldName).apply { isAccessible = true }
        return (field.get(this) as Lazy<*>).isInitialized()
    }

    enum class AutocompleteMode(val inputMode: AddressInputMode, val condensed: Boolean) {
        Manual(AddressInputMode.NoAutocomplete(), false),
        Expanded(AddressInputMode.AutocompleteExpanded("key", setOf("US"), hidden, hidden, hidden) {}, false),
        Condensed(AddressInputMode.AutocompleteCondensed("key", setOf("US"), hidden, hidden, hidden) {}, true),
        UnsupportedCountry(
            AddressInputMode.AutocompleteCondensed("key", setOf("CA"), hidden, hidden, hidden) {},
            false,
        ),
        NoApiKey(AddressInputMode.AutocompleteCondensed(null, setOf("US"), hidden, hidden, hidden) {}, false),
        Inline(AddressInputMode.AutocompleteInline("key", setOf("US"), hidden, hidden, hidden), true),
    }

    private class FakeInlineAutocompleteHandler : InlineAutocompleteHandler {
        val calls = Turbine<String>()
        override val predictionsState = MutableStateFlow<AutocompleteAddressInteractor.InlinePredictionsState>(
            AutocompleteAddressInteractor.InlinePredictionsState.Idle
        )
        override fun onPredictionSelected(predictionId: String) = calls.add("prediction:$predictionId")
        override fun onDismissed() = calls.add("dismissed")
        override fun onFocusLost() = calls.add("focusLost")
        override fun onFocusGained() = calls.add("focusGained")
        override fun onEnterManually() = calls.add("enterManually")
        override fun onSearchActivated() = calls.add("searchActivated")
        override fun getAttributionDrawable(isDarkTheme: Boolean): Int? {
            calls.add("attribution:$isDarkTheme")
            return null
        }
    }

    private companion object {
        val hidden = AddressFieldConfiguration.HIDDEN
    }
}
