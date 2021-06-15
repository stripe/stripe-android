package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Locale

class DropdownControllerTest {
    private val countryConfig = CountryConfig(Locale.US)
    private val dropdownController = DropdownFieldController(countryConfig)

    @Test
    fun `Verify that when the selected index changes the paymentMethod param value updates`() =
        runBlocking {
            assertThat(dropdownController.fieldValue.first()).isEqualTo("United States")
            dropdownController.onValueChange(1)
            assertThat(dropdownController.fieldValue.first()).isEqualTo("Afghanistan")
        }

    @Test
    fun `Verify display items gets the display items form the config`() {
        assertThat(dropdownController.displayItems).isEqualTo(countryConfig.getDisplayItems())
    }

    @Test
    fun `Verify label gets the label from the config`() {
        assertThat(dropdownController.label).isEqualTo(countryConfig.label)
    }

    @Test
    fun `Verify dropdown is always complete`() = runBlocking {
        assertThat(dropdownController.isComplete.first()).isEqualTo(true)
    }
}