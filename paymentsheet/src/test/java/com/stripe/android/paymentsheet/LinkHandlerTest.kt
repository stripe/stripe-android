@file:OptIn(ExperimentalCoroutinesApi::class)

package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_SELECTION
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    @Test
    fun `setupLink when loginState is loggedIn`() = runLinkTest {
        handler.setupLinkLaunchingEagerly(
            testScope,
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isTrue()
        assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)

        verify(linkLauncher).present(configuration, null)
        verifyNoMoreInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `setupLink when loginState is loggedOut`() = runLinkTest {
        handler.setupLinkLaunchingEagerly(
            testScope,
            LinkState(configuration, LinkState.LoginState.LoggedOut),
        )
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()

        verifyNoInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `setupLink when loginState is NeedsVerification with successful verification`() =
        runLinkTest {
            handler.setupLinkLaunchingEagerly(
                testScope,
                LinkState(configuration, LinkState.LoginState.NeedsVerification),
            )
            assertThat(handler.isLinkEnabled.first()).isTrue()
            assertThat(handler.activeLinkSession.first()).isFalse()
            assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
            processingStateTurbine.ensureAllEventsConsumed()
            assertThat(handler.showLinkVerificationDialog.first()).isFalse()
            verifyNoInteractions(linkLauncher)
            verifyNoInteractions(eventReporter)

            handler.handleLinkVerificationResult(true)

            assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
            assertThat(handler.showLinkVerificationDialog.first()).isTrue()
            assertThat(handler.activeLinkSession.first()).isTrue()

            verify(linkLauncher).present(configuration, null)
        }

    @Test
    fun `setupLink when loginState is NeedsVerification with unsuccessful verification`() =
        runLinkTest {
            handler.setupLinkLaunchingEagerly(
                testScope,
                LinkState(configuration, LinkState.LoginState.NeedsVerification),
            )
            assertThat(handler.isLinkEnabled.first()).isTrue()
            assertThat(handler.activeLinkSession.first()).isFalse()
            assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
            processingStateTurbine.ensureAllEventsConsumed()
            assertThat(handler.showLinkVerificationDialog.first()).isFalse()
            verifyNoInteractions(linkLauncher)
            verifyNoInteractions(eventReporter)

            handler.handleLinkVerificationResult(false)

            assertThat(handler.activeLinkSession.first()).isFalse()
            assertThat(handler.showLinkVerificationDialog.first()).isFalse()

            verifyNoInteractions(linkLauncher)
        }

    @Test
    fun `setupLink when linkState is null`() = runLinkTest {
        handler.setupLinkLaunchingEagerly(testScope, null)
        assertThat(handler.isLinkEnabled.first()).isFalse()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(handler.linkConfiguration.first()).isNull()
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        verifyNoInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `prepareLink when loggedIn`() = runLinkTest {
        handler.prepareLink(LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isTrue()
        assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION))
            .isEqualTo(PaymentSelection.Link)
        verifyNoInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `prepareLink when not logged in`() = runLinkTest {
        handler.prepareLink(LinkState(configuration, LinkState.LoginState.LoggedOut))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION))
            .isNull()
        verifyNoInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `test launchLink`() = runLinkTest {
        handler.launchLink(configuration, launchedDirectly = false)

        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        processingStateTurbine.ensureAllEventsConsumed()

        verify(linkLauncher).present(configuration, null)
        verifyNoMoreInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `test onLinkActivityResult with Completed result`() = runLinkTest {
        handler.setupLinkLaunchingEagerly(
            testScope,
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Completed)
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Complete)
    }

    @Test
    fun `test onLinkActivityResult with Cancelled result after back pressed`() = runLinkTest {
        handler.setupLinkLaunchingEagerly(
            testScope,
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.BackPressed))
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Cancelled)
    }

    @Test
    fun `test onLinkActivityResult with Cancelled result`() = runLinkTest {
        handler.setupLinkLaunchingEagerly(
            testScope,
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut))
        val resultState =
            processingStateTurbine.awaitItem() as LinkHandler.ProcessingState.CompletedWithPaymentResult
        assertThat(resultState.result).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun `test onLinkActivityResult with CompletedWithPaymentResult result`() = runLinkTest {
        handler.setupLinkLaunchingEagerly(
            testScope,
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Failed(AssertionError("Expected payment result error.")))
        val result =
            processingStateTurbine.awaitItem() as LinkHandler.ProcessingState.CompletedWithPaymentResult
        val paymentResult = result.result as PaymentResult.Failed
        assertThat(paymentResult.throwable).hasMessageThat()
            .isEqualTo("Expected payment result error.")
    }
    // TODO(linkextraction): Add more tests with setup link
}

private fun runLinkTest(testBlock: suspend LinkTestData.() -> Unit): Unit = runTest {
    val linkLauncher = mock<LinkPaymentLauncher>()
    val eventReporter = mock<EventReporter>()
    val savedStateHandle = SavedStateHandle()
    val handler = LinkHandler(
        linkLauncher = linkLauncher,
        savedStateHandle = savedStateHandle,
        eventReporter = eventReporter,
        paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
    )
    val processingStateTurbine = handler.processingState.testIn(backgroundScope)
    val configuration = LinkPaymentLauncher.Configuration(
        stripeIntent = mock(),
        merchantName = "Merchant, Inc",
        customerName = "Name",
        customerEmail = "customer@email.com",
        customerPhone = "1234567890",
        customerBillingCountryCode = "US",
        shippingValues = null,
    )
    with(
        LinkTestData(
            testScope = this,
            handler = handler,
            linkLauncher = linkLauncher,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
            configuration = configuration,
            processingStateTurbine = processingStateTurbine,
        )
    ) {
        testBlock()
        processingStateTurbine.ensureAllEventsConsumed()
    }
}

private data class LinkTestData(
    val testScope: TestScope,
    val handler: LinkHandler,
    val linkLauncher: LinkPaymentLauncher,
    val eventReporter: EventReporter,
    val savedStateHandle: SavedStateHandle,
    val configuration: LinkPaymentLauncher.Configuration,
    val processingStateTurbine: ReceiveTurbine<LinkHandler.ProcessingState>,
)
