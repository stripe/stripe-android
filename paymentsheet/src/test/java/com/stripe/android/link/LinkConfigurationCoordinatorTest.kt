package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.LinkInlineSignupAssistedViewModelFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.utils.FakeAndroidKeyStore
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkConfigurationCoordinatorTest {
    private val linkComponentFactory: LinkComponent.Factory = mock()

    private var linkConfigurationCoordinator = RealLinkConfigurationCoordinator(linkComponentFactory)

    init {
        FakeAndroidKeyStore.setup()

        val configurationCapture = argumentCaptor<LinkConfiguration>()

        whenever(linkComponentFactory.create(configurationCapture.capture())).thenAnswer {
            val component = mock<LinkComponent>()
            val linkAccountManager = FakeLinkAccountManager()
            whenever(component.linkAccountManager).thenReturn(linkAccountManager)
            linkAccountManager.setAccountStatus(AccountStatus.Verified(consentPresentation = null))
            linkAccountManager.signInWithUserInputResult = Result.failure(IllegalStateException("Test"))

            whenever(component.configuration).thenReturn(configurationCapture.lastValue)

            component
        }
    }

    @Test
    fun `verify component is reused for same configuration`() = runTest {
        val component = linkConfigurationCoordinator.getComponent(TestFactory.LINK_CONFIGURATION)

        linkConfigurationCoordinator.getAccountStatusFlow(TestFactory.LINK_CONFIGURATION)
        linkConfigurationCoordinator.signInWithUserInput(TestFactory.LINK_CONFIGURATION, mock<UserInput.SignIn>())

        assertThat(linkConfigurationCoordinator.getComponent(TestFactory.LINK_CONFIGURATION)).isEqualTo(component)
    }

    @Test
    fun `verify component is recreated for different configuration`() {
        val component = linkConfigurationCoordinator.getComponent(TestFactory.LINK_CONFIGURATION)

        assertThat(
            linkConfigurationCoordinator.getComponent(
                configuration = TestFactory.LINK_CONFIGURATION.copy(merchantName = "anotherName")
            )
        ).isNotEqualTo(component)
    }

    @Test
    fun `attachExistingCardToAccount delegates to LinkAccountManager and returns Saved result`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(random = true)
        val accountManager = FakeLinkAccountManager().apply {
            createPaymentDetailsFromPaymentMethodResult = Result.success(
                LinkPaymentDetails.Saved(
                    paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    paymentMethod = paymentMethod,
                )
            )
        }
        val coordinator = RealLinkConfigurationCoordinator(
            linkComponentFactory = FakeLinkComponent.Factory(
                linkAccountManager = accountManager,
            )
        )

        val result = coordinator.attachExistingCardToAccount(
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethod = paymentMethod,
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isInstanceOf<LinkPaymentDetails.Saved>()

        val linkPaymentDetails = result.getOrNull() as LinkPaymentDetails.Saved

        assertThat(linkPaymentDetails.paymentMethod).isEqualTo(paymentMethod)
        assertThat(linkPaymentDetails.paymentDetails).isEqualTo(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)

        assertThat(accountManager.awaitCreatePaymentDetailsFromPaymentMethodTurbineCall())
            .isEqualTo(paymentMethod)

        accountManager.ensureAllEventsConsumed()
    }

    private class FakeLinkComponent private constructor(
        override val linkAccountManager: LinkAccountManager,
        override val configuration: LinkConfiguration,
        override val linkGate: LinkGate,
        override val linkAttestationCheck: LinkAttestationCheck,
        override val inlineSignupViewModelFactory: LinkInlineSignupAssistedViewModelFactory,
    ) : LinkComponent() {
        class Factory(
            private val linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
            private val linkGate: LinkGate = FakeLinkGate(),
            private val linkAttestationCheck: LinkAttestationCheck = FakeLinkAttestationCheck(),
            private val inlineSignupViewModelFactory: LinkInlineSignupAssistedViewModelFactory = mock()
        ) : LinkComponent.Factory {
            override fun create(configuration: LinkConfiguration): LinkComponent {
                return FakeLinkComponent(
                    linkAccountManager = linkAccountManager,
                    configuration = configuration,
                    linkGate = linkGate,
                    linkAttestationCheck = linkAttestationCheck,
                    inlineSignupViewModelFactory = inlineSignupViewModelFactory,
                )
            }
        }
    }

    companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "email"
        const val CUSTOMER_PHONE = "phone"
        const val CUSTOMER_NAME = "name"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "country_code"
    }
}
