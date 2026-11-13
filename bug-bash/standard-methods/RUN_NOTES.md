# Remaining documented mobile payment methods — 2026-11-12

## Outcome

One confirmed integration defect: [BLIK confirmation drops the entered code](../../ISSUE_standard_methods_blik_confirmation_drops_code.md). Both Android mapping and current Checkout Client protocol lack the required options carrier. This does not establish a client-only fix. All other method categories in the source-verified mobile testing table were exercised with real test backend results. No production edits, commit, staging, push, remote filing or external message.

## Environment and contracts

- SDK HEAD8cbb695eabcafa957a86c24e1f025643979a500a, tracked source clean before/after. Physical Pixel6a2B211JEGR05252/API36 only. Parent owns wake/UNKNOWN keepalive. No emulator or instrumentation task.
- Existing installed baseDebug, DIRECTLY_TO_PLAYGROUND=false, STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true. No build/install needed for device work.
- Read root AGENTS.md, bug-bash skill/Android reference, shared AGENT_BRIEF, STANDARD_METHODS_DOCS_EXCERPTS, DOCS_INDEX, BACKEND_PROXY_ROUTES, BLIK_AND_MERCHANTS, adaptive-pricing harness notes, and five existing root reports. Applied write-unit-tests/network-tests for regression and delegate-low-reasoning-work for Gradle. Parent executed exact checks under documented unavailable-low-cost-executor fallback.
- Test backend https://stp-mobile-playground-backend-v7.stripedemos.com, create version2026-08-26.preview. Loopback relay18774 forwards actual response unchanged, explicitly disables adaptive_pricing only. Uses verified US/FR/GB/DE test merchants and pk_test_/cs_test_ checks. Private credentials retained only in temporary scratch, deleted after cleanup.
- Guest; fixed methods@example.com; no saving, tax, shipping, Express, embedded wallets or Currency Selector. Normal cart2x3500 shirt+5000 hoodie. Four reproducible configs: [unionpay](unionpay.json), [eur](eur.json), [gbp](gbp.json), [blik](blik.json).
- Native adb/UIAutomator used for app and authorized payment test redirects. Before actions inspected current UI/domain. Redirects were payments.stripe.com, with Pay by Bank bank selection at pay-by-bank.stripe.com. URL bars/token-bearing JSON suppressed from retained dumps. No browser screenshot, personal login, wallet invocation or autofill saving. Every retained PNG viewed. No video.

## Tested behavior and evidence

| Configuration | Actual actions and outcome |
| --- | --- |
| US/USD120 card | Documented19-digit UnionPay6205500000000000004,12/30,CVC123,US94103 accepted; [valid form](unionpay-valid.png), [native completion](unionpay-result.png), [backend](backend-unionpay-confirm.json) complete/paid/succeeded. |
| FR/EUR120 card,sepa_debit,ideal,bancontact | All four available in native UI and [backend retrieval](backend-eur-initial.json); no forced unsupported method. |
| FR SEPA success | Test Customer,methods@example.com,AT321904300235473204,Austria/Teststrasse1/1010/Wien. SDK Completed and [immediate backend](backend-sepa-success-immediate.json) complete/unpaid/confirmation succeeded. Later [same Session](backend-sepa-success-paid.json) paid. |
| FR SEPA failure | Separate Session, same billing details,AT861904300235473202. SDK Completed and [immediate](backend-sepa-failure-immediate.json) complete/unpaid/confirmation succeeded; [later](backend-sepa-failure-later.json) confirmation failed/unpaid. No claim of signed webhook or directly retrieved PI status. |
| FR Bancontact success | Synthetic Test Customer; Confirm→Stripe test page→AUTHORIZE TEST PAYMENT; [native](bancontact-success.png), [backend](backend-bancontact-success.json) complete/paid/succeeded. |
| FR Bancontact failure | Fresh Session; explicit FAIL TEST PAYMENT returns [actionable authentication error](bancontact-failed.png); [backend](backend-bancontact-failed.json) failed/unpaid/open. |
| Same FR Session→iDEAL | Select iDEAL,Test Customer,Confirm→explicit FAIL TEST PAYMENT; [native error](ideal-failed.png), [backend](backend-ideal-failed.json) failed/unpaid/open. Retry Confirm→AUTHORIZE TEST PAYMENT; [native completion](ideal-recovered.png), [same hash backend](backend-ideal-recovered.json) complete/paid/succeeded. This covers method-switch and same-method recovery after failures. |
| GB/GBP120 card,pay_by_bank | [Available backend](backend-gbp-initial.json). Select Pay by Bank,Confirm→Stripe bank selector→Lloyds→Continue→Stripe AUTHORIZE TEST PAYMENT. [Native](paybybank-success.png), [backend](backend-paybybank-success.json) complete/paid/succeeded. No personal bank credentials or remembered-bank action. |
| GB Pay by Bank failure/retry | Fresh Session; same bank selector→FAIL TEST PAYMENT; [native declined](paybybank-failed.png), [backend](backend-paybybank-failed.json) failed/unpaid/open. Retry same Session through same selector→AUTHORIZE TEST PAYMENT; [native completion dump](paybybank-recovered-ui.jsonl), [backend](backend-paybybank-recovered.json) complete/paid/succeeded. This fixture returned terminal paid by first success probe; no separate delayed interval asserted. |
| DE/PLN120 card,blik | [Backend availability](backend-blik-initial.json) and native BLIK code form. Enter123456,Continue,Confirm: required-code error. Repeat explicit entry/confirmation: same. [Report](../../ISSUE_standard_methods_blik_confirmation_drops_code.md). |
| Same DE/PLN Session card recovery | Select Visa4242,12/30,CVC123,US94103 after BLIK failures; [native](blik-card-recovery.png), [backend](backend-blik-card-recovery.json) complete/paid/succeeded, matching failed BLIK hash2b2418310d57. This is a distinct merchant/currency/recovery control, not repetition of foundation card basics. |

