package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

internal class LinkPaymentMethodSelectionLauncherTest {
    private val linkGate = FakeLinkGate().apply { setShowRuxInFlowController(true) }
    private val launcher = mock<LinkPaymentLauncher>()
    private val selection = PaymentSelection.Link(
        brand = LinkBrand.Link,
        selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null,
        ),
    )

    @Test
    fun `launchIfEligible launches with payment method selection arguments`() {
        val metadata = PaymentMethodMetadataFactory.create()
        val accountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
        val subject = LinkPaymentMethodSelectionLauncher(FakeLinkGate.Factory(linkGate))

        val launched = subject.launchIfEligible(
            launcher = launcher,
            selection = selection,
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethodMetadata = metadata,
            linkAccountInfo = accountInfo,
            hasUserDeclinedVerification = false,
            statusBarColor = 123,
        )

        assertThat(launched).isTrue()
        verify(launcher).present(
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethodMetadata = metadata,
            linkAccountInfo = accountInfo,
            launchMode = LinkLaunchMode.PaymentMethodSelection(
                TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
            ),
            linkExpressMode = LinkExpressMode.ENABLED,
            statusBarColor = 123,
        )
    }

    @Test
    fun `launchIfEligible does not launch without Link selection`() {
        assertNotLaunched(selection = PaymentSelection.GooglePay)
    }

    @Test
    fun `launchIfEligible does not launch without configuration`() {
        assertNotLaunched(configuration = null)
    }

    @Test
    fun `launchIfEligible does not launch without Link account`() {
        assertNotLaunched(linkAccountInfo = LinkAccountUpdate.Value(null))
    }

    @Test
    fun `launchIfEligible does not launch after verification dismissal`() {
        assertNotLaunched(hasUserDeclinedVerification = true)
    }

    @Test
    fun `launchIfEligible does not launch when RUX is disabled`() {
        linkGate.setShowRuxInFlowController(false)

        assertNotLaunched()
    }

    private fun assertNotLaunched(
        selection: PaymentSelection? = this.selection,
        configuration: LinkConfiguration? = TestFactory.LINK_CONFIGURATION,
        linkAccountInfo: LinkAccountUpdate.Value = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
        hasUserDeclinedVerification: Boolean = false,
    ) {
        val subject = LinkPaymentMethodSelectionLauncher(FakeLinkGate.Factory(linkGate))

        val launched = subject.launchIfEligible(
            launcher = launcher,
            selection = selection,
            configuration = configuration,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            linkAccountInfo = linkAccountInfo,
            hasUserDeclinedVerification = hasUserDeclinedVerification,
            statusBarColor = null,
        )

        assertThat(launched).isFalse()
        verify(launcher, never()).present(
            configuration = any(),
            paymentMethodMetadata = any(),
            linkAccountInfo = any(),
            launchMode = any(),
            linkExpressMode = any(),
            statusBarColor = anyOrNull(),
        )
    }
}
