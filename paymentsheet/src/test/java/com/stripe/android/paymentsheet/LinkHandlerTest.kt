package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_SELECTION
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    @Test
    fun `Prepares state correctly for logged in user`() = runLinkTest {
        accountStatusFlow.emit(AccountStatus.Verified)
        handler.setupLink(LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isTrue()
        assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.Verified)
    }

    @Test
    fun `Prepares state correctly for logged out user`() = runLinkTest {
        accountStatusFlow.emit(AccountStatus.SignedOut)
        handler.setupLink(LinkState(configuration, LinkState.LoginState.LoggedOut))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(handler.activeLinkSession.first()).isFalse()
        assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.SignedOut)
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION)).isNull()
    }

    @Test
    fun `launchLink presents with configuration`() = runLinkTest {
        handler.launchLink(configuration)

        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        processingStateTurbine.ensureAllEventsConsumed()

        verify(linkLauncher).present(configuration, null)
        verifyNoMoreInteractions(linkLauncher)
    }

    @Test
    fun `Completed result sets processing state to Completed`() = runLinkTest {
        handler.setupLink(
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        handler.launchLink()
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Completed(mock()))
        assertThat(processingStateTurbine.awaitItem()).isInstanceOf(
            LinkHandler.ProcessingState.PaymentMethodCollected::class.java
        )
    }

    @Test
    fun `Back pressed result sets processing state to Cancelled`() = runLinkTest {
        handler.setupLink(
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        handler.launchLink()
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.BackPressed))
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Cancelled)
    }

    @Test
    fun `Cancelled result sets processing state to Cancelled`() = runLinkTest {
        handler.setupLink(
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        handler.launchLink()
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut))
        val resultState =
            processingStateTurbine.awaitItem() as LinkHandler.ProcessingState.CompletedWithPaymentResult
        assertThat(resultState.result).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun `exception causes processing state to be CompletedWithPaymentResult`() = runLinkTest {
        handler.setupLink(
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        handler.launchLink()
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        handler.onLinkActivityResult(LinkActivityResult.Failed(AssertionError("Expected payment result error.")))
        val result =
            processingStateTurbine.awaitItem() as LinkHandler.ProcessingState.CompletedWithPaymentResult
        val paymentResult = result.result as PaymentResult.Failed
        assertThat(paymentResult.throwable).hasMessageThat()
            .isEqualTo("Expected payment result error.")
    }

    @Test
    fun `payWithLinkInline completes successfully for existing verified user in complete flow`() = runLinkInlineTest(
        shouldCompleteLinkFlowValues = listOf(true),
    ) {
        val userInput = UserInput.SignIn("example@example.com")

        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedIn,
                configuration = configuration,
            )
        )

        handler.processingState.test {
            accountStatusFlow.emit(AccountStatus.Verified)
            ensureAllEventsConsumed() // Begin with no events.
            testScope.launch {
                handler.payWithLinkInline(userInput, cardSelection(), shouldCompleteLinkFlow)
            }
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
            assertThat(handler.activeLinkSession.value).isTrue()
            verify(linkLauncher).present(eq(configuration), any())
        }

        handler.accountStatus.test {
            assertThat(awaitItem()).isEqualTo(AccountStatus.Verified)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline completes successfully for existing verified user in custom flow`() = runLinkInlineTest(
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        val userInput = UserInput.SignIn("example@example.com")

        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.NeedsVerification,
                configuration = configuration,
            )
        )

        handler.processingState.test {
            accountStatusFlow.emit(AccountStatus.Verified)
            ensureAllEventsConsumed() // Begin with no events.
            testScope.launch {
                handler.payWithLinkInline(userInput, cardSelection(), shouldCompleteLinkFlow)
            }
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(awaitItem()).isInstanceOf(LinkHandler.ProcessingState.PaymentDetailsCollected::class.java)
            assertThat(handler.activeLinkSession.value).isTrue()
            verify(linkLauncher, never()).present(eq(configuration), any())
        }

        handler.accountStatus.test {
            assertThat(awaitItem()).isEqualTo(AccountStatus.Verified)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline completes successfully for signedOut user in complete flow`() = runLinkInlineTest(
        MutableSharedFlow(replay = 0),
        shouldCompleteLinkFlowValues = listOf(true),
    ) {
        val userInput = UserInput.SignIn(email = "example@example.com")

        accountStatusTurbine.ensureAllEventsConsumed()
        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedOut,
                configuration = configuration,
            )
        )

        handler.processingState.test {
            ensureAllEventsConsumed() // Begin with no events.
            whenever(linkConfigurationCoordinator.signInWithUserInput(any(), any()))
                .thenReturn(Result.success(true))
            testScope.launch {
                handler.payWithLinkInline(userInput, cardSelection(), shouldCompleteLinkFlow)
            }
            accountStatusFlow.emit(AccountStatus.SignedOut)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.SignedOut)
            accountStatusFlow.emit(AccountStatus.Verified)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
            assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.Verified)
            assertThat(handler.activeLinkSession.value).isTrue()
            verify(linkLauncher).present(eq(configuration), any())
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `payWithLinkInline collects payment details for signedOut user in custom flow`() = runLinkInlineTest(
        accountStatusFlow = MutableSharedFlow(replay = 0),
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        val userInput = UserInput.SignIn(email = "example@example.com")

        accountStatusTurbine.ensureAllEventsConsumed()
        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedOut,
                configuration = configuration,
            )
        )

        handler.processingState.test {
            ensureAllEventsConsumed() // Begin with no events.
            whenever(linkConfigurationCoordinator.signInWithUserInput(any(), any()))
                .thenReturn(Result.success(true))
            testScope.launch {
                handler.payWithLinkInline(userInput, cardSelection(), shouldCompleteLinkFlow)
            }
            accountStatusFlow.emit(AccountStatus.SignedOut)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.SignedOut)

            accountStatusFlow.emit(AccountStatus.Verified)
            assertThat(awaitItem()).isInstanceOf(LinkHandler.ProcessingState.PaymentDetailsCollected::class.java)
            assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.Verified)
            assertThat(handler.activeLinkSession.value).isTrue()
            verify(linkLauncher, never()).present(eq(configuration), any())
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline fails for signedOut user`() = runLinkInlineTest {
        val userInput = UserInput.SignIn(email = "example@example.com")

        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedOut,
                configuration = configuration,
            )
        )

        handler.processingState.test {
            accountStatusFlow.emit(AccountStatus.SignedOut)
            ensureAllEventsConsumed() // Begin with no events.
            whenever(linkConfigurationCoordinator.signInWithUserInput(any(), any()))
                .thenReturn(Result.failure(IllegalStateException("Whoops")))
            testScope.launch {
                handler.payWithLinkInline(userInput, cardSelection(), shouldCompleteLinkFlow)
            }
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Error("Whoops"))
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Ready)
            assertThat(handler.activeLinkSession.value).isFalse()
            verify(linkLauncher, never()).present(eq(configuration), any())
        }

        handler.accountStatus.test {
            assertThat(awaitItem()).isEqualTo(AccountStatus.SignedOut)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline fails for signedOut user without userInput`() = runLinkInlineTest {
        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedOut,
                configuration = configuration,
            )
        )

        handler.processingState.test {
            accountStatusFlow.emit(AccountStatus.SignedOut)
            ensureAllEventsConsumed() // Begin with no events.
            testScope.launch {
                handler.payWithLinkInline(userInput = null, cardSelection(), shouldCompleteLinkFlow)
            }
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Ready)
            assertThat(handler.activeLinkSession.value).isFalse()
            verify(linkLauncher, never()).present(eq(configuration), any())
        }

        handler.accountStatus.test {
            assertThat(awaitItem()).isEqualTo(AccountStatus.SignedOut)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }
}

