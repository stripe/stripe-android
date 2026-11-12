# Order-summary bug bash — 2026-11-11

No new confirmed SDK bug. The initial pass completed three live payment configurations, separately retrieved three inclusive-tax address updates, and passed 14 focused JVM tests. The native follow-up below then completed two on-device payments and verified detailed cart rendering, payment-selection stability, and inclusive-tax display/update. No new root `ISSUE_order_summary_*.md` was warranted. The earlier environment and coverage notes describe the initially locked-device pass; the appended native follow-up and `native/handoff.json` provide the final state.

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `master`, `~/stripe/stripe-android-2`. Used the personal bug-bash skill and its Android and tax references, plus repository unit/network test and build-delegation skills. Read the existing billing-address issue and relevant LPM, automatic-tax, and adaptive-pricing notes. This pass adds order-detail, decimal-price, inclusive-tax, and special-currency coverage; earlier exclusive-tax and adaptive-currency coverage is inherited, not newly executed here.

## Environment

Physical Pixel 6a/API 36. Initial and final ordinary keyguard checks: `showing=true`, `secure=true`, screen off. No PIN, bypass, emulator, or connected-device tests were used. Reused the current-source debug APK installed by the customer pass, with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. This topic did not rebuild/install/launch the app or change device settings.

Live backend: `https://stp-mobile-playground-backend-v7.stripedemos.com`, creation version `2026-08-26.preview`, merchants `US` and `us_tax`. Every phase fetched and asserted a `pk_test_` key before provisioning. Shared configuration: `ui_mode=mobile_elements`, card only, adaptive pricing disabled, synthetic fixed customer email. Only synthetic item/address data and tokenized test Visa PaymentMethods were used; credentials stayed in process memory.

## Actual live coverage

[The standalone probe](live_probe.py) uses the same init/retrieve/update/confirm shapes as the SDK. It reuses only the redacting HTTP transport in `../email-collection/live_probe.py`. Each phase provisions fresh objects:

```sh
python3 bug-bash/order-summary/live_probe.py details
python3 bug-bash/order-summary/live_probe.py tax
python3 bug-bash/order-summary/live_probe.py currency
python3 bug-bash/order-summary/live_probe.py boundaries
python3 bug-bash/order-summary/verify_evidence.py
```

The last command is read-only. It passed, including successful confirmation responses and independently retrieved PaymentIntents for three final cases. Creation requests, redacted raw responses, and result summaries are retained alongside these notes. These calls are direct backend coverage, not native payment orchestration.

| Configuration | Observed result |
| --- | --- |
| Detailed USD cart: Unicode/percent product name; description and image URL; `unit_label=shirt`; integer 1299 × quantity 3; adjustable quantity min 0/max 12 | Name, image, unit label, quantity/bounds, stable keys, and line total 3897 retained through init, separate retrieve, and confirmation |
| Same cart: decimal price `12.345678901234` minor units × quantity 100, `unit_label=meter` | Exact decimal retained; server rounds the line to 1235 minor units. SDK decimal display `$0.12345678901234`; line total `$12.35` |
| Same cart: complimentary item with unit amount 0, quantity 1, no images/label | Zero-price row retained. Total 5132; Checkout complete/paid; independently retrieved PaymentIntent succeeded for USD 5132 |
| Inclusive-tax USD cart, two taxable items: 1000 × 2 and 2500 × 1; shipping tax source | Initial requires-address. California → Oregon → California gives included tax 357 → 0 → 357; exclusive tax always 0; subtotal and total remain 4500. Every update/retrieve agrees and group/item keys remain stable |
| Same inclusive-tax cart confirmed with California shipping/billing | Complete/paid at 4500; line included taxes 159 and 198; aggregate Sales Tax 357 at 8.625%; independently retrieved PaymentIntent succeeded for USD 4500 |
| ISK cart: 12300 × 2 plus 5000 × 1 | Init/retrieve/confirm agree at 29600 API units. SDK uses Stripe divisor 100, displays `ISK123.00`, `ISK246.00`, and total `ISK296.00`. Independently retrieved PaymentIntent succeeded for ISK 29600 |

Final-case summaries: [details](details-results.json), [inclusive tax](inclusive-tax-results.json), [ISK](isk-results.json). The recorded USD price rounding is a backend decision. The SDK preserves the authoritative line and session amounts, and confirmation's expected amount agrees with the actual PaymentIntent.

