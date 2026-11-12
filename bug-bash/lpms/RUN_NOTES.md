# LPM bug bash — 2026-11-11

No new confirmed bug. Covered each of the five requested subtopics with real test-backend controls and focused JVM tests. Native UI and actual bank-redirect completion remain untested because the physical Pixel stayed PIN locked. A processing SEPA PaymentIntent is not a settled payment, and a pending Bancontact redirect is not a cancellation.

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `master`, `~/stripe/stripe-android-2`. Used the personal bug-bash skill, Android and tax/saved-card references, and repository unit/network testing and build-delegation skills. Preserved the user's modified `AGENTS.md`, existing billing-address issue/swap file, and all prior evidence. No fixes or remote writes were requested or made.

## Environment and controls

Physical Pixel 6a/API 36. Initial and final ordinary keyguard checks showed `showing=true`, `secure=true`, screen off. No PIN, bypass, emulator, connected-device test, app settings, or debugger changes were used. Reused the current-source debug APK built/installed by the customer pass with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`; this topic did not rebuild, install, or launch the app.

Backend: `https://stp-mobile-playground-backend-v7.stripedemos.com`, merchant `US`, Checkout creation version `2026-08-26.preview`. Every live run asserted a `pk_test_` key before provisioning. Shared settings: `ui_mode=mobile_elements`, EUR 120.00, one synthetic one-time item, automatic tax off, adaptive pricing off, fixed test email. The incompatible-currency control deliberately requested USD. Created only synthetic test customers, tokenized Visa/Mastercard/Amex cards, SEPA test IBANs, and Bancontact PaymentMethods.

[live_probe.py](live_probe.py) reuses the redacting request helpers retained in `../email-collection/live_probe.py`. Run each phase once:

```sh
python3 bug-bash/lpms/live_probe.py methods
python3 bug-bash/lpms/live_probe.py redisplay
python3 bug-bash/lpms/live_probe.py outcomes
python3 bug-bash/lpms/verify_evidence.py
```

The last command provisions nothing and checks the recorded method, filtering, saving, and outcome assertions. It passed for seven method configurations, six redisplay configurations, and four payment outcomes. API calls used the SDK's init, confirm, retrieve-intent, and poll shapes. This is direct backend coverage, not native confirmation orchestration. Creation requests and redacted raw responses are stored alongside the three result summaries. Keys/client secrets were kept in process memory. The Bancontact redirect URL is replaced with a nonfunctional example URL in retained evidence.

## 1. Polling and async/redirect outcomes

[outcomes-results.json](outcomes-results.json) records these actual outcomes. Before each confirmation, `/poll` returned `not_started`. Three consecutive post-confirmation polls were captured for each case.

| Case | Confirmation and independently retrieved Intent | Poll result |
| --- | --- | --- |
| Visa control | Checkout complete/paid; PaymentIntent succeeded | succeeded, complete/paid |
| SEPA, save=false | Checkout complete/unpaid; PaymentIntent processing, no SFU | succeeded, complete/unpaid |
| SEPA, save=true | Checkout complete/unpaid; PaymentIntent processing, SFU off_session | succeeded, complete/unpaid |
| Bancontact | Checkout open/unpaid; PaymentIntent requires_action with redirect_to_url | pending, open/unpaid |

The real SEPA result demonstrates that confirmation success and payment settlement differ. Neither SEPA charge was observed through eventual settlement/webhooks. Bancontact's bank page was not opened or authorized, so redirect success, failure, customer cancellation, app return, and process restoration remain gaps. No timeout or cancellation was inferred from an abandoned await. No native polling screen was exercised.

Source path: `CheckoutSessionConfirmationInterceptor` sends the Checkout confirm, and `CheckoutSessionRepository.confirm` retrieves the referenced actual Intent. The interceptor launches next actions for `requiresAction`, otherwise completes with the Checkout response in metadata. `CheckoutOperationCoordinator` calls `DefaultCheckoutConfirmationFinalizer` after a successful confirmation/next-action result, then refreshes with the finalized response. Failed/canceled results instead re-fetch the open session. The finalizer intentionally skips polling when Checkout is already complete, retries pending/request-error results, and optimistically returns the last state on its 30-second timeout. This documented fallback was not treated as a bug.

