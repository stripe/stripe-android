package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_SELECTION
import com.stripe.android.testing.PaymentIntentFactory
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    @Test
    fun `Prepares state correctly for logged out user`() = runLinkTest {
        accountStatusFlow.emit(AccountStatus.SignedOut)
        handler.setupLink(LinkState(configuration, LinkState.LoginState.LoggedOut))
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.SignedOut)
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION)).isNull()
    }

    @Test
    fun `Completed result sets processing state to Completed`() = runLinkTest {
        handler.setupLink(
            LinkState(configuration, LinkState.LoginState.LoggedIn),
        )
        handler.launchLink()
        assertThat(processingStateTurbine.awaitItem()).isEqualTo(LinkHandler.ProcessingState.Launched)
        verify(linkLauncher).present(configuration)
        verifyNoMoreInteractions(linkLauncher)
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
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.CompleteWithoutLink)
            verify(linkAnalyticsHelper).onLinkPopupSkipped()
            verify(linkLauncher, never()).present(eq(configuration))
            verify(linkStore, never()).markLinkAsUsed()
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
            verify(linkLauncher, never()).present(eq(configuration))
            verify(linkStore).markLinkAsUsed()
        }

        handler.accountStatus.test {
            assertThat(awaitItem()).isEqualTo(AccountStatus.Verified)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline completes successfully for existing user in custom flow`() = runLinkInlineTest(
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        val userInput = UserInput.SignIn("example@example.com")

        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedOut,
                configuration = configuration,
            )
        )

        handler.processingState.test {
            accountStatusFlow.emit(AccountStatus.NeedsVerification)
            ensureAllEventsConsumed() // Begin with no events.
            testScope.launch {
                handler.payWithLinkInline(userInput, cardSelection(), shouldCompleteLinkFlow)
            }
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.CompleteWithoutLink)
            verify(linkAnalyticsHelper).onLinkPopupSkipped()
            verify(linkLauncher, never()).present(eq(configuration))
            verify(linkStore, never()).markLinkAsUsed()
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
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.CompleteWithoutLink)
            assertThat(accountStatusTurbine.awaitItem()).isEqualTo(AccountStatus.Verified)
            verify(linkAnalyticsHelper).onLinkPopupSkipped()
            verify(linkLauncher, never()).present(eq(configuration))
            verify(linkStore, never()).markLinkAsUsed()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `payWithLinkInline collects payment details`() = runLinkInlineTest(
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
            verify(linkLauncher, never()).present(eq(configuration))
            verify(linkStore).markLinkAsUsed()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline collects payment details in passthrough mode`() = runLinkInlineTest(
        accountStatusFlow = MutableSharedFlow(replay = 0),
        shouldCompleteLinkFlowValues = listOf(false),
        linkConfiguration = defaultLinkConfiguration().copy(passthroughModeEnabled = true),
        attachNewCardToAccountResult = Result.success(
            LinkPaymentDetails.Saved(
                paymentDetails = ConsumerPaymentDetails.Passthrough(
                    id = "pm_123",
                    last4 = "4242"
                ),
                paymentMethodCreateParams = mock(),
            )
        ),
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
            verify(linkLauncher, never()).present(eq(configuration))
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
            verify(linkLauncher, never()).present(eq(configuration))
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
            verify(linkLauncher, never()).present(eq(configuration))
        }

        handler.accountStatus.test {
            assertThat(awaitItem()).isEqualTo(AccountStatus.SignedOut)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
        accountStatusTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `Hides Link inline signup if no valid funding source`() = runLinkInlineTest(
        linkConfiguration = defaultLinkConfiguration(
            linkFundingSources = listOf("us_bank_account"),
        ),
    ) {
        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedOut,
                configuration = configuration,
            )
        )

        accountStatusFlow.emit(AccountStatus.SignedOut)

        handler.linkSignupMode.test {
            assertThat(awaitItem()).isNull()
        }

        accountStatusTurbine.cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `Hides Link inline signup if user already has an account`() = runLinkInlineTest {
        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.NeedsVerification,
                configuration = configuration,
            )
        )

        accountStatusFlow.emit(AccountStatus.NeedsVerification)

        handler.linkSignupMode.test {
            assertThat(awaitItem()).isNull()
        }

        accountStatusTurbine.cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `Shows Link inline signup if user has no account`() = runLinkInlineTest {
        handler.setupLink(
            state = LinkState(
                loginState = LinkState.LoginState.LoggedOut,
                configuration = configuration,
            )
        )

        accountStatusFlow.emit(AccountStatus.SignedOut)

        handler.linkSignupMode.test {
            assertThat(awaitItem()).isNotNull()
        }

        accountStatusTurbine.cancelAndIgnoreRemainingEvents()
    }
}

