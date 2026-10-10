package com.stripe.android.ui.core.elements

import androidx.compose.ui.autofill.ContentType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.FormFieldId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressCountryAutofillTest {
    @Test
    fun `address defaults construct a controller with country autofill`() {
        val element = AddressElement(
            FormFieldId.Generic("address"),
            countryCodes = setOf("AT", "IT"),
            sameAsShippingElement = null,
            shippingValuesMap = null,
        )

        assertThat(element.countryElement.controller.autofillType).isEqualTo(ContentType.AddressCountry)
    }

    @Test
    fun `billing address defaults construct a controller with country autofill`() {
        val element = BillingAddressElement(
            FormFieldId.Generic("billing"),
            countryCodes = setOf("AT", "IT"),
            autocompleteAddressInteractorFactory = null,
            sameAsShippingElement = null,
            shippingValuesMap = null,
            addressCollectionMode = BillingAddressCollectionMode.Country(emptyMap()),
        )

        assertThat(element.countryElement.controller.autofillType).isEqualTo(ContentType.AddressCountry)
    }
}
