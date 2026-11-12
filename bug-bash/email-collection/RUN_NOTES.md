# Email collection bug bash — 2026-11-11

No confirmed SDK bug. Six real backend confirmations succeeded, and a fresh Checkout Session for each resulting customer exposed the expected persisted email. Missing, empty, whitespace-only, and malformed collected email controls were rejected without completing the session. Native UI was not exercised: the physical Pixel remained PIN locked at the initial and final checks (`showing=true`, `secure=true`, screen off). No PIN, bypass, emulator, or connected-device test was used.

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `master`, `~/stripe/stripe-android-2`. Used the personal bug-bash skill and Android reference, plus unit/network testing and build-delegation skills. Preserved the user's modified `AGENTS.md`, existing billing-address issue/swap file, and all prior topic evidence.

## Environment and configuration

Physical Pixel 6a, Android 16/API 36. Reused the same-source debug APK built/installed by the customer pass, with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. This topic made no production changes and did not rebuild, reinstall, launch, or change app/device settings.

Backend: `https://stp-mobile-playground-backend-v7.stripedemos.com`, merchant `US`; Checkout creation API version `2026-08-26.preview`. All provisioning first asserted a `pk_test_` key. Shared creation parameters: `ui_mode=mobile_elements`, USD $120, one one-time item, card only, automatic tax off, adaptive pricing disabled. All names, email addresses, customers, and tokenized Visa PaymentMethods were synthetic. Keys and client secrets remained in process memory and were redacted from retained responses.

## Executed live coverage

Run [live_probe.py](live_probe.py) with:

```sh
python3 bug-bash/email-collection/live_probe.py
```

The probe uses the SDK's Checkout client init/retrieve/confirm request shapes, including mobile client parameters. This is direct backend coverage, not native SDK confirmation orchestration. [backend-results.json](backend-results.json) records ten results: four rejected inputs and six completed payments. Creation, confirmation requests, raw responses, retrieved PaymentIntents, and new returning-customer init responses are adjacent JSON files.

| Case | Observed result |
| --- | --- |
| Guest, no fixed or card billing email; omit collected email | `checkout_confirm_error` with reason `checkout_email_missing`; independent retrieve remains open/unpaid |
| Same session, collected email `""` | `email_invalid`, parameter `collected_information[email]`; remains open/unpaid |
| Same session, collected email containing three spaces | `email_invalid`; remains open/unpaid |
| Same session, collected email `not-an-email` | `email_invalid`; remains open/unpaid |
| Correct that session to `email-collection+recovered@example.com` | Complete/paid; PaymentIntent succeeded for $120; new session for resulting customer has that fixed email |
| Guest with fixed `customer_email=fixed-session@example.com`, conflicting card billing email, omit collected email | Complete/paid; resulting customer's next session has fixed-session email |
| Guest with `customer_creation=always`, collected `Email.Collection+Created@example.com`, conflicting card billing email | Complete/paid; next session preserves collected email, plus tag, and case |
| Guest without fixed or collected email; card billing email `card-billing@example.com` | Backend accepts the card billing fallback; next session shows that email |
| Explicit customer with `fixed-customer@example.com`, conflicting card billing email, omit collected email | Complete/paid; next session retains the original customer email |
| Explicit customer initially without email; collect `email-collection+customer@example.com`, conflicting card billing email | Complete/paid; next session for the same customer exposes the collected email |

All six final PaymentIntents separately retrieved as `succeeded`, amount 12000 USD. The post-confirmation customer IDs came from confirm responses. A fresh session created with each ID returned that same customer and the expected top-level `customer_email`, establishing customer persistence through an observable supported backend path. This does not assert receipt delivery, card billing email mutation, or completed-session fulfillment fields.

## SDK boundary validation

Existing focused tests: **40 passed, zero failures/errors/skips**, Gradle exit 0 in 19s. [jvm-existing-results.json](jvm-existing-results.json) records per-class counts. Coverage includes configured email defaults, local updates and clearing, fixed email precedence, public-session mapping, serialization alongside save choice, and confirmation inclusion/omission for new/saved methods and setup/payment mode.

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.*email*' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.*Email*' \
  --tests 'com.stripe.android.checkout.CheckoutCommonConfigurationFactoryTest.*email*' \
  --tests 'com.stripe.android.checkout.CheckoutSessionMappersTest.*email*' \
  --tests 'com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseJsonParserTest.*email*' \
  --tests 'com.stripe.android.paymentsheet.repositories.ConfirmCheckoutSessionParamsTest' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.*email*' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

