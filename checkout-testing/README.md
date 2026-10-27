# Checkout network fixtures

The full CheckoutClient and intent responses in `src/main/resources` follow the real
network recordings in [stripe-ios PR #7469](https://github.com/stripe/stripe-ios/pull/7469),
at commit `a688283f5adda85025ca60d8584b76650e316ffa`.
The recordings live under
`StripePayments/StripePaymentsTestUtils/Resources/recorded_network_traffic`.

| Fixture | Recording |
| --- | --- |
| `checkout-session-init.json` | `CheckoutTests/testLoadCheckoutSession`, CheckoutClient init |
| `checkout-session-confirm.json` | `CheckoutConfirmationFunctionalTests/testconfirmwithcardandcheckoutsessioncustomeremailcompleteswithpaymentintent`, CheckoutClient confirm |
| `checkout-payment-intent.json` | Same test, GET payment intent with expanded payment method |
| `checkout-session-confirm-setup.json` | `CheckoutConfirmationFunctionalTests/testconfirmzeroamountwithcardandlocalemailcompleteswithsetupintent`, CheckoutClient confirm |
| `checkout-setup-intent.json` | Same test, GET setup intent with expanded payment method |
| `checkout-session-adaptive-pricing-default.json` | `CheckoutAdaptivePricingTests/testAdaptivePricingUsesIntegrationAndPresentmentCurrencies`, CheckoutClient init |
| `checkout-session-automatic-tax.json` | `CheckoutTests/testUpdateBillingTaxRegionIfNecessary`, CheckoutClient init; partial override |
| `checkout-session-returning-customer.json` | `STPAPIClientCheckoutSessionTest/testUpdatePaymentMethodBillingDetails`, CheckoutClient init; partial override with a saved bank-account variation |
| `checkout-session-customer-without-detach-capability.json` | Customer envelope and disabled saved-method options from the card confirmation above; partial override |

These are normalized test responses. IDs, secrets, timestamps, product names, and
amounts retain stable Android test values. Nested item, group, and session amounts
agree. Android reads item totals and taxes from nested `amount_details`; the flat
amount fields remain in the recordings to reflect the actual backend response.
Adaptive pricing keeps the integration currency USD while presenting EUR.
The default init response includes Link and Cash App for Android test coverage;
the recorded card-only session lists card, Apple Pay, and Google Pay.

Session feature flags, experiment assignments, passive captcha, and Link captcha
configuration are omitted so shared fixtures do not start captcha requests or opt
tests into unrelated experiments. Link funding and onboarding are limited to cards,
consumer incentives are disabled, and displayable default values are disabled.
Tests for those features should add their configuration explicitly.

The confirmation fixtures omit `link_settings` to preserve their card and Google Pay
configuration. Including Link's card-brand mode enables Link during the post-confirm
reload and starts an account lookup for the fixture's customer email. Tests that
need Link after confirmation should configure it and mock the lookup explicitly.

The returning-customer override retains Android's stable customer and payment-method
IDs, save/remove enablement, and a saved US bank account alongside the recorded card
shape. The PR has no saved-bank-account CheckoutClient response, so that variation
remains unverified by these recordings. Save/remove capabilities belong to the
top-level `saved_payment_method_options`, rather than the customer object.

Add synthetic boundary cases with `testBodyFromFile` JSON modifications in the
test that needs them. For example, the both-intents confirmation case is constructed
inline; the recorded confirmation responses contain one intent type.
