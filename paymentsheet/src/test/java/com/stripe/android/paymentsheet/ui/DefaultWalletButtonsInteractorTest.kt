package com.stripe.android.paymentsheet.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.link.verification.NoOpLinkInlineInteractor
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.model.PassiveCaptchaParamsFactory
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.WalletButtonsViewClickHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.ButtonThemes.LinkButtonTheme
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.AnalyticEventCallbackRule
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalAnalyticEventCallbackApi::class, WalletButtonsPreview::class)
@Suppress("LargeClass")
class DefaultWalletButtonsInteractorTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @get:Rule
    val analyticsEventCallbackRule = AnalyticEventCallbackRule()

    @Test
    fun `on init with no arguments, state should be empty`() = runTest {
        val interactor = createInteractor(arguments = null)

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).isEmpty()
            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with GPay enabled in arguments, state should have only GPay button`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.GooglePay),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull())
                .isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with Link enabled in arguments, state should have only Link button`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with GPay & Link enabled in arguments, state should have both GPay & Link buttons`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(2)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()
            assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with GPay & Link enabled and should always be visible, state should have GPay & Link`() =
        walletsVisibilityTest(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            walletButtonsViewVisibility = mapOf(
                PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
            ),
        ) { state ->
            assertThat(state.walletButtons).hasSize(2)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()
            assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            assertThat(state.buttonsEnabled).isTrue()
        }

    @Test
    fun `on init with GPay & Link enabled and automatic visibility, state should have GPay & Link`() =
        walletsVisibilityTest(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            walletButtonsViewVisibility = mapOf(
                PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
            ),
        ) { state ->
            assertThat(state.walletButtons).hasSize(2)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()
            assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            assertThat(state.buttonsEnabled).isTrue()
        }

    @Test
    fun `on init with GPay & Link enabled but only Link allowed to be visible, state should have only Link`() =
        walletsVisibilityTest(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            walletButtonsViewVisibility = mapOf(
                PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
            ),
        ) { state ->
            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            assertThat(state.buttonsEnabled).isTrue()
        }

    @Test
    fun `on init with GPay & Link enabled but only GPay can be visible, state should have only GPay`() =
        walletsVisibilityTest(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            walletButtonsViewVisibility = mapOf(
                PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
            ),
        ) { state ->
            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            assertThat(state.buttonsEnabled).isTrue()
        }

    @Test
    fun `on init with GPay & Link enabled but cannot be visible, state should no buttons`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                walletButtonsViewVisibility = mapOf(
                    PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                ),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(0)
            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with GPay enabled, Link enabled, & different wallet type order, state should reflect the order`() =
        runTest {
            val interactor = createInteractor(
                arguments = createArguments(
                    availableWallets = listOf(WalletType.GooglePay, WalletType.Link),
                )
            )

            interactor.state.test {
                val state = awaitItem()

                assertThat(state.walletButtons).hasSize(2)
                assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()
                assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

                assertThat(state.buttonsEnabled).isTrue()
            }
        }

    @Test
    fun `on button pressed with no arguments, should report unexpected error`() = runTest {
        val errorReporter = FakeErrorReporter()
        val interactor = createInteractor(
            arguments = null,
            errorReporter = errorReporter,
        )

        interactor.handleViewAction(
            WalletButtonsInteractor.ViewAction.OnButtonPressed(
                button = WalletButtonsInteractor.WalletButton.Link(
                    state = LinkButtonState.Default,
                    theme = LinkButtonTheme.DEFAULT,
                ),
                clickHandler = { false },
            )
        )

        analyticsEventCallbackRule.assertMatchesExpectedEvent(
            AnalyticEvent.TapsButtonInWalletsButtonsView(walletType = "link")
        )

        val call = errorReporter.awaitCall()

        assertThat(call.errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.WALLET_BUTTONS_NULL_WALLET_ARGUMENTS_ON_CONFIRM)
        assertThat(call.stripeException).isNull()
        assertThat(call.additionalNonPiiParams).isEmpty()

        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `on button pressed with no confirmation option, should report unexpected error`() = runTest {
        val errorReporter = FakeErrorReporter()
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link),
                linkState = null,
            ),
            errorReporter = errorReporter,
        )

        interactor.handleViewAction(
            WalletButtonsInteractor.ViewAction.OnButtonPressed(
                button = WalletButtonsInteractor.WalletButton.Link(
                    state = LinkButtonState.Default,
                    theme = LinkButtonTheme.DEFAULT,
                ),
                clickHandler = { false },
            )
        )

        val call = errorReporter.awaitCall()

        analyticsEventCallbackRule.assertMatchesExpectedEvent(
            AnalyticEvent.TapsButtonInWalletsButtonsView(walletType = "link")
        )

        assertThat(call.errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.WALLET_BUTTONS_NULL_CONFIRMATION_ARGS_ON_CONFIRM)
        assertThat(call.stripeException).isNull()
        assertThat(call.additionalNonPiiParams).isEmpty()

        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `on confirmation handler processing, state for buttons should be disabled`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                linkEmail = null,
            ),
            confirmationHandler = FakeConfirmationHandler().apply {
                state.value = ConfirmationHandler.State.Confirming(
                    LinkConfirmationOption(
                        linkExpressMode = LinkExpressMode.DISABLED,
                        configuration = mock(),
                        passiveCaptchaParams = PassiveCaptchaParamsFactory.passiveCaptchaParams()
                    )
                )
            }
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.buttonsEnabled).isFalse()
        }
    }

    @Test
    fun `on Link button without an email, should have expected text`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            val button = state.walletButtons.first().asLinkWalletButton()

            assertThat(button.state).isInstanceOf<LinkButtonState.Default>()
        }
    }

    @Test
    fun `on Link button with an email, should have expected text`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link),
                linkEmail = "email@email.com",
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            val button = state.walletButtons.first().asLinkWalletButton()

            val buttonState = button.state as LinkButtonState.Email
            assertThat(buttonState.email).isEqualTo("email@email.com")
        }
    }

    @Test
    fun `on Link button pressed with valid configuration, should call linkPaymentLauncher`() = runTest {
        RecordingLinkPaymentLauncher.test {
            val linkConfiguration = mock<LinkConfiguration>()
            val interactor = createInteractor(
                arguments = createArguments(
                    availableWallets = listOf(WalletType.Link),
                    linkState = LinkState(
                        configuration = linkConfiguration,
                        loginState = LinkState.LoginState.LoggedOut,
                        signupMode = null,
                    )
                ),
                linkPaymentLauncher = launcher,
            )

            interactor.state.test {
                val state = awaitItem()

                assertThat(state.walletButtons).hasSize(1)
                assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

                interactor.handleViewAction(
                    WalletButtonsInteractor.ViewAction.OnButtonPressed(state.walletButtons.first()) { false }
                )
            }

            analyticsEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.TapsButtonInWalletsButtonsView(walletType = "link")
            )

            val call = presentCalls.awaitItem()
            assertThat(call.configuration).isEqualTo(linkConfiguration)
            assertThat(call.linkExpressMode).isNotEqualTo(LinkExpressMode.DISABLED)
        }
    }

    @Test
    fun `on Link button pressed, should pass attestOnIntentConfirmation true to linkPaymentLauncher`() = runTest {
        testLinkButtonWithAttestOnIntentConfirmation(attestOnIntentConfirmation = true)
    }

    @Test
    fun `on Link button pressed, should pass attestOnIntentConfirmation false to linkPaymentLauncher`() = runTest {
        testLinkButtonWithAttestOnIntentConfirmation(attestOnIntentConfirmation = false)
    }

    private suspend fun testLinkButtonWithAttestOnIntentConfirmation(attestOnIntentConfirmation: Boolean) {
        RecordingLinkPaymentLauncher.test {
            val linkConfiguration = mock<LinkConfiguration>()
            val interactor = createInteractor(
                arguments = createArguments(
                    availableWallets = listOf(WalletType.Link),
                    linkState = LinkState(
                        configuration = linkConfiguration,
                        loginState = LinkState.LoginState.LoggedOut,
                        signupMode = null,
                    ),
                    attestOnIntentConfirmation = attestOnIntentConfirmation
                ),
                linkPaymentLauncher = launcher,
            )

            interactor.state.test {
                val state = awaitItem()

                assertThat(state.walletButtons).hasSize(1)
                assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

                interactor.handleViewAction(
                    WalletButtonsInteractor.ViewAction.OnButtonPressed(state.walletButtons.first()) { false }
                )
            }

            analyticsEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.TapsButtonInWalletsButtonsView(walletType = "link")
            )

            val call = presentCalls.awaitItem()
            assertThat(call.attestOnIntentConfirmation).isEqualTo(attestOnIntentConfirmation)
        }
    }

    @Test
    fun `on Google Pay button, should have expected state`() = googlePayButtonRenderTest(
        arguments = createArguments(
            availableWallets = listOf(WalletType.GooglePay),
            linkEmail = null,
        ),
        expectedAllowCreditCards = true,
        expectedGooglePayButtonType = GooglePayButtonType.Pay,
        expectedBillingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
            isRequired = false,
            format = GooglePayJsonFactory.BillingAddressParameters.Format.Min,
            isPhoneNumberRequired = false
        ),
        expectedCardBrandFilter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.all())
    )

    @Test
    fun `on Google Pay button with configuration for GPay & card acceptance, should have expected state`() =
        googlePayButtonRenderTest(
            arguments = createArguments(
                availableWallets = listOf(WalletType.GooglePay),
                linkEmail = null,
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US",
                    buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Book,
                ),
                cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                    brands = listOf(
                        PaymentSheet.CardBrandAcceptance.BrandCategory.Amex,
                        PaymentSheet.CardBrandAcceptance.BrandCategory.Discover,
                    )
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                )
            ),
            expectedAllowCreditCards = true,
            expectedGooglePayButtonType = GooglePayButtonType.Book,
            expectedBillingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = true,
                format = GooglePayJsonFactory.BillingAddressParameters.Format.Full,
                isPhoneNumberRequired = true
            ),
            expectedCardBrandFilter = PaymentSheetCardBrandFilter(
                PaymentSheet.CardBrandAcceptance.disallowed(
                    brands = listOf(
                        PaymentSheet.CardBrandAcceptance.BrandCategory.Amex,
                        PaymentSheet.CardBrandAcceptance.BrandCategory.Discover,
                    )
                )
            )
        )

    @Test
    fun `On Google Pay pressed, should call confirmation handler with expected selection`() = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
            brands = listOf(
                PaymentSheet.CardBrandAcceptance.BrandCategory.Amex,
                PaymentSheet.CardBrandAcceptance.BrandCategory.Discover,
            )
        )
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        )
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.GooglePay),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                    countryCode = "CA",
                    amount = 5050L,
                    currencyCode = "CAD",
                    label = "This is a purchase!"
                ),
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                cardBrandAcceptance = cardBrandAcceptance,
            ),
            confirmationHandler = confirmationHandler,
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull())
                .isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            interactor.handleViewAction(
                WalletButtonsInteractor.ViewAction.OnButtonPressed(state.walletButtons.first()) { false }
            )

            analyticsEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.TapsButtonInWalletsButtonsView(walletType = "google_pay")
            )

            val arguments = confirmationHandler.startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                GooglePayConfirmationOption(
                    config = GooglePayConfirmationOption.Config(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                        merchantName = "Example, Inc.",
                        merchantCountryCode = "CA",
                        merchantCurrencyCode = "CAD",
                        customAmount = 5050L,
                        customLabel = "This is a purchase!",
                        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                        cardBrandFilter = PaymentSheetCardBrandFilter(cardBrandAcceptance)
                    ),
                    passiveCaptchaParams = null,
                    clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
                )
            )
        }
    }

    @Test
    fun `On wallet buttons rendered, should call provided callback with true`() = runTest {
        val completable = CompletableDeferred<Boolean>()

        val interactor = createInteractor { isRendered ->
            completable.complete(isRendered)
        }

        interactor.handleViewAction(WalletButtonsInteractor.ViewAction.OnShown)

        assertThat(completable.await()).isTrue()
    }

    @Test
    fun `On wallet buttons un-rendered, should call provided callback with false`() = runTest {
        val completable = CompletableDeferred<Boolean>()

        val interactor = createInteractor { isRendered ->
            completable.complete(isRendered)
        }

        interactor.handleViewAction(WalletButtonsInteractor.ViewAction.OnHidden)

        assertThat(completable.await()).isFalse()
    }

    @Test
    fun `on init with ShopPay enabled in arguments, state should have only ShopPay button`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.ShopPay),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull())
                .isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with GPay, Link & ShopPay enabled in arguments, state should have all buttons`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay, WalletType.ShopPay),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(3)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()
            assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()
            assertThat(state.walletButtons[2]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with all wallets enabled but only ShopPay visible, state should have only ShopPay`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay, WalletType.ShopPay),
                walletButtonsViewVisibility = mapOf(
                    PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                ),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with ShopPay & GPay enabled, different wallet type order, state should reflect the order`() =
        runTest {
            val interactor = createInteractor(
                arguments = createArguments(
                    availableWallets = listOf(WalletType.ShopPay, WalletType.GooglePay),
                    linkEmail = null,
                )
            )

            interactor.state.test {
                val state = awaitItem()

                assertThat(state.walletButtons).hasSize(2)
                assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()
                assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

                assertThat(state.buttonsEnabled).isTrue()
            }
        }

    @Test
    fun `on ShopPay button, should have expected default state`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.ShopPay),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull())
                .isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()

            assertThat(state.walletButtons.first())
                .isInstanceOf(WalletButtonsInteractor.WalletButton.ShopPay::class.java)
        }
    }

    @Test
    fun `on ShopPay available & should always be visible, should show ShopPay button`() =
        walletsVisibilityTest(
            availableWallets = listOf(WalletType.ShopPay),
            walletButtonsViewVisibility = mapOf(
                PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
            ),
        ) { state ->
            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()
            assertThat(state.buttonsEnabled).isTrue()
        }

    @Test
    fun `on ShopPay available but not allowed to be visible by merchant, should not show ShopPay button`() =
        walletsVisibilityTest(
            availableWallets = listOf(WalletType.ShopPay),
            walletButtonsViewVisibility = mapOf(
                PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
            ),
        ) { state ->
            assertThat(state.walletButtons).hasSize(0)
        }

    @Test
    fun `on init with ShopPay not available, state should not have ShopPay button`() = walletsVisibilityTest(
        availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        walletButtonsViewVisibility = mapOf(
            PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
            PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
            PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
        ),
    ) { state ->
        assertThat(state.walletButtons).hasSize(2)
        assertThat(state.walletButtons.none { it is WalletButtonsInteractor.WalletButton.ShopPay }).isTrue()
        assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()
        assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()
    }

    @Test
    fun `on init with only ShopPay available and visible, state should have only ShopPay`() = walletsVisibilityTest(
        availableWallets = listOf(WalletType.ShopPay),
        walletButtonsViewVisibility = mapOf(
            PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
            PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
            PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
        ),
    ) { state ->
        assertThat(state.walletButtons).hasSize(1)
        assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()
        assertThat(state.buttonsEnabled).isTrue()
    }

    @Test
    fun `when linkEnableDisplayableDefaultValuesInEce is false, should not use payment details`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link),
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION.copy(
                        enableDisplayableDefaultValuesInEce = false
                    ),
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
                linkEmail = "test@example.com"
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            val button = state.walletButtons.first().asLinkWalletButton()

            // Should show email since payment details are disabled by flag
            val buttonState = button.state as LinkButtonState.Email
            assertThat(buttonState.email).isEqualTo("test@example.com")
        }
    }

    @Test
    fun `when linkEnableDisplayableDefaultValuesInEce is true, should use payment details in the button`() =
        runTest {
            val paymentDetails = DisplayablePaymentDetails(
                defaultCardBrand = "VISA",
                defaultPaymentType = "CARD",
                last4 = "4242",
                numberOfSavedPaymentDetails = 3L
            )

            val linkAccount = LinkAccount(
                consumerSession = TestFactory.CONSUMER_SESSION,
                consumerPublishableKey = TestFactory.PUBLISHABLE_KEY,
                displayablePaymentDetails = paymentDetails
            )

            val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
            linkAccountHolder.set(LinkAccountUpdate.Value(linkAccount))

            val interactor = createInteractor(
                arguments = createArguments(
                    availableWallets = listOf(WalletType.Link),
                    linkEmail = "test@example.com",
                    linkState = LinkState(
                        configuration = TestFactory.LINK_CONFIGURATION.copy(
                            enableDisplayableDefaultValuesInEce = true
                        ),
                        loginState = LinkState.LoginState.LoggedOut,
                        signupMode = null,
                    )
                ),
                linkAccountHolder = linkAccountHolder
            )

            interactor.state.test {
                val state = awaitItem()

                assertThat(state.walletButtons).hasSize(1)
                val button = state.walletButtons.first().asLinkWalletButton()

                val buttonState = button.state as LinkButtonState.DefaultPayment
                assertThat(buttonState.paymentUI.last4).isEqualTo("4242")
            }
        }

    @Test
    fun `on init with mixed wallet order including ShopPay, state should preserve order`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.GooglePay, WalletType.ShopPay, WalletType.Link),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(3)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()
            assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()
            assertThat(state.walletButtons[2]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()
        }
    }

    @Test
    fun `on confirmation handler processing with ShopPay, buttons should be disabled`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.ShopPay),
                linkEmail = null,
            ),
            confirmationHandler = FakeConfirmationHandler().apply {
                state.value = ConfirmationHandler.State.Confirming(
                    LinkConfirmationOption(
                        linkExpressMode = LinkExpressMode.DISABLED,
                        configuration = mock(),
                        passiveCaptchaParams = null
                    )
                )
            }
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()
            assertThat(state.buttonsEnabled).isFalse()
        }
    }

    @Test
    fun `when click handler returns true, should not proceed with default action`() = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val walletTypeTurbine = Turbine<String>()

        val walletButtonsViewClickHandler = WalletButtonsViewClickHandler { walletType ->
            walletTypeTurbine.add(walletType)
            true
        }

        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.GooglePay),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    amount = 0L,
                    countryCode = "CA",
                ),
            ),
            confirmationHandler = confirmationHandler,
        )

        turbineScope {
            val stateTurbine = interactor.state.testIn(this)

            val state = stateTurbine.awaitItem()
            val googlePayButton = state.walletButtons.first()

            interactor.handleViewAction(
                WalletButtonsInteractor.ViewAction.OnButtonPressed(googlePayButton, walletButtonsViewClickHandler)
            )

            assertThat(walletTypeTurbine.awaitItem()).isEqualTo("google_pay")

            analyticsEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.TapsButtonInWalletsButtonsView(walletType = "google_pay")
            )

            confirmationHandler.startTurbine.expectNoEvents()

            stateTurbine.ensureAllEventsConsumed()
            walletTypeTurbine.ensureAllEventsConsumed()

            stateTurbine.cancel()
            walletTypeTurbine.cancel()
        }
    }

    @Test
    fun `when click handler returns false, should proceed with default action`() = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val walletTypeTurbine = Turbine<String>()

        val walletButtonsViewClickHandler = WalletButtonsViewClickHandler { walletType ->
            walletTypeTurbine.add(walletType)
            false
        }

        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.GooglePay),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    amount = 0L,
                    countryCode = "CA",
                ),
            ),
            confirmationHandler = confirmationHandler,
        )

        turbineScope {
            val stateTurbine = interactor.state.testIn(this)

            val state = stateTurbine.awaitItem()
            val googlePayButton = state.walletButtons.first()

            interactor.handleViewAction(
                WalletButtonsInteractor.ViewAction.OnButtonPressed(googlePayButton, walletButtonsViewClickHandler)
            )

            assertThat(walletTypeTurbine.awaitItem()).isEqualTo("google_pay")

            analyticsEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.TapsButtonInWalletsButtonsView(walletType = "google_pay")
            )

            val arguments = confirmationHandler.startTurbine.awaitItem()
            assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

            stateTurbine.ensureAllEventsConsumed()
            walletTypeTurbine.ensureAllEventsConsumed()

            stateTurbine.cancel()
            walletTypeTurbine.cancel()
        }
    }

    private fun walletsVisibilityTest(
        availableWallets: List<WalletType>,
        walletButtonsViewVisibility: Map<
            PaymentSheet.WalletButtonsConfiguration.Wallet,
            PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility
            >,
        test: (WalletButtonsInteractor.State) -> Unit
    ) = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = availableWallets,
                walletButtonsViewVisibility = walletButtonsViewVisibility,
            )
        )

        interactor.state.test {
            test(awaitItem())
        }
    }

    private fun googlePayButtonRenderTest(
        arguments: DefaultWalletButtonsInteractor.Arguments,
        expectedGooglePayButtonType: GooglePayButtonType,
        expectedAllowCreditCards: Boolean,
        expectedCardBrandFilter: CardBrandFilter,
        expectedBillingAddressParameters: GooglePayJsonFactory.BillingAddressParameters,
    ) = runTest {
        val interactor = createInteractor(
            arguments = arguments,
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull())
                .isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            val actualButton = state.walletButtons.first().asGooglePayWalletButton()

            assertThat(actualButton.googlePayButtonType).isEqualTo(expectedGooglePayButtonType)
            assertThat(actualButton.allowCreditCards).isEqualTo(expectedAllowCreditCards)
            assertThat(actualButton.cardBrandFilter).isEqualTo(expectedCardBrandFilter)
            assertThat(actualButton.billingAddressParameters).isEqualTo(expectedBillingAddressParameters)
        }
    }

    private fun createArguments(
        availableWallets: List<WalletType> = listOf(WalletType.Link, WalletType.GooglePay, WalletType.ShopPay),
        linkEmail: String? = null,
        appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        googlePay: PaymentSheet.GooglePayConfiguration? = null,
        linkState: LinkState? = null,
        cardBrandAcceptance: PaymentSheet.CardBrandAcceptance = PaymentSheet.CardBrandAcceptance.all(),
        walletButtonsViewVisibility: Map<
            PaymentSheet.WalletButtonsConfiguration.Wallet,
            PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility
            > = emptyMap(),
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.SetupIntent(clientSecret = "seti_123_secret_123"),
        attestOnIntentConfirmation: Boolean = false
    ): DefaultWalletButtonsInteractor.Arguments {
        return DefaultWalletButtonsInteractor.Arguments(
            linkEmail = linkEmail,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWallets = availableWallets,
                linkState = linkState,
                clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
                attestOnIntentConfirmation = attestOnIntentConfirmation,
            ),
            configuration = CommonConfigurationFactory.create(
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                googlePay = googlePay,
                cardBrandAcceptance = cardBrandAcceptance,
                walletButtons = PaymentSheet.WalletButtonsConfiguration(
                    visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                        walletButtonsView = walletButtonsViewVisibility,
                    )
                ),
            ),
            appearance = appearance,
            initializationMode = initializationMode,
            paymentSelection = null,
        )
    }

    private fun createInteractor(
        arguments: DefaultWalletButtonsInteractor.Arguments? = null,
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        linkPaymentLauncher: LinkPaymentLauncher = RecordingLinkPaymentLauncher.noOp(),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        onWalletButtonsRenderStateChanged: (isRendered: Boolean) -> Unit = {
            error("Should not be called!")
        },
    ): DefaultWalletButtonsInteractor {
        return DefaultWalletButtonsInteractor(
            arguments = stateFlowOf(arguments),
            confirmationHandler = confirmationHandler,
            coroutineScope = CoroutineScope(testDispatcher),
            errorReporter = errorReporter,
            linkInlineInteractor = NoOpLinkInlineInteractor(),
            linkPaymentLauncher = linkPaymentLauncher,
            linkAccountHolder = linkAccountHolder,
            analyticsCallbackProvider = { analyticsEventCallbackRule },
            onWalletButtonsRenderStateChanged = onWalletButtonsRenderStateChanged,
        )
    }

    private fun WalletButtonsInteractor.WalletButton.asLinkWalletButton() =
        this as WalletButtonsInteractor.WalletButton.Link

    private fun WalletButtonsInteractor.WalletButton.asGooglePayWalletButton() =
        this as WalletButtonsInteractor.WalletButton.GooglePay
}
