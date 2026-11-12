# Customer logic bug bash — 2026-11-11

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a` on `master`. Preserved pre-existing changes to `AGENTS.md`, earlier `bug-bash/` evidence, and the existing billing-address report and swap file. This pass uses `~/.codex/skills/bug-bash/SKILL.md` and its Android and tax/saved-card references.

No new confirmed bug. Backend coverage below does not establish native UI correctness. The physical Pixel 6a (Android 16/API 36) was available through adb but PIN locked; normal wake and keyguard dismissal showed `Enter PIN`, and `dumpsys window policy` confirmed `showing=true`, `secure=true`. The coordinator asked the user to unlock the device without disclosing the PIN. No PIN or bypass was attempted.

## Build and environment

Built `:paymentsheet-example:assembleBaseDebug` with `-PDIRECTLY_TO_PLAYGROUND=false -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`; successful in 1 second, all 363 actionable tasks up to date. Installed `paymentsheet-example/build/outputs/apk/base/debug/paymentsheet-example-base-debug.apk` successfully on the physical device. Debugger settings were `debug_app=null`, `wait_for_debugger=0` and were not changed.

Read `delegate-low-reasoning-work` and attempted a narrow Luna build executor. Its initial task and two follow-ups arrived empty or failed, so required Gradle work used the skill's unavailable-executor fallback, sequentially in the active topic agent.

Backend: `https://stp-mobile-playground-backend-v7.stripedemos.com`, merchant `US`, Checkout creation version `2026-08-26.preview`. Every provisioner verifies a `pk_test_` key. Test details use `example.com` emails and tokenized test cards. No real customer or wallet details were used.

## Verified backend controls

The six controls in [exploratory_probe.py](exploratory_probe.py), command `backend-controls`, created real Checkout Sessions and called `/v1/checkout_client/{id}/init` with the SDK client parameters. All six passed. [backend-controls.json](backend-controls.json) contains redacted server responses and per-case creation options. Shared configuration: `ui_mode=mobile_elements`, USD 120.00, card only, one item, exclusive tax behavior, automatic tax off, adaptive pricing disallowed.

| Case | Observed result |
| --- | --- |
| Guest, fixed `customer_email`, no customer or customer-creation setting | Open session, no customer object, expected fixed email, saving/removal disabled |
| Guest, `customer_creation=always`, saving/removal enabled | Open session, no customer object at init, expected fixed email, saving/removal enabled in response |
| Newly created explicit customer with email and no saved cards | Correct customer ID, empty saved inventory, fixed email supplied as top-level `customer_email`, save/remove enabled |
| Returning Alpha customer with saved Visa 4242 | Correct Alpha customer ID/email and exactly Alpha's saved card |
| Returning Beta customer with saved Mastercard 4444 | Correct Beta customer ID/email and exactly Beta's distinct saved card |
| Explicit customer without email | Correct customer ID, no fixed email |

These controls did not confirm payments or prove post-confirmation customer creation, email/address persistence, or payment attachment.

## Source investigation and rejected leads

- `CheckoutSessionFactory` explicitly supports guest, new, and returning customer modes. The playground persists a successfully configured new customer as returning through `saveReturningCustomer`; actual UI persistence remains untested in this pass.
- `CheckoutSessionParamsFactory` supplies `customer_email` for guests and `customer` for explicit customers; its `customer_update` wiring is enabled for selected automatic-tax collection settings. Tax/address behavior belongs to the automatic-tax pass.
- The probe initially assumed an explicit customer's email would appear under raw `customer.email`. Live init instead returned top-level `customer_email`. The SDK's parser and `fixedEmail` accessor support that representation; the corrected probe passed. This was a probe setup error, not an SDK finding.
- `CreateCustomerMetadata` and `CreateCustomerState` load the current Checkout response's customer and saved inventory. `CheckoutStateLoader.loadInitial` starts fresh selection carry-forward state; `publish` updates the customer state holder.
- The loader reads the guest preference slot for Checkout because its ordinary configuration has no customer access configuration. Before applying a stored payment-method ID it checks membership in the current customer's inventory. A shared preference slot alone therefore does not establish cross-customer disclosure or charging. Per-customer preference retention and wallet behavior were not reproduced; no report was filed.
- A guest session with `customer_creation=always` still had no customer object at init. The SDK's no-customer metadata path does not establish that native save-checkbox behavior is supported. This needs an explicit supported-contract/UI check in save-for-future-use coverage, not a speculative bug report.
- Checked root local issues and GitHub issues using `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'checkout customer'`. No matching new customer-logic issue was identified. Did not rerun or duplicate the known saved-card billing-tax editor bug.

## Validation and gaps