// Used to run through both complete flow, and custom flow for link inline tests.
private fun runLinkInlineTest(
    accountStatusFlow: MutableSharedFlow<AccountStatus> = MutableSharedFlow(replay = 1),
    shouldCompleteLinkFlowValues: List<Boolean> = listOf(true, false),
    testBlock: suspend LinkInlineTestData.() -> Unit,
) {
    for (shouldCompleteLinkFlowValue in shouldCompleteLinkFlowValues) {
        runLinkTest(accountStatusFlow) {
            with(LinkInlineTestData(shouldCompleteLinkFlowValue, this)) {
                testBlock()
            }
        }
    }
}

private fun runLinkTest(
    accountStatusFlow: MutableSharedFlow<AccountStatus> = MutableSharedFlow(replay = 1),
    testBlock: suspend LinkTestData.() -> Unit
): Unit = runTest {
    val linkLauncher = mock<LinkPaymentLauncher>()
    val linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>()
    val savedStateHandle = SavedStateHandle()
    val handler = LinkHandler(
        linkLauncher = linkLauncher,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        savedStateHandle = savedStateHandle,
    )
    val processingStateTurbine = handler.processingState.testIn(backgroundScope)
    val accountStatusTurbine = handler.accountStatus.testIn(backgroundScope)
    val configuration = LinkConfiguration(
        stripeIntent = mock(),
        merchantName = "Merchant, Inc",
        merchantCountryCode = "US",
        customerName = "Name",
        customerEmail = "customer@email.com",
        customerPhone = "1234567890",
        customerBillingCountryCode = "US",
        shippingValues = null,
    )

    whenever(linkConfigurationCoordinator.getAccountStatusFlow(eq(configuration))).thenReturn(accountStatusFlow)

    with(
        LinkTestDataImpl(
            testScope = this,
            handler = handler,
            linkLauncher = linkLauncher,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            savedStateHandle = savedStateHandle,
            configuration = configuration,
            accountStatusFlow = accountStatusFlow,
            processingStateTurbine = processingStateTurbine,
            accountStatusTurbine = accountStatusTurbine,
        )
    ) {
        testBlock()
        processingStateTurbine.ensureAllEventsConsumed()
        accountStatusTurbine.ensureAllEventsConsumed()
    }
}

