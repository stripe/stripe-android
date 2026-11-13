@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

import com.stripe.android.checkout.CheckoutElementsController

// Correcting the controller spelling still leaves the guide's Result<Unit> mismatch.
suspend fun documentedConfigureReturn(
    controller: CheckoutElementsController,
    clientSecret: String,
): Result<Unit> {
    return controller.configure(
        clientSecret = clientSecret,
        configuration = CheckoutElementsController.Configuration(),
    )
}
