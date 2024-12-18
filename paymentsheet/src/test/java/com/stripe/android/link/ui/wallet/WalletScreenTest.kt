package com.stripe.android.link.ui.wallet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.ui.PrimaryButtonTag
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
import org.mockito.kotlin.mock

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
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                paymentDetails = listOf(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                )
            )
        )
        val viewModel = createViewModel(linkAccountManager)
        composeTestRule.setContent {
            WalletScreen(viewModel)
        }
        composeTestRule.waitForIdle()

        onWalletCollapsedHeader().assertIsDisplayed()
        onWalletCollapsedChevron().assertIsDisplayed()
        onWalletCollapsedPaymentDetails().assertIsDisplayed()
        onCollapsedWalletRow().assertIsDisplayed().assertHasClickAction()
        onWalletPayButton().assertIsDisplayed().assertIsEnabled().assertHasClickAction()
        onWalletPayAnotherWayButton().assertIsDisplayed().assertIsEnabled().assertHasClickAction()
    }

    @Test
    fun `wallet list is collapsed and pay button is disabled for expired card`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                paymentDetails = listOf(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                        expiryYear = 1999
                    ),
                    TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                )
            )
        )
        val viewModel = createViewModel(linkAccountManager)
        composeTestRule.setContent {
            WalletScreen(viewModel)
        }
        composeTestRule.waitForIdle()

        onWalletCollapsedHeader().assertIsDisplayed()
        onWalletCollapsedChevron().assertIsDisplayed()
        onWalletCollapsedPaymentDetails().assertIsDisplayed()
        onCollapsedWalletRow().assertIsDisplayed().assertHasClickAction()
        onWalletPayButton().assertIsDisplayed().assertIsNotEnabled().assertHasClickAction()
        onWalletPayAnotherWayButton().assertIsDisplayed().assertIsEnabled().assertHasClickAction()
    }

    @Test
    fun `wallet list is expanded on expand clicked`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                paymentDetails = listOf(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                )
            )
        )
        val viewModel = createViewModel(linkAccountManager)
        composeTestRule.setContent {
            WalletScreen(viewModel)
        }

        composeTestRule.waitForIdle()

        onCollapsedWalletRow().performClick()

        composeTestRule.waitForIdle()

        onWalletAddPaymentMethodRow().assertIsDisplayed().assertHasClickAction()
        onExpandedWalletHeader().assertIsDisplayed()
        onPaymentMethodList().assertCountEquals(2)
        onWalletPayButton().assertIsDisplayed().assertIsEnabled().assertHasClickAction()
        onWalletPayAnotherWayButton().assertIsDisplayed().assertIsEnabled().assertHasClickAction()
    }

    @Test
    fun `wallet list is expanded and pay button should be disabled for expired card`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.success(
            ConsumerPaymentDetails(
                paymentDetails = listOf(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                        expiryYear = 1999
                    ),
                    TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                )
            )
        )
        val viewModel = createViewModel(linkAccountManager)
        composeTestRule.setContent {
            WalletScreen(viewModel)
        }

        composeTestRule.waitForIdle()

        onCollapsedWalletRow().performClick()

        composeTestRule.waitForIdle()

        onWalletAddPaymentMethodRow().assertIsDisplayed().assertHasClickAction()
        onExpandedWalletHeader().assertIsDisplayed()
        onPaymentMethodList().assertCountEquals(2)
        onWalletPayButton().assertIsDisplayed().assertIsNotEnabled().assertHasClickAction()
        onWalletPayAnotherWayButton().assertIsDisplayed().assertIsEnabled().assertHasClickAction()
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
            confirmationHandler = mock(),
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
        composeTestRule.onNodeWithTag(PrimaryButtonTag, useUnmergedTree = true)

    private fun onWalletPayAnotherWayButton() =
        composeTestRule.onNodeWithTag(WALLET_SCREEN_PAY_ANOTHER_WAY_BUTTON, useUnmergedTree = true)

    private fun onLoader() = composeTestRule.onNodeWithTag(WALLET_LOADER_TAG)
}
