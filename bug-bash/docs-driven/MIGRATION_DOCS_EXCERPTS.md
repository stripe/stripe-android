# Migration documentation contracts

Parent read common/Android source at revision `b4da179f9b139180a428c5a91a3d543c150a77c5` on 2026-11-12. Curated excerpt.

Page: https://docs.corp.stripe.com/payments/mobile/checkout-sessions/migrate-from-payment-intents?platform=android
Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@b4da179f9b139180a428c5a91a3d543c150a77c5/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/migrate-from-payment-intents.md

- Lines 9–49: full order state, server line items/totals, built-in coupons/tax/AP, shared session/shipping. Preview one-time payments; subscriptions/setup unsupported.
- Lines 60–69: replace PaymentIntent creation with Session items, ui_mode mobile_elements, existing customer_email or editable-email flow, allowed shipping countries when collecting shipping. Return Session client_secret. Session generates PI on confirmation; text says retain *supported* payment-level parameters via payment_intent_data (no specific extra parameter in example; verify validity before diagnosing).
- Example87–116: USD1099 T-shirt, items:[{type:'one_time_price',one_time_price:{items:[{price_data:{currency:'usd',unit_amount:1099,product_data:{name:'T-shirt'}},quantity:1}]}}], fixed synthetic email, US/CA shipping. No mode. Line121 says preserve PaymentIntents integration for unsupported preview options.
- Options125–178: items required, mobile_elements required; email unless alternate collection; automatic_tax.enabled; allow_promotion_codes; billing_address_collection auto(default)/required; optional payment_method_types with dynamic default; optional allowed-country shipping. Render Session totals, do not calculate duplicate totals.
- Android293–349: ViewModel+SavedStateHandle, controller builder callback; configure PaymentElement; destroy onCleared; create one presenter in Activity.onCreate. Snippet uses obsolete CheckoutController + Result<Unit>, already same API-drift root cause.
- Android473–488: retain presenter.paymentElement(); Content inline or present() from select button.
- Confirmation526: disable Pay while updating or no payment option; require nonnull shipping only when shipping collection configured. Android606–627 uses `session?.paymentOptionDisplayData != null`, conditional shipping, presenter.confirm; Completed/Canceled/Failed callback. **Verify paymentOptionDisplayData against actual API**; Foundation verified actual property is paymentOption. Add any confirmed occurrence to existing API-drift report, don't duplicate.
- Postpayment678–710: signed checkout.session.completed; delayed methods may still be unpaid, wait for async_payment_succeeded before fulfillment; async_payment_failed invites retry; expired creates new Session when customer retries. Idempotent handler uses Session ID as order reference; server payment status before delivery. No webhook receipt is claimed in current bug-bash environment.

The real Checkout initialization/payment/state paths are already covered across earlier passes. Migration review should verify documented wiring/parameters/API compatibility and a focused example where it adds coverage, without repeating every prior payment. Excluded preview capabilities remain exclusions, not regressions.
