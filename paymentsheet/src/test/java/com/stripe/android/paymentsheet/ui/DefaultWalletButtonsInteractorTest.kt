package com.stripe.android.paymentsheet.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.verification.NoOpLinkInlineInteractor
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
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock

@Suppress("LargeClass")
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
    fun `on init with GPay & Link enabled but only Link allowed, state should have only Link`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                allowedWalletTypes = listOf(WalletType.Link),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with GPay & Link enabled but only GPay allowed, state should have only GPay`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                allowedWalletTypes = listOf(WalletType.GooglePay),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()

            assertThat(state.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `on init with GPay & Link enabled but none allowed, state should no buttons`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                allowedWalletTypes = emptyList(),
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
                availableWallets = listOf(WalletType.Link),
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
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                linkEmail = null,
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
                availableWallets = listOf(WalletType.Link),
                linkEmail = null,
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
                availableWallets = listOf(WalletType.Link),
                linkEmail = "email@email.com",
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
                    WalletButtonsInteractor.ViewAction.OnButtonPressed(state.walletButtons.first())
                )
            }

            val call = presentCalls.awaitItem()
            assertThat(call.configuration).isEqualTo(linkConfiguration)
            assertThat(call.useLinkExpress).isTrue()
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
    fun `on init with all wallets enabled but only ShopPay allowed, state should have only ShopPay`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay, WalletType.ShopPay),
                allowedWalletTypes = listOf(WalletType.ShopPay),
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
    fun `on ShopPay available but not allowed by merchant, should not show ShopPay button`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.ShopPay),
                allowedWalletTypes = emptyList(),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(0)
        }
    }

    @Test
    fun `on init with ShopPay not available, state should not have ShopPay button`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
                allowedWalletTypes = listOf(WalletType.Link, WalletType.GooglePay, WalletType.ShopPay),
                linkEmail = null,
            )
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(2)
            assertThat(state.walletButtons.none { it is WalletButtonsInteractor.WalletButton.ShopPay }).isTrue()
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.Link>()
            assertThat(state.walletButtons[1]).isInstanceOf<WalletButtonsInteractor.WalletButton.GooglePay>()
        }
    }

    @Test
    fun `on init with only ShopPay available and allowed, state should have only ShopPay`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.ShopPay),
                allowedWalletTypes = listOf(WalletType.ShopPay),
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
    fun `on init with mixed wallet order including ShopPay, state should preserve order`() = runTest {
        val interactor = createInteractor(
            arguments = createArguments(
                availableWallets = listOf(WalletType.GooglePay, WalletType.ShopPay, WalletType.Link),
                allowedWalletTypes = listOf(WalletType.GooglePay, WalletType.ShopPay, WalletType.Link),
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
                allowedWalletTypes = listOf(WalletType.ShopPay),
                linkEmail = null,
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

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons[0]).isInstanceOf<WalletButtonsInteractor.WalletButton.ShopPay>()
            assertThat(state.buttonsEnabled).isFalse()
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
        allowedWalletTypes: List<WalletType> = listOf(WalletType.Link, WalletType.GooglePay, WalletType.ShopPay),
        linkEmail: String? = null,
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
            linkEmail = linkEmail,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWallets = availableWallets,
                linkState = linkState,
            ),
            configuration = CommonConfigurationFactory.create(
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                googlePay = googlePay,
                cardBrandAcceptance = cardBrandAcceptance,
            ),
            appearance = appearance,
            initializationMode = initializationMode,
            walletsAllowedByMerchant = allowedWalletTypes,
        )
    }

    private fun createInteractor(
        arguments: DefaultWalletButtonsInteractor.Arguments? = null,
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        linkPaymentLauncher: LinkPaymentLauncher = RecordingLinkPaymentLauncher.noOp(),
        onWalletButtonsRenderStateChanged: (isRendered: Boolean) -> Unit = {
            error("Should not be called!")
        },
    ): DefaultWalletButtonsInteractor {
        return DefaultWalletButtonsInteractor(
            arguments = stateFlowOf(arguments),
            confirmationHandler = confirmationHandler,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            errorReporter = errorReporter,
            linkInlineInteractor = NoOpLinkInlineInteractor(),
            linkPaymentLauncher = linkPaymentLauncher,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            onWalletButtonsRenderStateChanged = onWalletButtonsRenderStateChanged,
        )
    }

    private fun WalletButtonsInteractor.WalletButton.asLinkWalletButton() =
        this as WalletButtonsInteractor.WalletButton.Link

    private fun WalletButtonsInteractor.WalletButton.asGooglePayWalletButton() =
        this as WalletButtonsInteractor.WalletButton.GooglePay
}
