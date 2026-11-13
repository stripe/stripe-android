# Docs foundation bug bash — 2026-11-12

## Environment and scope

- SDK revision: `8cbb695eabcafa957a86c24e1f025643979a500a`. Initial tracked tree clean. No source fixes, commits, staging, pushes, remote issues, or external messages.
- Docs source verified through Home `stripe_doc` plus Sourcegraph Markdoc, revision `4eba7bda17224ef0942d322773eff0f542a3e960`. Inventory: [DOCS_INDEX.md](../docs-driven/DOCS_INDEX.md). Mixed-platform indexing artifacts were not treated as doc defects. No browser used for docs.
- Physical Pixel 6a, serial `2B211JEGR05252`, API36. `emulator-5556` never used. Native UI via adb, fresh UIAutomator hierarchy before meaningful navigation. Parent exclusively owns wake/UNKNOWN keepalive; this agent owned all other UI interaction until restoration.
- Parent built `./gradlew :paymentsheet-example:assembleBaseDebug -PDIRECTLY_TO_PLAYGROUND=false`: exit0, 1m16s. Luna child and root executor task deliveries were empty; parent used documented no-usable-executor fallback. Installed APK: `paymentsheet-example/build/outputs/apk/base/debug/paymentsheet-example-base-debug.apk`.
- No cleartext build flag, no relay, no adb reverse mappings. Backend `https://stp-mobile-playground-backend-v7.stripedemos.com`; US publishable-key endpoint HTTP200 and `pk_test_` prefix asserted without retaining the key. Session creation version `2026-08-26.preview` verified in source.

## Confirmed local reports

1. [Android guide does not compile](../../ISSUE_docs_foundation_android_guide_does_not_compile.md): nonexistent `CheckoutController`; even after correcting that name, documented `Result<Unit>` conflicts with current `OperationResult`. Isolated compiler probes prove both errors and pass the actual-API control. [Compiler output](docs-compiler-results.txt).
2. [Fixed-email error contract is incorrect](../../ISSUE_docs_foundation_fixed_email_error_contract.md): docs promise an error, whereas Android deliberately returns Succeeded and preserves fixed email. Physical UI says “Email updated”; existing focused test passed, explicitly checking both success and original email. [Test result](email-contract-test-result.txt), [UI](server-email-update-succeeds.png).

Reports use exactly Summary/Repro/Test/Elaboration. GitHub issue searches with `GH_HOST=github.com`, `--state all`, queries `CheckoutController`, `checkout docs`, and `checkout email` found no matching report. Local root reports were absent before these two were written.

## Actual runtime coverage

Common configuration in [card-control.json](card-control.json): guest, US merchant, USD120 (2×$35 shirt + $50 hoodie), card only, server email `foundation.bugbash@example.com`, no saving/tax/shipping, Payment Element configured, Google Pay/Link hidden. The example uses the real `CheckoutElementsController` in its ViewModel, `SavedStateHandle`, destroy in onCleared, presenter created during Activity onCreate, and retained Payment Element. This wiring was inspected; it is not a lifecycle stress test.

| Case | Action | Observed result |
| --- | --- | --- |
| Basic sheet card | Select → enter 4242424242424242, 12/30, CVC123, ZIP94103 → Continue | Sheet closes; inline Card row and controller footer show Visa4242; total USD120 and no shipping. [Selection](card-selected-no-shipping.png). |
| Card confirmation | Confirm same session | SDK Completed callback, playground “Payment complete!”. [Outcome](card-payment-complete.png). |
| Inline entry point / decline | Fresh session → inline Card row → 4000000000009995, same test expiry/CVC/ZIP → Continue → Confirm | Localized “Your card has insufficient funds. Use a different payment method…”; selection retained, controls available. [Decline](card-decline.png). Inline Content uses a compact row whose card form opens as a sheet. |
| Retry after decline | Change → replace card with 4242 → Continue → Confirm on same session | SDK Completed; [outcome](decline-retry-complete.png). |
| Immutable server email | Fresh control session → replace email with changed.foundation@example.com → Update email | Operation success / “Email updated”. Existing test independently confirms public session.email remains fixed. Docs mismatch reported. |
| Missing checkout email | [collect-email.json](collect-email.json), which omits server email → select 4000002500003155 → Confirm | Failed before authentication, display text “An error has occurred confirming the Checkout Session.” [Outcome](missing-email-error.png). This violates the documented integration precondition; no SDK bug reported for the failed confirmation. |
| Client-provided email | Update email to foundation.collect@example.com; reselect retained card, Continue → Confirm | Reached real 3D Secure 2 test page. [Challenge](auth-challenge.png). See selection-reset lead below. |
| Explicit authentication cancellation | Tap CANCEL on fully loaded 3DS test page | SDK Canceled / “Confirmation canceled”; Visa3155 and checkout controls retained. [Outcome](auth-canceled.png). Cancellation was explicit, not inferred from a wait. |
| Retry after authentication cancellation | Confirm same card/session → wait for test challenge → COMPLETE | SDK Completed / “Payment complete!”. [Outcome](auth-retry-complete.png). |

All retained PNGs were opened and visually inspected. No video recorded. Native autofill/save-to-Google prompts were dismissed with “No thanks” / “Not now”; test cards were not saved. Helper now suppresses autofill-wallet contents in dump output; no personal wallet screenshots or files retained.

