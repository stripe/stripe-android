package com.stripe.android.link

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class LinkPaymentMethodSelectionLauncherTest {
    private val linkGate = FakeLinkGate().apply { setShowRuxInFlowController(true) }
    private val selection = PaymentSelection.Link(
        brand = LinkBrand.Link,
        selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null,
        ),
    )

    @Test
    fun `launchIfEligible launches with payment method selection arguments`() = runScenario {
        val metadata = PaymentMethodMetadataFactory.create()
        val accountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        val launched = subject.launchIfEligible(
            selection = selection,
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethodMetadata = metadata,
            hasUserDeclinedVerification = false,
        )

        assertThat(launched).isTrue()
        assertThat(launcher.presentCalls.awaitItem()).isEqualTo(
            FakeLinkPaymentPresenter.PresentCall(
                configuration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = metadata,
                linkAccountInfo = accountInfo,
                launchMode = LinkLaunchMode.PaymentMethodSelection(
                    TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                ),
                linkExpressMode = LinkExpressMode.ENABLED,
                statusBarColor = 123,
            )
        )
    }

    @Test
    fun `launchIfEligible does not launch without Link selection`() = runScenario {
        assertNotLaunched(selection = PaymentSelection.GooglePay)
    }

    @Test
    fun `launchIfEligible does not launch without configuration`() = runScenario {
        assertNotLaunched(configuration = null)
    }

    @Test
    fun `launchIfEligible does not launch without Link account`() = runScenario(
        linkAccountInfo = LinkAccountUpdate.Value(null),
    ) {
        assertNotLaunched()
    }

    @Test
    fun `launchIfEligible does not launch after verification dismissal`() = runScenario {
        assertNotLaunched(hasUserDeclinedVerification = true)
    }

    @Test
    fun `launchIfEligible does not launch when RUX is disabled`() = runScenario {
        linkGate.setShowRuxInFlowController(false)

        assertNotLaunched()
    }

    private suspend fun Scenario.assertNotLaunched(
        selection: PaymentSelection? = this@LinkPaymentMethodSelectionLauncherTest.selection,
        configuration: LinkConfiguration? = TestFactory.LINK_CONFIGURATION,
        hasUserDeclinedVerification: Boolean = false,
    ) {
        val launched = subject.launchIfEligible(
            selection = selection,
            configuration = configuration,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            hasUserDeclinedVerification = hasUserDeclinedVerification,
        )

        assertThat(launched).isFalse()
        launcher.presentCalls.expectNoEvents()
    }

    private fun runScenario(
        linkAccountInfo: LinkAccountUpdate.Value = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val launcher = FakeLinkPaymentPresenter()
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply {
            set(linkAccountInfo)
        }
        val scenario = Scenario(
            subject = LinkPaymentMethodSelectionLauncher(
                launcher = launcher,
                linkGateFactory = FakeLinkGate.Factory(linkGate),
                linkAccountHolder = linkAccountHolder,
                statusBarColor = 123,
            ),
            launcher = launcher,
        )

        scenario.block()

        launcher.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val subject: LinkPaymentMethodSelectionLauncher,
        val launcher: FakeLinkPaymentPresenter,
    )
}

internal class FakeLinkPaymentPresenter : LinkPaymentPresenter {
    val presentCalls = Turbine<PresentCall>()

    override fun present(
        configuration: LinkConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountInfo: LinkAccountUpdate.Value,
        launchMode: LinkLaunchMode,
        linkExpressMode: LinkExpressMode,
        statusBarColor: Int?,
    ) {
        presentCalls.add(
            PresentCall(
                configuration = configuration,
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountInfo = linkAccountInfo,
                launchMode = launchMode,
                linkExpressMode = linkExpressMode,
                statusBarColor = statusBarColor,
            )
        )
    }

    fun ensureAllEventsConsumed() {
        presentCalls.ensureAllEventsConsumed()
    }

    data class PresentCall(
        val configuration: LinkConfiguration,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val linkAccountInfo: LinkAccountUpdate.Value,
        val launchMode: LinkLaunchMode,
        val linkExpressMode: LinkExpressMode,
        val statusBarColor: Int?,
    )
}