Five additional named captured-response tests **passed, zero failures/errors/skips**, Gradle exit 0 in 19s; [jvm-replay-results.json](jvm-replay-results.json) records their names and outcome. They are preserved as an evidence-only patch, [email-boundary-tests.patch](email-boundary-tests.patch). Three configure the actual public controller through its Dagger graph and mocked network init: replace/clear a configured guest prefill and observe session Flow plus confirmation metadata; preserve invalid input in local state then correct it; and retain a returning customer's fixed email after replacement and clearing attempts. Two replay the captured missing/invalid-email errors through the real confirmation repository/interceptor and assert transaction failures reach the caller. These are JVM network replays, not a live SDK or native UI end-to-end run.

The controller replay keeps captured email/customer data and wallet eligibility. It disables Link through public configuration and removes only `experiments_data` from the replay response to keep the tests scoped to email state. Three initial attempts each had three passes and two test-harness failures from unmocked consumer lookups. Inspection traced these to `DefaultLogLinkHoldbackExperiment`, which performs exposure lookups when experiment data and email are present, including when Link display is disabled. Disabling display or removing wallet eligibility alone did not remove that separate experiment lookup; those attempted response edits are absent from the final patch. These failures did not show incorrect email behavior, and no Link/privacy finding was investigated or reported.

To reproduce the final replay, apply the patch to this revision, run the command below, then reverse only that patch:

```sh
git apply bug-bash/email-collection/email-boundary-tests.patch
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.email bash*' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.email bash*' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
git apply -R bug-bash/email-collection/email-boundary-tests.patch
```

The Luna executor received empty initial and follow-up task payloads and performed no work. It was stopped. The build-delegation skill's unavailable-executor fallback ran exact Gradle commands sequentially in this topic agent.

## Source path, rejected leads, and gaps

- The example's `SessionOperations` custom email input invokes `CheckoutElementsControllerExampleViewModel.updateEmail`, which trims input and converts empty strings to null. The SDK's public `updateEmail` stores the supplied value locally without email validation or a Checkout update request. `CheckoutStateLoader`, the public session mapper, and embedded configuration propagate it into collected email/default billing details. A successful local operation is not a claim that the backend accepted the email.
- `CheckoutSessionResponse.fixedEmail` is `customerEmail ?: customer.email`. Public session/default billing mappings prefer that fixed email. `CheckoutSessionConfirmationInterceptor` sends local email as `collected_information[email]` only if fixed email is absent. Fixed email overriding local replacement/clear is covered by existing intentional-contract tests and the live controls; no report was warranted.
- The first direct missing-email probe accidentally gave the card a valid billing email and completed successfully. The corrected probe removed billing email for validation and tested billing fallback separately. The obsolete success file was removed; the final evidence reflects the corrected cases. This was a probe assumption, not an SDK defect.
- Rechecked the root billing report and searched GitHub with `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'checkout email'` and `'CheckoutElementsController email'`. The former returned unrelated older issues; the latter returned none. No `ISSUE_email_collection_*.md` was justified.
- Native example input rendering, focus/keyboard/autofill behavior, interactive error messages, native selection/confirmation orchestration, process death, accessibility, and real UI persistence remain untested because the Pixel stayed locked. No screenshot or recording is cited.
- No claim for Link, Google Pay, ECE, LPM-specific email collection, 3DS, receipt delivery, or combined tax/shipping behavior. Unsupported preview flows and invalid integration setup were excluded. Existing shared `EmailConfig` implementation was inspected, but its form validation is not the public controller's local `updateEmail` contract and was not counted as this topic's UI coverage.

## Cleanup and handoff

The temporary edits to `CheckoutElementsControllerTest.kt` and `CheckoutSessionConfirmationInterceptorTest.kt` were saved in the evidence patch and reversed. No production or test source changes remain. All Gradle/helper executions have finished, and the unused executor is stopped. Temporary Gradle logs were removed after preserving summaries.

No relay, recorder, private credential file, device scratch file, preference replacement, or adb mapping was created in this pass. Final `adb reverse --list` is empty. The original playground preference file was absent and remains absent. The device remains locked; installed source/build flags are unchanged from the environment description above. No debugger/device settings changed. Retained JSON was parsed and scanned for real keys/client secrets; the Python probe syntax was checked.

