# Checkout Elements docs inventory

Verified 2026-11-12 through Home's `stripe_doc` index and Sourcegraph's docs source. Sourcegraph revision: `4eba7bda17224ef0942d322773eff0f542a3e960`. SDK under test: `8cbb695eabcafa957a86c24e1f025643979a500a`.

Source root: `stripe-internal/mint/pay-server/docs/content/payments/mobile/checkout-sessions/`.

| Page | Source path under root | Sequential owner |
| --- | --- | --- |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions | `index.md` | Foundation / all |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/build-a-checkout-page | `build-a-checkout-page.md`, `partials/build-a-checkout-page.md`, `partials/collect-customer-email.md` | Foundation; shipping; saved methods |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/express-checkout-element | `express-checkout-element.md` | Express wallets |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/express-checkout-element/accept-a-payment | `express-checkout-element/accept-a-payment.md` | Express wallets |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/add-discounts | `add-discounts.md` | Discounts |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/collect-taxes | `collect-taxes.md` | Tax |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/adaptive-pricing | `adaptive-pricing.md` | Adaptive pricing |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/customize-appearance | `customize-appearance.md` | Appearance |
| https://docs.corp.stripe.com/payments/mobile/checkout-sessions/migrate-from-payment-intents | `migrate-from-payment-intents.md` | Migration |

Sourcegraph directory listing confirms only one Express subpage. Shared partials are implementation of these pages, not additional routes. Native Android is in scope; iOS/React Native and web ECE are outside this device run.

The Home scraper merges platform branches, strips generic type/JSX syntax, and drops Link branding/card numbers. Verify suspected documentation problems in the Markdoc platform branch before reporting them.

Private-preview exclusions: subscriptions/setup; on_behalf_of; server line-item updates/manual approval; shipping options; custom payment methods/multiprocessor; tax ID/checkout phone collection; tap-to-add; card-funding filtering.
