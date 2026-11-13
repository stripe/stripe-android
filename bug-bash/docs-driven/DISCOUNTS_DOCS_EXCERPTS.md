# Discount documentation contracts

Parent read Sourcegraph Markdoc on 2026-11-12, revision `ffad89daddb431f7a5340bcbea2dbd38234db7c5`. Curated common/Android extract, not a full page export.

Page: https://docs.corp.stripe.com/payments/mobile/checkout-sessions/add-discounts?platform=android

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@ffad89daddb431f7a5340bcbea2dbd38234db7c5/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/add-discounts.md

- Lines 9–18: apply coupon at Session creation or let customer enter promotion code. Coupon fixed/percentage; customer-facing code maps to coupon. One coupon or promotion code at a time.
- Lines 31–52: coupon currency, percent_off/amount_off, max_redemptions, redeem_by, applies_to products. Max/expiry span all applications; deleting prevents future application. Duration is subscription-specific; mobile preview does not support subscription.
- Lines 58–88: create Session with discounts:[{coupon: COUPON_ID}]. Raw codegen args here use `checkout_items:[{type:'one_time_price_item',one_time_price_item:{items:[{price: PRICE_ID,quantity:1}]}}]`, gated checkout_items/ui_mode mobile_elements. This differs from handwritten base guide's items/one_time_price; verify actual generated output/backend before reporting, do not assume codegen raw args equal rendered request.
- Lines 94–101: create code with promotion:{type:'coupon',coupon:COUPON_ID}; optional restrictions/customer; code case-insensitive, unique among active codes for any customer; multiple customer-specific same-code instances permitted but not alongside unrestricted same code.
- Lines 118–148: customer restriction; first_time_transaction permits absent customer or no prior payments/nonvoid invoices (guest Sessions also accepted); minimum_amount/currency, expiry bounded by coupon redeem_by, max redemption bounded by coupon, inactive behavior.
- Lines 166–194: customer-entered codes require allow_promotion_codes:true on Session. Same checkout_items codegen args as above.
- Lines 198–206: pass input to applyPromotionCode. Disable mutations/confirmation while controller.isUpdating.
- Android 347–361: ViewModel holds promotionCode MutableStateFlow; apply launches coroutine and calls `controller.applyPromotionCode(promotionCode.value).onSuccess { promotionCode.value = "" }`. UI text field disabled updating; Apply enabled when not updating and input nonempty (366–388).
- Lines 394–460: `removePromotionCode` removes a customer-applied promotion. Android coroutine calls controller.removePromotionCode(), button disabled while updating.
- Lines 467–469: render latest Session totals rather than calculate locally so coupon restrictions, tax, Adaptive Pricing remain consistent.

Exact Android totals excerpt, 543–560 (potential API drift must be verified; existing report covers stale Android API family):
```kotlin
@Composable
fun OrderTotals(session: CheckoutController.Session) {
    val totals = session.totalSummary ?: return
    Column {
        Text("Subtotal ${formatAmount(totals.subtotal, session.currency)}")
        totals.discountAmounts.forEach { discount ->
            Text("${discount.displayName} " + "-${formatAmount(discount.amount, session.currency)}")
        }
        Text("Total ${formatAmount(totals.totalDueToday, session.currency)}")
    }
}
```

Validation should establish a real discounted control, replacement/removal and invalid-code recovery, inspect server-calculated totals, and meaningfully cover supported coupon restrictions/rounding where test-backend provisioning permits. Don't claim unprovisioned restrictions passed. Avoid duplicating the API-drift report for further confirmed occurrences.
