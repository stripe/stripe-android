package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentMethodEmbeddedLayoutLinkInlineCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `without the card every separator shows`() = runScenario(
        paymentMethods = listOf(LINK, GOOGLE_PAY, CARD, CASH_APP),
        showsCard = false,
    ) {
        // Top, three between the rows, and bottom.
        dividers().assertCountEquals(5)
        composeRule.onNodeWithTag(rowTag("link")).assertExists()
        composeRule.onNodeWithTag(CARD_TAG).assertDoesNotExist()
    }

    @Test
    fun `card replaces the Link row`() = runScenario(
        paymentMethods = listOf(LINK, GOOGLE_PAY, CARD, CASH_APP),
    ) {
        composeRule.onNodeWithTag(CARD_TAG).assertExists()
        composeRule.onNodeWithTag(rowTag("link")).assertDoesNotExist()

        val cardBounds = composeRule.onNodeWithTag(CARD_TAG).getBoundsInRoot()
        val googlePayBounds = composeRule.onNodeWithTag(rowTag("google_pay")).getBoundsInRoot()
        assertThat(cardBounds.bottom).isLessThan(googlePayBounds.top)
    }

    @Test
    fun `card replaces only the Link row when Instant Bank Payments also uses the Link code`() = runScenario(
        paymentMethods = listOf(LINK, CARD, INSTANT_DEBITS),
    ) {
        composeRule.onAllNodesWithTag(CARD_TAG).assertCountEquals(1)
        composeRule.onNodeWithTag(rowTag("link")).assertDoesNotExist()
        composeRule.onNodeWithTag(rowTag("link_instant_debits")).assertExists()

        val cardBounds = composeRule.onNodeWithTag(CARD_TAG).getBoundsInRoot()
        val cardRowBounds = composeRule.onNodeWithTag(rowTag("card")).getBoundsInRoot()
        assertThat(cardBounds.bottom).isLessThan(cardRowBounds.top)
    }

    @Test
    fun `card at the top hides the top separator and the one below it`() = runScenario(
        paymentMethods = listOf(LINK, GOOGLE_PAY, CARD, CASH_APP),
    ) {
        dividers().assertCountEquals(3)
    }

    @Test
    fun `card between rows hides the separators above and below it`() = runScenario(
        paymentMethods = listOf(GOOGLE_PAY, LINK, CARD, CASH_APP),
    ) {
        dividers().assertCountEquals(3)
    }

    @Test
    fun `card at the bottom hides the separator above it and the bottom separator`() = runScenario(
        paymentMethods = listOf(CARD, CASH_APP, LINK),
    ) {
        // The top separator and the one between card and Cash App remain.
        dividers().assertCountEquals(2)
    }

    @Test
    fun `card below a saved payment method hides the separator between them`() = runScenario(
        paymentMethods = listOf(LINK, CARD),
        displayedSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
    ) {
        // The top and bottom separators remain.
        dividers().assertCountEquals(2)
    }

    @Test
    fun `floating rows show the card without separators`() = runScenario(
        paymentMethods = listOf(LINK, GOOGLE_PAY, CARD),
        rowStyle = RowStyle.FloatingButton.default,
    ) {
        composeRule.onNodeWithTag(CARD_TAG).assertExists()
        dividers().assertCountEquals(0)
    }

    @Test
    fun `collapsing the card restores the separators and the Link row in its place`() = runScenario(
        paymentMethods = listOf(LINK, GOOGLE_PAY, CARD, CASH_APP),
    ) {
        dividers().assertCountEquals(3)

        showsCard = false
        composeRule.waitForIdle()

        dividers().assertCountEquals(5)
        composeRule.onNodeWithTag(CARD_TAG).assertDoesNotExist()
        val linkBounds = composeRule.onNodeWithTag(rowTag("link")).getBoundsInRoot()
        val googlePayBounds = composeRule.onNodeWithTag(rowTag("google_pay")).getBoundsInRoot()
        assertThat(linkBounds.bottom).isAtMost(googlePayBounds.top)
    }

    private fun dividers() = composeRule.onAllNodesWithTag(TEST_TAG_EMBEDDED_DIVIDER)

    private fun rowTag(code: String) = "${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"

    private fun runScenario(
        paymentMethods: List<DisplayablePaymentMethod>,
        showsCard: Boolean = true,
        displayedSavedPaymentMethod: DisplayableSavedPaymentMethod? = null,
        rowStyle: RowStyle = RowStyle.FlatWithRadio.Builder()
            .topSeparatorEnabled(true)
            .bottomSeparatorEnabled(true)
            .build(),
        block: Scenario.() -> Unit,
    ) {
        val scenario = Scenario(showsCard = showsCard)
        val imageLoader = FakeStripeImageLoader()
        composeRule.setContent {
            StripeTheme {
                PaymentMethodEmbeddedLayoutUI(
                    paymentMethods = paymentMethods,
                    displayedSavedPaymentMethod = displayedSavedPaymentMethod,
                    savedPaymentMethodAction = PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE,
                    selection = null,
                    linkBrand = LinkBrand.Link,
                    isEnabled = true,
                    onViewMorePaymentMethods = {},
                    onManageOneSavedPaymentMethod = {},
                    onSelectSavedPaymentMethod = {},
                    imageLoader = imageLoader,
                    linkInlineCard = if (scenario.showsCard) {
                        { TestCard() }
                    } else {
                        null
                    },
                    appearance = Embedded(rowStyle),
                )
            }
        }
        scenario.block()
        imageLoader.ensureAllEventsConsumed()
    }

    @Composable
    private fun TestCard() {
        Box(
            modifier = Modifier
                .height(200.dp)
                .testTag(CARD_TAG)
        )
    }

    private class Scenario(showsCard: Boolean) {
        var showsCard by mutableStateOf(showsCard)
    }

    private companion object {
        const val CARD_TAG = "test_link_inline_card"

        val LINK = displayablePaymentMethod("link")
        val GOOGLE_PAY = displayablePaymentMethod("google_pay")
        val CARD = displayablePaymentMethod("card")
        val CASH_APP = displayablePaymentMethod("cashapp")
        val INSTANT_DEBITS = displayablePaymentMethod("link", syntheticCode = "link_instant_debits")

        fun displayablePaymentMethod(
            code: String,
            syntheticCode: String = code,
        ) = DisplayablePaymentMethod(
            code = code,
            syntheticCode = syntheticCode,
            displayName = code.resolvableString,
            iconResource = R.drawable.stripe_ic_paymentsheet_link_arrow,
            iconResourceNight = null,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            iconRequiresTinting = false,
            subtitle = null,
            isExpanded = false,
            onClick = {},
        )
    }
}