Existing tests passed for immediate complete, successful terminal poll, pending plus transient error retry, both timeout fallback cases, failed terminal poll, unexpected not_started, next-action launch, and confirmation argument/result handling. Captured-response replay preserved complete/unpaid for SEPA and open/unpaid for Bancontact. These are JVM checks; they do not establish device cancellation or redirect behavior.

## 2. Saving for future usage and redisplay filters

The two SEPA confirmations used separate explicit customers with `saved_payment_method_options.payment_method_save=enabled`. The no-save request supplied `collected_information[save_payment_method]=false` and an unspecified-redisplay method. The save request supplied true and an always-redisplay method, matching the SDK's consent-derived parameter shapes.

- Save=false: actual PI had no setup_future_usage, and a fresh session for that customer returned no saved method even with all three redisplay filters enabled.
- Save=true: actual PI had setup_future_usage=off_session; the confirm response and a fresh session returned the same SEPA PM ID with allow_redisplay=always.

[redisplay-results.json](redisplay-results.json) compares a separately provisioned customer's saved Visa/always, Mastercard/limited, Amex/unspecified, and three SEPA PMs with those respective redisplay states.

| Server filter | Raw backend inventory |
| --- | --- |
| Omitted/default | Visa/always and SEPA/always |
| always, limited, unspecified | All three card brands plus one SEPA/unspecified |
| limited only | Mastercard/limited and SEPA/limited |
| unspecified only | Amex/unspecified and SEPA/unspecified |
| All redisplay values, SEPA-only allowed type | SEPA only |
| All redisplay values, card-only allowed type | Three cards only |

The three SEPA PMs intentionally used the same test IBAN and have the same fingerprint. The all-filter backend response coalesced these into one visible bank instrument. Each restricted filter returned its corresponding distinct SEPA PM ID, demonstrating the other methods were still available. This behavior is visible in the raw backend response before Android parsing; it is not local SDK loss. The exact server deduplication policy was not independently traced. Distinct IBANs and backend deduplication rules were outside this focused pass.

Captured-response controller replay proved that local inventory retained every server-returned PM, including limited and unspecified values, rather than silently imposing the default always filter. `CreateCustomerMetadata` maps Checkout's saved-method save setting into consent behavior; `CreateCustomerState` first restricts supported saved types, then applies local card/billing filters. `PaymentMethodMetadata.allowRedisplay` and the save-consent helper set new-method redisplay, while the confirmation interceptor sends the user's save choice for payment mode when consent is enabled and omits it for already-saved methods.

Gaps: the native checkbox and mandate presentation, guest/customer_creation saving UX, post-settlement off-session reuse, redirect-generated reusable methods, and setup/subscription modes. The later Confirm topic owns the general SFU/PMO SFU matrix; this pass only established the SEPA payment/save integration above. Guest init without customer metadata was not promoted into a supported-behavior bug from code inspection alone.

## 3. Card-brand filtering

The raw all-redisplay response contained Visa, Mastercard, Amex, and SEPA. Public-controller graph replay with `PaymentElement.Configuration.cardBrandAcceptance` verified:

- All brands: all four backend-returned methods retained.
- Visa allowed: Visa plus SEPA retained; Mastercard and Amex filtered locally.
- Visa disallowed: Mastercard, Amex, and SEPA retained.

The backend response remained unchanged in those replays. This is a client setting: the public KDoc explicitly describes it as client-side and not supported in Link. Source tracing followed the configuration through `CheckoutCommonConfigurationFactory`/embedded configuration to `PaymentSheetCardBrandFilter`; `DefaultPaymentMethodFilter` applies it to saved cards while accepting non-card methods. New entry uses `CardNumberConfig` and `CardNumberController` for validation, icons, and selection.

Focused existing card-brand tests also passed for brand acceptance, display-brand fallback, non-card handling, new-card allowed/disallowed validation with and without CBC, icon filtering, and rejection reporting. Native entry/error rendering, co-branded card selection on device, Link, Google Pay, and backend brand enforcement were not exercised. No claim is made that this local setting enforces a server-side brand restriction.

## 4. Getting an order summary in LPM flows

