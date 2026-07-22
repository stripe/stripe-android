package com.stripe.android.checkout.ece

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultExpressCheckoutElementInteractorTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val paymentConfigurationRule = PaymentConfigurationTestRule(applicationContext)

    @Test
    fun `state contains provided express buttons`() {
        val googlePayConfiguration = createGooglePayConfiguration()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = listOf(
                WalletType.Link,
                WalletType.GooglePay,
            )
        )
        val interactor = createInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            googlePayConfiguration = googlePayConfiguration,
        )

        assertThat(interactor.state.value.expressButtons).containsExactly(
            ExpressButton.Link.create(
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountInfo = LinkAccountUpdate.Value(null),
            ),
            ExpressButton.GooglePay.create(
                paymentMethodMetadata = paymentMethodMetadata,
                googlePayConfiguration = googlePayConfiguration
            ),
        )
    }

    @Test
    fun `state updates when link account info changes`() = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = listOf(WalletType.Link),
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    enableDisplayableDefaultValuesInEce = true,
                ),
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null,
            ),
        )
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val interactor = createInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountHolder = linkAccountHolder,
        )
        val linkAccount = TestFactory.LINK_ACCOUNT.copy(
            displayablePaymentDetails = DisplayablePaymentDetails(
                defaultCardBrand = "VISA",
                defaultPaymentType = "CARD",
                last4 = "4242",
                numberOfSavedPaymentDetails = 3L,
            ),
        )

        interactor.state.test {
            assertThat(awaitItem()).isEqualTo(
                ExpressCheckoutElementInteractor.State(
                    expressButtons = listOf(
                        ExpressButton.Link.create(
                            paymentMethodMetadata = paymentMethodMetadata,
                            linkAccountInfo = LinkAccountUpdate.Value(null),
                        ),
                    ),
                ),
            )

            linkAccountHolder.set(LinkAccountUpdate.Value(linkAccount))

            assertThat(awaitItem()).isEqualTo(
                ExpressCheckoutElementInteractor.State(
                    expressButtons = listOf(
                        ExpressButton.Link.create(
                            paymentMethodMetadata = paymentMethodMetadata,
                            linkAccountInfo = LinkAccountUpdate.Value(linkAccount),
                        ),
                    ),
                ),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects available express button types from checkoutSession`() {
        val googlePayConfiguration = createGooglePayConfiguration(
            buttonType = GooglePayConfiguration.ButtonType.Checkout,
            additionalEnabledNetworks = listOf("INTERAC"),
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = listOf(
                WalletType.Link,
                WalletType.GooglePay,
            )
        )
        val availableExpressButtonTypes = listOf(
            ExpressButtonType.GooglePay(
                googlePayConfiguration = googlePayConfiguration,
            )
        )
        val interactor = createInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            googlePayConfiguration = googlePayConfiguration,
            availableExpressButtonTypes = availableExpressButtonTypes,
        )

        assertThat(interactor.state.value.expressButtons).containsExactly(
            ExpressButton.GooglePay.create(
                paymentMethodMetadata = paymentMethodMetadata,
                googlePayConfiguration = googlePayConfiguration,
            ),
        )
    }

    @Test
    fun `handleViewAction OnDisplayed reports displayed event`() {
        val eventReporter = FakeExpressCheckoutElementEventReporter()
        val interactor = createInteractor(
            eventReporter = eventReporter,
        )

        interactor.handleViewAction(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)

        assertThat(eventReporter.calls)
            .containsExactly(FakeExpressCheckoutElementEventReporter.Call.OnEceDisplayed)
    }

    @Test
    fun `handleViewAction OnDisplayed only reports displayed event once across restored interactors`() {
        val savedStateHandle = SavedStateHandle()
        val eventReporter = FakeExpressCheckoutElementEventReporter()
        val firstInteractor = createInteractor(
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
        )
        val restoredInteractor = createInteractor(
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
        )

        firstInteractor.handleViewAction(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)
        restoredInteractor.handleViewAction(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)

        assertThat(eventReporter.calls)
            .containsExactly(FakeExpressCheckoutElementEventReporter.Call.OnEceDisplayed)
    }

    @Test
    fun `handleViewAction OnWalletTapped reports wallet tapped event`() {
        val eventReporter = FakeExpressCheckoutElementEventReporter()
        val interactor = createInteractor(
            eventReporter = eventReporter,
        )

        interactor.handleViewAction(ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped)

        assertThat(eventReporter.calls)
            .containsExactly(FakeExpressCheckoutElementEventReporter.Call.OnEceWalletTapped)
    }

    private fun createInteractor(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        configuration: ExpressCheckoutElement.Configuration = ExpressCheckoutElement.Configuration(),
        googlePayConfiguration: GooglePayConfiguration.State = createGooglePayConfiguration(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        eventReporter: ExpressCheckoutElementEventReporter = FakeExpressCheckoutElementEventReporter(),
        availableExpressButtonTypes: List<ExpressButtonType> = paymentMethodMetadata.availableWallets.map {
            when (it) {
                WalletType.Link -> ExpressButtonType.Link
                WalletType.GooglePay -> ExpressButtonType.GooglePay(googlePayConfiguration)
            }
        },
    ): DefaultExpressCheckoutElementInteractor {
        val stateHolder = CheckoutControllerStateHolder(
            savedStateHandle = savedStateHandle,
            errorReporter = FakeErrorReporter(),
            paymentOptionFactory = { _, _ -> null },
            availableExpressButtonTypesFactory = FakeAvailableExpressButtonTypesFactory(
                availableExpressButtonTypes = availableExpressButtonTypes,
            ),
        )
        stateHolder.state = CheckoutControllerStateFactory.create(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = CheckoutController.Configuration()
                .expressCheckoutElement(configuration)
                .build(),
        )

        return DefaultExpressCheckoutElementInteractor(
            linkAccountHolder = linkAccountHolder,
            stateHolder = stateHolder,
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
        )
    }

    private fun createGooglePayConfiguration(
        buttonType: GooglePayConfiguration.ButtonType = GooglePayConfiguration.ButtonType.Pay,
        additionalEnabledNetworks: List<String> = emptyList(),
    ): GooglePayConfiguration.State {
        return GooglePayConfiguration(
            GooglePayConfiguration.Environment.Test,
        )
            .buttonType(buttonType)
            .additionalEnabledNetworks(additionalEnabledNetworks)
            .build()
    }
}
