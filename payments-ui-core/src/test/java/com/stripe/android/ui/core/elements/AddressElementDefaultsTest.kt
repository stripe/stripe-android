package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.FormFieldId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AddressElementDefaultsTest {
    @Test
    fun `raw lowercase state code is serialized with canonical country and state codes`() = runTest {
        val addressElement = AddressElement(
            FormFieldId.Generic("address"),
            rawValuesMap = mapOf(
                FormFieldId.Country to "US",
                FormFieldId.State to "wa",
            ),
            countryCodes = setOf("US"),
            sameAsShippingElement = null,
            shippingValuesMap = null,
        )

        val formValues = addressElement.getFormFieldValueFlow().value.toMap()

        assertThat(formValues[FormFieldId.Country]?.value).isEqualTo("US")
        assertThat(formValues[FormFieldId.State]?.value).isEqualTo("WA")
    }

    @Test
    fun `raw state name is serialized with canonical country and state codes`() = runTest {
        val addressElement = AddressElement(
            FormFieldId.Generic("address"),
            rawValuesMap = mapOf(
                FormFieldId.Country to "US",
                FormFieldId.State to "Washington",
            ),
            countryCodes = setOf("US"),
            sameAsShippingElement = null,
            shippingValuesMap = null,
        )

        val formValues = addressElement.getFormFieldValueFlow().value.toMap()

        assertThat(formValues[FormFieldId.Country]?.value).isEqualTo("US")
        assertThat(formValues[FormFieldId.State]?.value).isEqualTo("WA")
    }

    @Test
    fun `raw country name is serialized as canonical country code`() = runTest {
        val defaultLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val countryCodes = setOf("AT", "IT")
            val config = CountryConfig(countryCodes, Locale.US)
            assertThat(config.rawItems.indexOf("IT")).isGreaterThan(0)

            val addressElement = AddressElement(
                FormFieldId.Generic("address"),
                rawValuesMap = mapOf(FormFieldId.Country to "Italy"),
                countryCodes = countryCodes,
                sameAsShippingElement = null,
                shippingValuesMap = null,
            )
            val formValues = addressElement.getFormFieldValueFlow().value.toMap()

            assertThat(formValues[FormFieldId.Country]?.value).isEqualTo("IT")
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }
}
