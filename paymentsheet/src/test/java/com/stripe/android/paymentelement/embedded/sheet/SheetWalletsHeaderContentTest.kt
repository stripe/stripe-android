package com.stripe.android.paymentelement.embedded.sheet

import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SheetWalletsHeaderContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext<Context>(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )
    }

    @Test
    fun `wallet buttons invoke their click actions`() = runScenario {
        composeRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).performClick()
        composeRule.onNodeWithTag(LinkButtonTestTag).performClick()

        assertThat(googlePayClicks.awaitItem()).isEqualTo(Unit)
        assertThat(linkClicks.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `disabled wallet buttons do not invoke their click actions`() = runScenario(buttonsEnabled = false) {
        composeRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertIsNotEnabled().performClick()
        composeRule.onNodeWithTag(LinkButtonTestTag).assertIsNotEnabled().performClick()

        googlePayClicks.expectNoEvents()
        linkClicks.expectNoEvents()
    }

    private fun runScenario(
        buttonsEnabled: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val googlePayClicks = Turbine<Unit>()
        val linkClicks = Turbine<Unit>()
        composeRule.setContent {
            SheetWalletsHeaderContent(
                state = createWalletsState(
                    buttonsEnabled = buttonsEnabled,
                    onGooglePayPressed = { googlePayClicks.add(Unit) },
                    onLinkPressed = { linkClicks.add(Unit) },
                ),
                dividerSpacing = 16.dp,
            )
        }

        Scenario(googlePayClicks, linkClicks).block()
        googlePayClicks.ensureAllEventsConsumed()
        linkClicks.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val googlePayClicks: Turbine<Unit>,
        val linkClicks: Turbine<Unit>,
    )

    private fun createWalletsState(
        buttonsEnabled: Boolean,
        onGooglePayPressed: () -> Unit,
        onLinkPressed: () -> Unit,
    ): WalletsState {
        return WalletsState(
            link = WalletsState.Link(
                state = LinkButtonState.Default,
                linkBrand = LinkBrand.Link,
            ),
            googlePay = WalletsState.GooglePay(
                apiConfiguration = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = false,
                billingAddressParameters = null,
                additionalEnabledNetworks = emptyList(),
            ),
            walletsAllowedInHeader = WalletType.entries,
            buttonsEnabled = buttonsEnabled,
            dividerTextResource = R.string.stripe_paymentsheet_or_pay_using,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
            onGooglePayPressed = onGooglePayPressed,
            onLinkPressed = onLinkPressed,
        )
    }
}
