package com.stripe.android.link.ui.oauth

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.LinkAuthIntentInfo
import com.stripe.android.model.ConsentUi
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class OAuthConsentViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `init with null consent pane dismisses immediately with success`() = runTest(dispatcher) {
        val dismissedResults = mutableListOf<LinkActivityResult>()

        val linkAccountWithoutConsent = LinkAccount(
            consumerSession = TestFactory.CONSUMER_SESSION
        )

        createViewModel(
            linkAccount = linkAccountWithoutConsent,
            dismissWithResult = { dismissedResults.add(it) }
        )

        assertThat(dismissedResults).hasSize(1)
        val result = dismissedResults.first() as LinkActivityResult.Completed
        assertThat(result.linkAccountUpdate).isEqualTo(LinkAccountUpdate.Value(linkAccountWithoutConsent))
        assertThat(result.authorizationConsentGranted).isNull()
    }

    @Test
    fun `init with valid consent pane creates correct initial state`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.viewState.test {
            awaitItem().let { state ->
                assertThat(state.merchantName).isEqualTo(TestFactory.MERCHANT_NAME)
                assertThat(state.merchantLogoUrl).isNull()
                assertThat(state.userEmail).isEqualTo(TestFactory.EMAIL)
                assertThat(state.consentPane).isNotNull()
                assertThat(state.errorMessage).isNull()
            }
        }
    }

    @Test
    fun `onAllowClick with successful consent update dismisses with granted consent`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.postConsentUpdateResult = Result.success(Unit)

        val dismissedResults = mutableListOf<LinkActivityResult>()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            dismissWithResult = { dismissedResults.add(it) }
        )

        viewModel.onAllowClick()

        assertThat(dismissedResults).hasSize(1)
        val result = dismissedResults.first() as LinkActivityResult.Completed
        assertThat(result.authorizationConsentGranted).isTrue()
    }

    @Test
    fun `onDenyClick with successful consent update dismisses with denied consent`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.postConsentUpdateResult = Result.success(Unit)

        val dismissedResults = mutableListOf<LinkActivityResult>()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            dismissWithResult = { dismissedResults.add(it) }
        )

        viewModel.onDenyClick()

        assertThat(dismissedResults).hasSize(1)
        val result = dismissedResults.first() as LinkActivityResult.Completed
        assertThat(result.authorizationConsentGranted).isFalse()
    }

    @Test
    fun `onAllowClick with failed consent update shows error message`() = runTest(dispatcher) {
        val errorMessage = "Consent update failed"
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.postConsentUpdateResult = Result.failure(RuntimeException(errorMessage))

        val dismissedResults = mutableListOf<LinkActivityResult>()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            dismissWithResult = { dismissedResults.add(it) }
        )

        viewModel.viewState.test {
            assertThat(awaitItem().errorMessage).isNull()

            viewModel.onAllowClick()

            assertThat(awaitItem().errorMessage).isNotNull()
        }

        // Should not dismiss on failure
        assertThat(dismissedResults).isEmpty()
    }

    @Test
    fun `onDenyClick with failed consent update shows error message`() = runTest(dispatcher) {
        val errorMessage = "Consent update failed"
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.postConsentUpdateResult = Result.failure(RuntimeException(errorMessage))

        val dismissedResults = mutableListOf<LinkActivityResult>()

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            dismissWithResult = { dismissedResults.add(it) }
        )

        viewModel.viewState.test {
            assertThat(awaitItem().errorMessage).isNull()

            viewModel.onDenyClick()

            assertThat(awaitItem().errorMessage).isNotNull()
        }

        // Should not dismiss on failure
        assertThat(dismissedResults).isEmpty()
    }

    @Test
    fun `consent submission clears previous error messages before processing`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        // First call fails, second succeeds
        linkAccountManager.postConsentUpdateResult = Result.failure(RuntimeException("First error"))

        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        viewModel.viewState.test {
            assertThat(awaitItem().errorMessage).isNull()

            // First call - should show error
            viewModel.onAllowClick()
            assertThat(awaitItem().errorMessage).isNotNull()

            // Make second call succeed
            linkAccountManager.postConsentUpdateResult = Result.success(Unit)
            viewModel.onAllowClick()

            // Should briefly clear error before succeeding
            assertThat(awaitItem().errorMessage).isNull()

            expectNoEvents() // Should dismiss instead of emitting another state
        }
    }

    @Test
    fun `view state contains all merchant configuration details`() = runTest(dispatcher) {
        val customConfiguration = TestFactory.LINK_CONFIGURATION.copy(
            merchantName = "Custom Merchant",
            merchantLogoUrl = "https://example.com/logo.png"
        )

        val viewModel = createViewModel(linkConfiguration = customConfiguration)

        viewModel.viewState.test {
            awaitItem().let { state ->
                assertThat(state.merchantName).isEqualTo("Custom Merchant")
                assertThat(state.merchantLogoUrl).isEqualTo("https://example.com/logo.png")
                assertThat(state.userEmail).isEqualTo(TestFactory.EMAIL)
            }
        }
    }

    @Test
    fun `consent pane data is correctly extracted from link account`() = runTest(dispatcher) {
        val consentPane = TestFactory.CONSENT_PANE

        val linkAccountWithConsent = createLinkAccountWithConsent(consentPane)
        val viewModel = createViewModel(linkAccount = linkAccountWithConsent)

        viewModel.viewState.test {
            assertThat(awaitItem().consentPane).isEqualTo(consentPane)
        }
    }

    private fun createViewModel(
        linkAccount: LinkAccount = createLinkAccountWithConsent(),
        linkConfiguration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        dismissWithResult: (LinkActivityResult) -> Unit = { }
    ): OAuthConsentViewModel {
        return OAuthConsentViewModel(
            linkAccount = linkAccount,
            linkConfiguration = linkConfiguration,
            linkAccountManager = linkAccountManager,
            dismissWithResult = dismissWithResult
        )
    }

    private fun createLinkAccountWithConsent(
        consentPane: ConsentUi.ConsentPane? = TestFactory.CONSENT_PANE
    ): LinkAccount {
        val consumerSession = TestFactory.CONSUMER_SESSION

        return LinkAccount(
            consumerSession = consumerSession,
            linkAuthIntentInfo = LinkAuthIntentInfo(
                linkAuthIntentId = "lai_123",
                consentPresentation = consentPane?.let { ConsentPresentation.FullScreen(it) }
            )
        )
    }
}
