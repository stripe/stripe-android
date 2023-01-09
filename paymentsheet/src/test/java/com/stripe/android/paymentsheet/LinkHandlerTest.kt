package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.LinkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    private val configuration = LinkPaymentLauncher.Configuration(
        stripeIntent = mock(),
        merchantName = "Merchant, Inc",
        customerName = "Name",
        customerEmail = "customer@email.com",
        customerPhone = "1234567890",
        customerBillingCountryCode = "US",
        shippingValues = null,
    )

    @Test
    fun `setupLink when loginState is loggedIn`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isTrue()
        assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        processingStateTurbine.ensureAllEventsConsumed()

        verify(linkLauncher).present(configuration, null)
        verifyNoMoreInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `setupLink when loginState is loggedOut`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.LoggedOut))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        processingStateTurbine.ensureAllEventsConsumed()

        verifyNoInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `setupLink when loginState is NeedsVerification with successful verification`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.NeedsVerification))
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
        processingStateTurbine.ensureAllEventsConsumed()

        verify(linkLauncher).present(configuration, null)
    }

    @Test
    fun `setupLink when loginState is NeedsVerification with unsuccessful verification`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.NeedsVerification))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(handler.linkConfiguration.first()).isEqualTo(configuration)
        processingStateTurbine.ensureAllEventsConsumed()
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        verifyNoInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)

        handler.handleLinkVerificationResult(false)

        processingStateTurbine.ensureAllEventsConsumed()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()

        verifyNoInteractions(linkLauncher)
    }

    @Test
    fun `setupLink when linkState is null`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, null)
        assertThat(handler.isLinkEnabled.first()).isFalse()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(handler.linkConfiguration.first()).isNull()
        processingStateTurbine.ensureAllEventsConsumed()
        assertThat(handler.showLinkVerificationDialog.first()).isFalse()
        verifyNoInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `test launchLink`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.launchLink(configuration, launchedDirectly = false)

        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        processingStateTurbine.ensureAllEventsConsumed()

        verify(linkLauncher).present(configuration, null)
        verifyNoMoreInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `test onLinkActivityResult with Completed result`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Completed)
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Complete)
        processingStateTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `test onLinkActivityResult with Cancelled result after back pressed`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.BackPressed))
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Cancelled)
        processingStateTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `test onLinkActivityResult with Cancelled result`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut))
        val resultState = processingStateTurbine.awaitItem() as LinkHandler.ProcessingState.CompletedWithPaymentResult
        assertThat(resultState.result).isEqualTo(PaymentResult.Canceled)
        processingStateTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `test onLinkActivityResult with CompletedWithPaymentResult result`(): Unit = runTest {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Failed(AssertionError("Expected payment result error.")))
        val result = processingStateTurbine.awaitItem() as LinkHandler.ProcessingState.CompletedWithPaymentResult
        val paymentResult = result.result as PaymentResult.Failed
        assertThat(paymentResult.throwable).hasMessageThat().isEqualTo("Expected payment result error.")
        processingStateTurbine.ensureAllEventsConsumed()
    }
    // TODO(linkextraction): Add more tests with setup link
}
