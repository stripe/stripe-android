package com.stripe.android.elements

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutControllerStateFactory
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
    fun `present from a background thread delegates to the content helper on Main`() = runTest {
        val contentHelper = FakeEmbeddedContentHelper()
        val mandateState = CheckoutMandateState.initial()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle()).apply {
            state = CheckoutControllerStateFactory.create(mandateState = mandateState)
        }
        val paymentElement = PaymentElement(contentHelper, stateHolder, backgroundScope)

        withContext(Dispatchers.IO) {
            assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
            paymentElement.present()
        }

        assertThat(contentHelper.presentPaymentOptionsCalls.awaitItem())
            .isSameInstanceAs(Looper.getMainLooper().thread)
        contentHelper.presentPaymentOptionsCalls.ensureAllEventsConsumed()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    @Test
    fun `Content renders nothing when there is no embedded content`() = runTest {
        val contentHelper = FakeEmbeddedContentHelper(embeddedContent = MutableStateFlow(null))
        val mandateState = CheckoutMandateState.initial()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle()).apply {
            state = CheckoutControllerStateFactory.create(mandateState = mandateState)
        }
        val paymentElement = PaymentElement(contentHelper, stateHolder, backgroundScope)
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
    }

    @Test
    fun `Content renders the current embedded content`() = runTest {
        val content = EmbeddedContent(
            interactor = FakePaymentMethodVerticalLayoutInteractor.create(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            ),
            embeddedViewDisplaysMandateText = true,
            appearance = PaymentSheet.Appearance(
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            ),
            isImmediateAction = false,
        )
        val contentHelper = FakeEmbeddedContentHelper(embeddedContent = MutableStateFlow(content))
        val mandateState = CheckoutMandateState.initial()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle()).apply {
            state = CheckoutControllerStateFactory.create(mandateState = mandateState)
        }
        val paymentElement = PaymentElement(contentHelper, stateHolder, backgroundScope)
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()
        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
    }

    @Test
    fun `Content records access for the current configuration after reconfiguration`() = runTest {
        val contentHelper = FakeEmbeddedContentHelper(embeddedContent = MutableStateFlow(null))
        val mandateState = CheckoutMandateState.initial()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle()).apply {
            state = CheckoutControllerStateFactory.create(mandateState = mandateState)
        }
        val paymentElement = PaymentElement(contentHelper, stateHolder, backgroundScope)
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

    private companion object {
        val SAVED_SEPA = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
    }
}
