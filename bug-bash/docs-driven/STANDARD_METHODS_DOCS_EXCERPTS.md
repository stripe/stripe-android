# Mobile payment-method testing contracts

Parent verified source on 2026-11-12. This corrects the mixed-platform Home index's overbroad test list.

Build guide includes `partials/testing-auto-pm.md` with `mobile: true` at build-a-checkout-page.md line1079. Source revision `ec20bfa28c9f23b1daed345ae5da437160ed98fe`.

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@ec20bfa28c9f23b1daed345ae5da437160ed98fe/-/blob/pay-server/docs/content/partials/testing-auto-pm.md

## Actually shown for mobile

- Lines 15–25: card4242424242424242 success, 4000002500003155 authentication, 4000000000009995 insufficient funds. Foundation already covered these (and explicit cancel/retry); other passes have backend terminal proof. Don't repeat without a new reason.
- Lines 27–29: **UnionPay6205500000000000004**, variable card length13–19; any valid future expiry/CVC/postal.
- Lines 54–56 mobile branch: **Bancontact, iDEAL** redirect immediate-notification authentication failure. Select method, fill details, confirm, click **Fail test payment** on redirect page.
- Lines 58–64: **Pay by Bank**, delayed notification; Complete test payment and Fail test payment flows.
- Lines 66–68: **BLIK**, immediate invalid/expired errors, delayed bank declines, timeout; email failure patterns documented at /payments/blik/accept-a-payment#simulate-failures (ask parent for exact source if needed).
- Lines 88–94: **SEPA Direct Debit**, success IBAN `AT321904300235473204`, expected PaymentIntent processing then succeeded after3 minutes. Failure IBAN `AT861904300235473202`, expected processing then requires_payment_method.

## Hidden when mobile=true

Alipay wallet test (lines33–45), EPS/P24 alternatives (line54 else branch), BECS tests (79–87), and Boleto/OXXO voucher tests (98–111) are not in this mobile guide. They appeared in Home's mixed output. Do not treat them as mandatory untested mobile coverage or infer support from this partial.

Build guide fulfillment lines1070–1073 says signed checkout.session.completed plus async_payment_succeeded/async_payment_failed for delayed methods. Terminal delayed payment status cannot be inferred from an immediate SDK Completed callback. Observe real backend state, inspect actual cancellation/action before claiming it, and respect method/merchant/currency eligibility. No signed-webhook receipt should be claimed without testing it. Use meaningful successful control and documented failure cases where supported by test backend; accurately record eligibility gaps. Calls must remain test mode and synthetic; no personal bank login.
