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
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = listOf(
                WalletType.Link,
                WalletType.GooglePay,
            )
        )
        val interactor = createInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
        )

        assertThat(interactor.state.value.expressButtons).containsExactly(
            ExpressButton.Link.create(
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountInfo = LinkAccountUpdate.Value(null),
            ),
            ExpressButton.GooglePay.create(
                paymentMethodMetadata,
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
    fun `available button types keeps only wallets returned by metadata`() {
        val availableExpressButtonTypes = createAvailableButtonTypes(
            availableWallets = listOf(WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            WalletType.GooglePay
        )
    }

    @Test
    fun `available button types filters out wallets disabled by configuration`() {
        val availableExpressButtonTypes = createAvailableButtonTypes(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            WalletType.GooglePay
        )
    }

    @Test
    fun `available button types returns all available wallets`() {
        val availableExpressButtonTypes = createAvailableButtonTypes(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            WalletType.Link,
            WalletType.GooglePay,
        )
    }

    private fun createInteractor(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        configuration: ExpressCheckoutElement.Configuration = ExpressCheckoutElement.Configuration(),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
    ): DefaultExpressCheckoutElementInteractor {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateHolder(
            savedStateHandle = savedStateHandle,
            errorReporter = FakeErrorReporter(),
            paymentOptionFactory = { _, _ -> null },
            availableExpressButtonTypesFactory = DefaultAvailableExpressButtonTypesFactory(),
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
        )
    }

    private fun createAvailableButtonTypes(
        availableWallets: List<WalletType> = emptyList(),
        configuration: ExpressCheckoutElement.Configuration = ExpressCheckoutElement.Configuration(),
    ): List<WalletType> {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = availableWallets,
        )

        return DefaultAvailableExpressButtonTypesFactory().create(
            paymentMethodMetadata = paymentMethodMetadata,
            expressCheckoutElementConfiguration = configuration.build(),
        )
    }
}