Seven controller graph replays included a dedicated saved-SEPA case: after configure, clear the selection, observe the public session Flow, select the saved SEPA method through the SDK's selection path, and observe the next session emission. The order-summary item and EUR 120.00 total were accessible before selection and unchanged after selection; the payment option changed to SEPA.

Three raw parser/mapper replay tests then verified the item name, quantity, item total, and formatted EUR 120.00 session total in a pending Bancontact confirm response, a complete/unpaid SEPA confirm response, and a complete/paid Visa control. Applying the captured poll state retained those summaries and payment statuses.

Gaps: actual on-device rendering and merchant UI access during a live confirmation; summary behavior after completed bank redirects; updates involving discounts, tax, shipping, or adaptive currency. The later Order Summary topic owns the general summary matrix. This topic's evidence specifically covers LPM selection, pending redirect state, and async-confirmation state.

## 5. Allowed payment-method types

[methods-results.json](methods-results.json) records:

| Requested types/configuration | Observed backend result |
| --- | --- |
| card only, EUR | card plus card wallets apple_pay/google_pay |
| sepa_debit only, EUR | SEPA only |
| bancontact only, EUR | Bancontact only |
| card, sepa_debit, bancontact, EUR | Those methods plus card wallets |
| Omitted list, EUR | card, card wallets, Link, SEPA, iDEAL, Bancontact, P24, EPS, Multibanco |
| Empty list, EUR | Same eligible set as omitted list |
| SEPA only, USD | Creation rejected: SEPA supports EUR, not USD |

The SEPA-only session rejected a direct card confirmation with `payment_method_types_mismatch`. This confirms backend enforcement, not merely a hidden SDK option. The empty-list result is backend normalization and was not treated as an Android bug. Invalid currency/setup was excluded from product-failure claims.

Controller graph replay mapped the mixed set to exactly card/SEPA/Bancontact and the automatic set to exactly card/SEPA/iDEAL/Bancontact/P24/EPS/Multibanco with Link disabled. The SEPA-only captured customer session supported only SEPA and retained only the eligible saved SEPA. The parser consumes `ordered_payment_method_types_and_wallets`, and local payment-method definitions then determine supported native methods; wallet entries do not become ordinary LPM rows.

Gaps: a complete method/country/currency matrix, native forms for each returned method, external/custom methods, merchant dashboard activation changes, and unknown preview methods. Those were deliberately outside the representative supported-method scope.

## JVM validation and reproduction

- [jvm-existing-results.json](jvm-existing-results.json): **117 passed**, zero failures/errors/skips, Gradle exit 0 in 22s. Finalizer 7, poll parser 5, confirmation performer 22, confirmation interceptor 83.
- [jvm-replay-results.json](jvm-replay-results.json): **10 passed**, zero failures/errors/skips, Gradle exit 0 in 33s. Seven public-controller graph tests and three raw parser/mapper tests.
- [jvm-card-brand-results.json](jvm-card-brand-results.json): **19 passed**, zero failures/errors/skips, Gradle exit 0 in 26s. Brand filter 12, card-number validation 5, card-number controller 2. Total across the three runs: **146 passed**.

Commands for existing checks:

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.DefaultCheckoutConfirmationFinalizerTest' \
  --tests 'com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponseJsonParserTest' \
  --tests 'com.stripe.android.checkout.CheckoutConfirmationPerformerTest' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest' \
  -PDIRECTLY_TO_PLAYGROUND=false -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.paymentsheet.PaymentSheetCardBrandFilterTest' \
  :payments-ui-core:testDebugUnitTest \
  --tests 'com.stripe.android.ui.core.elements.CardNumberConfigTest.determineState*brand*' \
  --tests 'com.stripe.android.ui.core.elements.CardNumberControllerTest.trailingIcon should filter out disallowed brands' \
  --tests 'com.stripe.android.ui.core.elements.CardNumberControllerTest.on disallowed card brand entered*' \
  -PDIRECTLY_TO_PLAYGROUND=false -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

Temporary replay source was preserved as [lpm-boundary-tests.patch](lpm-boundary-tests.patch) and reversed. Reproduce at the recorded revision:

```sh
git apply bug-bash/lpms/lpm-boundary-tests.patch
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.lpm bash*' \
  --tests 'com.stripe.android.checkout.LpmBashResponseTest' \
  -PDIRECTLY_TO_PLAYGROUND=false -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
git apply -R bug-bash/lpms/lpm-boundary-tests.patch
```

