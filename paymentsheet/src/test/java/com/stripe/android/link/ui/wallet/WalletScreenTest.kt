package com.stripe.android.link.ui.wallet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.PrimaryButtonTag
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WalletScreenTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

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
            WalletScreen(
                viewModel = viewModel,
                showBottomSheetContent = {},
                hideBottomSheetContent = {}
            )
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
            WalletScreen(
                viewModel = viewModel,
                showBottomSheetContent = {},
                hideBottomSheetContent = {}
            )
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
            WalletScreen(
                viewModel = viewModel,
                showBottomSheetContent = {},
                hideBottomSheetContent = {}
            )
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
            WalletScreen(
                viewModel = viewModel,
                showBottomSheetContent = {},
                hideBottomSheetContent = {}
            )
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
            WalletScreen(
                viewModel = viewModel,
                showBottomSheetContent = {},
                hideBottomSheetContent = {}
            )
        }

        composeTestRule.waitForIdle()

        onLoader().assertIsDisplayed()
        onPaymentMethodList().assertCountEquals(0)
    }

    @Test
    fun `wallet menu is displayed on payment method menu clicked`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            var sheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
            Box {
                WalletScreen(
                    viewModel = viewModel,
                    showBottomSheetContent = {
                        sheetContent = it
                    },
                    hideBottomSheetContent = {
                        sheetContent = null
                    }
                )

                sheetContent?.let {
                    Column { it() }
                }
            }
        }

        composeTestRule.waitForIdle()

        onCollapsedWalletRow().performClick()

        composeTestRule.waitForIdle()

        onWalletPaymentMethodMenu().assertDoesNotExist()
        onWalletPaymentMethodRowMenuButton().onLast().performClick()

        composeTestRule.waitForIdle()

        onWalletPaymentMethodMenu().assertIsDisplayed()
    }

    @Test
    fun `wallet menu is dismissed on cancel clicked`() = runTest(dispatcher) {
        testMenu(
            nodeTag = onWalletPaymentMethodMenuCancelTag()
        )
    }

    @Test
    fun `wallet menu is dismissed on remove clicked`() = runTest(dispatcher) {
        testMenu(
            nodeTag = onWalletPaymentMethodMenuRemoveTag(),
            expectedRemovedCounter = 1
        )
    }

    @Test
    fun `wallet menu is dismissed on edit clicked`() = runTest(dispatcher) {
        testMenu(
            nodeTag = onWalletPaymentMethodMenuUpdateTag(),
            expectedEditPaymentMethodCounter = 1
        )
    }

    @Test
    fun `wallet menu is dismissed on setAsDefault clicked`() = runTest(dispatcher) {
        testMenu(
            nodeTag = onWalletPaymentMethodMenuSetAsDefaultTag(),
            expectedSetAsDefaultCounter = 1
        )
    }

    private fun testMenu(
        nodeTag: SemanticsNodeInteraction,
        expectedRemovedCounter: Int = 0,
        expectedSetAsDefaultCounter: Int = 0,
        expectedEditPaymentMethodCounter: Int = 0
    ) {
        var onSetDefaultCounter = 0
        var onRemoveClickedCounter = 0
        var onEditPaymentMethodClickedCounter = 0
        composeTestRule.setContent {
            var sheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
            Box {
                TestWalletBody(
                    onSetDefaultClicked = {
                        onSetDefaultCounter += 1
                    },
                    onRemoveClicked = {
                        onRemoveClickedCounter += 1
                    },
                    onEditPaymentMethodClicked = {
                        onEditPaymentMethodClickedCounter += 1
                    },
                    showBottomSheetContent = {
                        sheetContent = it
                    },
                    hideBottomSheetContent = {
                        sheetContent = null
                    }
                )

                sheetContent?.let {
                    Column { it() }
                }
            }
        }

        composeTestRule.waitForIdle()

        onWalletPaymentMethodRowMenuButton().onLast().performClick()

        composeTestRule.waitForIdle()

        onWalletPaymentMethodMenu().assertIsDisplayed()

        nodeTag.performClick()

        composeTestRule.waitForIdle()

        onWalletPaymentMethodMenu().assertDoesNotExist()
        assertThat(onSetDefaultCounter).isEqualTo(expectedSetAsDefaultCounter)
        assertThat(onRemoveClickedCounter).isEqualTo(expectedRemovedCounter)
        assertThat(onEditPaymentMethodClickedCounter).isEqualTo(expectedEditPaymentMethodCounter)
    }

    @Composable
    private fun TestWalletBody(
        onRemoveClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onSetDefaultClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onEditPaymentMethodClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        showBottomSheetContent: (BottomSheetContent?) -> Unit,
        hideBottomSheetContent: () -> Unit
    ) {
        val paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails
            .filterIsInstance<ConsumerPaymentDetails.Card>()
            .map { it.copy(isDefault = false) }
        WalletBody(
            state = WalletUiState(
                paymentDetailsList = paymentDetails,
                selectedItem = paymentDetails.firstOrNull(),
                isProcessing = false,
                hasCompleted = false,
                primaryButtonLabel = "Buy".resolvableString
            ),
            isExpanded = true,
            onItemSelected = {},
            onExpandedChanged = {},
            onPrimaryButtonClick = {},
            onPayAnotherWayClicked = {},
            onRemoveClicked = onRemoveClicked,
            onSetDefaultClicked = onSetDefaultClicked,
            onEditPaymentMethodClicked = onEditPaymentMethodClicked,
            showBottomSheetContent = showBottomSheetContent,
            hideBottomSheetContent = hideBottomSheetContent,
            onAddNewPaymentMethodClicked = {}
        )
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        linkConfirmationHandler: LinkConfirmationHandler = FakeLinkConfirmationHandler()
    ): WalletViewModel {
        return WalletViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler,
            logger = FakeLogger(),
            navigate = {},
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

    private fun onWalletPaymentMethodRowMenuButton() =
        composeTestRule.onAllNodes(hasTestTag(WALLET_PAYMENT_DETAIL_ITEM_MENU_BUTTON), useUnmergedTree = true)

    private fun onWalletPaymentMethodMenu() =
        composeTestRule.onNodeWithTag(WALLET_SCREEN_MENU_SHEET_TAG, useUnmergedTree = true)

    private fun onWalletPaymentMethodMenuCancelTag() =
        composeTestRule.onNodeWithTag(WALLET_MENU_CANCEL_TAG, useUnmergedTree = true)

    private fun onWalletPaymentMethodMenuRemoveTag() =
        composeTestRule.onNodeWithTag(WALLET_MENU_REMOVE_ITEM_TAG, useUnmergedTree = true)

    private fun onWalletPaymentMethodMenuUpdateTag() =
        composeTestRule.onNodeWithTag(WALLET_MENU_EDIT_CARD_TAG, useUnmergedTree = true)

    private fun onWalletPaymentMethodMenuSetAsDefaultTag() =
        composeTestRule.onNodeWithTag(WALLET_MENU_SET_AS_DEFAULT_TAG, useUnmergedTree = true)
}