Only local notes, the probe, sanitized backend evidence, result summaries, and the evidence-only test patch remain under `bug-bash/email-collection/`. Test-mode backend objects remain. No commit/amend, push, PR, remote issue, Slack, or email was created.

**Email collection has released the device/build/backend environment to the coordinator for the next sequential topic.**

## Native follow-up after device unlock — 2026-11-11

**No new confirmed SDK bug.** Two native payments completed, two invalid/missing collected-email confirmations were rejected, and fresh native sessions for both customers exposed the expected persisted email. This closes representative native prefill, fixed-email precedence, clearing, malformed-input rejection/recovery and customer-email persistence gaps from the initial pass. The earlier six direct-backend payments and 45 JVM tests remain historical evidence and were not rerun.

Used the bug-bash skill and Android reference. Source remains `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`; physical Pixel 6a `REDACTED_DEVICE_SERIAL`, Android 16/API 36. Reused the unchanged installed APK with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No app/SDK hooks, production source edits, test edits, build or install were needed. This was exclusive sequential device/build/backend ownership after Automatic Tax. The first inspection found the device already asleep and securely locked; testing began after a fresh user unlock. No PIN, lock bypass, emulator or connected-device test was used.

[Native helper](native_probe.py) reuses the customer helper's transport and provides a **logging-only** loopback relay. It forwards creation requests and actual backend responses unchanged; SDK confirmation uses its normal Stripe connection. Set a fresh private `EMAIL_NATIVE_STATE` and explicit physical `ANDROID_SERIAL`, run `serve`, then `prepare fixed`, `prepare fresh-fixed`, `prepare prefill`, or `prepare fresh-prefill`; operate with observed `tree`/`tap`/`enter`/`shot`/`snapshot`/`poll`, finish with `restore`, and stop/await the relay. Only supported playground settings are supplied. Credentials and full preference backups remain in owned private temporary state until cleanup.

Both fixtures are newly provisioned test-mode US customers with saved synthetic Visa 4242, `allow_redisplay=always`, US/94103 billing. The helper asserts a `pk_test_` key before provisioning. Checkout requests use `2026-08-26.preview`, `mobile_elements`, USD, card only, no automatic tax or shipping, and saving/removal disabled. The normal cart is two $35 shirts plus one $50 hoodie. [Fixed creation](native/fixed-creation.json), [collected creation](native/prefill-creation.json), and [result summary](native/native-results.json) record the configuration. No adaptive-pricing override was injected.

| Native action | Actual result and evidence |
| --- | --- |
| Fixed customer email `native-fixed@example.com`; conflicting controller default `native-prefill@example.com` and saved-card billing email `conflicting-card@example.com` | Native email input initializes to the fixed customer value. [UI](native/fixed-initial.png), [raw active session](native/fixed-initial.json), [fixture](native/fixed-fixture.json). |
| Replace the example input with `native-attempt@example.com`, press Update email, then clear the input and Update email | Example-owned input retains the entered/empty text and displays `Email updated`. The backend session still has `native-fixed@example.com` after clearing. [Replacement UI](native/fixed-after-update.png), [cleared UI](native/fixed-after-clear.png), [raw active session](native/fixed-after-clear.json). |
| Confirm fixed-email case with the input cleared | **Payment complete!**; independent Checkout poll reports `complete`, `succeeded`, `paid`, `livemode=false`. [UI](native/fixed-completed.png), [poll](native/fixed-completed-poll.json). |
| Create fresh native session for the same fixed-email customer, with no controller email default | Original fixed email renders again and raw session agrees. [UI](native/fresh-fixed.png), [raw session](native/fresh-fixed.json). |
| Customer and saved card both have no email; configure controller default `native-prefill@example.com` | Native input shows the prefill while raw session has no fixed `customer_email`. [UI](native/prefill-initial.png), [raw active session](native/prefill-initial.json), [fixture](native/prefill-fixture.json). |
| Clear the prefill, press Update email, then Confirm with the selected saved Visa | Native error: `Confirmation failed: An error has occurred confirming the Checkout Session.` Same session remains open/unpaid; selected card remains available. [UI](native/prefill-missing-rejected.png), [independent retrieve](native/prefill-missing-rejected.json). |
| Enter `not-an-email`, Update email, Confirm | Native error: `Confirmation failed: Invalid email address: not-an-email`. Same session remains open/unpaid. [UI](native/prefill-invalid-rejected.png), [independent retrieve](native/prefill-invalid-rejected.json). |
| Replace with `Native.Email+Recovered@example.com`, Update email, Confirm | **Payment complete!** with the same selected saved card; independent poll reports complete/succeeded/paid in test mode. [Corrected input](native/prefill-corrected-input.png), [completion](native/prefill-completed.png), [poll](native/prefill-completed-poll.json). |
| Create fresh native session for that same customer, omitting controller email defaults | Exact recovered email renders, including case and plus tag. The fresh backend session has the same customer ID and top-level `customer_email=Native.Email+Recovered@example.com`. [UI](native/fresh-prefill.png), [raw session](native/fresh-prefill.json), [creation](native/fresh-prefill-creation.json). |

