# BLIK failure patterns and backend merchants

Parent source reads, 2026-11-12.

## BLIK mobile one-time tests

Source revision `0fafb3bfb42fa8eef34ac68e6965d75e01ade919`:
https://stripe.sourcegraphcloud.com/stripe-internal/mint@0fafb3bfb42fa8eef34ac68e6965d75e01ade919/-/blob/pay-server/docs/content/partials/payments/payment-methods/blik/simulate-failures.md

Android accept-a-payment source explicitly includes this partial with platform mobile/ui android. Line9 says use email matching these patterns in **payment-method billing details**, with any6-digit BLIK code. Checkout email is separately modeled; ensure the pattern reaches PM billing email before interpreting failure-simulation outcome.

- Immediate `.*invalid_code@.*` -> payment_method_invalid_parameter/code_invalid.
- Immediate `.*expired_code@.*` -> payment_method_invalid_parameter/code_expired.
- After8 seconds: `.*limit_exceeded@.*` -> payment_method_provider_decline/payment_limit_exceeded; `.*insufficient_funds@.*` -> insufficient_funds; `.*customer_declined@.*` -> customer_declined; `.*bank_declined@.*` and `.*blik_declined@.*` -> partner_generic_decline.
- After60 seconds: `.*customer_timeout@.*`, `.*bank_timeout@.*`, `.*blik_timeout@.*` -> payment_method_provider_timeout/generic_decline.

These are documented simulated payment outcomes, not instructions to infer cancellation from a local timeout. Prefer a representative immediate and delayed case when meaningful rather than all equivalent patterns.

## Configured backend merchant identifiers

Source revision `48e8e831583fcbbe56de5163d90a56b87e178a32`:
https://stripe.sourcegraphcloud.com/stripe-internal/sandbox-apps@48e8e831583fcbbe56de5163d90a56b87e178a32/-/blob/stripe-mobile-playground-backend/merchant-config.js

Exact Merchants enum: US, GB, FR, DE, NO, IT, ES, AU, IN, NG, ng_wallet, mondu, SG, MY, TH, MX, BR, JP, CN, stripe_shop_test, us_tax.

Special country mappings: NG/ng_wallet -> US, mondu -> DE, stripe_shop_test/us_tax -> US; ordinary enum countries map to themselves. **No PL merchant is in this configured enum.** GB and EU identifiers are present. This does not prove current account-specific payment-method enablement. Use actual test-key/Session creation/init responses to verify availability. Don't read server key material or add/change gates. Thin create_checkout_session accepts arbitrary supported request_params (see BACKEND_PROXY_ROUTES.md).

Source infrastructure/paymentMethodTypes.js lists EUR card/ideal/sepa_debit/bancontact/etc for the older checkout route, but that routing list is not proof of capabilities for mobile_elements or a given deployed account.
