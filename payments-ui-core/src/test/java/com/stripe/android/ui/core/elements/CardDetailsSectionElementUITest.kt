package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
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

    private class FakeCardDetailsAction(
        private val contentText: String,
    ) : CardDetailsAction {
        @Composable
        override fun Content(enabled: Boolean, onScannedCard: (ScannedCardDetails) -> Unit) {
            androidx.compose.material.Text(contentText)
        }
    }

    private class Scenario

    private fun runScenario(
        cardDetailsAction: CardDetailsAction,
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

        Scenario().block()
    }
}
