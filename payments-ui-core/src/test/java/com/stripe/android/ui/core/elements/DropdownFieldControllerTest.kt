package com.stripe.android.ui.core.elements

import androidx.compose.ui.autofill.ContentType
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
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
    private val countryConfig = CountryConfig(locale = Locale.US, autofillType = ContentType.AddressCountry)
    private val controller = DropdownFieldController(countryConfig)

    @Test
    fun `an option with a null API value is still a complete selection`() {
        val controller = simpleController(
            DropdownItem(displayText = "No preference", apiValue = null),
            DropdownItem(displayText = "Other", apiValue = "other"),
        )

        assertThat(controller.rawFieldValue.value).isNull()
        assertThat(controller.fieldValue.value).isEqualTo("No preference")
        assertThat(controller.selectedIndex.value).isEqualTo(0)
        assertThat(controller.isComplete.value).isTrue()
    }

    @Test
    fun `options sharing an API value retain their selected label and index`() {
        val controller = simpleController(
            DropdownItem(displayText = "First", apiValue = "same"),
            DropdownItem(displayText = "Second", apiValue = "same"),
        )

        controller.onValueChange(1)

        assertThat(controller.rawFieldValue.value).isEqualTo("same")
        assertThat(controller.selectedItemLabel.value).isEqualTo("Second")
        assertThat(controller.selectedIndex.value).isEqualTo(1)
    }

    @Test
    fun `raw updates and autofill retain their matching precedence`() {
        val controller = simpleController(
            DropdownItem(displayText = "second", apiValue = "first"),
            DropdownItem(displayText = "Other", apiValue = "second"),
        )

        controller.onRawValueChange("second")
        assertThat(controller.rawFieldValue.value).isEqualTo("second")

        controller.onAutofillValue("SECOND")
        assertThat(controller.rawFieldValue.value).isEqualTo("first")
    }

    private fun simpleController(vararg items: DropdownItem) = DropdownFieldController(
        SimpleDropdownConfig(resolvableString(R.string.stripe_blank_and_required), items.toList())
    )

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
            mode = DropdownConfig.Mode.Condensed,
            autofillType = ContentType.AddressCountry,
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
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = true),
            autofillType = ContentType.AddressCountry,
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
            autofillType = ContentType.AddressCountry,
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
            autofillType = ContentType.AddressCountry,
        )
        val controller = DropdownFieldController(countryConfig)

        controller.validationMessage.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Verify error when no selection is made and validating`() = runTest {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false),
            autofillType = ContentType.AddressCountry,
        )
        val controller = DropdownFieldController(countryConfig)

        controller.validationMessage.test {
            assertThat(awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            assertThat(awaitItem()?.message).isEqualTo(R.string.stripe_blank_and_required)
        }
    }

    @Test
    fun `Verify no error when selection is made`() = runTest {
        val countryConfig = CountryConfig(
            locale = Locale.US,
            mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false),
            autofillType = ContentType.AddressCountry,
        )
        val controller = DropdownFieldController(countryConfig)

        controller.validationMessage.test {
            assertThat(awaitItem()).isNull()

            controller.onValueChange(0)
            expectNoEvents()

            controller.onValidationStateChanged(true)

            expectNoEvents()
        }
    }
}
