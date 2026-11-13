@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
import com.stripe.android.checkout.CheckoutElementsController
fun total(session: CheckoutElementsController.Session) {
    val totals = session.totalSummary ?: return
    println(totals.subtotal)
    println(totals.discountAmounts)
    println(totals.totalDueToday)
}
