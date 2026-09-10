package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedActivityArgsTest {
    @Test
    fun `legacy loading args restore loading state including customer state`() {
        val state = EmbeddedActivityState.LoadingPaymentOptions(
            context = context,
            initialSelection = PaymentSelection.GooglePay,
            previousNewSelections = previousNewSelections,
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        )

        val restored = parcelRoundTrip(state.toArgs()).toState()

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `legacy ready form args restore ready form state`() {
        val state = EmbeddedActivityState.Ready.Form(
            context = context,
            selectedPaymentMethodCode = "cashapp",
            initialSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            previousNewSelections = previousNewSelections,
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            promotion = PaymentMethodMessagePromotion(
                paymentMethodType = "cashapp",
                message = "Promotion",
                learnMore = PaymentMethodMessageLearnMore("Learn more", "https://stripe.com"),
            ),
        )

        val restored = parcelRoundTrip(state.toArgs()).toState() as EmbeddedActivityState.Ready.Form

        assertThat(restored.context).isEqualTo(state.context)
        assertThat(restored.selectedPaymentMethodCode).isEqualTo("cashapp")
        assertThat(restored.initialSelection?.paymentMethodType).isEqualTo("cashapp")
        assertThat(restored.previousNewSelections["card"]?.paymentMethodType).isEqualTo("card")
        assertThat(restored.customerState).isEqualTo(state.customerState)
        assertThat(restored.promotion).isEqualTo(state.promotion)
    }

    @Test
    fun `legacy ready manage args restore ready manage state`() {
        val state = EmbeddedActivityState.Ready.Manage(
            context = context,
            initialSelection = PaymentSelection.GooglePay,
            previousNewSelections = previousNewSelections,
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        )

        assertThat(parcelRoundTrip(state.toArgs()).toState()).isEqualTo(state)
    }

    @Test
    fun `legacy ready payment options args restore ready payment options state`() {
        val state = EmbeddedActivityState.Ready.PaymentOptions(
            context = context,
            initialSelection = PaymentSelection.GooglePay,
            previousNewSelections = previousNewSelections,
            customerState = null,
        )

        assertThat(parcelRoundTrip(state.toArgs()).toState()).isEqualTo(state)
    }

    @Test
    fun `invalid legacy loading mode is rejected`() {
        val state = EmbeddedActivityState.LoadingPaymentOptions(
            context = context,
            initialSelection = null,
            previousNewSelections = PreviousNewSelections.empty,
            customerState = null,
        )
        val invalidArgs = state.toArgs().copy(launchMode = EmbeddedLaunchMode.Manage)

        assertThat(invalidArgs.toState()).isNull()
    }

    @Test
    fun `previous new selections updates and merges immutably`() {
        val cardSelections = PreviousNewSelections.empty
            .updatedWith(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        val cashAppSelections = PreviousNewSelections.empty
            .updatedWith(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        val merged = cardSelections.mergedWith(cashAppSelections)

        assertThat(cardSelections["cashapp"]).isNull()
        assertThat(merged["card"]).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(merged["cashapp"]).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `previous new selections reads legacy Bundle parcel encoding`() {
        val legacyBundle = previousNewSelections.toBundle()
        val parcel = Parcel.obtain()
        parcel.writeBundle(legacyBundle)
        parcel.setDataPosition(0)

        val restored = PreviousNewSelectionsParceler.create(parcel)

        parcel.recycle()
        assertThat(restored).isEqualTo(previousNewSelections)
    }

    @Test
    fun `previous new selections writes legacy Bundle parcel encoding`() {
        val parcel = Parcel.obtain()
        with(PreviousNewSelectionsParceler) {
            previousNewSelections.write(parcel, 0)
        }
        parcel.setDataPosition(0)

        val restoredBundle = requireNotNull(parcel.readBundle(javaClass.classLoader))

        parcel.recycle()
        assertThat(PreviousNewSelections.fromBundle(restoredBundle)).isEqualTo(previousNewSelections)
    }

    private val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
    private val context = EmbeddedActivityState.Context(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        configuration = configuration,
        productUsage = setOf("EmbeddedPaymentElement"),
        paymentElementCallbackIdentifier = "callback_identifier",
        statusBarColor = null,
    )
    private val previousNewSelections = PreviousNewSelections.empty
        .updatedWith(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

    private inline fun <reified T : Parcelable> parcelRoundTrip(source: T): T {
        val bundle = bundleOf(PARCELABLE_KEY to source)
        val parcel = Parcel.obtain()
        bundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restoredBundle = Bundle.CREATOR.createFromParcel(parcel).apply {
            classLoader = T::class.java.classLoader
        }
        parcel.recycle()
        @Suppress("DEPRECATION")
        return requireNotNull(restoredBundle.getParcelable(PARCELABLE_KEY))
    }

    private companion object {
        const val PARCELABLE_KEY = "parcelable"
    }
}
