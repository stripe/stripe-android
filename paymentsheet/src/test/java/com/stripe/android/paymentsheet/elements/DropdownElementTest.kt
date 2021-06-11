package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.DropdownElement
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Locale

class DropdownElementTest {
    private val countryConfig = CountryConfig(Locale.US)
    private val dropdownElement = DropdownElement(countryConfig)

    @Test
    fun `Verify that when the selected index changes the paymentMethod param value updates`() =
        runBlocking {
            assertThat(dropdownElement.paymentMethodParams.first()).isEqualTo("US")
            dropdownElement.onValueChange(1)
            assertThat(dropdownElement.paymentMethodParams.first()).isEqualTo("AF")
        }

    @Test
    fun `Verify display items gets the display items form the config`() {
        assertThat(dropdownElement.displayItems).isEqualTo(countryConfig.getDisplayItems())
    }

    @Test
    fun `Verify label gets the label from the config`() {
        assertThat(dropdownElement.label).isEqualTo(countryConfig.label)
    }

    @Test
    fun `Verify dropdown is always complete`() = runBlocking {
        assertThat(dropdownElement.isComplete.first()).isEqualTo(true)
    }
}