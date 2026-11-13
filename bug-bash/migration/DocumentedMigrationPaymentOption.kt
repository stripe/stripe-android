@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
import com.stripe.android.checkout.CheckoutElementsController
fun hasSelection(session: CheckoutElementsController.Session?): Boolean = session?.paymentOptionDisplayData != null
