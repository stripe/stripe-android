package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.DropdownElement
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Locale

class DropdownElementTest {
    private val dropdownElement = DropdownElement(CountryConfig(Locale.US))

    @Test
    fun `Verify that when the selected index changes the paymentMethod param value updates`() =
        runBlocking {
            assertThat(dropdownElement.paymentMethodParams.first()).isEqualTo("US")
            dropdownElement.onValueChange(1)
            assertThat(dropdownElement.paymentMethodParams.first()).isEqualTo("AF")
        }

    @Test
    fun `Verify display items gets the display items form the config`() {
        assertThat(dropdownElement.displayItems).isEqualTo(CountryConfig().getDisplayItems())
    }

    @Test
    fun `Verify label gets the label from the config`() {
        assertThat(dropdownElement.label).isEqualTo(CountryConfig().label)
    }

    @Test
    fun `Verify dropdown is always complete`() = runBlocking {
        assertThat(dropdownElement.isComplete.first()).isEqualTo(true)
    }
}