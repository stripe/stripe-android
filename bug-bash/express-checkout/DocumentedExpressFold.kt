@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

import com.stripe.android.checkout.CheckoutElementsController

suspend fun documentedExpressFold(controller: CheckoutElementsController, secret: String) {
    controller.configure(secret, CheckoutElementsController.Configuration()).fold(
        onSuccess = { },
        onFailure = { },
    )
}
