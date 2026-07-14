package com.stripe.android.uicore.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DropdownFieldControllerTest {

    private val controller = DropdownFieldController(
        AdministrativeAreaConfig(AdministrativeAreaConfig.Country.US())
    )

    @Test
    fun `onAutofillValue with abbreviation selects matching state`() {
        controller.onAutofillValue("CA")
        assertThat(controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `onAutofillValue with full name selects matching state`() {
        controller.onAutofillValue("California")
        assertThat(controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `onAutofillValue with full name is case insensitive`() {
        controller.onAutofillValue("california")
        assertThat(controller.rawFieldValue.value).isEqualTo("CA")
    }
}
