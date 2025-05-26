package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock

class DefaultWalletButtonsInteractorTest {
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
                isLinkEnabled = false,
                linkEmail = null,
                isGooglePayReady = true,
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
                isLinkEnabled = true,
                linkEmail = null,
                isGooglePayReady = false,
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
                isLinkEnabled = true,
                linkEmail = null,
                isGooglePayReady = true,
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
    fun `on init with GPay enabled, Link enabled, & different wallet type order, state should reflect the order`() =
        runTest {
            val interactor = createInteractor(
                arguments = createArguments(
                    availableWalletTypes = listOf(
                        WalletType.GooglePay,
                        WalletType.Link,
                    ),
                    isLinkEnabled = true,
                    isGooglePayReady = true,
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
            WalletButtonsInteractor.ViewAction.OnButtonPressed(WalletButtonsInteractor.WalletButton.Link(email = null))
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
                isLinkEnabled = true,
                linkState = null,
            ),
            errorReporter = errorReporter,
        )

        interactor.handleViewAction(
            WalletButtonsInteractor.ViewAction.OnButtonPressed(WalletButtonsInteractor.WalletButton.Link(email = null))
        )

        val call = errorReporter.awaitCall()

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
                isLinkEnabled = true,
                linkEmail = null,
                isGooglePayReady = true,
            ),
            confirmationHandler = FakeConfirmationHandler().apply {
                state.value = ConfirmationHandler.State.Confirming(
                    LinkConfirmationOption(
                        useLinkExpress = false,
                        configuration = mock()
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
                isLinkEnabled = true,
                linkEmail = null,
                isGooglePayReady = false,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            val button = state.walletButtons.first().asLinkWalletButton()

            assertThat(button.email).isNull()
        }
    }

    @Test
    fun `on Link button with an email, should have expected text`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                isLinkEnabled = true,
                linkEmail = "email@email.com",
                isGooglePayReady = false,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            val button = state.walletButtons.first().asLinkWalletButton()

            assertThat(button.email).isEqualTo("email@email.com")
        }
    }

    @Test
    fun `on Link button pressed, should call confirmation handler with expected selection`() = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val linkConfiguration = mock<LinkConfiguration>()
        val interactor = createInteractor(
            arguments = createArguments(
                isLinkEnabled = true,
                isGooglePayReady = false,
                linkState = LinkState(
                    configuration = linkConfiguration,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                )
            ),
            confirmationHandler = confirmationHandler,
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            interactor.handleViewAction(
                WalletButtonsInteractor.ViewAction.OnButtonPressed(state.walletButtons.first())
            )

            val arguments = confirmationHandler.startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                LinkConfirmationOption(
                    useLinkExpress = false,
                    configuration = linkConfiguration,
                )
            )
        }
    }

    @Test
    fun `on Google Pay button, should have expected state`() = googlePayButtonRenderTest(
        arguments = createArguments(
            isLinkEnabled = false,
            linkEmail = null,
            isGooglePayReady = true,
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
                isLinkEnabled = false,
                linkEmail = null,
                isGooglePayReady = true,
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
                isLinkEnabled = false,
                isGooglePayReady = true,
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
                WalletButtonsInteractor.ViewAction.OnButtonPressed(state.walletButtons.first())
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
                    )
                )
            )
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
        availableWalletTypes: List<WalletType> = listOf(WalletType.Link, WalletType.GooglePay),
        isLinkEnabled: Boolean = true,
        linkEmail: String? = null,
        isGooglePayReady: Boolean = true,
        appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        googlePay: PaymentSheet.GooglePayConfiguration? = null,
        linkState: LinkState? = null,
        cardBrandAcceptance: PaymentSheet.CardBrandAcceptance = PaymentSheet.CardBrandAcceptance.all(),
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.SetupIntent(clientSecret = "seti_123_secret_123")
    ): DefaultWalletButtonsInteractor.Arguments {
        return DefaultWalletButtonsInteractor.Arguments(
            isLinkEnabled = isLinkEnabled,
            linkEmail = linkEmail,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWalletTypes = availableWalletTypes,
                isGooglePayReady = isGooglePayReady,
                linkState = linkState,
            ),
            configuration = CommonConfigurationFactory.create(
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                googlePay = googlePay,
                cardBrandAcceptance = cardBrandAcceptance,
            ),
            appearance = appearance,
            initializationMode = initializationMode,
        )
    }

    private fun createInteractor(
        arguments: DefaultWalletButtonsInteractor.Arguments? = null,
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): DefaultWalletButtonsInteractor {
        return DefaultWalletButtonsInteractor(
            arguments = stateFlowOf(arguments),
            confirmationHandler = confirmationHandler,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            errorReporter = errorReporter,
        )
    }

    private fun WalletButtonsInteractor.WalletButton.asLinkWalletButton() =
        this as WalletButtonsInteractor.WalletButton.Link

    private fun WalletButtonsInteractor.WalletButton.asGooglePayWalletButton() =
        this as WalletButtonsInteractor.WalletButton.GooglePay
}
