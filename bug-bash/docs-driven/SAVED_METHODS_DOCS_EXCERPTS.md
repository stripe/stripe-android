# Saved payment method documentation contracts

Parent read authoritative common Markdoc via Sourcegraph on 2026-11-12. Curated extract, not a full page export. Revision `4ed3418fb5a9b0af2cf2dcdca3aefb89b5b400cf`.

Page: https://docs.corp.stripe.com/payments/mobile/checkout-sessions/build-a-checkout-page?platform=android#save-payment-methods

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@4ed3418fb5a9b0af2cf2dcdca3aefb89b5b400cf/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/setup.md?L174-242

- Lines 177–179: Session must be associated with Customer to save. Pass existing `customer`, replacing `customer_email`, or set `customer_creation: 'always'` and retain Customer ID from completed Session.
- Lines 181–196: optional saving uses `saved_payment_method_options: {payment_method_save: 'enabled'}`. Payment Element shows “Save payment details for future purchases” checkbox. If selected, saved PM gets `allow_redisplay: 'always'`; later Session with same Customer displays it by default.
- Lines 198–213: unconditional saving uses `payment_settings: {setup_future_usage: 'off_session'}`. No customer saving choice; available payment methods restricted to those supporting setup_future_usage.
- Lines 213–226: type-specific alternative uses `payment_method_options: {card: {setup_future_usage: 'off_session'}}`.
- Lines 228–239: newly saved PM under unconditional flow gets `allow_redisplay: 'limited'`. Default Checkout redisplays only `always`. To show `limited`, later Session with same Customer must set `saved_payment_method_options: {allow_redisplay_filters: ['always', 'limited']}`.

All examples use ui_mode mobile_elements plus normal line items/other valid Session configuration. Preview supports one_time_price only; omit mode when using items. See bug-bash skill tax-and-saved-cards reference for real backend test setup and redisplay verification. Do not confuse server filtering with SDK local filtering. Test actual save result/consent and second checkout; inspect backend allow_redisplay and customer association where available. No claim that removal/logout behavior is explicitly documented in this excerpt; check its own public contract/source.
