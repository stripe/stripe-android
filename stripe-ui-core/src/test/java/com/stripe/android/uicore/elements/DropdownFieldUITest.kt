package com.stripe.android.uicore.elements

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class DropdownFieldUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `country code autofill updates selection and canonical value`() {
        val controller = countryController(setOf("AT", "IT"), Locale.US)

        runScenario(controller) {
            assertCountryAutofill()
            autofill("IT")

            assertThat(controller.rawFieldValue.value).isEqualTo("IT")
            composeTestRule.onNodeWithText("Italy").assertIsDisplayed()
        }
    }

    @Test
    fun `undecorated country name autofill updates selection and canonical value`() {
        val controller = countryController(setOf("AT", "IT"), Locale.US)

        runScenario(controller) {
            assertCountryAutofill()
            autofill("Italy")

            assertThat(controller.rawFieldValue.value).isEqualTo("IT")
            composeTestRule.onNodeWithText("Italy").assertIsDisplayed()
        }
    }

    @Test
    fun `localized country name autofill uses the configured locale`() {
        val controller = countryController(setOf("AT", "IT"), Locale.FRANCE)

        runScenario(controller) {
            assertCountryAutofill()
            autofill("Italie")

            assertThat(controller.rawFieldValue.value).isEqualTo("IT")
            composeTestRule.onNodeWithText("Italie").assertIsDisplayed()
        }
    }

    @Test
    fun `country autofill preserves the allowed country list`() {
        val controller = countryController(setOf("AT", "US"), Locale.US)

        runScenario(controller) {
            assertCountryAutofill()
            autofill("Italy")

            assertThat(controller.rawFieldValue.value).isEqualTo("US")
            composeTestRule.onNodeWithText("United States").assertIsDisplayed()
        }
    }

    @Test
    fun `country selector without an autofill type does not register autofill`() {
        val controller = DropdownFieldController(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT", "IT"),
                locale = Locale.US,
                autofillType = null,
            ),
            initialValue = "AT",
        )

        runScenario(controller) {
            assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentType))
            assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnAutofillText))
        }
    }

    @Test
    fun `phone number keeps its national hint without registering address country`() {
        val controller = PhoneNumberController.createPhoneNumberController(
            initiallySelectedCountryCode = "US",
        )

        composeTestRule.setContent {
            PhoneNumberElementUI(enabled = true, controller = controller)
        }

        composeTestRule.onNodeWithTag("DropDown:tiny").assertIsDisplayed()
        composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.AddressCountry),
            useUnmergedTree = true,
        ).assertCountEquals(0)
        composeTestRule.onNodeWithTag(PHONE_NUMBER_TEXT_FIELD_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.PhoneNumberNational))
    }

    @Test
    fun `state autofill retains its region hint and canonical value`() {
        val controller = DropdownFieldController(
            AdministrativeAreaConfig(AdministrativeAreaConfig.Country.US()),
            initialValue = "AL",
        )

        runScenario(controller) {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.AddressRegion))
            autofill("WASHINGTON")

            assertThat(controller.rawFieldValue.value).isEqualTo("WA")
            composeTestRule.onNodeWithText("Washington").assertIsDisplayed()
        }
    }

    private fun countryController(countryCodes: Set<String>, locale: Locale): DropdownFieldController {
        val controller = DropdownFieldController(
            CountryConfig(
                onlyShowCountryCodes = countryCodes,
                locale = locale,
            ),
            initialValue = "AT",
        )
        assertThat(controller.rawFieldValue.value).isEqualTo("AT")
        return controller
    }

    private fun runScenario(controller: DropdownFieldController, block: SemanticsNodeInteraction.() -> Unit) {
        composeTestRule.setContent {
            DropDown(
                controller = controller,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG).block()
    }

    private fun SemanticsNodeInteraction.assertCountryAutofill() {
        assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.AddressCountry))
        assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDataType, ContentDataType.Text))
    }

    private fun SemanticsNodeInteraction.autofill(value: String) {
        performSemanticsAction(SemanticsActions.OnAutofillText) { action ->
            assertThat(action(AnnotatedString(value))).isTrue()
        }
    }

    private companion object {
        const val TEST_TAG = "AutofillDropdown"
    }
}
