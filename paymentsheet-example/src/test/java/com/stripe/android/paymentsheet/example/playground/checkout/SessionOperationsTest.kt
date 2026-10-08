@file:OptIn(
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
)

package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SessionOperationsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `current shipping address appears below email controls`() = runScenario(
        shippingAddress = shippingAddress(),
    ) {
        page.shippingAddress.assertIsDisplayed()
        page.shippingAddress.assertTextEquals(
            "Shipping address",
            "John Doe\n123 Main St\nApt 4\nSan Francisco CA 94103\nUS",
        )
        assertThat(page.shippingAddress.fetchSemanticsNode().boundsInRoot.top)
            .isAtLeast(page.updateEmail.fetchSemanticsNode().boundsInRoot.bottom)
    }

    @Test
    fun `shipping address reflects updated and cleared session details`() = runScenario(
        shippingAddress = shippingAddress(),
    ) {
        page.shippingAddress.assertTextContains("123 Main St", substring = true)

        updateShippingAddress(shippingAddress(name = "Jane Doe", line1 = "456 Market St"))

        page.shippingAddress.assertTextContains("Jane Doe", substring = true)
        page.shippingAddress.assertTextContains("456 Market St", substring = true)
        composeRule.onNodeWithText("John Doe", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("123 Main St", substring = true).assertDoesNotExist()

        updateShippingAddress(null)

        page.shippingAddress.assertTextEquals("Shipping address", "No shipping address")
        composeRule.onNodeWithText("Jane Doe", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("456 Market St", substring = true).assertDoesNotExist()
    }

    @Test
    fun `partial shipping address omits absent and blank fields`() = runScenario(
        shippingAddress = shippingAddress(
            name = "",
            line1 = null,
            line2 = " ",
            city = null,
            postalCode = null,
        ),
    ) {
        page.shippingAddress.assertTextEquals("Shipping address", "CA\nUS")
    }

    private fun runScenario(
        shippingAddress: Session.ShippingAddress?,
        block: Scenario.() -> Unit,
    ) {
        val currentShippingAddress = mutableStateOf(shippingAddress)
        composeRule.setContent {
            Column {
                SessionOperations(
                    initialEmail = "customer@example.com",
                    shippingAddress = currentShippingAddress.value,
                    isUpdating = false,
                    message = null,
                    onApplyPromotionCode = {},
                    onRemovePromotionCode = {},
                    onUpdateEmail = {},
                )
            }
        }

        block(
            Scenario(
                page = SessionOperationsPage(composeRule),
                currentShippingAddress = currentShippingAddress,
            )
        )
    }

    private inner class Scenario(
        val page: SessionOperationsPage,
        private val currentShippingAddress: MutableState<Session.ShippingAddress?>,
    ) {
        fun updateShippingAddress(shippingAddress: Session.ShippingAddress?) {
            composeRule.runOnIdle {
                currentShippingAddress.value = shippingAddress
            }
            composeRule.waitForIdle()
        }
    }
}

private class SessionOperationsPage(
    composeRule: ComposeContentTestRule,
) {
    val shippingAddress = composeRule.onNodeWithTag(SHIPPING_ADDRESS_TEST_TAG)
    val updateEmail = composeRule.onNodeWithText("Update email")
}

private fun shippingAddress(
    name: String? = "John Doe",
    line1: String? = "123 Main St",
    line2: String? = "Apt 4",
    city: String? = "San Francisco",
    state: String? = "CA",
    postalCode: String? = "94103",
    country: String = "US",
): Session.ShippingAddress = ShippingAddressFixture.create(name, line1, line2, city, state, postalCode, country)
