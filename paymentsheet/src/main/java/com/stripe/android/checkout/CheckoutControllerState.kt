package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Internal state for [CheckoutController]. Unlike [InternalState] (used by [Checkout]), this holds
 * the controller's own [CheckoutController.Configuration.State] directly, so the configuration
 * doesn't have to be reconstructed and can be read back via [configuration].
 */
@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class CheckoutControllerState(
    val key: String,
    val configuration: CheckoutController.Configuration.State,
    override val checkoutSessionResponse: CheckoutSessionResponse,
    val flagImages: Map<String, Bitmap>?,
    override val shippingName: String?,
    override val billingName: String?,
    override val shippingPhoneNumber: String?,
    override val billingPhoneNumber: String?,
    override val shippingAddress: Address.State?,
    override val billingAddress: Address.State?,
    val integrationLaunched: Boolean,
) : Parcelable, CheckoutSessionData {
    val initializationMode: PaymentElementLoader.InitializationMode.CheckoutSession
        get() = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = key,
            checkoutSessionResponse = checkoutSessionResponse,
        )

    fun asCheckoutSession(): CheckoutSession {
        return checkoutSessionResponse.asCheckoutSession(flagImages)
    }

    companion object {
        /**
         * Builds the initial state for a freshly configured checkout session: a new [key], the
         * given [configuration] and [checkoutSessionResponse], and no resolved flag images or
         * collected shipping/billing details yet.
         */
        fun defaultState(
            configuration: CheckoutController.Configuration.State,
            checkoutSessionResponse: CheckoutSessionResponse,
        ): CheckoutControllerState = CheckoutControllerState(
            key = UUID.randomUUID().toString(),
            configuration = configuration,
            checkoutSessionResponse = checkoutSessionResponse,
            flagImages = null,
            shippingName = null,
            billingName = null,
            shippingPhoneNumber = null,
            billingPhoneNumber = null,
            shippingAddress = null,
            billingAddress = null,
            integrationLaunched = false,
        )
    }
}
