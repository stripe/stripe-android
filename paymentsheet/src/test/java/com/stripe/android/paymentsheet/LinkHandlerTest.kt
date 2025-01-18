package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.wallets.Wallet
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
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    @Test
    fun `Prepares state correctly for logged out user`() = runLinkTest {
        accountStatusFlow.emit(AccountStatus.SignedOut)
        handler.setupLink(
            createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse
            )
        )
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION)).isNull()
    }

    @Test
    fun `Prepares state correctly with SFU signup mode`() = runLinkTest {
        accountStatusFlow.emit(AccountStatus.SignedOut)
        handler.setupLink(
            createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse
            )
        )
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION)).isNull()
    }

    @Test
    fun `payWithLinkInline completes successfully for existing verified user in complete flow`() = runLinkInlineTest(
        shouldCompleteLinkFlowValues = listOf(true),
    ) {
        val userInput = UserInput.SignIn("example@example.com")

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedIn,
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
            verify(linkStore, never()).markLinkAsUsed()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline completes successfully for existing verified user in custom flow`() = runLinkInlineTest(
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        val userInput = UserInput.SignIn("example@example.com")

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.NeedsVerification,
            )
        )

        handler.processingState.test {
            accountStatusFlow.emit(AccountStatus.Verified)
            ensureAllEventsConsumed() // Begin with no events.
            testScope.launch {
                handler.payWithLinkInline(userInput, cardSelection(), shouldCompleteLinkFlow)
            }
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            assertThat(awaitItem()).isInstanceOf<LinkHandler.ProcessingState.PaymentDetailsCollected>()
            verify(linkStore).markLinkAsUsed()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline completes successfully for existing user in custom flow`() = runLinkInlineTest(
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        val userInput = UserInput.SignIn("example@example.com")

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
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
            verify(linkStore, never()).markLinkAsUsed()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline completes successfully for signedOut user in complete flow`() = runLinkInlineTest(
        MutableSharedFlow(replay = 0),
        shouldCompleteLinkFlowValues = listOf(true),
    ) {
        val userInput = UserInput.SignIn(email = "example@example.com")

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
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
            accountStatusFlow.emit(AccountStatus.Verified)
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.CompleteWithoutLink)
            verify(linkAnalyticsHelper).onLinkPopupSkipped()
            verify(linkStore, never()).markLinkAsUsed()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline collects payment details`() = runLinkInlineTest(
        accountStatusFlow = MutableSharedFlow(replay = 0),
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        val userInput = UserInput.SignIn(email = "example@example.com")

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
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

            accountStatusFlow.emit(AccountStatus.Verified)
            assertThat(awaitItem()).isInstanceOf<LinkHandler.ProcessingState.PaymentDetailsCollected>()
            verify(linkStore).markLinkAsUsed()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline requests payment is saved if selection requested reuse`() = runLinkInlineTest(
        accountStatusFlow = MutableSharedFlow(replay = 0),
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        setupBasicLink()

        handler.processingState.test {
            ensureAllEventsConsumed()

            payWithLinkInline(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )

            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            accountStatusFlow.emit(AccountStatus.Verified)

            val linkInlineSelection = assertAndGetInlineLinkSelection(awaitItem())

            assertThat(linkInlineSelection.customerRequestedSave).isEqualTo(
                PaymentSelection.CustomerRequestedSave.RequestReuse
            )

            cancelAndConsumeRemainingEvents()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `payWithLinkInline requests payment is not saved if selection doesn't request it`() = runLinkInlineTest(
        accountStatusFlow = MutableSharedFlow(replay = 0),
        shouldCompleteLinkFlowValues = listOf(false),
    ) {
        setupBasicLink()

        handler.processingState.test {
            ensureAllEventsConsumed()

            payWithLinkInline(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )

            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.Started)
            accountStatusFlow.emit(AccountStatus.Verified)

            val linkInlineSelection = assertAndGetInlineLinkSelection(awaitItem())

            assertThat(linkInlineSelection.customerRequestedSave).isEqualTo(
                PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )

            cancelAndConsumeRemainingEvents()
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents()
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

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
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

            accountStatusFlow.emit(AccountStatus.Verified)
            assertThat(awaitItem()).isEqualTo(
                LinkHandler.ProcessingState.PaymentDetailsCollected(
                    paymentSelection = PaymentSelection.Saved(
                        paymentMethod = PaymentMethod.Builder()
                            .setId("pm_123")
                            .setCode("card")
                            .setCard(
                                PaymentMethod.Card(
                                    last4 = "4242",
                                    wallet = Wallet.LinkWallet("4242")
                                )
                            )
                            .setType(PaymentMethod.Type.Card)
                            .build(),
                        walletType = PaymentSelection.Saved.WalletType.Link,
                        paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                        ),
                    ),
                )
            )
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `if lookup fails, payWithLinkInline emits event to pay without Link`() = runLinkInlineTest {
        val userInput = UserInput.SignIn(email = "example@example.com")

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
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
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.CompleteWithoutLink)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `if sign up fails, payWithLinkInline emits event to pay without Link`() = runLinkInlineTest {
        val userInput = UserInput.SignUp(
            name = "John Doe",
            email = "example@example.com",
            phone = "+11234567890",
            country = "US",
            consentAction = SignUpConsentAction.Checkbox,
        )

        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
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
            assertThat(awaitItem()).isEqualTo(LinkHandler.ProcessingState.CompleteWithoutLink)
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    @Test
    fun `payWithLinkInline fails for signedOut user without userInput`() = runLinkInlineTest {
        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
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
        }

        processingStateTurbine.cancelAndIgnoreRemainingEvents() // Validated above.
    }

    private suspend fun LinkInlineTestData.setupBasicLink() {
        handler.setupLink(
            state = createLinkState(
                loginState = LinkState.LoginState.LoggedIn,
            )
        )

        whenever(linkConfigurationCoordinator.signInWithUserInput(any(), any()))
            .thenReturn(Result.success(true))
    }

    private suspend fun LinkInlineTestData.payWithLinkInline(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave
    ) {
        testScope.launch {
            handler.payWithLinkInline(
                UserInput.SignIn(email = "example@example.com"),
                cardSelection().copy(
                    customerRequestedSave = customerRequestedSave
                ),
                shouldCompleteLinkFlow
            )
        }
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
                last4 = "4242",
                expiryYear = 2024,
                expiryMonth = 4,
                brand = CardBrand.DinersClub,
                cvcCheck = CvcCheck.Fail,
                isDefault = false,
                billingAddress = ConsumerPaymentDetails.BillingAddress(
                    countryCode = CountryCode.US,
                    postalCode = "42424"
                )
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

private fun assertAndGetInlineLinkSelection(
    processingState: LinkHandler.ProcessingState
): PaymentSelection.New.LinkInline {
    assertThat(processingState).isInstanceOf<LinkHandler.ProcessingState.PaymentDetailsCollected>()

    val paymentDetailsCollectedState = processingState as LinkHandler.ProcessingState.PaymentDetailsCollected
    val selection = paymentDetailsCollectedState.paymentSelection

    assertThat(selection).isInstanceOf<PaymentSelection.New.LinkInline>()

    return selection as PaymentSelection.New.LinkInline
}

private fun runLinkTest(
    accountStatusFlow: MutableSharedFlow<AccountStatus> = MutableSharedFlow(replay = 1),
    linkConfiguration: LinkConfiguration = defaultLinkConfiguration(),
    attachNewCardToAccountResult: Result<LinkPaymentDetails>? = null,
    testBlock: suspend LinkTestData.() -> Unit
): Unit = runTest {
    val linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>()
    val savedStateHandle = SavedStateHandle()
    val linkAnalyticsHelper = mock<LinkAnalyticsHelper>()
    val linkStore = mock<LinkStore>()
    val handler = LinkHandler(
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

    val testScope = this
    turbineScope {
        val processingStateTurbine = handler.processingState.testIn(backgroundScope)

        whenever(linkConfigurationCoordinator.getAccountStatusFlow(eq(linkConfiguration))).thenReturn(accountStatusFlow)
        whenever(linkConfigurationCoordinator.attachNewCardToAccount(eq(linkConfiguration), any())).thenReturn(
            attachNewCardToAccountResult
        )

        with(
            LinkTestDataImpl(
                testScope = testScope,
                handler = handler,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                linkStore = linkStore,
                savedStateHandle = savedStateHandle,
                configuration = linkConfiguration,
                accountStatusFlow = accountStatusFlow,
                processingStateTurbine = processingStateTurbine,
                linkAnalyticsHelper = linkAnalyticsHelper,
            )
        ) {
            testBlock()
            processingStateTurbine.ensureAllEventsConsumed()
        }
    }
}

private fun LinkTestData.createLinkState(
    loginState: LinkState.LoginState,
    signupMode: LinkSignupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
): LinkState {
    return LinkState(
        loginState = loginState,
        signupMode = signupMode,
        configuration = configuration,
    )
}

private fun defaultLinkConfiguration(
    linkFundingSources: List<String> = emptyList(),
): LinkConfiguration {
    return LinkConfiguration(
        stripeIntent = PaymentIntentFactory.create(
            linkFundingSources = linkFundingSources,
        ),
        merchantName = "Merchant, Inc",
        merchantCountryCode = "US",
        customerInfo = LinkConfiguration.CustomerInfo(
            name = "Name",
            email = "customer@email.com",
            phone = "1234567890",
            billingCountryCode = "US",
        ),
        shippingDetails = null,
        passthroughModeEnabled = false,
        cardBrandChoice = null,
        flags = emptyMap(),
        useAttestationEndpointsForLink = true,
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
    override val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    override val linkStore: LinkStore,
    override val savedStateHandle: SavedStateHandle,
    override val configuration: LinkConfiguration,
    override val accountStatusFlow: MutableSharedFlow<AccountStatus>,
    override val processingStateTurbine: ReceiveTurbine<LinkHandler.ProcessingState>,
    override val linkAnalyticsHelper: LinkAnalyticsHelper,
) : LinkTestData

private interface LinkTestData {
    val testScope: TestScope
    val handler: LinkHandler
    val linkConfigurationCoordinator: LinkConfigurationCoordinator
    val linkStore: LinkStore
    val savedStateHandle: SavedStateHandle
    val configuration: LinkConfiguration
    val accountStatusFlow: MutableSharedFlow<AccountStatus>
    val processingStateTurbine: ReceiveTurbine<LinkHandler.ProcessingState>
    val linkAnalyticsHelper: LinkAnalyticsHelper
}

private class LinkInlineTestData(
    val shouldCompleteLinkFlow: Boolean,
    linkTestData: LinkTestData,
) : LinkTestData by linkTestData