Controller replay uses the real Dagger graph and NetworkRule init response. The loader removes only `experiments_data`, as established by the earlier email pass, to exclude unrelated Link experiment lookups; Link display is disabled through public configuration. Relevant method/customer fields and wallet eligibility remain from the live response. The mapping tests use the redacted raw response without that normalization. No replay test failed.

The required low-cost executor again received an empty task payload and did no work. It was stopped. Exact Gradle commands ran sequentially in this agent under the delegation skill's unavailable-executor fallback. No broad suite or instrumentation task was run.

## Findings and environment handoff

Rechecked the known root billing-address issue and searched GitHub using `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'checkout LPM'` and `'CheckoutElementsController'`; both returned no matches. No `ISSUE_lpms_*.md` was justified. Did not rerun or duplicate the known billing/tax saved-card editor cause.

All temporary SDK/test edits have been reversed. All helper/Gradle executions finished and the unused executor is stopped. No relay, recorder, private secret file, app preference replacement, or device scratch file was created. Final adb reverse list is empty; the playground preference file was absent and remains absent. The installed source and flags are unchanged, and the Pixel remains PIN locked. No screenshot/video is cited as LPM UI evidence.

Only this directory's local probe, evidence validator, redacted responses/results, replay patch, and notes remain. Test-mode backend objects remain. No commit/amend, push, PR, remote issue, Slack, or email was created. **LPMs has released the device/build/backend environment to the coordinator for the next sequential topic.**

## Native follow-up — 2026-11-11, after the user unlocked the Pixel

**One new confirmed accessibility-label defect:** [saved Amex exposes Unknown instead of its brand](../../ISSUE_lpms_saved_amex_accessibility_unknown_brand.md). The native filtering, saved-SEPA confirmation, and representative Bancontact redirect controls passed. This follow-up closes the specific native gaps below; the earlier backend/JVM matrix remains separate coverage.

