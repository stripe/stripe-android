# Adaptive Pricing documentation contracts

Source read by parent on 2026-11-12 at revision `fe9c832f73564dbefefdcc0ec393772bf73d2f0b`. Curated common/Android excerpts.

Page: https://docs.corp.stripe.com/payments/mobile/checkout-sessions/adaptive-pricing?platform=android

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@fe9c832f73564dbefefdcc0ec393772bf73d2f0b/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/adaptive-pricing.md

- Lines 9–16: build mobile checkout first; check supported regions/currencies/restrictions at /payments/currencies/localize-prices/adaptive-pricing?payment-ui=embedded-components#restrictions. Client Session has localized line items/totals; selecting currency updates shared Session, available PMs, and other Session-backed Elements. **Server Checkout Session stays denominated in integration currency; presentment context is separate.** Don't report this intentional distinction as mismatch.
- Lines 18–47: enable in Dashboard Payments settings OR `adaptive_pricing.enabled:true` at Session creation. Integration currency must be a settlement currency. Example uses ui_mode mobile_elements, items one_time_price.
- Lines 51–53: configuring Currency Selector means integration must render it and display localized prices throughout checkout.
- Android 195–233: controller in ViewModel with SavedStateHandle; configure paymentElement and CurrencySelectorElement, appearance.cornerRadiusDp(12f), LabelContent.AMOUNT; destroy onCleared. Uses obsolete CheckoutController and Result<Unit>, already covered by API-drift report.
- Lines 237–245: configuration does not guarantee availability. Place element near order total; only displays when AP available; selection updates Session.
- Android 329–343: create one presenter during Activity creation, retain returned `presenter.currencySelectorElement()`, render Content only if `session?.isCurrencySelectorAvailable == true`.
- Lines 349–351: every displayed amount (line items, discounts, tax, shipping, totals) must use latest client Session.
- Android 428–442: collect controller.session; display `currentSession.totals.total.amount`.
- **Testing lines 448–482:** server `customer_email` can be `test+location_FR@example.com` to simulate France; `+location_XX` in email local part with valid ISO two-letter country. Example server enables AP and uses mobile_elements/items one_time_price. Stripe test cards work normally.

Meaningful coverage: eligible localized control; toggling integration/local currency and shared UI amounts/PMs; server/client enablement and ineligible visibility; confirmation with backend presentment evidence; exchange disclosure/expansion when available. Don't infer backend currency bug by comparing integration and presentment fields. Saved shipping/tax/discount cases can be reused only if they test a new interaction, not repeat known bugs. Ask parent for additional exact restrictions/source reads if needed.
