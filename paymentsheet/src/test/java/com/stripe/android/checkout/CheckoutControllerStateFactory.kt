package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeErrorReporter

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutControllerStateFactory {
    const val DEFAULT_KEY = "test_key"

    fun create(
        key: String = DEFAULT_KEY,
        configuration: CheckoutController.Configuration.State = CheckoutController.Configuration().build(),
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
        flagImages: Map<String, Bitmap>? = null,
        collectedDetails: CheckoutCollectedDetails = CheckoutCollectedDetails(),
        integrationLaunched: Boolean = false,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        embeddedConfiguration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        paymentSelection: PaymentSelection? = null,
        temporarySelection: String? = null,
        previousNewSelections: Bundle = Bundle(),
    ): CheckoutControllerState {
        return CheckoutControllerState(
            key = key,
            configuration = configuration,
            checkoutSessionResponse = checkoutSessionResponse,
            flagImages = flagImages,
            collectedDetails = collectedDetails,
            integrationLaunched = integrationLaunched,
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedConfiguration = embeddedConfiguration,
            paymentSelection = paymentSelection,
            temporarySelection = temporarySelection,
            previousNewSelections = previousNewSelections,
        )
    }

    fun createStateHolder(
        savedStateHandle: SavedStateHandle,
        errorReporter: ErrorReporter = FakeErrorReporter(),
        paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory =
            CheckoutPaymentOptionDisplayDataFactory { _, _ -> null },
    ): CheckoutControllerStateHolder {
        return CheckoutControllerStateHolder(
            savedStateHandle = savedStateHandle,
            errorReporter = errorReporter,
            paymentOptionFactory = paymentOptionFactory,
        )
    }
}
