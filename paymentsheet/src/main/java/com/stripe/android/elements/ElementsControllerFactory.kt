package com.stripe.android.elements

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.elements.injection.DaggerElementsControllerComponent
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import java.util.UUID

internal fun createElementsController(
    application: Application,
    savedStateHandle: SavedStateHandle,
    builder: ElementsController.Builder,
    integrationName: String,
): ElementsController {
    val savedState = ElementsControllerSavedState(savedStateHandle, "ElementsController:$integrationName")
    val identifier = savedState.handle.get<String>("callback_identifier") ?: UUID.randomUUID().toString().also {
        savedState.handle["callback_identifier"] = it
    }
    val component = DaggerElementsControllerComponent.factory().create(
        application = application,
        savedState = savedState,
        resultCallback = builder.resultCallback,
        callbackIdentifier = identifier,
    )
    PaymentElementCallbackReferences[identifier] = builder.callbacks()
    return component.controller
}

private fun ElementsController.Builder.callbacks(): PaymentElementCallbacks =
    PaymentElementCallbacks.Builder()
        .createIntentCallback(createIntentCallback)
        .shippingAddressChangeCallback(shippingAddressChangeCallback)
        .build()