Focused existing JVM customer/session tests passed: **113 tests, zero failures/errors/skips**, Gradle exit 0 in 4m 30s. [jvm-validation.json](jvm-validation.json) records per-class counts. The SDK classes cover customer metadata, customer inventory filtering, state-holder replacement/clearing, fresh selection state, and local Link authentication clearing. The example classes cover customer provisioning/selection, session parameters, and playground settings persistence. Exact command:

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.paymentsheet.state.CreateCustomerMetadataTest' \
  --tests 'com.stripe.android.paymentsheet.state.CreateCustomerStateTest' \
  --tests 'com.stripe.android.checkout.CheckoutStateLoaderTest' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerClearLocalCustomerDataTest' \
  :paymentsheet-example:testBaseDebugUnitTest \
  --tests 'com.stripe.android.paymentsheet.example.playground.checkout.CheckoutSessionFactoryTest' \
  --tests 'com.stripe.android.paymentsheet.example.playground.checkout.CheckoutSessionParamsFactoryTest' \
  --tests 'com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettingsTest' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

UI coverage is currently absent because the device is locked. The prepared guest start did not reach the example menu. Guest/new/returning payment forms, new-to-returning app preference persistence, same-controller customer switching, confirmation, customer_update persistence, Link logout/session clearing on device, process restoration, and accessibility remain untested. No screenshots of customer behavior or recordings are cited.

## Environment handoff

Cleanup verified: the original playground preference file was absent and the temporary file was removed; the owned loopback relay on port 49942 was stopped; `adb reverse --list` was empty; `/sdcard/customer-logic-bash.xml` was removed. The private scratch directory, transient keys/client secrets, and temporary lock-screen screenshot were deleted. No recorder was used. All Gradle and helper processes finished, and the build executor is idle.

The physical Pixel remains PIN locked. The example main activity was launched beneath the lock screen, so its visible menu state was not verified. The installed debug APK remains at the source revision above, with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No SDK/app flags or device debugger settings were changed beyond that explicitly recorded debug build. A following topic should first check whether the owner has unlocked the device.

Only local files under `bug-bash/customer-logic/` are owned by this pass: this handoff, the standalone probe, redacted backend evidence, and the JVM validation summary. No SDK, app, or test source was modified; no commit, push, PR, remote issue, or message to others was created. Test-mode backend objects were retained.

## Native follow-up — 2026-11-11

**No new confirmed bug.** This follow-up closes the native guest/new/returning form, playground New→Returning persistence, representative card confirmation, and same-controller customer-inventory/selection gaps described above. It does not replace the earlier six backend controls or 113 JVM test results and does not claim the remaining gaps passed.

Reused the installed clean debug APK at `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `DIRECTLY_TO_PLAYGROUND=false`, `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No build, SDK/app source edit, test-source hook, or JVM rerun was needed. Device discovery reconfirmed physical Pixel 6a `REDACTED_DEVICE_SERIAL`, Android 16/API 36. All adb commands selected that serial. Read the bug-bash skill and Android/saved-card references, repository guidance, prior Confirm native handoff, and test skills before preparing the local helper.

The device had returned to a PIN-locked state before native testing. Normal wake/dismiss showed `Enter PIN`; no PIN or bypass was attempted. Read-only power evidence showed power connected, plug type 1, stay-awake setting 7, `mStayOn=false`, and an enforced device-admin maximum idle timeout of 300000 ms. Device policy was left unchanged. The owner unlocked during the first cleanup; a subsequent actual keyguard read showed `showing=false`, `SCREEN_STATE_ON`, and the native menu was observed. Fresh backups and a new relay were then created before proceeding. [Initial observation](native/initial-lock-observation.json) is scoped to that first locked preparation, not the final result.

[native_probe.py](native_probe.py) owns a private directory specified by `CUSTOMER_BASH_STATE`, an ephemeral loopback relay, and its adb reverse mapping. It imports the existing customer probe's backend/redaction functions. The initial Guest settings are injected after fresh backup; every subsequent customer change below uses the real native settings UI. The relay only adds `adaptive_pricing.enabled=false` to session creation and forwards the real backend response unchanged. SDK configuration, payment-method collection and confirmation go directly to Stripe. Active-session retrieve and completion poll observations are separate read-only backend checks; they do not intercept or replace SDK traffic.

Shared native configuration: backend `https://stp-mobile-playground-backend-v7.stripedemos.com`, merchant `US`, creation version `2026-08-26.preview`, USD 120.00, two default playground line items, card only, tax/shipping/full-billing collection off, Link and Google Pay hidden. Save/remove are enabled for explicit customers; guest configuration omits them. Test keys were checked for `pk_test_` before provisioning. Synthetic guest/first-customer email was `native-customer@example.com`; Beta used `customer-beta@example.com` with an attached test Mastercard 4444. All sessions in the following table ran without restarting the Checkout activity/controller.

