package com.stripe.android.uicore.elements

import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.uicore.utils.collectAsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddressElementUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /*
     * This test ensures that there is no regression to 'AddressElement' behavior in which
     * input is reset when the user is typing.
     */
    @Test
    fun `On input updated, input is not reset in 'AddressElement' when observed`() {
        val element = AddressElement(
            _identifier = IdentifierSpec.BillingAddress,
            rawValuesMap = mapOf(
                IdentifierSpec.Line1 to "123 Main Street",
                IdentifierSpec.Line2 to "456",
                IdentifierSpec.City to "San Francisco",
                IdentifierSpec.State to "CA",
                IdentifierSpec.Country to "US",
                IdentifierSpec.PostalCode to "94111",
            ),
            countryCodes = setOf("US"),
            sameAsShippingElement = SameAsShippingElement(
                IdentifierSpec.SameAsShipping,
                SameAsShippingController(false)
            ),
            shippingValuesMap = mapOf(
                IdentifierSpec.Country to "US"
            ),
        )

        val formValues = element.getFormFieldValueFlow()

        composeTestRule.setContent {
            val formValuesState by formValues.collectAsState()

            Text(formValuesState.toString())

            AddressElementUI(
                enabled = true,
                controller = element.controller,
                hiddenIdentifiers = setOf(),
                lastTextFieldIdentifier = null,
            )
        }

        composeTestRule.onNodeWithText("123 Main Street")
            .performTextReplacement("123 Main ")

        composeTestRule.onNodeWithText("123 Main ")
            .performTextInput("Road")

        composeTestRule.onNodeWithText("123 Main Road").assertExists()
        composeTestRule.onNodeWithText("123 Main Street").assertDoesNotExist()
    }
}
