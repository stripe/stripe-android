@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
import com.stripe.android.checkout.CheckoutElementsController
suspend fun apply(controller: CheckoutElementsController, code: String) {
    controller.applyPromotionCode(code).onSuccess { }
}