private fun cardSelection(): PaymentSelection.New.Card {
    val createParams = PaymentMethodCreateParams.create(
        PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 1,
            expiryYear = 34,
        )
    )
    return PaymentSelection.New.Card(
        createParams,
        CardBrand.Visa,
        PaymentSelection.CustomerRequestedSave.RequestReuse
    )
}

private class LinkTestDataImpl(
    override val testScope: TestScope,
    override val handler: LinkHandler,
    override val linkLauncher: LinkPaymentLauncher,
    override val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    override val savedStateHandle: SavedStateHandle,
    override val configuration: LinkConfiguration,
    override val accountStatusFlow: MutableSharedFlow<AccountStatus>,
    override val processingStateTurbine: ReceiveTurbine<LinkHandler.ProcessingState>,
    override val accountStatusTurbine: ReceiveTurbine<AccountStatus>,
) : LinkTestData

private interface LinkTestData {
    val testScope: TestScope
    val handler: LinkHandler
    val linkLauncher: LinkPaymentLauncher
    val linkConfigurationCoordinator: LinkConfigurationCoordinator
    val savedStateHandle: SavedStateHandle
    val configuration: LinkConfiguration
    val accountStatusFlow: MutableSharedFlow<AccountStatus>
    val processingStateTurbine: ReceiveTurbine<LinkHandler.ProcessingState>
    val accountStatusTurbine: ReceiveTurbine<AccountStatus>
}

private class LinkInlineTestData(
    val shouldCompleteLinkFlow: Boolean,
    linkTestData: LinkTestData,
) : LinkTestData by linkTestData
