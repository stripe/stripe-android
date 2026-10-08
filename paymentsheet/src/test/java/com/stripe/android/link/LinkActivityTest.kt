package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.assertNoUnverifiedIntents
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.wallet.AddPaymentMethodOptions
import com.stripe.android.link.ui.wallet.WALLET_SCREEN_PAY_ANOTHER_WAY_BUTTON
import com.stripe.android.link.utils.TestNavigationManager
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteLauncher
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityTest {
    private val dispatcher = StandardTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<LinkActivity>()

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `finishes with a cancelled result when no arg is passed`() {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), LinkActivity::class.java)

        val scenario = ActivityScenario.launchActivityForResult<LinkActivity>(intent)

        assertThat(scenario.result.resultCode)
            .isEqualTo(Activity.RESULT_CANCELED)
        assertNoUnverifiedIntents()
    }

    @Test
    fun `verification dialog is displayed when link screen state is VerificationDialog`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification())

        setupActivityController(
            linkLaunchMode = LinkLaunchMode.Full(showSecondaryButton = true),
            use2faDialog = true,
            linkAccountManager = linkAccountManager
        )

        dispatcher.scheduler.advanceUntilIdle()

        verificationDialog()
            .assertExists()
        fullScreenContent()
            .assertDoesNotExist()
    }

    @Test
    fun `full screen content is displayed when link screen state is FullScreen`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification())

        setupActivityController(
            linkLaunchMode = LinkLaunchMode.Full(showSecondaryButton = true),
            use2faDialog = false,
            linkAccountManager = linkAccountManager
        )

        dispatcher.scheduler.advanceUntilIdle()

        verificationDialog()
            .assertDoesNotExist()
        fullScreenContent()
            .assertIsDisplayed()
    }

    @Test
    fun `secondary button is displayed when full mode shows it`() {
        verifySecondaryButtonVisibility(showSecondaryButton = true)
    }

    @Test
    fun `secondary button is not displayed when full mode hides it`() {
        verifySecondaryButtonVisibility(showSecondaryButton = false)
    }

    private fun verifySecondaryButtonVisibility(showSecondaryButton: Boolean) = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        linkAccountManager.setAccountStatus(AccountStatus.Verified(consentPresentation = null))

        setupActivityController(
            linkLaunchMode = LinkLaunchMode.Full(showSecondaryButton = showSecondaryButton),
            use2faDialog = false,
            linkAccountManager = linkAccountManager,
        )

        dispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        dispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        val secondaryButton = composeTestRule.onNodeWithTag(
            testTag = WALLET_SCREEN_PAY_ANOTHER_WAY_BUTTON,
            useUnmergedTree = true,
        )
        if (showSecondaryButton) {
            secondaryButton.performScrollTo().assertIsDisplayed()
        } else {
            secondaryButton.assertDoesNotExist()
        }
    }

    private fun verificationDialog() = composeTestRule
        .onNodeWithTag(VERIFICATION_DIALOG_CONTENT_TAG)

    private fun fullScreenContent() = composeTestRule
        .onNodeWithTag(FULL_SCREEN_CONTENT_TAG)

    private fun setupActivityController(
        linkLaunchMode: LinkLaunchMode,
        use2faDialog: Boolean = true,
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager()
    ): LinkActivity {
        val linkExpressMode = if (use2faDialog) LinkExpressMode.ENABLED else LinkExpressMode.DISABLED
        val intent = LinkActivity.createIntent(
            context = context,
            args = TestFactory.NATIVE_LINK_ARGS.copy(
                linkExpressMode = linkExpressMode,
                launchMode = linkLaunchMode,
            )
        )

        val activityController = Robolectric.buildActivity(LinkActivity::class.java, intent)

        activityController.get().viewModelFactory = linkViewModelFactory(
            linkLaunchMode = linkLaunchMode,
            linkExpressMode = linkExpressMode,
            linkAccountManager = linkAccountManager
        )

        return activityController
            .setup()
            .get()
    }

    private fun linkViewModelFactory(
        linkLaunchMode: LinkLaunchMode,
        linkExpressMode: LinkExpressMode = LinkExpressMode.ENABLED,
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager()
    ): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            LinkActivityViewModel(
                fraudDetectionDataRepository = mock(),
                activityRetainedComponent = FakeNativeLinkComponent(
                    linkAccountManager = linkAccountManager,
                    linkLaunchMode = linkLaunchMode,
                    addPaymentMethodOptionsFactory = addPaymentMethodOptionsFactory(linkLaunchMode),
                    viewModel = mock {
                        on { confirmationHandler } doReturn FakeConfirmationHandler()
                    },
                ),
                confirmationHandlerFactory = { FakeConfirmationHandler() },
                linkAccountManager = linkAccountManager,
                linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
                eventReporter = FakeEventReporter(),
                linkAttestationCheck = FakeLinkAttestationCheck(),
                savedStateHandle = SavedStateHandle(),
                linkConfiguration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                linkExpressMode = linkExpressMode,
                navigationManager = TestNavigationManager(),
                linkLaunchMode = linkLaunchMode,
                linkConfirmationHandlerFactory = { FakeLinkConfirmationHandler() },
                autocompleteLauncher = TestAutocompleteLauncher.noOp(),
                addPaymentMethodOptionsFactory = mock()
            )
        }
    }

    private fun addPaymentMethodOptionsFactory(
        linkLaunchMode: LinkLaunchMode,
    ): AddPaymentMethodOptions.Factory {
        return object : AddPaymentMethodOptions.Factory {
            override fun create(linkAccount: LinkAccount): AddPaymentMethodOptions {
                return AddPaymentMethodOptions(
                    linkAccount = linkAccount,
                    configuration = TestFactory.LINK_CONFIGURATION,
                    linkLaunchMode = linkLaunchMode,
                )
            }
        }
    }
}
