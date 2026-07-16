package com.stripe.android.checkout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContent
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `PaymentOptionsContent renders the current embedded content`() {
        val content = EmbeddedContent(
            interactor = FakePaymentMethodVerticalLayoutInteractor.create(),
            embeddedViewDisplaysMandateText = true,
            appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
            isImmediateAction = false,
        )
        val contentHelper = FakeEmbeddedContentHelper(
            embeddedContent = MutableStateFlow<EmbeddedContent?>(content),
        )
        val paymentElement = PaymentElement(contentHelper)

        composeRule.setContent {
            paymentElement.PaymentOptionsContent()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertExists()
        contentHelper.validate()
    }

    @Test
    fun `PaymentOptionsContent renders nothing when there is no embedded content`() {
        val contentHelper = FakeEmbeddedContentHelper(
            embeddedContent = MutableStateFlow<EmbeddedContent?>(null),
        )
        val paymentElement = PaymentElement(contentHelper)

        composeRule.setContent {
            paymentElement.PaymentOptionsContent()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertDoesNotExist()
        contentHelper.validate()
    }

    @Test
    fun `presentPaymentOptions delegates to the content helper`() = runTest {
        val contentHelper = FakeEmbeddedContentHelper()
        val paymentElement = PaymentElement(contentHelper)

        paymentElement.presentPaymentOptions()

        contentHelper.presentPaymentOptionsTurbine.awaitItem()
        contentHelper.validate()
    }
}