The example's Session controls input is merchant-owned UI backed by `rememberSaveable(initialEmail)`; it is not itself a readout of every effective SDK state change. `Email updated` acknowledges the local operation, not backend email validation. The existing intentional contracts above establish fixed-email precedence and local update semantics. This run verifies successful payment and fresh-session outcomes rather than treating the retained example input or confirm-time validation as an SDK defect. The missing-email message is generic and the malformed-email message specific; both allow recovery. No incorrect completed payment or lost customer email was observed. Both root reports were rechecked; neither root cause applies. The initial pass's GitHub searches remain inherited duplicate-search evidence; no new issue report was warranted.

All 16 retained screenshots were opened and visually inspected; matching XML is adjacent. An early keyboard-transition tap hit a personal autofill suggestion instead of the intended app control. The unintended value was immediately cleared, its screenshot/XML discarded, and the helper was strengthened to reject captures with non-test email. That interruption did not confirm a payment. The empty input and keyboard dismissal were separately observed before the successful fixed-email clear/update/confirm sequence. No personal value is retained in topic artifacts; subsequent keyboard transitions were observed separately. No personal wallet card was selected or saved. One automatic permission review timed out before fresh-session preparation; its explicitly allowed retry succeeded. These were probe interruptions, not product findings.

Coverage remains focused on the merchant-owned native email input with saved cards. New-card form email collection, LPM-specific forms, Link, Google Pay/ECE, whitespace normalization in native UI, receipt delivery, accessibility, process death and concurrent updates were not newly exercised. The initial direct-backend/JVM coverage and other topics' native SEPA/customer observations remain separate inherited evidence. No raw native confirmation request or response body was intercepted. Poll verifies completion/payment status, and fresh-session retrieval verifies persisted customer email, but this native follow-up does not independently verify PaymentIntent amounts, receipt delivery, or card billing email mutation.

Retained JSON/XML and helper syntax parse; assertions verify both successful polls, both rejected sessions remain open/unpaid, identical customer IDs across fresh sessions, and exact expected emails. Text was scanned for actual keys/client secrets and non-test email with no matches. No source changes or JVM reruns were needed. Retained changes are this helper, `native/` evidence and these appended notes; this follow-up did not modify the existing user `AGENTS.md`, either root issue, the billing swap file or earlier evidence. No commit/amend, push, PR, remote issue, Slack or email was created. Synthetic test backend objects remain.

Fresh backup found Checkout preferences absent and PaymentConfiguration/DefaultPrefsRepository present. Cleanup restores those two files byte-for-byte, removes the newly created Checkout preferences, stops and awaits the owned relay, removes only its adb reverse mapping and `/sdcard/email-native-ui.xml` plus `/sdcard/email_followup_initial.xml`, and deletes owned private state. No recorder, debugger or device settings were changed. The inherited device-admin five-minute idle lock policy and `stay_on_while_plugged_in=7` remain unchanged. Final verified cleanup, actual screen/keyguard state, main menu and environment release are recorded in [native handoff](native/handoff.json).

Cleanup completed and the device/build/backend environment was released at `2026-11-11T21:12:59Z`: actual main menu verified, keyguard `showing=false`, screen ON, stay-awake `7`, reverse mappings empty. Both present preference files were restored byte-for-byte; Checkout preferences, owned scratch files and private state were removed, and the relay was stopped and awaited. LPMs is the next sequential native topic.
