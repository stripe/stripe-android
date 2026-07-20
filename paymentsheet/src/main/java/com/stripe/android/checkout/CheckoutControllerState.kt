package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import com.stripe.android.checkout.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.parcelize.Parcelize

/**
 * Internal state for [CheckoutController] and its single source of truth. Unlike [InternalState]
 * (used by [Checkout]), this holds the controller's own [CheckoutController.Configuration.State]
 * directly, so the configuration doesn't have to be reconstructed and can be read back via
 * [configuration]. It is only ever built by [CheckoutStateLoader] after a payment element load, so
 * the resolved [paymentMethodMetadata] and [embeddedConfiguration] are always present; everything
 * the controller and its collaborators observe is derived from this one value.
 *
 * [CheckoutStateLoader] builds every committed state after a load, but the selection setters on
 * [CheckoutControllerStateHolder] (which implements [EmbeddedSelectionHolder]) also [copy] it to
 * update [paymentSelection]/[temporarySelection]/[previousNewSelections] without a reload.
 */
@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class CheckoutControllerState(
    val key: String,
    val configuration: CheckoutController.Configuration.State,
    override val checkoutSessionResponse: CheckoutSessionResponse,
    val flagImages: Map<String, Bitmap>?,
    val collectedDetails: CheckoutCollectedDetails,
    val integrationLaunched: Boolean,
    val paymentMethodMetadata: PaymentMethodMetadata,
    val embeddedConfiguration: EmbeddedPaymentElement.Configuration,
    val paymentSelection: PaymentSelection?,
    val temporarySelection: String?,
    val previousNewSelections: Bundle,
) : Parcelable, CheckoutSessionData {
    override val shippingName: String? get() = collectedDetails.shippingName
    override val billingName: String? get() = collectedDetails.billingName
    override val shippingPhoneNumber: String? get() = collectedDetails.shippingPhoneNumber
    override val billingPhoneNumber: String? get() = collectedDetails.billingPhoneNumber
    override val shippingAddress: Address.State? get() = collectedDetails.shippingAddress
    override val billingAddress: Address.State? get() = collectedDetails.billingAddress

    fun asCheckoutSession(
        paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory,
        availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory,
    ): CheckoutSession {
        return checkoutSessionResponse.asCheckoutSession(
            flagImages = flagImages,
            paymentOptionDisplayData = paymentOptionFactory.create(
                selection = paymentSelection,
                paymentMethodMetadata = paymentMethodMetadata,
            ),
            availableExpressButtonTypes = availableExpressButtonTypesFactory.create(
                paymentMethodMetadata = paymentMethodMetadata,
                expressCheckoutElementConfiguration = configuration.expressCheckoutElementConfiguration,
                googlePayConfiguration = configuration.googlePayConfiguration,
            )
        )
    }
}