## Supported-configuration boundaries

These were real creation requests rejected by the backend before an SDK session could load. They are not SDK failures:

- [Multiple groups](multiple-groups-error.json): only one `one_time_price` group is allowed per Checkout Session.
- [Group description](group-description-error.json): `items[0][one_time_price][description]` is an unknown parameter. Product descriptions were accepted in the detailed cart, but a product description is not the public group-description field.
- [Zero quantity](zero-quantity-error.json): quantity must be at least 1, even with adjustable quantity enabled and minimum 0. A zero-price item with quantity 1 is supported and was covered separately.
- [Mixed inclusive/exclusive tax](mixed-tax-error.json): all checkout items must use the same `tax_behavior`. The supported all-inclusive control was then tested; prior automatic-tax evidence covers all-exclusive behavior.
- [KWD on the US merchant](kwd-error.json): this account does not support KWD. Three-decimal currencies remain a live coverage gap. ISK was chosen from the returned supported currencies to exercise a real special divisor instead.

The `boundaries` phase reproduces the first three; exact creation requests for mixed tax and KWD are separately retained as `mixed-tax-creation.json` and `kwd-creation.json`.

## SDK path and focused validation

`CheckoutSessionRepository` parses actual init/retrieve/update responses through `CheckoutSessionResponseJsonParser`. The parser reads nested `amount_details`, exact decimal strings into `BigDecimal`, stable group/inner keys, product name/images, unit labels, quantities, bounds, and tax fields. `CheckoutStateLoader` commits the response into `CheckoutElementsControllerStateHolder`; its `session` Flow maps through `CheckoutSessionMappers` to public `Session.orderSummaryItems` and totals.

`updateShippingAddress` serializes the matching shipping tax region and reloads the returned state. `runServerUpdate` waits for the merchant callback, fetches the active session, and reloads it. The confirmation interceptor uses the current response's summed line totals for `expected_amount`. Confirmation retrieves the actual Intent and supplies the finalized response to `DefaultCheckoutSessionRefresher`, which can commit that response without fetching a completed session again. The example renders `item.amountDetails.total.amount` beside the product name/quantity and renders the public subtotal/tax/total fields.

[Four evidence-only mapper tests](OrderSummaryLiveResponseBashTest.kt) cover detailed items/decimal precision, inclusive tax and stable identity, ISK divisor/formatting, and French-locale decimal formatting. They apply the actual retrieved PaymentIntent to each final confirmation response and verify paid status, unchanged public summary fields, exact totals, and agreement with the Intent amount.

[Two temporary controller tests](controller-replays.patch) replay captured responses through the actual public-controller dependency graph. One checks detailed-cart `runServerUpdate` refresh; the other calls public shipping-address updates California → Oregon → California and then refreshes. Both check public Flow values and retained fields. The callback in these replay tests returns success; no merchant quantity mutation is performed. New tests use captured tax updates that were independently exercised live.

Controller replay removes `experiments_data` only from in-memory response copies, following the established unrelated Link-holdback telemetry issue in prior topics; Link display is also disabled. Raw retained responses keep the original experiment metadata. This normalization does not alter order fields, amounts, tax, or payment outcomes.

**14 tests passed, zero failures/errors/skips; Gradle exit 0 in 33s.** [Exact classes/names/counts](jvm-results.json): four new mapper tests, two new controller tests, six existing `runServerUpdate` tests, and two existing refresher tests.

To replay, copy the evidence-only Kotlin file to `paymentsheet/src/test/java/com/stripe/android/checkout/` and apply the controller patch, run the following command, then remove only those temporary changes:

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.OrderSummaryLiveResponseBashTest' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.order summary bash*' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.runServerUpdate*' \
  --tests 'com.stripe.android.checkout.DefaultCheckoutSessionRefresherTest' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

The low-cost executor path was already established unavailable by repeated empty task deliveries in the preceding sequential topics. The coordinator confirmed using the skill's local fallback; no new executor was spawned. The initial sandbox Gradle attempt could not write the existing wrapper-cache lock. The normal escalated run above passed.

## Rejected leads and gaps

