package com.stripe.android.elements

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement.Configuration.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContent
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentElementTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

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
    fun `present delegates to the content helper`() = runTest {
        val contentHelper = FakeEmbeddedContentHelper()
        val paymentElement = PaymentElement(contentHelper, FakeHorizontalContentHelper())

        paymentElement.present()

        contentHelper.presentPaymentOptionsCalls.awaitItem()
        contentHelper.presentPaymentOptionsCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `Content renders nothing when there is no embedded content`() {
        val contentHelper = FakeEmbeddedContentHelper(embeddedContent = MutableStateFlow(null))
        val paymentElement = PaymentElement(contentHelper, FakeHorizontalContentHelper())

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
    }

    @Test
    fun `Content renders the current embedded content`() {
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
        val paymentElement = PaymentElement(contentHelper, FakeHorizontalContentHelper())

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()
    }

    @Test
    fun `Content renders the entire horizontal form when one LPM is available`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        )
        val horizontalContent = DefaultPaymentElementHorizontalContent(
            interactor = FakeAddPaymentMethodInteractor(
                initialState = FakeAddPaymentMethodInteractor.createState(metadata = metadata),
            ),
            mandate = stateFlowOf(null),
            embeddedViewDisplaysMandateText = true,
            appearance = PaymentSheet.Appearance(),
            eventReporter = FakeEventReporter(),
            elementsSessionId = null,
        )
        val embeddedContent = EmbeddedContent(
            interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata),
            embeddedViewDisplaysMandateText = true,
            appearance = PaymentSheet.Appearance(
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            ),
            isImmediateAction = false,
        )
        val paymentElement = PaymentElement(
            contentHelper = FakeEmbeddedContentHelper(MutableStateFlow(embeddedContent)),
            horizontalContentHelper = FakeHorizontalContentHelper(MutableStateFlow(horizontalContent)),
        )

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(FORM_ELEMENT_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
    }

    @Test
    fun `Content renders horizontal tabs and form when multiple LPMs are available`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        )
        val horizontalContent = DefaultPaymentElementHorizontalContent(
            interactor = FakeAddPaymentMethodInteractor(
                initialState = FakeAddPaymentMethodInteractor.createState(metadata = metadata),
            ),
            mandate = stateFlowOf(null),
            embeddedViewDisplaysMandateText = true,
            appearance = PaymentSheet.Appearance(),
            eventReporter = FakeEventReporter(),
            elementsSessionId = null,
        )
        val paymentElement = PaymentElement(
            contentHelper = FakeEmbeddedContentHelper(),
            horizontalContentHelper = FakeHorizontalContentHelper(MutableStateFlow(horizontalContent)),
        )

        composeRule.setContent {
            paymentElement.Content()
        }

        composeRule.onNodeWithTag(TEST_TAG_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(FORM_ELEMENT_TEST_TAG).assertIsDisplayed()
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

    private class FakeHorizontalContentHelper(
        override val content: MutableStateFlow<PaymentElementHorizontalContent?> = MutableStateFlow(null),
    ) : PaymentElementHorizontalContentHelper
}