| Native action | Observed native and backend result |
| --- | --- |
| Guest → Create Checkout Session → Select | No customer in raw response, fixed guest email, no selected method, empty card form and no save checkbox. |
| Native settings: choose New → Create Checkout Session | Real explicit customer with empty inventory. Successful configuration changed persisted `session.customer` to `returning`, with both customer ID fields matching the created customer. |
| New customer's card form: Visa 4242, expiry 12/30, CVC 123, US ZIP 94103, save checked → Continue → Confirm | Native `Payment complete!`; separate poll reported `status=complete`, `confirmation_status=succeeded`, `payment_status=paid`. |
| Back to settings → Create Checkout Session with the persisted Returning customer | Fresh response returned exactly the saved Visa with `allow_redisplay=always`; native UI displayed and selected that card and the original email. |
| Edit native Customer ID to Beta → Create Checkout Session | Same controller displayed/selected only Beta's Mastercard 4444 and Beta's email. Raw response contained Beta's exact ID and distinct method. No first-customer Visa remained. |
| Edit native Customer ID back to first customer → Create Checkout Session | Same controller displayed/selected only the first customer's original saved Visa 4242 and original email; raw method ID matched the prior fresh session. No Beta card remained. |
| Choose Guest → Create Checkout Session → Select | Raw customer null; native saved inventory and selection cleared. Card form was blank with no save checkbox. |

[Result summary](native/results.json) records exact customer/method IDs, native actions, and scope limits. All nine retained PNGs were opened and visually inspected; each has adjacent UIAutomator XML:

- [Guest form](native/guest-card-form.png), [new card with save checked](native/new-save-checked.png), [native completion](native/new-completed.png), [native Returning settings](native/new-now-returning-settings.png).
- [Fresh first customer selected](native/returning-new-selected.png), [Beta selected](native/beta-selected.png), [first customer selected after switch back](native/returning-back-selected.png).
- [Guest inventory/selection cleared](native/guest-after-customers-cleared.png), [blank Guest form after customer switching](native/guest-after-customers-form.png).

Redacted creation requests and actual retrieved responses are retained under `native/`. Persistence evidence is in [before](native/new-before-configure.json) and [after configuration](native/new-after-configure.json), and completion evidence in [poll](native/new-poll.json). The initial helper poll incorrectly included retrieve-only locale/mobile fields and received `parameter_unknown`; the helper was corrected to the poll endpoint's client-secret-only parameters. This was a probe error, not a product finding. The native payment had already completed before that read-only probe error.

The observed personal autofill offer was dismissed using `No thanks`, and Google's offer to save the synthetic card was dismissed using `Not now`. No personal wallet contents or screenshot were retained, and no card was saved to Google. No recording was necessary for these successful controls. A scan of retained native JSON found no API keys or client secrets. Evidence consistency checks verified exact New→Returning ID persistence, distinct first/Beta inventories, identical first-customer method after switching back, null Guest customer, and complete/succeeded/paid polling.

Rechecked both local root reports and `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'checkout customer'`; no new issue was filed. The earlier theoretical shared preference-slot lead did not reproduce as cross-customer inventory/selection leakage in this native sequence. Only one saved method per customer was used, so per-customer retention of a choice among multiple saved cards remains untested. The known billing-tax saved-card editor and Confirm SEPA-save issues were not rerun or duplicated.

Remaining native gaps: `customer_update` email/address persistence, customer creation at guest confirmation, Link logout/session clearing, wallet state, multiple-card preference retention, authentication/3DS, process restoration, accessibility, and independent Intent/fulfillment/webhook inspection. No native Intent response body was captured. The completion claim is the actual native result plus Checkout poll; completed Checkout client retrieve is unavailable. Guest and Beta payments were not confirmed; the representative payment was the newly created first customer.

Cleanup restored the fresh `PaymentConfiguration` and `DefaultPrefsRepository` backups byte-for-byte and removed Checkout preferences because they were absent at fresh backup. The owned relay has stopped and its mapping/XML have been removed. No SDK/app/test source or existing report was changed. Only this appended handoff, the new helper, and native evidence are retained, uncommitted. Test-mode backend fixtures remain. Final device/private-state verification is recorded below.

Final verification: fresh preference backups match exactly; Checkout preferences absent; reverse mappings empty; owned relay port closed; private credentials/backups and owned device XML removed; no recorder or native test execution remains. Final actual main-menu XML contains `CheckoutElementsController Example`; keyguard `showing=false`, screen `SCREEN_STATE_ON`, stay-awake setting `7`. Installed revision/flags are unchanged. [Cleanup evidence](native/cleanup.json).

**Native customer follow-up has released device/build/backend access. The coordinator may start Adaptive Pricing next.**