- Decimal-only prices have null integer amounts on the backend. The current public integer `unitAmount` fallback is zero, while `unitAmountDecimal` retains the exact real price and `amountDetails.total` retains the correct line total. This is a real reachable field observation, but this pass did not establish an intended integer rounding contract or an incorrect consumer display/charge: the example renders line totals. No speculative report was filed, and this is not an endorsement of the integer fallback contract.
- Public group descriptions and discounts currently contain placeholders. The group-description creation probe was rejected; no supported discounted response was established. These constants alone were not treated as bugs. Product descriptions and the public group description are different scopes.
- The first inclusive-tax confirmation was complete/paid, but an overly broad probe assertion compared nonpublic `tax_rate.active` metadata, which changed from true to false on completion. The public amounts, rates, names and inclusive flags did not change. The assertion was narrowed to public fields and a fresh session completed with independent Intent retrieval. [The first confirmation](inclusive-tax-first-confirm.json) is retained to make that harness correction explicit; it is additional paid-response evidence, not a fourth independently retrieved final payment case.
- Duplicate check: `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search '"order summary" in:title,body'` returned no matches. The broader unquoted search returned unrelated issue titles. Rechecked the existing root billing-address report; no duplicate or speculative report was created.
- Native line layout, image rendering in an integrating app, clipping/long names, accessibility, actual native confirmation, errors/latency, and on-device locale behavior were not exercised. No screenshot/video was captured while the device remained locked.
- No live merchant quantity/cart mutation endpoint is exposed by this playground backend interface. Quantity values/bounds and refresh were tested, but merchant quantity changes, removal/reordering, concurrent updates, and process restoration remain gaps. No claim for recurring prices, shipping charges/rates, discounts, flat-amount taxes, tax exemptions, three-decimal currencies, or other unfinished preview surfaces.
- Post-completion server persistence beyond the confirmation response/PaymentIntent remains unverified. Earlier topics established that the available Checkout client retrieve rejects completed sessions; this pass did not repeat that known limitation.

## Cleanup and environment handoff

All backend processes and Gradle executions finished. The temporary test-class copy and controller insertion were removed after preserving the Kotlin file and patch. `git diff --stat` again shows only the user's pre-existing `AGENTS.md` edit. The pre-existing billing issue/swap and all other topic evidence were preserved. No production SDK/app changes remain, and no commit/amend, push, PR, remote issue, Slack, or email was made.

