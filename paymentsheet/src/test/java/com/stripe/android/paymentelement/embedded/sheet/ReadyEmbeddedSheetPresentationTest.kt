package com.stripe.android.paymentelement.embedded.sheet

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class ReadyEmbeddedSheetPresentationTest {

    @Test
    fun `Manage dismissal includes stored Checkout Session response`() {
        val response = CheckoutSessionResponseFactory.create()
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val intent = Intent()
        val activity = mock<EmbeddedSheetActivity>()
        whenever(activity.intent).thenReturn(intent)
        val presentation = ReadyEmbeddedSheetPresentation(
            activity = activity,
            args = EmbeddedActivityArgs(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                productUsage = setOf("EmbeddedPaymentElement"),
                paymentElementCallbackIdentifier = "ReadyEmbeddedSheetPresentationTest",
                statusBarColor = null,
                selection = selection,
                previousNewSelections = Bundle(),
                customerState = null,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                promotions = emptyList(),
                launchMode = EmbeddedLaunchMode.Manage,
                presentationState = EmbeddedActivityArgs.PresentationState.Ready,
            ),
            activityResultCaller = mock(),
            eventReporter = mock<EventReporter>(),
            customerStateHolder = FakeCustomerStateHolder(),
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            embeddedNavigator = mock(),
            selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply { setSelection(selection) },
            sheetActivityRegistrar = mock(),
            sheetActivityStateHolder = FakeSheetActivityStateHolder().apply { checkoutSessionResponse = response },
        )

        presentation.onDismissed()

        val result = EmbeddedActivityResult.fromIntent(intent) as EmbeddedActivityResult.Complete
        assertThat(result.checkoutSessionResponse).isEqualTo(response)
    }
}