// Used to run through both complete flow, and custom flow for link inline tests.
private fun runLinkInlineTest(
    accountStatusFlow: MutableSharedFlow<AccountStatus> = MutableSharedFlow(replay = 1),
    shouldCompleteLinkFlowValues: List<Boolean> = listOf(true, false),
    linkConfiguration: LinkConfiguration = defaultLinkConfiguration(
        linkFundingSources = listOf("card"),
    ),
    attachNewCardToAccountResult: Result<LinkPaymentDetails> = Result.success(
        LinkPaymentDetails.New(
            paymentDetails = ConsumerPaymentDetails.Card(
                id = "pm_123",
                expiryYear = 2050,
                expiryMonth = 4,
                brand = CardBrand.Visa,
                last4 = "4242",
                cvcCheck = CvcCheck.Pass,
            ),
            paymentMethodCreateParams = mock(),
            originalParams = mock(),
        )
    ),
    testBlock: suspend LinkInlineTestData.() -> Unit,
) {
    for (shouldCompleteLinkFlowValue in shouldCompleteLinkFlowValues) {
        runLinkTest(accountStatusFlow, linkConfiguration, attachNewCardToAccountResult) {
            with(LinkInlineTestData(shouldCompleteLinkFlowValue, this)) {
                testBlock()
            }
        }
    }
}

private fun runLinkTest(
    accountStatusFlow: MutableSharedFlow<AccountStatus> = MutableSharedFlow(replay = 1),
    linkConfiguration: LinkConfiguration = defaultLinkConfiguration(),
    attachNewCardToAccountResult: Result<LinkPaymentDetails>? = null,
    testBlock: suspend LinkTestData.() -> Unit
): Unit = runTest {
    val linkLauncher = mock<LinkPaymentLauncher>()
    val linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>()
    val savedStateHandle = SavedStateHandle()
    val linkAnalyticsHelper = mock<LinkAnalyticsHelper>()
    val linkStore = mock<LinkStore>()
    val handler = LinkHandler(
        linkLauncher = linkLauncher,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        savedStateHandle = savedStateHandle,
        linkStore = linkStore,
        linkAnalyticsComponentBuilder = mock<LinkAnalyticsComponent.Builder>().stub {
            val component = object : LinkAnalyticsComponent {
                override val linkAnalyticsHelper: LinkAnalyticsHelper = linkAnalyticsHelper
            }
            whenever(it.build()).thenReturn(component)
        },
    )
    val processingStateTurbine = handler.processingState.testIn(backgroundScope)
    val accountStatusTurbine = handler.accountStatus.testIn(backgroundScope)

    whenever(linkConfigurationCoordinator.getAccountStatusFlow(eq(linkConfiguration))).thenReturn(accountStatusFlow)
    whenever(linkConfigurationCoordinator.attachNewCardToAccount(eq(linkConfiguration), any())).thenReturn(attachNewCardToAccountResult)

    with(
        LinkTestDataImpl(
            testScope = this,
            handler = handler,
            linkLauncher = linkLauncher,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkStore = linkStore,
            savedStateHandle = savedStateHandle,
            configuration = linkConfiguration,
            accountStatusFlow = accountStatusFlow,
            processingStateTurbine = processingStateTurbine,
            accountStatusTurbine = accountStatusTurbine,
            linkAnalyticsHelper = linkAnalyticsHelper,
        )
    ) {
        testBlock()
        processingStateTurbine.ensureAllEventsConsumed()
        accountStatusTurbine.ensureAllEventsConsumed()
    }
}

private fun defaultLinkConfiguration(
    linkFundingSources: List<String> = emptyList(),
): LinkConfiguration {
    return LinkConfiguration(
        stripeIntent = PaymentIntentFactory.create(
            linkFundingSources = linkFundingSources,
        ),
        signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
        merchantName = "Merchant, Inc",
        merchantCountryCode = "US",
        customerInfo = LinkConfiguration.CustomerInfo(
            name = "Name",
            email = "customer@email.com",
            phone = "1234567890",
            billingCountryCode = "US",
            shouldPrefill = false,
        ),
        shippingValues = null,
        passthroughModeEnabled = false,
    )
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
    override val linkStore: LinkStore,
    override val savedStateHandle: SavedStateHandle,
    override val configuration: LinkConfiguration,
    override val accountStatusFlow: MutableSharedFlow<AccountStatus>,
    override val processingStateTurbine: ReceiveTurbine<LinkHandler.ProcessingState>,
    override val accountStatusTurbine: ReceiveTurbine<AccountStatus>,
    override val linkAnalyticsHelper: LinkAnalyticsHelper,
) : LinkTestData

private interface LinkTestData {
    val testScope: TestScope
    val handler: LinkHandler
    val linkLauncher: LinkPaymentLauncher
    val linkConfigurationCoordinator: LinkConfigurationCoordinator
    val linkStore: LinkStore
    val savedStateHandle: SavedStateHandle
    val configuration: LinkConfiguration
    val accountStatusFlow: MutableSharedFlow<AccountStatus>
    val processingStateTurbine: ReceiveTurbine<LinkHandler.ProcessingState>
    val accountStatusTurbine: ReceiveTurbine<AccountStatus>
    val linkAnalyticsHelper: LinkAnalyticsHelper
}

private class LinkInlineTestData(
    val shouldCompleteLinkFlow: Boolean,
    linkTestData: LinkTestData,
) : LinkTestData by linkTestData
