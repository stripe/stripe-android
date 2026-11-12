# Confirm SFU / PMO SFU bug bash — 2026-11-11

One new confirmed save-offer defect: [ISSUE_confirm_sepa_none_offers_ineffective_save_checkbox.md](../../ISSUE_confirm_sepa_none_offers_ineffective_save_checkbox.md). A valid mixed card/SEPA session offers SEPA's save checkbox even though its explicit PMO SFU `none` prevents attachment. The save flag reaches the backend correctly. The backend's SFU precedence is correct; the SDK offers an ineffective action.

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `master`, `~/stripe/stripe-android-2`. This is the final, sequential topic after order-summary released the environment. Used the bug-bash skill, Android and tax/saved-card references, repository unit/network test skills, and build-delegation skill. Used current backend source to resolve the modeless SFU contract. Preserved the user's modified `AGENTS.md`, existing billing-address report/swap, and other topics' evidence.

## Environment and evidence boundaries

Physical Pixel 6a/API 36. Ordinary keyguard checks at the beginning and before cleanup returned `showing=true`, `secure=true`. No unlock, PIN, bypass, emulator, connected-device test, native tap, screenshot, or recording. Reused the already installed current-source debug APK with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`; this topic did not build/install/launch the app or change preferences/debugger settings.

Backend: `https://stp-mobile-playground-backend-v7.stripedemos.com`, test merchant `US`, Checkout creation version `2026-08-26.preview`. Every live phase fetched and asserted `pk_test_` before provisioning. Common configuration: `ui_mode=mobile_elements`, EUR 120.00 one-time synthetic item, explicit fresh customer with synthetic email, tax off, adaptive pricing off. New cards used `tok_visa`; SEPA used the documented test IBAN `DE89370400440532013000`. Credentials remained in process memory and retained JSON is redacted.

The 19 live confirmations use inline `payment_method_data`, the same relevant SFU/save/redisplay shapes sent by the SDK, and independently retrieve the resulting PaymentIntent with expanded PaymentMethod. Every case creates two new sessions for the same customer: default redisplay filtering and all three values (`always`, `limited`, `unspecified`). These are real backend calls, separate from JVM SDK replays. All 11 card PaymentIntents succeeded/Checkout complete-paid. All 8 SEPA PaymentIntents were processing/Checkout complete-unpaid; eventual settlement was not observed.

## 1. Session / resulting PaymentIntent SFU

