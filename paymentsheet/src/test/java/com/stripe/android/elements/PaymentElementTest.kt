package com.stripe.android.elements

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutMandateState
import com.stripe.android.elements.PaymentElement.Configuration.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContent
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentElementTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(StandardTestDispatcher())

    @Test
    fun `configuration builds default Google Pay values`() {
        val googlePayConfiguration = PaymentElement.Configuration().build().googlePayConfiguration

        assertThat(googlePayConfiguration.display).isEqualTo(CheckoutGooglePayConfiguration.Display.Automatic)
        assertThat(googlePayConfiguration.label).isNull()
        assertThat(googlePayConfiguration.buttonType).isEqualTo(PaymentSheet.GooglePayConfiguration.ButtonType.Pay)
        assertThat(googlePayConfiguration.additionalEnabledNetworks).isEmpty()
    }

    @Test
    fun `configuration builds requested Google Pay values`() {
        val googlePayConfiguration = PaymentElement.Configuration()
            .googlePayConfiguration(
                GooglePayConfiguration()
                    .display(GooglePayConfiguration.Display.Never)
                    .label("Complete your purchase")
                    .buttonType(GooglePayConfiguration.ButtonType.Checkout)
                    .additionalEnabledNetworks(listOf("INTERAC"))
            )
            .build()
            .googlePayConfiguration

        assertThat(googlePayConfiguration.display).isEqualTo(CheckoutGooglePayConfiguration.Display.Never)
        assertThat(googlePayConfiguration.label).isEqualTo("Complete your purchase")
        assertThat(googlePayConfiguration.buttonType)
            .isEqualTo(PaymentSheet.GooglePayConfiguration.ButtonType.Checkout)
        assertThat(googlePayConfiguration.additionalEnabledNetworks).containsExactly("INTERAC")
    }

    @Test
    fun `present from a background thread delegates to the content helper on Main`() = runScenario {
        withContext(Dispatchers.IO) {
            assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
            paymentElement.present()
        }

        assertThat(contentHelper.presentPaymentOptionsCalls.awaitItem())
            .isSameInstanceAs(Looper.getMainLooper().thread)
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    @Test
    fun `present does nothing when payment element is not configured`() = runScenario(
        configuration = CheckoutController.Configuration().build(),
    ) {
        paymentElement.present()
        testScheduler.runCurrent()

        contentHelper.presentPaymentOptionsCalls.expectNoEvents()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    @Test
    fun `present does nothing before checkout is configured`() = runScenario {
        stateHolder.state = null

        paymentElement.present()
        testScheduler.runCurrent()

        contentHelper.presentPaymentOptionsCalls.expectNoEvents()
    }

    @Test
    fun `present does nothing if payment element is removed before dispatch to Main`() = runScenario {
        paymentElement.present()
        stateHolder.state = requireNotNull(stateHolder.state).copy(
            configuration = CheckoutController.Configuration().build(),
        )
        testScheduler.runCurrent()

        contentHelper.presentPaymentOptionsCalls.expectNoEvents()
    }

    @Test
    fun `Content ignores retained embedded content when payment element is not configured`() = runScenario(
        configuration = CheckoutController.Configuration().build(),
        embeddedContent = createEmbeddedContent(),
    ) {
        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    @Test
    fun `Content does nothing before checkout is configured`() = runScenario(
        embeddedContent = createEmbeddedContent(),
    ) {
        stateHolder.state = null

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    @Test
    fun `Content hides retained embedded content when payment element configuration is removed`() = runScenario(
        embeddedContent = createEmbeddedContent(),
    ) {
        composeRule.setContent {
            paymentElement.Content()
        }
        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()
        val newMandateState = CheckoutMandateState.initial()

        composeRule.runOnIdle {
            stateHolder.state = requireNotNull(stateHolder.state).copy(
                configuration = CheckoutController.Configuration().build(),
                mandateState = newMandateState,
            )
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
        assertThat(newMandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    @Test
    fun `Content starts rendering when payment element configuration is added`() = runScenario(
        configuration = CheckoutController.Configuration().build(),
        embeddedContent = createEmbeddedContent(),
    ) {
        composeRule.setContent {
            paymentElement.Content()
        }
        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()

        composeRule.runOnIdle {
            stateHolder.state = requireNotNull(stateHolder.state).copy(
                configuration = CheckoutController.Configuration()
                    .paymentElement(PaymentElement.Configuration())
                    .build(),
            )
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
    }

    @Test
    fun `Content renders nothing when there is no embedded content`() = runScenario {
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
    }

    @Test
    fun `Content renders the current embedded content`() = runScenario(
        embeddedContent = createEmbeddedContent(),
    ) {
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
    }

    @Test
    fun `Content records access for the current configuration after reconfiguration`() = runScenario {
        composeRule.setContent {
            paymentElement.Content()
        }
        composeRule.waitForIdle()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
        val newMandateState = CheckoutMandateState.initial()
        assertThat(newMandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()

        composeRule.runOnIdle {
            stateHolder.state = requireNotNull(stateHolder.state).copy(mandateState = newMandateState)
        }
        composeRule.waitForIdle()

        assertThat(newMandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
    }

    @Test
    fun `default row selection behavior has no immediate action`() {
        val immediateAction = PaymentElement.RowSelectionBehavior.getImmediateAction(
            rowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
        )

        assertThat(immediateAction).isNull()
    }

    @Test
    fun `immediate row selection behavior invokes its callback`() {
        var callbackInvoked = false
        val immediateAction = PaymentElement.RowSelectionBehavior.getImmediateAction(
            rowSelectionBehavior = PaymentElement.RowSelectionBehavior.immediateAction {
                callbackInvoked = true
            },
        )

        requireNotNull(immediateAction).invoke()

        assertThat(callbackInvoked).isTrue()
    }

    private fun createEmbeddedContent() = EmbeddedContent(
        interactor = FakePaymentMethodVerticalLayoutInteractor.create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        ),
        embeddedViewDisplaysMandateText = true,
        appearance = PaymentSheet.Appearance(
            embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
        ),
        isImmediateAction = false,
    )

    private fun runScenario(
        configuration: CheckoutController.Configuration.State = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration())
            .build(),
        embeddedContent: EmbeddedContent? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val contentHelper = FakeEmbeddedContentHelper(MutableStateFlow(embeddedContent))
        val mandateState = CheckoutMandateState.initial()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle()).apply {
            state = CheckoutControllerStateFactory.create(
                configuration = configuration,
                mandateState = mandateState,
            )
        }
        Scenario(
            paymentElement = PaymentElement(contentHelper, stateHolder, backgroundScope),
            contentHelper = contentHelper,
            stateHolder = stateHolder,
            mandateState = mandateState,
            testScheduler = testScheduler,
        ).block()
        contentHelper.presentPaymentOptionsCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val paymentElement: PaymentElement,
        val contentHelper: FakeEmbeddedContentHelper,
        val stateHolder: CheckoutControllerStateHolder,
        val mandateState: CheckoutMandateState,
        val testScheduler: TestCoroutineScheduler,
    )

    private companion object {
        val SAVED_SEPA = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
    }
}