Environment: physical Pixel 6a `REDACTED_DEVICE_SERIAL`, Android 16/API 36, initially unlocked and on the main menu. Opened the actual CheckoutElementsController example promptly. Reused the clean installed APK from `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `DIRECTLY_TO_PLAYGROUND=false`, cleartext=true. No app build/install, production change, emulator, connected-device instrumentation, debugger change, TalkBack change or device-policy change. The existing stay-awake value `7` and device-admin five-minute idle policy were left unchanged. The initial accidental main-menu tap opened the older Playground; Back and the observed CheckoutElementsController Example label corrected navigation before any settings or backend action.

Read the bug-bash Android and saved-card references, prior native handoffs, root issue reports, and required unit/network/build-delegation skills. [native_probe.py](native_probe.py) owns a fresh `LPMS_NATIVE_STATE`, fresh preference backups, an ephemeral loopback relay and one adb reverse mapping. The relay adds only supported creation options: adaptive pricing disabled, and all three redisplay filters except in the default control. Actual backend responses are forwarded unchanged. SDK init/confirm/retrieve traffic goes directly to Stripe. Independent active-session retrieves and polls provide backend observations. All provisioning asserts `pk_test_`; only synthetic objects were created.

Shared configuration: merchant US, creation version `2026-08-26.preview`, EUR 120.00 (Classic T-Shirt EUR 35 × 2 and Zip-Up Hoodie EUR 50 × 1), no tax/shipping/full billing collection, Link and Google Pay hidden, saving/removal disabled. One fresh customer uses `native-lpms@example.com`. Four attached methods have complete synthetic US billing details: Visa 4242/always, Mastercard 4444/limited, Amex 8431/unspecified and SEPA 3000/always. Brand-filter cases use supported public PaymentElement configuration through playground settings. Six fresh sessions reuse that exact customer; all remain distinguishable in [results](native/results.json).

### Native coverage across all five LPM subtopics

| Subtopic | Actual native and backend evidence |
| --- | --- |
| Polling and redirects | Saved SEPA Confirm completed in the app; poll changed not_started/open/unpaid → succeeded/complete/paid. Bancontact Confirm opened an owned Chrome custom tab and a real Bancontact test-bank page for EUR 120; independent poll was pending/open/unpaid before authorization. AUTHORIZE TEST PAYMENT returned automatically to the app, which displayed Payment complete!; poll became succeeded/complete/paid. The test-bank page's actual native PaymentIntent was requires_action/redirect_to_url. An independent retrieve of the same PI after return was succeeded, amount 12000 EUR, livemode=false. |
| Saving and redisplay | The default backend filter and native saved list contained only SEPA/always and Visa/always. All-filter backend/native lists contained all four methods, including Mastercard/limited and Amex/unspecified. Limited Mastercard was selected successfully. Native checked SEPA saving and its explicit-PMO-none defect are inherited from [Confirm native evidence](../confirm/RUN_NOTES.md#native-follow-up--2026-11-11-after-the-user-unlocked-the-pixel), not rerun or counted again. This run also confirmed the already-saved SEPA method. |
| Card-brand filters | With identical four-method raw inventories, allowed Visa displayed only Visa plus SEPA; disallowed Visa displayed Mastercard, Amex and SEPA. Under allowed Visa, the native new-card field advertised Visa and a valid synthetic Mastercard number displayed Mastercard is not accepted. The form was not completed or confirmed; disabled Continue also had missing expiry/CVC/ZIP, so it is not used as isolated proof of brand-based gating. One No thanks autofill prompt was observed and dismissed; no personal card was used or retained. |
| Order-summary access | EUR 70 + EUR 50 line items and EUR 120 subtotal/total remained visible in the mixed/saved-method flow, after selecting the limited Mastercard, in SEPA-only selection and completion, and before/after the Bancontact redirect. No amount discrepancy. This is representative access/retention; the separate Order Summary topic owns broader summary behavior. |
| Allowed payment-method types | Mixed card/SEPA/Bancontact rendered New card, SEPA Debit and Bancontact. SEPA-only rendered the saved SEPA plus new SEPA, with no new-card/Bancontact row and only SEPA in raw inventory. Bancontact-only rendered only Bancontact and no saved instrument; raw response likewise returned no saved card/bank and only Bancontact as an ordered type. The earlier backend mismatched-type enforcement test remains separate evidence. |

All retained PNGs were opened and visually inspected. Evidence includes [mixed summary](native/mixed-summary.png), [all saved methods](native/mixed-saved-all.png), [limited Mastercard selected](native/mixed-mastercard-selected.png), [default saved methods](native/default-saved.png), [Visa-only saved list](native/visa-only-saved.png), [Visa-disallowed list](native/no-visa-saved.png), [new Mastercard validation](native/visa-only-new-mastercard-rejected.png), [SEPA-only summary](native/sepa-only-summary.png), [SEPA completion](native/sepa-completed.png), [Bancontact summary](native/bancontact-summary.png), [name form](native/bancontact-name.png), [selected Bancontact](native/bancontact-selected.png), [test-bank page](native/bancontact-bank-cropped.png) and [native redirect completion](native/bancontact-completed.png). Native screenshots have adjacent XML; the bank page has explicitly redacted XML.

The bank screenshot is cropped to the observed heading, EUR amount and authorization controls (original Pixel coordinates x=0..1080, y=290..1610). Browser toolbar and raw Intent object are excluded. A first crop attempt found Pillow unavailable; macOS `sips` produced the inspected safe crop. The full screenshot was only a private temporary file and was deleted. The bank page exposed a test-only raw Intent object; its capabilities remained private, while [redacted initial Intent](native/bancontact-bank-payment_intent.json) and [independently retrieved final Intent](native/bancontact-native-payment-intent-final.json) preserve actual outcomes. The owned custom tab closed automatically on redirect; activity inspection showed the Checkout activity and no Chrome custom-tab activity. Existing browser tabs/profile were not navigated, closed, cleared or otherwise modified. No video was necessary for these controls.

### Confirmed Amex label defect and focused regression

The real raw inventory contains `brand=amex`, `display_brand=american_express`, last4 8431. Both the all-brand sheet and Visa-disallowed sheet display the correct Amex icon, but expose `content-desc="Unknown ending in 8 4 3 1 "`. Visa/Mastercard expose their supported brand names. This is observed accessibility-label output; TalkBack speech was not tested.

`PaymentMethodJsonParser` retains both correct fields. `DisplayableSavedPaymentMethod.brandDisplayName()` falls back to card.brand only for null displayBrand. `CardBrand.fromCode("american_express")` returns non-null Unknown because its Amex code is `amex`; the row formats that name into its accessibility description. Icon and brand-filter code already discard Unknown before falling back, explaining why those controls work. One root report covers this description-fallback cause.

[LpmNativeCardDescriptionBashTest.kt](LpmNativeCardDescriptionBashTest.kt) retains the minimized real response fields through the production parser and description formatter, with synthetic IDs. The actual final run had **8 passing controls and 1 expected-failing Amex regression**, Gradle exit 1 in 28s: [results](native/jvm-description-results.json). Expected American Express ending in 8431, actual Unknown ending in 8431. The captured Visa control and seven existing DisplayableSavedPaymentMethod tests passed. Existing `getDescription_usesBrandIfDisplayBrandIsUnknown` covers null, not an unrecognized non-null string.

The first minimized attempt omitted the parser's required ID: both new cases failed setup while seven existing tests passed. Adding synthetic IDs corrected the test harness; no card fields or expected behavior were changed. The repeated low-cost executor delivery failures were already established by previous topics, so the build-delegation skill's unavailable-executor fallback ran these exact scoped commands sequentially here. No broad suite was repeated.

To replay, copy the evidence test into `paymentsheet/src/test/java/com/stripe/android/paymentsheet/`, run the command below, then remove only that copied test. The Amex case is expected to fail until fixed.

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.paymentsheet.LpmNativeCardDescriptionBashTest' \
  --tests 'com.stripe.android.paymentsheet.DisplayableSavedPaymentMethodTest' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

[Duplicate searches](native/duplicate-searches.json) used `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all` for exact Amex/american_express/description/Unknown/displayBrand/accessibility/TalkBack terms. No matching issue was found; the few returned titles concern unrelated CVC, 3DS recreation and legacy savePaymentMethod errors. Both existing root billing and SEPA-save reports cover different causes and were preserved.

### Remaining gaps and handoff

This pass does not establish redirect failure/cancellation, interrupted authentication, polling timeout UI, activity/process recreation, rapid concurrency, every LPM's native form or country/currency combination, card-only native confirmation, new-card acceptance recovery, broader SFU/save matrix, guest/setup/subscription flows, off-session reuse, wallet/Link/Google Pay behavior, full accessibility speech/navigation, or independent settlement/webhook fulfillment. SEPA's completion poll was paid at observation time, but no native SEPA Intent body or settlement event was captured. Earlier processing/unpaid SEPA evidence is separate. Pending Bancontact before authorization was never called cancellation.

`python3 bug-bash/lpms/verify_native.py` passes without creating objects. It verifies six session inventories, local brand-filter results, default/all redisplay, native completion labels, both payment polls, the Bancontact requires_action→succeeded Intent identity/amount, actual Amex label evidence, JSON/XML validity, helper syntax and absence of key/client-secret/redirect-capability patterns. Test-mode objects remain. No fix, commit/amend, push, PR, remote issue, Slack or email was created.

The temporary test source has been removed after retaining the standalone evidence test. Production code was unchanged. Cleanup restores the fresh PaymentConfiguration and DefaultPrefsRepository backups byte-for-byte, removes originally absent Checkout preferences, stops/awaits the owned relay, removes its sole reverse mapping and owned device XML, and deletes private credentials/backups/temporary logs. The installed clean source and flags remain unchanged. The final cleanup/menu/unlocked verification is recorded immediately before release in [native/handoff.json](native/handoff.json).

Cleanup completed and device/build/backend access released at `2026-11-11T21:38:07.293573Z`. Fresh preferences match byte-for-byte; Checkout preferences/private state/scratch/logs are absent; relay stopped and awaited, port closed, reverse mappings empty. **The final menu assertion caught the device-admin idle timeout during documentation: keyguard showing=true/secure=true, screen OFF, Dozing, timeout reason.** MainActivity was launched by restore, but the final visible main menu was not verified because the lockscreen covered it. No PIN/bypass or device-policy change was attempted. The failed assertion was an environment-handoff check, not an LPM failure; no lockscreen screenshot or personal contents were retained. **Native LPM follow-up has released the environment. The final Order Summary topic needs the user to unlock the device again.**