**Backend evidence limit:** These were real test-mode backend/device interactions, and confirmation/cancel/decline outcomes are observed SDK callbacks. No proxy or independent server retrieval captured Session/PaymentIntent terminal status. `CheckoutConfirmationFinalizer` can optimistically succeed when polling times out/PENDING; therefore SDK Completed is not claimed as independent proof of paid/completed server state. Future backend-focused tests should retain sanitized confirm/poll/retrieve responses through a relay. No webhook fulfillment tested.

## Open lead and rejected candidates

- **Unconfirmed selected-card reset after updateEmail:** In the no-email scenario, a selected 3155 card first failed confirmation for missing email. Next, tapping Email, typing a valid email, hiding keyboard, tapping Update email, then Confirm yielded “No payment method selected” + generic “Something went wrong”. Reopening Select showed all card fields retained; Continue restored selection and confirmation proceeded to 3DS. The actions were too close together to distinguish update reload, failed-confirm cleanup, or a race. No standalone report. Isolate selected-card updateEmail before any confirm, after failed confirm, and with adequate await; capture session state before/after. Relevant source: updateEmail → withCheckoutState → local collectedDetails → reload. Parent plans later email/state/lifecycle specialist.
- Docs' unconditional shipping-address Pay predicate looks restrictive when shipping is omitted, but the guide's full sample creates shipping collection. Successful card control without shipping proves SDK supports that case. Do not claim SDK failure; optional integration adaptation was not reported.
- Initially suspected `session.paymentOption` name mismatch was disproven: it exists at this SDK revision (type PaymentOptionDisplayData). Do not reopen.
- Home strips generic/JSX syntax, Link branding, and card numbers and combines platform branches. These are indexing artifacts, not proven page errors.
- One initial automated card replacement lost a digit; fixed helper to select all, delete, await briefly, and type. Verified exact 4242 before confirmation. Not a product defect.

## Untested coverage / later sequential topics

1. **Express wallets:** Read both Express routes. Eligibility/availability, Never/Automatic/order, Link vs Google Pay, same callback, wallet cancel/retry, shipping/tax update during wallet, no second merchant Pay required. Avoid personal-wallet screenshots. Existing app build can exercise source presets; no foundation relay is running.
2. **Shipping:** Build-page shipping section and shared session/confirmation. Allowed-country constraints, defaults/edit/clear/cancel, own UI, totals refresh. Preview excludes shipping options. Foundation only tested absent shipping.
3. **Saved methods:** Build-page saving section + skill tax-and-saved-cards reference. New/existing Customer, explicit opt-in versus unconditional save, allow_redisplay always/limited, redisplay/remove, preserving intended setup-future-usage. No saved methods covered here.
4. **Discounts:** add-discounts route, coupon/promotion apply/remove, invalid/expired, totals/payment metadata. No discount coverage here.
5. **Tax:** collect-taxes route, registered US_TAX merchant, shipping vs billing source, ready/failure/update; backend tax evidence. No tax coverage here.
6. **Adaptive pricing:** adaptive-pricing route, eligible country/defaults, currency-selector visibility, amount/PM updates, confirm currency. No AP coverage here.
7. **Appearance/migration:** corresponding routes; native supported config, screen/layout/theme transitions, actual current API names. Reuse docs report instead of duplicating controller-name drift.
8. **Standard payment methods:** UnionPay variable length, Bancontact/iDEAL redirect failure, Pay by Bank success/failure, BLIK failures, and delayed SEPA outcomes were not tested. Parent subsequently verified `mobile: true` in the source partial: Alipay, EPS/P24, BECS, and Boleto/OXXO cases in the mixed Home output are hidden from the mobile guide, so are not required by this docs-driven plan. See [corrected mobile source contracts](../docs-driven/STANDARD_METHODS_DOCS_EXCERPTS.md).
9. **Email/state/lifecycle:** invalid/null email and defaults.email, Customer.email precedence, clearing mutable email, updating selected card before/after confirmation failure, process death and Activity recreation, retained selection/result delivery, logout/clearLocalCustomerData. No rotation/process-death/logout/duplicate-confirm/destroy-race testing here. The only lifecycle exercised was presenting/dismissing auth and returning to the existing activity.

## Setup and final handoff

- `device.py` provides `dump`, label-based `tap`, `text`, `replace`, `key`, `swipe`, `shot`, `settings`, `launch`, `restore`, always selecting the physical serial. It is a helper, not a full unattended test. Inspect modals and keyboard transitions. Settings JSON labels use serialized enum names (`Never`, not a UI display label). Settings helper accepts a full scenario overlay and otherwise uses source defaults.
- Original entire preferences were backed up privately at `/tmp/checkout-foundation/original-settings.xml`; not copied into the repository. `device.py restore` restored that file **byte-for-byte**, verified by comparing actual run-as bytes, force-stopped the app, and removed `/sdcard/checkout-foundation.xml`.
- No owned relay, recorder, or reverse mapping exists. Parent's keepalive remains running and is parent-owned. Device is available for the next sequential agent.
- Installed build remains the successful baseDebug APK with DIRECTLY_TO_PLAYGROUND=false and normal cleartext policy. Rebuild with `-PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true` only if a later agent needs a loopback HTTP relay, then document the changed flag. MainActivity launch is exported; CheckoutElementsController Example is reached via the main menu.
- Final tracked source is unchanged. Only local reports and `bug-bash/` evidence/helpers are untracked. Compiler scratch outputs remain under `/tmp/checkout-foundation/` and contain no session secrets.

Final parent cleanup: playground preferences were compared byte-for-byte with the initial private backup and matched. The backup was then removed after verification; see [final device check](../docs-driven/FINAL_DEVICE_CHECK.json).
