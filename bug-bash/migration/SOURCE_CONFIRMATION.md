# Migration source confirmation

Parent verified exact source at revision `d58e5fffbb7103361e761f892c683836152ba1a3` on 2026-11-12.

Documentation line69:
> Each Checkout Session generates a PaymentIntent upon confirmation. To retain supported payment-level parameters from your current integration, pass them in payment_intent_data.

https://stripe.sourcegraphcloud.com/stripe-internal/mint@d58e5fffbb7103361e761f892c683836152ba1a3/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/migrate-from-payment-intents.md?L60-70

Backend CreateMethod validates payment_intent_data at1943–1954. If present and mode is modeless, setup, or subscription, it raises checkout_mode_invalid_parameter using the supplied parameter and mode. This directly matches the real request's400 response; it is not merely a disabled discount feature gate.

https://stripe.sourcegraphcloud.com/stripe-internal/mint@d58e5fffbb7103361e761f892c683836152ba1a3/-/blob/pay-server/lib/payment_pages/api/checkout/session/create_method.rb?L1942-1957

The backend's payment-method filtering parity test explicitly selects `:payment_settings` for modeless and `:payment_intent_data` for payment mode, then applies setup_future_usage off_session at50–63, matching the successful alternative in the real probe.

https://stripe.sourcegraphcloud.com/stripe-internal/mint@d58e5fffbb7103361e761f892c683836152ba1a3/-/blob/pay-server/lib/payment_pages/api/test/checkout/session/create_method/payment_method_filtering_parity.test.rb?L50-66
