@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

import com.stripe.android.checkout.CheckoutElementsController

fun documentedAvailableMethods(session: CheckoutElementsController.Session): List<String> =
    session.availableExpressCheckoutPaymentMethods