The supported modeless global parameter is **`payment_settings[setup_future_usage]`**. The old `payment_intent_data` key is rejected in modeless mode, and explicit `mode=payment` is rejected with `ui_mode=mobile_elements`. Those early setup errors did not establish that global SFU was unsupported. Current [creation schema](https://stripe.sourcegraphcloud.com/stripe-internal/mint/-/blob/pay-server/lib/payment_pages/api/checkout/session/create_params/payment_settings_api_params.rb?L6-34) and [CheckoutClient init test](https://stripe.sourcegraphcloud.com/stripe-internal/mint/-/blob/pay-server/lib/payment_pages/api/checkout_client/test/init_method.test.rb?L92-115) identify the correct key; subsequent live global cases below confirmed it.

| Session SFU / save offer / selected choice | Actual PI SFU | Attachment and redisplay in fresh sessions |
| --- | --- | --- |
| Absent / disabled / omitted | null | Unattached; absent with default and all filters |
| Absent / enabled / unchecked | null | Unattached; absent with default and all filters |
| Absent / enabled / checked | off_session | Attached, always; visible with default and all filters |
| on_session / disabled / omitted | on_session | Attached, limited; only visible with all filters |
| off_session / enabled / unchecked | off_session | Attached, limited; only visible with all filters |
| on_session / enabled / checked | on_session | Attached, always; visible with default and all filters |

Evidence: [baseline results](sfu-results.json), [global results](global-results.json), creation/init/confirm/Intent and returning-session JSON for each named case. Checking save preserves explicitly configured global `on_session`; it does not broaden it to `off_session`. Unchecking does not remove a merchant-required global SFU. The server normalized inline new-method `allow_redisplay=unspecified` to `limited` for the unattached controls; it did not attach those methods. This is a raw backend observation, not Android filtering.

`sfu-results.json` also retains the three first-pass `payment_intent_data` errors. Their names start `sfu-on-` / `sfu-off-`; the successful corrected global cases start `global-`. The standalone probe's `sfu` phase now runs only the three baseline controls so future runs do not repeat the obsolete setup.

## 2. Per-method PMO SFU and precedence

| Configuration / choice | Effective initial method SFU | Resulting PI fields and persisted outcome |
| --- | --- | --- |
| Card PMO off_session, global absent, save disabled | off_session | Top-level null; card PMO off_session; attached/limited, all-filter only |
| Card PMO on_session, global absent, save disabled | on_session | Top-level null; card PMO on_session; attached/limited, all-filter only |
| Card PMO off_session, save enabled/unchecked | off_session | Top-level null; PMO off_session retained; attached/limited, all-filter only |
| Card PMO on_session, save enabled/checked | on_session | Top-level off_session; PMO on_session retained; attached/always, default visible |
| Global on_session + card PMO off_session, save disabled | off_session | Top-level on_session; PMO off_session wins for card; attached/limited, all-filter only |
| SEPA PMO off_session, global absent, save disabled | off_session | Top-level null; PMO off_session; attached/limited, all-filter only |
| SEPA PMO on_session, global absent, save disabled | on_session | Top-level null; PMO on_session; attached/limited, all-filter only |
| SEPA PMO none, global absent, save disabled | none | Top-level null; PMO none; unattached and absent with all filters |
| Global off_session + SEPA PMO none, save disabled | none | Top-level off_session; PMO none wins; unattached and absent with all filters |
| Global off_session + SEPA PMO none, save enabled/checked | none | Top-level off_session; PMO none retained; PM always but unattached/absent |

Evidence: [PMO card results](pmo-results.json), [SEPA results](sepa-results.json), [global-none precedence](precedence-results.json). The current [backend helper](https://stripe.sourcegraphcloud.com/stripe-internal/mint/-/blob/pay-server/lib/payment_pages/helpers/payment_method_helper.rb?L96-130) explicitly treats PMO `none` as overriding top-level SFU to nil. SDK parsing preserves the effective per-method policy in the synthetic Intent's PMO map, so a synthetic top-level SFU of null does not lose global policy. The actual retrieved Intent retains its real top-level and per-method fields.

Supported boundaries, with actual rejected creation responses retained:

- Global `payment_settings[setup_future_usage]=none` is rejected; valid values are on_session/off_session. See [correct-key error](boundary-modeless-global-none-error.json).
- Card PMO `none` is rejected with or without global SFU or enabled saving. It was not substituted into a fixture and presented as supported.
- SEPA PMO `none` and on_session are accepted and were confirmed live. The boundary phase's accepted SEPA-on init was followed by a fresh completed SEPA-on case.
- Bare top-level `setup_future_usage` is unknown. Explicit mode and old `payment_intent_data` are rejected for this mobile_elements/modeless interface. See `boundary-*-error.json`, `discovery-payment-mode-global-off-error.json`, and the preserved first-pass SFU errors.

## Confirmed save-offer defect

The minimal [fresh control/candidate pair](save-none-results.json) differs only by SEPA PMO `none`. Both enable saving and submit checked save with always redisplay:

- Control: processing Intent with off_session, PM attached to the exact customer, same PM returned by default and all-filter fresh sessions.
- Explicit none: processing Intent with top-level off_session plus SEPA PMO none, PM always but customer null, no method returned by either fresh session.

A separate [mixed card/SEPA session](mixed-results.json) reproduced the candidate. This is a coherent merchant configuration: card saving is enabled, SEPA is explicitly one-time. The JVM regression first proves both methods are supported and card's save checkbox exists, then fails because SEPA also receives `SaveForFutureUseElement`.

The source path is public `CheckoutElementsController.configure` → actual loader/parser/customer metadata → `SepaDebitDefinition` → `isSaveForFutureUseValueChangeable`. Enabled save consent currently only checks that customer metadata exists. It ignores PMO none even though `metadata.hasIntentToSetup("sepa_debit")` is correctly false. The customer-facing resource says "Save payment details to %s for future purchases". Form state, save-choice conversion, and the interceptor correctly produce save=true/allow_redisplay=always; that cannot defeat the intentional backend `none` override.

This is one root cause, including the ineffective checkbox and absent future method. No report is filed for backend precedence, raw redisplay normalization, async processing, or saved-method filtering. No fix is included.

Duplicate search used `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all` with `"setup_future_usage" "checkout"`, `"setup future usage"`, and `"SEPA" "save"` title/body searches. Returned issues were unrelated; the closest #6505 concerns old PaymentSheet SetupIntent/card attachment and was read. Rechecked the root billing-address report; it has a different cause.

## SDK replay validation

[controller-confirm-replays.patch](controller-confirm-replays.patch) contains only temporary test additions to `CheckoutElementsControllerTest`. They configure captured live responses through the real public dependency graph, check per-method SFU and generated form save controls, toggle the actual save controller, build a matching selection, run the production selection-to-confirmation conversion, and exercise real `CheckoutSessionConfirmationInterceptor` plus `CheckoutSessionRepository` using `NetworkRule`. Request matchers verify save flag omission/true/false, new-method redisplay, and absence of client SFU/PMO overrides. The repository retrieves the captured actual Intent, and assertions verify its status/SFU/PMO/redisplay interpretation.

The replays use synthetic valid client secrets/Intent IDs and the configured test API key/account. They remove only `experiments_data` from in-memory Checkout responses to avoid unrelated Link-holdback telemetry, following the prior topics; raw JSON retains the experiment metadata. Link display is disabled. No SFU/save/customer/payment result fields are normalized. Form field entry is supplied as test data; no native View/Compose rendering or actual public presenter-to-authentication flow is simulated.

- [Controls](jvm-controls-results.json): **18 passed**, zero failures/errors/skips, Gradle exit 0 in 30s. Twelve new response/form/selection/interceptor cases and six existing public-presenter/save-serialization checks.
- [Mixed case and regression](jvm-mixed-results.json): mixed response/form/confirm case passed; explicit-none form regression failed as expected with `expected to be empty` / actual `SaveForFutureUseElement(initialValue=false, merchantName=Mobile Example Account)`. Gradle exit 1 in 19s. This gives **19 passing checks plus one expected-failing regression** across the final scoped runs.
- The same regression was first run against the SEPA-only minimal init and failed identically: [result](jvm-sepa-only-regression-results.json). The final patch uses the stronger mixed-method setup.
- Harness corrections before the passing controls: a test-only Any-to-Map cast; synthetic Intent-secret format; missing test Stripe account in request options. The header mismatch caused 10 replay failures while six existing checks passed; it was not a product failure. No fixture-result mismatch was suppressed.

Apply the patch to the clean source revision, then run:

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.confirm sfu bash*' \
  --tests 'com.stripe.android.checkout.CheckoutConfirmationPerformerTest.presenter confirm from a background thread*' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.intercept with new payment method passes shouldSave*' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.intercept with new payment method omits savePaymentMethod*' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.intercept with saved payment method passes null for savePaymentMethod' \
  -PDIRECTLY_TO_PLAYGROUND=false -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true

./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.confirm sfu regression explicit none does not offer an ineffective save checkbox' \
  -PDIRECTLY_TO_PLAYGROUND=false -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

The second command is expected to fail until the save-offer defect is fixed. The final aggregate control command includes the added mixed control, so a fresh replay contains 19 passing tests. The low-cost executor was already unavailable after repeated empty task deliveries in earlier topics. The coordinator authorized the skill's sequential local fallback; no additional agent was spawned. Gradle used ordinary sandbox escalation for existing external cache access.

The live probe reuses only the prior email topic's redacting transport. Reproduce selected phases with `python3 bug-bash/confirm/live_probe.py PHASE`; phases are `boundaries`, `sfu`, `global`, `pmo`, `sepa`, `precedence`, `save-none`, `mixed`. Each phase provisions fresh test objects. `python3 bug-bash/confirm/verify_evidence.py` provisions nothing and passed for all 19 recorded confirmations, SFU/PMO precedence, attachment/redisplay outcomes, JSON validity, and credential/capability-URL checks.

## Gaps

Native layout, taps/checkbox rendering, accessibility, authentication/3DS, redirect completion, retry after decline, process restoration, actual off-session charges, and SEPA settlement remain untested. No Google Pay/Link, guest customer creation, recurring/setup modes, or broad LPM matrix. Saved-method save-flag omission is covered by an existing JVM check; this pass did not reconfirm an already saved method live. Completed Checkout retrieval is not claimed; persistence is measured by independently retrieved Intents and fresh active customer sessions. Previous topic evidence is inherited context, not additional tests counted here.

## Cleanup and environment release

All backend and Gradle executions finished. The temporary controller-test edits were preserved as the patch and restored; production SDK/app source was never edited. Only the user's pre-existing `AGENTS.md` change remains in the tracked diff. Prior evidence and the billing-address issue/swap were preserved. No commit/amend, push, PR, remote issue, Slack, or email.

No relay, recorder, debugger setting, app preference, private credential file, adb mapping, or device scratch file was created. Original absent playground preferences remain absent; reverse mappings remain empty. Owned `/tmp` test backup/logs were removed after preserving the compact results. The same current-source debug build and flags remain installed, and the Pixel remains PIN locked. Retained changes are the one root report and redacted evidence/probe/checker/test patch/notes under `bug-bash/confirm/`. Test-mode backend objects remain.

**Confirm has released the device/build/backend environment. All seven requested topic passes are finished; the coordinator may consolidate results.**

## Native follow-up — 2026-11-11, after the user unlocked the Pixel

The existing save-offer defect is now **reproduced through the actual example-app UI and real backend**, closing the earlier native gap for this scenario. No duplicate report or SDK fix was added. Three native payments and three fresh same-customer UI/inventory checks were performed. The prior 19-payment backend/JVM matrix was not repeated.

The Pixel 6a/API 36 was `showing=false`, `inputRestricted=false`. The installed APK remained the clean source revision `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `DIRECTLY_TO_PLAYGROUND=false`, cleartext=true. No build, install, test-source hook, API response substitution, emulator or instrumentation task. The user's stay-awake value was read as `7` and never changed. Fresh backups showed Checkout playground preferences absent; `com.stripe.android.PaymentConfiguration.xml` was backed up opaquely and later restored byte-for-byte.

[native_probe.py](native_probe.py) supplied only the backend-only loopback relay and test settings. Session creation requests were patched with supported SFU fields and adaptive pricing disabled, then forwarded to the real backend unchanged apart from those creation options. SDK init/confirm/retrieve requests went directly to Stripe. The helper separately retrieved active Checkout state and polled completion using the test publishable key; it did not intercept SDK confirmation traffic. Private credentials/backups were removed after cleanup.

All configurations used merchant `US`, API creation version `2026-08-26.preview`, an explicit fresh test customer with email `native-confirm@example.com`, EUR 120.00, the playground's two one-time items, card+SEPA allowed, save enabled, tax off and adaptive pricing off. The subsequent session reused that exact customer, removed SFU/PMO overrides and used all redisplay filters. [Native result summary](native/results.json).

| Native configuration/action | Actual native and backend outcome |
| --- | --- |
| Candidate: SEPA PMO SFU `none`; enter synthetic name, test IBAN ending 3000 and full US billing address; check save; Continue → Confirm | App displayed `Payment complete!`. A separately polled backend response was complete/succeeded/paid. Fresh all-filter session had no saved methods in raw response or native UI. |
| Matched control: same inputs/options, only omit SEPA PMO `none`; check save; Continue → Confirm | App displayed `Payment complete!` and added bank ending 3000. Fresh all-filter session returned one SEPA PM with `allow_redisplay=always`, displayed it, and selected it. |
| Merchant SFU: `payment_settings[setup_future_usage]=off_session`; enter test Visa 4242, expiry 12/30, CVC 123 and US ZIP 94103; leave save unchecked; Continue → Confirm | Native card form displayed the future-charge mandate and unchecked save control. App completed, backend poll was complete/succeeded/paid. Fresh all-filter session returned one Visa with `allow_redisplay=limited`, displayed it, and selected it. |

The SEPA inputs used `Native Confirm Tester`, test IBAN `DE89370400440532013000`, and the observed autocomplete suggestion for 510 Townsend Street, San Francisco, CA 94103. Fixed customer email prefilled correctly. Native screenshots show complete billing fields, so the earlier unrelated incomplete saved-card tax issue does not apply. The card autofill prompt was dismissed with `No thanks`; Google's offer to save the synthetic test card was dismissed with `Not now`. No personal wallet selection or screenshot was retained, and nothing was saved to Google.

All retained screenshots were opened and visually inspected:

- Candidate [save checked](native/candidate-save-checked.png), [native complete](native/candidate-completed.png), [fresh method missing](native/candidate-fresh-missing.png); raw [initial effective none](native/candidate-initial.json), [completion poll](native/candidate-poll.json), [fresh all-filter empty inventory](native/candidate-fresh-all.json).
- Control [save checked](native/control-save-checked.png), [native complete](native/control-completed.png), [fresh saved SEPA selected](native/control-fresh-saved.png); [fresh raw inventory](native/control-fresh-all.json).
- Global SFU [unchecked save and mandate](native/global-off-unchecked.png), [native complete](native/global-off-completed.png), [fresh limited card selected](native/global-off-fresh-limited.png); [initial effective off_session](native/global-off-initial.json), [fresh raw inventory](native/global-off-fresh-all.json).
- [53.9-second candidate recording](native/candidate-confirm.mp4) covers the checked SEPA form through Continue, selection, native confirmation and `Payment complete!`. It does not cover the later fresh session. [Video metadata](native/video-validation.json). [verify_video.swift](verify_video.swift) decoded exact first/middle/final frames via AVFoundation and those frames were viewed. An initial sandbox decode failed; the ordinary escalated run succeeded. The first decoder's default time tolerance selected the initial keyframe repeatedly; setting both tolerances to zero verified the actual later completion frames. No video failure was inferred from that decoder behavior.

The native follow-up does **not** newly claim the actual resulting Intent's SFU fields or its processing/settlement timing: no native Intent body was captured. The post-confirm polls for all three native cases reported paid at observation time, unlike the earlier direct-backend SEPA captures that were processing/unpaid. The exact earlier Intent-field claims remain supported by those separate direct-backend/JVM tests. The native assertion here is checked save → completed payment → absent fresh customer method, contrasted with the matched saved-SEPA control.

Native gaps still remaining: authentication/3DS, redirects, failed-payment retry, process restoration, broad on_session/off_session/PMO native combinations, accessibility, actual off-session reuse, and independent settlement/webhook inspection. This follow-up does not repeat or close those gaps. No further Gradle/JVM suite was needed because neither SDK nor test source changed.

Practical reuse for the next sequential agent: reuse the installed clean APK; discover the physical serial again and open `CheckoutElementsController Example` from MainActivity. The helper's `serve` uses an ephemeral loopback port and its own adb reverse mapping. `start candidate|control|global-off|returning-CASE` creates appropriate fresh test customers/settings; `tree`, `tap`, `enter`, `shot`, `snapshot`, `poll` support observed navigation. Back up current settings afresh for each topic; do not copy a previous private state directory. `restore` restores both backed-up preferences, removes the mapping and device scratch, and returns to MainActivity. Stop the identified owned relay process and delete its private state afterward. A final `tree` creates a scratch XML again, so remove that after verification. Card entry triggers both an autofill prompt and a Google-save prompt; observe and dismiss them before continuing.

Cleanup verified: settings match fresh backups; Checkout preferences are absent again; reverse mappings empty; owned recorder and backend relay ended; owned device XML/video and private credential/backup/video-cache files removed. No source/test edits, builds, commits, pushes, PRs, remote issues or messages. Only local helper/media/redacted evidence, this appended handoff and the existing report's Test evidence were updated. `git diff --stat` remains only the pre-existing `AGENTS.md` edit. The Pixel is unlocked at the main menu, with stay-awake `7` and the original installed flags unchanged.

**Native Confirm follow-up has released device/build/backend access. The coordinator may start customer logic next.**
