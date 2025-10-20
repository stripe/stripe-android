package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale

class DropdownFieldControllerTest {
    private val countryConfig = CountryConfig(locale = Locale.US)
    private val controller = DropdownFieldController(countryConfig)

    @Test
    fun `Verify that when the selected index changes the paymentMethod param value updates`() =
        runBlocking {
            assertThat(controller.fieldValue.first()).isEqualTo("🇺🇸 United States")
            controller.onValueChange(1)
            assertThat(controller.fieldValue.first()).isEqualTo("🇦🇫 Afghanistan")
        }

    @Test
    fun `Verify display items gets the display items form the config`() {
        assertThat(controller.displayItems).isEqualTo(countryConfig.displayItems)
    }

    @Test
    fun `Verify label gets the label from the config`() = runBlocking {
        assertThat(controller.label.first()).isEqualTo(countryConfig.label)
    }

    @Test
    fun `Verify 'tinyMode' is true & complete when mode is 'Condensed'`() = runBlocking {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Condensed
        )
        val controller = DropdownFieldController(countryConfig)

        assertThat(controller.tinyMode).isTrue()
        assertThat(controller.selectedIndex.value).isEqualTo(0)
        assertThat(controller.isComplete.value).isTrue()
    }

    @Test
    fun `Verify 'tinyMode' is false & complete when mode is 'Full'`() = runBlocking {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = true)
        )
        val controller = DropdownFieldController(countryConfig)

        assertThat(controller.tinyMode).isFalse()
        assertThat(controller.selectedIndex.value).isEqualTo(0)
        assertThat(controller.isComplete.value).isTrue()
    }

    @Test
    fun `Verify 'tinyMode' is false & not complete when mode is 'Full' and does select first option`() = runBlocking {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false),
        )
        val controller = DropdownFieldController(countryConfig)

        assertThat(controller.tinyMode).isFalse()
        assertThat(controller.selectedIndex.value).isNull()
        assertThat(controller.isComplete.value).isFalse()
    }

    @Test
    fun `Verify no error when no selection is made and not validating`() = runTest {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false),
        )
        val controller = DropdownFieldController(countryConfig)

        controller.error.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Verify error when no selection is made and validating`() = runTest {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false),
        )
        val controller = DropdownFieldController(countryConfig)

        controller.error.test {
            assertThat(awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            assertThat(awaitItem()?.errorMessage).isEqualTo(R.string.stripe_blank_and_required)
        }
    }

    @Test
    fun `Verify no error when selection is made`() = runTest {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false),
        )
        val controller = DropdownFieldController(countryConfig)

        controller.error.test {
            assertThat(awaitItem()).isNull()

            controller.onValueChange(0)
            expectNoEvents()

            controller.onValidationStateChanged(true)

            expectNoEvents()
        }
    }
}
