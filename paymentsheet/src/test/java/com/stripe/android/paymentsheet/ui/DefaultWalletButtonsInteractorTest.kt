package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DefaultWalletButtonsInteractorTest {
    private val paymentConfigurationTestRule = PaymentConfigurationTestRule(
        context = ApplicationProvider.getApplicationContext(),
    )

    private val composeTestRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(composeTestRule)
        .around(paymentConfigurationTestRule)

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
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<GooglePayWalletButton>()

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
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<LinkWalletButton>()

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
            assertThat(state.walletButtons[0]).isInstanceOf<LinkWalletButton>()
            assertThat(state.walletButtons[1]).isInstanceOf<GooglePayWalletButton>()

            assertThat(state.buttonsEnabled).isTrue()
        }
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
                        selectedLinkPayment = null,
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
    fun `on Link button render without an email, should have expected text`() = runTest {
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
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<LinkWalletButton>()

            val button = state.walletButtons.first().asLinkWalletButton()

            composeTestRule.setContent {
                button.Content(enabled = true)
            }

            composeTestRule.waitUntilExactlyOneExists(hasText(text = "Pay with", substring = true))
        }
    }

    @Test
    fun `on Link button render with an email, should have expected text`() = runTest {
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
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<LinkWalletButton>()

            val button = state.walletButtons.first().asLinkWalletButton()

            composeTestRule.setContent {
                button.Content(enabled = true)
            }

            composeTestRule.waitUntilExactlyOneExists(hasText(text = "email@email.com", substring = true))
        }
    }

    @Test
    fun `on Link button rendered and disabled, should have expected state`() = runTest {
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
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<LinkWalletButton>()

            val button = state.walletButtons.first().asLinkWalletButton()

            composeTestRule.setContent {
                button.Content(enabled = false)
            }

            composeTestRule.waitUntilExactlyOneExists(hasTestTag(LinkButtonTestTag).and(isNotEnabled()))
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
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<LinkWalletButton>()

            val button = state.walletButtons.first().asLinkWalletButton()

            composeTestRule.setContent {
                button.Content(enabled = true)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNode(hasTestTag(LinkButtonTestTag)).performClick()
            composeTestRule.waitForIdle()

            val arguments = confirmationHandler.startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                LinkConfirmationOption(
                    useLinkExpress = false,
                    selectedLinkPayment = null,
                    configuration = linkConfiguration,
                )
            )
        }
    }

    @Test
    fun `on Google Pay button render, should have expected state`() = googlePayButtonRenderTest(
        arguments = createArguments(
            isLinkEnabled = false,
            linkEmail = null,
            isGooglePayReady = true,
        ),
        expectedConfig = GooglePayButtonConfig(
            allowCreditCards = true,
            buttonType = GooglePayButtonType.Pay,
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = false,
                format = GooglePayJsonFactory.BillingAddressParameters.Format.Min,
                isPhoneNumberRequired = false
            ),
            cardBrandFilter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.all())
        )
    )

    @Test
    fun `on Google Pay button render with configuration for GPay & card acceptance, should have expected state`() =
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
            expectedConfig = GooglePayButtonConfig(
                allowCreditCards = true,
                buttonType = GooglePayButtonType.Book,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                    isRequired = true,
                    format = GooglePayJsonFactory.BillingAddressParameters.Format.Full,
                    isPhoneNumberRequired = true
                ),
                cardBrandFilter = PaymentSheetCardBrandFilter(
                    PaymentSheet.CardBrandAcceptance.disallowed(
                        brands = listOf(
                            PaymentSheet.CardBrandAcceptance.BrandCategory.Amex,
                            PaymentSheet.CardBrandAcceptance.BrandCategory.Discover,
                        )
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
            assertThat(state.walletButtons[0]).isInstanceOf<GooglePayWalletButton>()

            val button = state.walletButtons.first().asGooglePayWalletButton()

            composeTestRule.setContent {
                button.Content(enabled = true)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNode(hasTestTag(GOOGLE_PAY_BUTTON_TEST_TAG)).performClick()
            composeTestRule.waitForIdle()

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
        expectedConfig: GooglePayButtonConfig,
    ) = runTest {
        val interactor = createInteractor(
            arguments = arguments,
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.walletButtons).hasSize(1)
            assertThat(state.walletButtons.firstOrNull()).isInstanceOf<GooglePayWalletButton>()

            val button = state.walletButtons.first().asGooglePayWalletButton()

            composeTestRule.setContent {
                button.Content(enabled = true)
            }

            composeTestRule.waitUntilExactlyOneExists(hasGooglePayButtonConfig(expectedConfig))
        }
    }

    private fun createArguments(
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
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler()
    ): DefaultWalletButtonsInteractor {
        return DefaultWalletButtonsInteractor(
            arguments = stateFlowOf(arguments),
            confirmationHandler = confirmationHandler,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
        )
    }

    private fun hasGooglePayButtonConfig(
        expectedConfig: GooglePayButtonConfig
    ) = SemanticsMatcher(
        description = "google_pay_button_config"
    ) { node ->
        val actualConfig = node.config.getOrNull(GPayButtonConfig)

        expectedConfig == actualConfig
    }

    private fun WalletButtonsInteractor.WalletButton.asLinkWalletButton() = this as LinkWalletButton

    private fun WalletButtonsInteractor.WalletButton.asGooglePayWalletButton() = this as GooglePayWalletButton
}
