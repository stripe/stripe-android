package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Locale

class DropdownFieldControllerTest {
    private val countryConfig = CountryConfig(locale = Locale.US)
    private val controller = DropdownFieldController(countryConfig)

    @Test
    fun `Verify that when the selected index changes the paymentMethod param value updates`() =
        runBlocking {
            assertThat(controller.fieldValue.first()).isEqualTo("United States")
            controller.onValueChange(1)
            assertThat(controller.fieldValue.first()).isEqualTo("Afghanistan")
        }

    @Test
    fun `Verify display items gets the display items form the config`() {
        assertThat(controller.displayItems).isEqualTo(countryConfig.getDisplayItems())
    }

    @Test
    fun `Verify label gets the label from the config`() {
        assertThat(controller.label).isEqualTo(countryConfig.label)
    }

    @Test
    fun `Verify dropdown is always complete`() = runBlocking {
        assertThat(controller.isComplete.first()).isEqualTo(true)
    }
}
