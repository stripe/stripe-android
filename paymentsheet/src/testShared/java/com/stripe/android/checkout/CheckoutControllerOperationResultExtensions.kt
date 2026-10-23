@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

internal val CheckoutController.OperationResult.isSuccess: Boolean
    get() = this is CheckoutController.OperationResult.Succeeded

internal val CheckoutController.OperationResult.isFailure: Boolean
    get() = this is CheckoutController.OperationResult.Failed

internal fun CheckoutController.OperationResult.getOrThrow() {
    when (this) {
        is CheckoutController.OperationResult.Succeeded -> Unit
        is CheckoutController.OperationResult.Failed -> throw error
    }
}

internal fun CheckoutController.OperationResult.exceptionOrNull(): Throwable? {
    return (this as? CheckoutController.OperationResult.Failed)?.error
}
