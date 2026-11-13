# Tax documentation contracts

Parent read common/Android source via Sourcegraph on 2026-11-12. Revision `ffad89daddb431f7a5340bcbea2dbd38234db7c5`; curated excerpt, not full page export.

Page: https://docs.corp.stripe.com/payments/mobile/checkout-sessions/collect-taxes?platform=android

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@ffad89daddb431f7a5340bcbea2dbd38234db7c5/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/collect-taxes.md

- Lines 9–11 include tax settings-status and add-registrations partials. Validate merchant tax setup/registration before interpreting zero tax. Skill tax-and-saved-cards reference supplies usable test merchant/preconditions.
- Lines 15–20: automatic_tax.enabled=true; product-specific tax_code or dashboard preset; price tax_behavior or dashboard default; collect customer location.
- Lines 22–59 example: ui_mode mobile_elements; synthetic customer_email; shipping_address_collection.allowed_countries ['US','CA']; items one_time_price; USD 2000-unit T-shirt with tax_behavior exclusive and product tax_code txcd_99999999; quantity1; automatic_tax.enabled=true.
- Lines 61–66: choose specific applicable tax code, General if none. Exclusive adds tax separately; inclusive price includes tax.
- Lines 70–87: shipping-based tax requires allowed_countries plus Shipping Address Element or own UI. Own UI calls CheckoutController.updateShippingAddress; Session.shippingAddress is source of truth. Billing-based tax uses billing_address_collection=auto; Payment Element automatically collects minimum required address details. required collects full billing address.
- Lines 91–93: render totals from latest Session whenever it changes.
- Lines 188–193: server Checkout Session total_details.amount_tax or Dashboard tax outcome can verify tax.

Exact Android totals example at144–160, whose API shape should be checked against existing API-drift finding:
```kotlin
@Composable
fun OrderTotals(controller: CheckoutController) {
    val session by controller.session.collectAsState()
    val currentSession = session ?: return
    val totals = currentSession.totalSummary ?: return
    val tax = totals.taxAmounts
        .filterNot { it.inclusive }
        .sumOf { it.amount }
    Column {
        Text("Subtotal ${formatAmount(totals.subtotal, currentSession.currency)}")
        Text("Tax ${formatAmount(tax, currentSession.currency)}")
        Text("Total ${formatAmount(totals.totalDueToday, currentSession.currency)}")
    }
}
```

Use real backend for tax outcome claims. Meaningful transitions: valid tax-ready control, shipping vs billing, billing auto vs required, address edit/recalculation and confirmation amount, inclusive vs exclusive if supported. Read shipping handoff first to avoid duplicate roots. Tax ID/checkout phone collection remain intentionally excluded preview features.
