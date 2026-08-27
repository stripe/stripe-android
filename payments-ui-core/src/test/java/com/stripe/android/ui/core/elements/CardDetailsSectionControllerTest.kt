package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsSectionControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val coroutineTestRule = CoroutineTestRule(testDispatcher)

    private val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(coroutineTestRule)
        .around(coroutineScopeCleanupRule)

    private val coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(testDispatcher))

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `shouldHideHeader is false when card pill is not shown`() = runTest {
        val controller = createController()

        controller.shouldHideHeader.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `shouldHideHeader is true when validated scanned card shows pill`() = runTest {
        val controller = createController()

        controller.shouldHideHeader.test {
            assertThat(awaitItem()).isFalse()

            controller.onScannedCard(
                ScannedCardDetails.Validated(
                    cardNumber = "4242424242424242",
                    expirationYear = 2030,
                    expirationMonth = 6,
                )
            )

            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `shouldHideHeader stays false when unvalidated scanned card is applied`() = runTest {
        val controller = createController()

        controller.shouldHideHeader.test {
            assertThat(awaitItem()).isFalse()

            controller.onScannedCard(
                ScannedCardDetails.Unvalidated(
                    cardNumber = "4242424242424242",
                    expirationYear = 2030,
                    expirationMonth = 6,
                )
            )

            expectNoEvents()
        }
    }

    private fun createController() = CardDetailsSectionController(
        cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
        initialValues = emptyMap(),
        coroutineScope = coroutineScope,
        collectName = false,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        cardBrandFilter = DefaultCardBrandFilter,
        cardDetailsAction = null,
    )
}