No relay, recorder, private credential file, device scratch file, app/debugger settings, or adb mapping was created. Final reverse mapping list is empty. Original playground preferences were absent and remain absent. The installed current-source build still has `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. The Pixel remains PIN locked. Owned temporary logs and Python caches were removed after validation; retained JSON is parseable, contains no actual publishable/secret/client keys, and its only HTTPS URL host is `stripe.com` for the synthetic product image.

Only local files under `bug-bash/order-summary/` are retained: notes, probe/checker, evidence-only tests/patch, sanitized backend requests/responses, and test summaries. Test-mode backend objects remain.

**Order summary has released the device/build/backend environment. The coordinator may now start the final SFU/PMO SFU confirmation topic; it must not overlap this pass.**

## Native follow-up — 2026-11-11

**Two native test payments completed; no new confirmed SDK bug.** This follow-up closes the detailed-cart rendering/selection/confirmation gap and the inclusive-tax rendering/update gap listed above. It ran sequentially after LPM released the physical Pixel, using the unchanged `cf7dd4b99965a4e754abdadacf4f337e764f6e6a` APK (`DIRECTLY_TO_PLAYGROUND=false`, cleartext enabled). The user unlocked the Pixel 6a `REDACTED_DEVICE_SERIAL` / API 36 before native work. No new build, JVM rerun, emulator, connected-device test, production edit, or test hook was used.

[Native helper](native_probe.py) imports the prior customer transport and uses an owned loopback relay to replace only creation `items` with the already supported detailed/inclusive fixtures and disable adaptive pricing. Real backend responses are forwarded unchanged. Each case uses a fresh synthetic customer and saved tokenized test Visa; `pk_test_` and `livemode=false` are asserted. The real native app creates, loads, updates and confirms the sessions. Direct client `retrieve` verifies active backend state; `poll` independently checks completion.

| Native configuration/action | Observed result and evidence |
| --- | --- |
| Detailed USD cart | Exact `Café shirt — blue & white 100% x3` / $38.97, `Ribbon x100` / $12.35, `Complimentary gift x1` / $0.00; subtotal and total $51.32. No clipping in these names at the current device settings. [Initial screenshot](native/details-initial.png), [XML](native/details-initial.xml), [raw active response](native/details-initial.json). |
| Clear payment selection, open Select, reselect saved Visa | All detailed names/quantities/amounts remain unchanged; cleared footer shows no payment method, native sheet offers Visa, and reselection restores it. [Cleared](native/details-cleared.png), [selector](native/details-selector.png), [reselected](native/details-reselected.png). |
| Confirm detailed cart natively | Native **Payment complete!** with all three lines and $51.32 still displayed; independent poll is `complete` / `succeeded` / `paid`, `livemode=false`. [Before-confirm response](native/details-before-confirm.json), [completion](native/details-completed.png), [poll](native/details-completed-poll.json). |
| Inclusive-tax USD cart, California default shipping | `Tax included item x2` / $20.00 and `Second inclusive item x1` / $25.00. `Sales Tax (included)` is $3.57; subtotal/total stay $45.00. Backend is shipping-source, ready, inclusive tax 357, exclusive tax 0. [Screenshot](native/tax-ca-initial.png), [response](native/tax-ca-initial.json). |
| Native shipping edit to Portland, Oregon 97204 | Entered `1120 SW 5th Ave`, retained synthetic Suite 17/name, selected Oregon, and saved. Both line totals and subtotal/total remain $45.00; included tax becomes $0.00 and Visa stays selected. Backend agrees at 4500 with zero inclusive/exclusive tax. [Form](native/tax-or-form.png), [updated summary](native/tax-or-updated.png), [active response](native/tax-or-before-confirm.json). |
| Confirm inclusive-tax Oregon cart natively | **Payment complete!** retains the two correct lines and $45.00 total. Independent poll is `complete` / `succeeded` / `paid`, `livemode=false`. [Completion](native/tax-completed.png), [poll](native/tax-completed-poll.json). |

The Oregon response has `total_tax_amounts=[{amount:0, display_name:"", inclusive:true, percentage:0.0}]`. Consequently the sample renders the bare label **“(included) $0.00”**, including after completion. This cosmetic observation is retained explicitly; the backend supplies the empty name, the SDK maps it without changing it, and `TotalSummarySection` adds the included suffix. No SDK data-corruption or amount defect was established and no root issue was created for this observation. The sample consumes product name, quantity, and line total; its omission of optional images, unit labels and descriptions is not claimed as a defect.

All nine flow screenshots were opened and visually inspected. Native XML and raw active backend responses were checked for exact labels, decimal unit price, quantities, line totals, included tax, and summary stability; both polls were checked for successful paid test-mode status. JSON/XML parse and redaction assertions passed. [Native results](native/results.json) records those checks. No personal autofill/wallet data or browser state was selected or captured, and no system prompt appeared in this saved-card follow-up. No video was needed. [All-state GitHub duplicate search](native/duplicate-search.json) for `"order summary" in:title,body` returned no matches; existing root reports were rechecked and preserved.

These native polls do not expose amounts or a PaymentIntent secret, so **the final PaymentIntent amount and completed Checkout fulfillment/address persistence were not independently retrieved for these two native payments**. Native completion displays and active server totals agree; prior direct API tests above independently retrieved their own final Intents. No live merchant cart-mutation endpoint was available, so quantity mutation/removal/reordering, concurrent updates, refresh after a real cart change, and process death remain gaps. This follow-up did not repeat ISK/adaptive-currency, reverse Oregon→California, shipping clearing, other payment methods, locale/font-size/accessibility, extreme names, discounts, shipping charges, tax exemption, or unavailable preview surfaces.

Fresh backups found Checkout settings absent and PaymentConfiguration/DefaultPrefsRepository present. Cleanup restores those two files byte-for-byte and removes the new Checkout settings. The owned relay, reverse mapping, `/sdcard/summary-native-ui.xml`, and private state are removed; no recorder or test hooks were created. Device-admin lock policy, stay-awake setting 7, debugger settings, installed APK, and prior evidence remain unchanged. No commit/amend, push, PR, remote issue, Slack or email was made; synthetic test backend objects remain. Final cleanup checks, actual menu/keyguard state and release time are recorded in [native/handoff.json](native/handoff.json).
