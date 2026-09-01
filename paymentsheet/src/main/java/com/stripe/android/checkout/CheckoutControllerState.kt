package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.elements.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.parcelize.Parcelize

@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class CheckoutControllerState(
    val configuration: CheckoutController.Configuration.State,
    val checkoutSessionResponse: CheckoutSessionResponse,
    val flagImages: Map<String, Bitmap>?,
    val collectedDetails: CheckoutCollectedDetails,
    val paymentElementPaymentMethodMetadata: PaymentMethodMetadata,
    val expressCheckoutElementPaymentMethodMetadata: PaymentMethodMetadata,
    val embeddedConfiguration: EmbeddedPaymentElement.Configuration,
    val paymentSelection: PaymentSelection?,
    val temporarySelection: String?,
    val previousNewSelections: Bundle,
) : Parcelable {
    fun asCheckoutSession(
        paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory,
        availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory,
    ): Session {
        return checkoutSessionResponse.asCheckoutSession(
            collectedEmail = collectedDetails.email,
            flagImages = flagImages,
            paymentOptionDisplayData = paymentOptionFactory.create(
                selection = paymentSelection,
                paymentMethodMetadata = paymentElementPaymentMethodMetadata,
            ),
            availableExpressButtonTypes = availableExpressButtonTypesFactory.create(
                paymentMethodMetadata = expressCheckoutElementPaymentMethodMetadata,
                expressCheckoutElementConfiguration = configuration.expressCheckoutElementConfiguration,
            )
        )
    }
}
