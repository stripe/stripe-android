package com.stripe.android.elements.ece

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption

/** Shared wallet confirmation behavior for Checkout and intent-based Express Checkout Elements. */
internal fun ExpressButton.toConfirmationOption(
    configuration: CommonConfiguration,
    paymentMethodMetadata: PaymentMethodMetadata,
    googlePayShippingAddressParameters: GooglePayJsonFactory.ShippingAddressParameters?,
): ConfirmationHandler.Option? = toSelection().toConfirmationOption(
    configuration = configuration,
    linkConfiguration = paymentMethodMetadata.linkState?.configuration,
    cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
    googlePayShippingAddressParameters = googlePayShippingAddressParameters,
    showSecondaryButtonForLink = false,
)
