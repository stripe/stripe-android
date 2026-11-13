@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
import com.stripe.android.checkout.CheckoutElementsController
suspend fun apply(controller: CheckoutElementsController, code: String): Boolean =
    controller.applyPromotionCode(code) is CheckoutElementsController.OperationResult.Succeeded
fun total(session: CheckoutElementsController.Session): String = session.totals.total.amount
fun discounts(session: CheckoutElementsController.Session): List<String> = session.discountAmounts.map { it.amount }