[Compact outcomes](outcomes.json) retain hashes/statuses. SEPA credentials were separately preserved before subsequent creations; probes always used intended Session. [Observation timeline](sepa-observation-timeline.json) records original file completion timestamps: success unpaid at first, +67s,+117s,+170s; paid observed +242s. Failure succeeded/unpaid initially, failed/unpaid at+45s and again+117s. These are sampling intervals after first probe, not exact settlement latency from confirmation. Early progress messages rounded the success window to~5min; the timeline is authoritative. No direct final PaymentIntent retrieval route exists in this fixture; failed poll is evidence of failed confirmation, not an independent observation of PI.requires_payment_method. No webhook validation performed.

## Regression and root cause

Read [backend contract](BLIK_BACKEND_CONTRACT.md) for complete source-reviewed protocol gap. [Patch](blik-confirmation-regression.patch) adds one intended-preservation test to existing CheckoutSessionConfirmationInterceptorTest. It supplies BLIK create params plus optionsParams(code123456), asserts that precondition, and requires code on the outgoing real interceptor/repository request using NetworkRule. Response fixture merely completes the request; no claim of actual BLIK success from the mock. [Wire mismatch](blik-regression-wire-mismatch.txt) shows all route/type matchers pass, only the code fails. Mock500 is a missing-matcher mechanism, distinct from native backend missing-parameter error.

Parent command:
```
./gradlew :paymentsheet:testDebugUnitTest --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.blik bugbash*' --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.open confirmation completes with the unmodified confirm response for polling'
```
Exit1,25s,2tests/1expected failure/0errors/skips; existing card control passed. [Results XML](blik-regression-results.xml), [log](blik-regression-results.txt). Temporary tracked file restored byte-for-byte after archiving patch. No production fix. [Exact GitHub search](github-code-duplicate-search.json) empty; broad [BLIK checkout search](github-duplicate-search.json) returns only unrelated BouncyCastle3333. Rechecked local root reports, no duplicate.

## Rejected leads and gaps

- First rapid adb UnionPay input retained16digits before BIN metadata resolved; complete re-entry accepted19digits and paid. No independent ordinary typing/paste/autofill repro established; considered automation/BIN-timing observation, not a confirmed bug. [Initial invalid input](unionpay-entered.png) retained for honesty; no report or broad input-race investigation.
- SEPA immediate Completed with unpaid status is expected asynchronous semantics. Waited for separate actual backend outcomes; no settlement inference from callback.
- Pay by Bank native declined/auth errors after explicit FAIL are expected outcomes, not bugs. Both retry and method-switch recovery passed.
- BLIK invalid/expired,8s bank-decline,60s timeout simulations could not be reached because the successful-code control fails before bank authentication. Source patterns require PM billing email, while this BLIK form collects only code and Checkout maps billing collection automatically without attaching defaults. Did not claim checkout email alone activated simulations or inject unsupported configuration/options. No BLIK success claimed.
- No unsupported mobile=false Alipay,EPS/P24,BECS,Boleto/OXXO obligations added. Those mixed-platform entries are hidden from this mobile guide. Foundation owns4242/decline/3155 basics; earlier topics own tax/shipping/saved/discounts/AP.
- Untested: normal UnionPay paste/autofill/race, other card lengths, separate BLIK legacy PaymentIntent control, backend gate changes, signed webhooks, final PI/charge retrieval, long-delay/manual-notification variants, additional banks/countries/merchant enablement, redirect cancellation (foundation already owns explicit auth cancel/retry), process death/rotation, connectivity loss, accessibility/localization matrix.

## Cleanup and reuse

Before settings replacement, original whole preferences backed up privately. To reuse: start backend_probe.py serve, adb reverse18774, back up original preferences to /tmp/checkout-standard/original-settings.xml, device.py settings CONFIG, launch main→CheckoutElementsController Example→Create Checkout Session. Helpers inspect fresh hierarchy for each labeled tap and stop on ambiguous labels; use separate inspection after menus/keyboard transitions. Separate private credentials can be selected with backend_probe.py probe LABEL filename.json. Do not print these raw files.

Original whole preferences restored and byte-identical verification passed; app stopped, relay72219 stopped, reverse18774 removed, /sdcard/checkout-standard.xml removed. All test redirect tabs returned to merchant app; did not change browser preferences, remember a bank or manipulate personal history. Private scratch removed after test-file restoration. [Device cleanup](device-cleanup.json). Installed cleartext-enabled baseDebug unchanged, no recorder. Parent keepalive untouched. Device released; report and local evidence remain.
