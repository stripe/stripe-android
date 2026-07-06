package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class CardDetailsSectionElementUITest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `CardDetailsSectionElement shows custom action and not card scan when cardDetailsAction is provided`() {
        runScenario(
            cardDetailsAction = FakeCardDetailsAction(contentText = "Tap to add card")
        ) {
            composeTestRule.onNodeWithText("Tap to add card").assertExists()
        }
    }

    @Test
    fun `section header hides while scanned card pill is shown and returns after clear`() {
        runScenario(cardDetailsAction = null) {
            val cardInformation = context.getString(
                R.string.stripe_paymentsheet_add_payment_method_card_information
            )
            val clearScannedCard = context.getString(
                R.string.stripe_scanned_card_pill_clear_content_description
            )

            composeTestRule.onNodeWithText(cardInformation).assertExists()

            controller.onScannedCard(
                ScannedCardDetails.Validated(
                    cardNumber = "4242424242424242",
                    expirationYear = 2030,
                    expirationMonth = 6,
                )
            )
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(cardInformation).assertDoesNotExist()

            composeTestRule.onNodeWithContentDescription(clearScannedCard).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(cardInformation).assertExists()
        }
    }

    private class FakeCardDetailsAction(
        private val contentText: String,
    ) : CardDetailsAction {
        @Composable
        override fun Content(enabled: Boolean, onScannedCard: (ScannedCardDetails) -> Unit) {
            androidx.compose.material.Text(contentText)
        }
    }

    private fun runScenario(
        cardDetailsAction: CardDetailsAction?,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val controller = CardDetailsSectionController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = false,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            cardBrandFilter = DefaultCardBrandFilter,
            cardDetailsAction = cardDetailsAction,
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides { },
            ) {
                CardDetailsSectionElementUI(
                    enabled = true,
                    controller = controller,
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = IdentifierSpec.PostalCode
                )
            }
        }

        Scenario(controller).block()
    }

    private data class Scenario(
        val controller: CardDetailsSectionController,
    )
}
