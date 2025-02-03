package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import com.stripe.android.link.account.FakeLinkAuth
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<LinkActivity>()

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

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
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        setupActivityController(
            use2faDialog = true,
            linkAccountManager = linkAccountManager
        )

        verificationDialog()
            .assertExists()
        fullScreenContent()
            .assertDoesNotExist()
    }

    @Test
    fun `full screen content is displayed when link screen state is FullScreen`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        setupActivityController(
            use2faDialog = false,
            linkAccountManager = linkAccountManager
        )

        verificationDialog()
            .assertDoesNotExist()
        fullScreenContent()
            .assertIsDisplayed()
    }

    private fun verificationDialog() = composeTestRule
        .onNodeWithTag(VERIFICATION_DIALOG_CONTENT_TAG)

    private fun fullScreenContent() = composeTestRule
        .onNodeWithTag(FULL_SCREEN_CONTENT_TAG)

    private fun setupActivityController(
        use2faDialog: Boolean = true,
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager()
    ): LinkActivity {
        val intent = LinkActivity.createIntent(
            context = context,
            args = TestFactory.NATIVE_LINK_ARGS.copy(
                startWithVerificationDialog = use2faDialog
            )
        )

        val activityController = Robolectric.buildActivity(LinkActivity::class.java, intent)

        activityController.get().viewModelFactory = linkViewModelFactory(
            use2faDialog = use2faDialog,
            linkAccountManager = linkAccountManager
        )

        return activityController
            .create()
            .start()
            .resume()
            .visible()
            .get()
    }

    private fun linkViewModelFactory(
        use2faDialog: Boolean = true,
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager()
    ): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            LinkActivityViewModel(
                activityRetainedComponent = FakeNativeLinkComponent(),
                confirmationHandlerFactory = { FakeConfirmationHandler() },
                linkAccountManager = linkAccountManager,
                linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
                eventReporter = FakeEventReporter(),
                integrityRequestManager = FakeIntegrityRequestManager(),
                linkGate = FakeLinkGate(),
                errorReporter = FakeErrorReporter(),
                linkAuth = FakeLinkAuth(),
                linkConfiguration = TestFactory.LINK_CONFIGURATION,
                startWithVerificationDialog = use2faDialog
            )
        }
    }
}
