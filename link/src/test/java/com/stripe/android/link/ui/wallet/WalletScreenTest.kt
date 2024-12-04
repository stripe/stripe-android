package com.stripe.android.link.ui.wallet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WalletScreenTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun `wallet list is collapsed on start`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            WalletScreen(viewModel)
        }
        composeTestRule.waitForIdle()

        onWalletCollapsedHeader().assertIsDisplayed()
        onWalletCollapsedChevron().assertIsDisplayed()
        onWalletCollapsedPaymentDetails().assertIsDisplayed()
        onCollapsedWalletRow().assertIsDisplayed().assertHasClickAction()
        onWalletPayButton().assertIsDisplayed()
        onWalletPayAnotherWayButton().assertIsDisplayed()
    }

    @Test
    fun `wallet list is expanded on expand clicked`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            WalletScreen(viewModel)
        }

        composeTestRule.waitForIdle()

        onCollapsedWalletRow().performClick()

        composeTestRule.waitForIdle()

        onWalletAddPaymentMethodRow().assertIsDisplayed().assertHasClickAction()
        onExpandedWalletHeader().assertIsDisplayed()
        onPaymentMethodList().assertCountEquals(3)
    }

    @Test
    fun `wallet loader should be displayed when no payment method is available`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(ConsumerPaymentDetails(emptyList()))

        val viewModel = createViewModel(linkAccountManager)
        composeTestRule.setContent {
            WalletScreen(viewModel)
        }

        composeTestRule.waitForIdle()

        onLoader().assertIsDisplayed()
        onPaymentMethodList().assertCountEquals(0)
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager()
    ): WalletViewModel {
        return WalletViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkAccountManager = linkAccountManager,
            logger = FakeLogger(),
            navigateAndClearStack = {},
            dismissWithResult = {}
        )
    }

    private fun onWalletCollapsedHeader() =
        composeTestRule.onNodeWithTag(COLLAPSED_WALLET_HEADER_TAG, useUnmergedTree = true)

    private fun onWalletCollapsedChevron() =
        composeTestRule.onNodeWithTag(COLLAPSED_WALLET_CHEVRON_ICON_TAG, useUnmergedTree = true)

    private fun onWalletCollapsedPaymentDetails() =
        composeTestRule.onNodeWithTag(COLLAPSED_WALLET_PAYMENT_DETAILS_TAG, useUnmergedTree = true)

    private fun onCollapsedWalletRow() = composeTestRule.onNodeWithTag(COLLAPSED_WALLET_ROW, useUnmergedTree = true)

    private fun onWalletAddPaymentMethodRow() =
        composeTestRule.onNodeWithTag(WALLET_ADD_PAYMENT_METHOD_ROW, useUnmergedTree = true)

    private fun onPaymentMethodList() = composeTestRule.onAllNodes(hasTestTag(WALLET_SCREEN_PAYMENT_METHODS_LIST))

    private fun onExpandedWalletHeader() =
        composeTestRule.onNodeWithTag(WALLET_SCREEN_EXPANDED_ROW_HEADER, useUnmergedTree = true)

    private fun onWalletPayButton() =
        composeTestRule.onNodeWithTag(WALLET_SCREEN_PAY_BUTTON, useUnmergedTree = true)

    private fun onWalletPayAnotherWayButton() =
        composeTestRule.onNodeWithTag(WALLET_SCREEN_PAY_ANOTHER_WAY_BUTTON, useUnmergedTree = true)

    private fun onLoader() = composeTestRule.onNodeWithTag(WALLET_LOADER_TAG)
}
