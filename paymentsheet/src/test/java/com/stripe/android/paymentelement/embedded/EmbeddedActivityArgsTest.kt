package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class EmbeddedActivityArgsTest {
    @Test
    fun `loading arguments survive a Parcel round trip`() {
        val args = EmbeddedActivityArgs.LoadingPaymentOptions(
            appearance = configuration.appearance,
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            linkAccountInfo = context.linkAccountInfo,
        )

        assertThat(parcelRoundTrip(args)).isEqualTo(args)
    }

    @Test
    fun `form arguments with selection promotion and history survive a Parcel round trip`() {
        val args = createFormArgs()

        val restored = parcelRoundTrip(args)

        assertThat(restored).isEqualTo(args)
        assertThat(restored.context.previousNewSelections["card"])
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(restored.promotions).containsExactly(promotion)
        assertThat(restored.launchMode).isEqualTo(EmbeddedLaunchMode.Form("card"))
        assertThat(restored.appearance).isEqualTo(configuration.appearance)
    }

    @Test
    fun `Cash App form preserves its payment parameters in a Parcel round trip`() {
        val args = createFormArgs().copy(
            selectedPaymentMethodCode = "cashapp",
            initialSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            promotion = promotion.copy(paymentMethodType = "CASHAPP"),
        )

        val restored = parcelRoundTrip(args)

        assertThat(restored.context).isEqualTo(args.context)
        assertThat(restored.selectedPaymentMethodCode).isEqualTo("cashapp")
        assertThat(restored.initialSelection?.paymentMethodCreateParams?.toParamMap())
            .isEqualTo(args.initialSelection?.paymentMethodCreateParams?.toParamMap())
        assertThat(restored.customerState).isEqualTo(args.customerState)
        assertThat(restored.promotion).isEqualTo(args.promotion)
    }

    @Test
    fun `form accepts absent selection and promotion`() {
        val args = createFormArgs().copy(initialSelection = null, promotion = null)

        assertThat(parcelRoundTrip(args)).isEqualTo(args)
        assertThat(args.promotions).isEmpty()
    }

    @Test
    fun `form accepts matching selection without promotion`() {
        val args = createFormArgs().copy(promotion = null)

        assertThat(parcelRoundTrip(args)).isEqualTo(args)
        assertThat(args.promotions).isEmpty()
    }

    @Test
    fun `form accepts matching promotion without selection`() {
        val args = createFormArgs().copy(initialSelection = null)

        assertThat(parcelRoundTrip(args)).isEqualTo(args)
        assertThat(args.promotions).containsExactly(promotion)
    }

    @Test
    fun `form rejects selection for another payment method`() {
        assertFailsWith<IllegalArgumentException> {
            createFormArgs().copy(initialSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }
    }

    @Test
    fun `form rejects promotion for another payment method`() {
        assertFailsWith<IllegalArgumentException> {
            createFormArgs().copy(promotion = promotion.copy(paymentMethodType = "KLARNA"))
        }
    }

    @Test
    fun `manage arguments survive a Parcel round trip`() {
        val args = EmbeddedActivityArgs.Ready.Manage(
            context = context,
            initialSelection = PaymentSelection.GooglePay,
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        )

        assertThat(parcelRoundTrip(args)).isEqualTo(args)
        assertThat(args.promotions).isEmpty()
        assertThat(args.launchMode).isEqualTo(EmbeddedLaunchMode.Manage)
    }

    @Test
    fun `payment options arguments with multiple promotions survive a Parcel round trip`() {
        val args = EmbeddedActivityArgs.Ready.PaymentOptions(
            context = context,
            initialSelection = PaymentSelection.GooglePay,
            customerState = null,
            promotions = listOf(promotion, promotion.copy(paymentMethodType = "KLARNA")),
        )

        assertThat(parcelRoundTrip(args)).isEqualTo(args)
        assertThat(args.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
    }

    @Test
    fun `activity result preserves selection history in a Parcel round trip`() {
        val result = EmbeddedActivityResult.Complete(
            selection = PaymentSelection.GooglePay,
            previousNewSelections = previousNewSelections,
            hasBeenConfirmed = false,
            customerState = null,
            linkAccountInfo = context.linkAccountInfo,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = true,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        assertThat(parcelRoundTrip(result)).isEqualTo(result)
    }

    @Test
    fun `Checkout state preserves selection history in a Parcel round trip`() {
        val state = CheckoutControllerStateFactory.create(previousNewSelections = previousNewSelections)

        assertThat(parcelRoundTrip(state).previousNewSelections).isEqualTo(previousNewSelections)
    }

    @Test
    fun `embedded element state preserves selection history in a Parcel round trip`() {
        val state = EmbeddedPaymentElement.State(
            confirmationState = EmbeddedConfirmationStateHolder.State(
                paymentMethodMetadata = context.paymentMethodMetadata,
                selection = null,
                configuration = configuration,
                statusBarColor = null,
            ),
            customer = null,
            previousNewSelections = previousNewSelections,
        )

        assertThat(parcelRoundTrip(state)).isEqualTo(state)
    }

    private fun createFormArgs() = EmbeddedActivityArgs.Ready.Form(
        context = context,
        selectedPaymentMethodCode = "card",
        initialSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        promotion = promotion,
    )

    private val previousNewSelections = PreviousNewSelections.empty
        .updatedWith(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    private val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
    private val context = EmbeddedActivityArgs.ReadyContext(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        configuration = configuration,
        productUsage = setOf("EmbeddedPaymentElement"),
        paymentElementCallbackIdentifier = "callback_identifier",
        statusBarColor = null,
        linkAccountInfo = LinkAccountUpdate.Value(null),
        previousNewSelections = previousNewSelections,
    )
    private val promotion = PaymentMethodMessagePromotion(
        paymentMethodType = "CARD",
        message = "Promotion",
        learnMore = PaymentMethodMessageLearnMore("Learn more", "https://stripe.com"),
    )

    private inline fun <reified T : Parcelable> parcelRoundTrip(source: T): T {
        val parcel = Parcel.obtain()
        try {
            bundleOf(PARCELABLE_KEY to source).writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restoredBundle = Bundle.CREATOR.createFromParcel(parcel).apply {
                classLoader = T::class.java.classLoader
            }
            @Suppress("DEPRECATION")
            return requireNotNull(restoredBundle.getParcelable(PARCELABLE_KEY))
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        const val PARCELABLE_KEY = "parcelable"
    }
}
