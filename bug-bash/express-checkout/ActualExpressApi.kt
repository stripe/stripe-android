@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

import com.stripe.android.checkout.CheckoutElementsController

suspend fun actualExpressApi(controller: CheckoutElementsController, secret: String): String =
    when (val result = controller.configure(secret, CheckoutElementsController.Configuration())) {
        is CheckoutElementsController.OperationResult.Succeeded -> "loaded"
        is CheckoutElementsController.OperationResult.Failed -> result.error.message.orEmpty()
    }

fun actualAvailableMethods(session: CheckoutElementsController.Session): Set<String> =
    session.availableExpressCheckoutPaymentMethods.mapTo(mutableSetOf()) { it.type }
