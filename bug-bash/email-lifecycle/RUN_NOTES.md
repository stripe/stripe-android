# Email, selection, and lifecycle bug bash — 2026-11-12

## Outcome and environment

One confirmed SDK issue: [changing checkout email clears selected card](../../ISSUE_email_lifecycle_update_email_clears_selected_card.md). This resolves Foundation's unconfirmed lead. The trigger works before any failed confirmation and after awaiting update completion. The generic Embedded chooser deliberately invalidates selection on changed billing defaults; Checkout maps the separate checkout email into those defaults, so this report concerns the Checkout integration boundary.

- SDK HEAD `8cbb695eabcafa957a86c24e1f025643979a500a`. No SDK fixes, staged files, commits, pushes, remote issues, or external messages. Two temporary reproduction tests were archived as a patch and removed from tracked source after execution.
- Physical Pixel6a `2B211JEGR05252`, API36. No emulator or connected test task used. Parent owned wake/UNKNOWN keepalive; this agent exclusively owned meaningful native UI.
- Reused installed baseDebug with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No build/install in this topic. Parent ran exact local JVM checks because Luna/Sol execution task delivery was unavailable, following the read build-delegation skill fallback.
- Read bug-bash skill/Android reference, root AGENTS.md, shared brief/index/email docs excerpts, Foundation lead, Express and shipping handoffs, and existing root reports. Invoked unit/network skills before temporary test code. No browser used.
- Source-verified docs: [email/lifecycle excerpts](../docs-driven/EMAIL_LIFECYCLE_DOCS_EXCERPTS.md). Email Markdoc revision `bc0cb468f4869ecbd66ef6f996ff3851a80b48ce`; contract requires valid checkout email before confirm and distinguishes it from payment-method billing email. Fixed-email error-doc mismatch and obsolete API snippets were already reported; not duplicated.
- GitHub issue searches used `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all`. Query `checkout email` returned unrelated older reports; narrowed `checkout selected email` returned [no results](github-duplicate-search.json). Local duplicates checked.
- Owned loopback relay [backend_probe.py](backend_probe.py), port18769, forwards only publishable_key/create_checkout_session and asserts test prefixes. Guest only. Raw keys/secrets kept privately in `/tmp/checkout-email-lifecycle`; sanitized probes retain Session hashes. Backend creation version2026-08-26.preview; checkout-client probe version2020-03-02 and required SDK client parameters.

## Runtime configurations and evidence

Both scenarios: guest, no server customer_email, no Customer object, US merchant, card-only allowed type, save false, no tax/shipping/discount/adaptive pricing; Payment Element configured; Express, Google Pay, Link disabled; USD120 cart. Card expiry12/30, CVC123, billing US94103. No personal wallet data was entered or retained.

### Session A: no email default, 4242 — hash6d6c6048c89a

Configuration [mutable-email.json](mutable-email.json).

| Action | Actual result |
| --- | --- |
| Select4242424242424242; Continue | Card selected in inline row and footer. No confirmation had occurred. |
| Enter before.confirm.email@example.com; Update email; await “Email updated” | Card row and footer lose selection. [UI](valid-update-before-confirm.png), [hierarchy](valid-update-before-confirm-ui.jsonl). Completion message is assigned only after the suspend API returns. |
| Confirm without reselecting | “Confirmation failed: Something went wrong” and no payment selected. [UI](confirm-after-update.png). Same Session remains [open/unpaid/not_started](backend-after-update-confirm.json), matching [initial hash](backend-before-update.json). |
| Select again | All original card fields retained: [form hierarchy](reopened-retained-card-ui.jsonl). Continue restores selection. |
| Update same unchanged email | Selection stays Visa4242: [UI](unchanged-update-selection.png). The old failure message is still visible from the prior attempt; this is not a new failure. |
| Confirm | [Payment complete](reselect-payment-complete.png). Independent [terminal poll](backend-reselect-control.json) shows same Session complete/paid/succeeded, livemode=false. Retrieve rejects inactive Session, as expected. |

### Session B: defaults.email, 3155 — hash0009cf48f83a

Configuration [default-email.json](default-email.json), controller defaults.email=default.email.bugbash@example.com. No server email.

| Action | Actual result |
| --- | --- |
| Initialize and select4000002500003155 | Default populated checkout Email field; selected3155 visible. [UI](default-email-selected.png), [initial backend](backend-default-initial.json). |
| Rotate portrait→landscape→portrait | Card retained. [Landscape](landscape.png), [portrait hierarchy](portrait-after-rotation-ui.jsonl). Framework [wm_relaunch_resume_activity event](rotation-events.txt) proves Activity recreation, not just layout resizing. Email preserved on return to portrait. |
| Home; am kill only example app; resume original task | PID14245 became absent, new PID14933. Explicit launcher intent created an extra MainActivity above the preserved Checkout Activity; inspected task3531, then Back popped only that extra Activity. Original Checkout Activity record25962652 restored with3155 and email: [UI](process-restored-checkout.png), [process evidence](process-recreation.json). Intermediate main menu [screenshot](process-launcher-menu.png) is navigation evidence, not SDK state loss. |
| Verify Session identity after process recreation | Original relay credentials retained; [before](backend-default-initial.json) and [after](backend-after-process-recreation.json) hash0009cf48f83a. Relay had exactly two create calls total (A and B), none during recreation. Later terminal payment on the original hash completes the same-Session proof. |
| Clear email; Update email | “Email updated,” field empty, selection cleared by the reported bug: [hierarchy](cleared-email-ui.jsonl). Reopened card fields retained: [hierarchy](cleared-email-retained-form-ui.jsonl). |
| Continue retained card; Confirm with empty email | Expected required-email confirmation failure. Visa3155 stays selected, [UI](missing-email-selected-card.png); [backend](backend-missing-email-confirm.json) stays open/unpaid/not_started. This failure alone does not clear selection. |
| Enter after.missing.email@example.com; await Update email completion | Email succeeds and selected3155 disappears: [UI](valid-update-after-missing.png). This reproduces Foundation's exact ordering, now with explicit boundaries. |
| Update not-an-email; reselect retained card; Confirm | Update itself says “Email updated”; confirmation rejects with “Invalid email address: not-an-email,” retains3155. [UI](invalid-email-confirm.png), [backend](backend-invalid-email-confirm.json) open/unpaid/not_started. Validation occurs at confirmation in this observed case. |
| Correct email to final.lifecycle.email@example.com; reselect card; Confirm | Reaches fully loaded real “3D Secure 2 Test Page.” This uses the restored original Session. |
| Explicit CANCEL on loaded challenge | SDK “Confirmation canceled,”3155 retained: [hierarchy](canceled-after-process-ui.jsonl). [Backend](backend-canceled-after-process.json) open/unpaid, confirmation_status=failed. Cancellation is an explicit user action, not inferred from status/timeout. |
| Rotate after cancellation | Card and message persist. Process-filtered [callback counts](callback-counts.json) retain exactly1 Canceled before/after rotation; no callback replay observed. [Restored portrait hierarchy](canceled-restored-portrait-ui.jsonl). |
| Confirm again; COMPLETE loaded challenge | [SDK Payment complete](restored-session-payment.png), [settled hierarchy](restored-session-payment-ui.jsonl). Independent [terminal poll](backend-restored-session-paid.json) proves original hash0009cf48f83a complete/paid/succeeded. Same original Session identity is verified across process recreation. |

## Focused regression and logout contract

[Regression patch](selected-card-email-regression.patch) adds two named tests to existing CheckoutElementsControllerTest, using its production Dagger graph and real factories, loader and DefaultEmbeddedSelectionChooser. NetworkRule supplies only initial Session fixture; its helper seeds a prior new-card selection through the real state holder. Assertions use public Session identity/email/paymentOption plus actual card-selection retention; no fake chooser or fabricated null result.

Command: `./gradlew :paymentsheet:testDebugUnitTest --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.email bugbash*'`.

- `email bugbash changing checkout email preserves selected card`: **expected failure**, public paymentOption is null after successful update.
- `email bugbash unchanged checkout email preserves selected card`: **passes**.
- Final [results](regression-results.txt)/[XML](regression-results.xml):2 tests,1 expected failure,0 errors/skips; exit1/18s. This is reproduction evidence, not a passing regression suite or a fix.
- Initial test draft used whole display-object equality; the unchanged control failed on fresh image-provider lambda identity despite retained card. Corrected both assertions to check public non-null state plus original actual PaymentSelection. Not an SDK finding. Only final corrected results retained.
- Temporary source patch was archived and exactly its two added tests removed; no production changes.

Logout doc/source contract was checked: `CheckoutElementsController.clearLocalCustomerData(context)` calls `DefaultLinkStore.clear()`. No Checkout example runtime control invokes it. Parent ran existing `CheckoutElementsControllerClearLocalCustomerDataTest`: [1 pass,0 failures/errors/skips](clear-local-data-results.txt), [XML](clear-local-data-results.xml); exit0/14s. This establishes persisted Link-state clearing in the existing focused unit test; no physical app logout/login matrix claimed. Express already checked synthetic Link sign-out.

## Reachability, rejected leads, and limits

- updateEmail→withCheckoutState→CheckoutStateLoader reload preserves the previous selection as input. `CheckoutConfigurationMappers.toBillingDetails` maps collected checkout email into defaultBillingDetails.email. `DefaultEmbeddedSelectionChooser.shouldUsePreviousSelection` rejects when `CommonConfiguration.containsVolatileDifferences` sees these changed defaults. No replacement selection exists for this guest, so published selection is null. Generic Embedded invalidation policy is deliberate; the reachable Checkout-email consequence is the bug.
- This is not a failed-confirm cleanup or timing issue: fresh4242 reproduces before any confirm; completed-update UI is explicit; production-graph regression also awaits the suspend call. A same-value update preserves selection both on device and in the focused control. Missing-email confirmation alone retains3155.
- No generic update-reset claim: shipping specialist already verified shipping US→CA retained4242. Only changed email covered here.
- Process restoration is established with PID disappearance/new PID, retained original task/Activity, no new create request, and same original Session backend probe. Launching the extra MainActivity was an automation navigation artifact, corrected by returning to the preserved task.
- Invalid-email update's success is not separately reported: the observed confirmation validation enforces the documented valid-email precondition. The docs' “handle validation errors” text does not explicitly guarantee update-time validation.
- Checkout/PM billing-email separation is source-traced; exact outbound PM.billing_details.email vs checkout collected email was not independently captured. The default-email card form contains no email input, so no separate PM billing-email edit was tested. Backend probes omit email to avoid accidental private data; they prove Session identity/status/totals, not final persisted email fields.
- Server email immutability/Customer.email was not rerun; existing foundation report covers fixed server email. No arbitrary email syntax matrix, simultaneous mutations, unawaited confirm race, force-stop restoration expectation, OS eviction during a network call or 3DS, rotation during form typing, changing integration identifiers, or multiple controllers tested. Card security behavior across other storage/lifecycle boundaries not inferred.
- Screenshots retained are synthetic app UI only, all opened and visually inspected. No video recorded. No webhook/fulfillment assertion.

## Final callback and cleanup handoff

- Final SDK callback counts show Canceled once and Completed once in process14933, with no Canceled replay after rotation. This counts the example's actual result callback logs, not just persistent display text. The result UI is a retained message, so showing it after rotation is expected.
- An unrelated notification permission prompt appeared before the retry. Label-based automation safely refused the missing Confirm target; Back dismissed the observed prompt without granting/denying permissions. Re-observed checkout before retrying. No permissions intentionally changed.
- Restored the entire original playground preferences byte-for-byte, verified comparison, and force-stopped app. Restored original accelerometer_rotation=0/user_rotation=0. [Cleanup evidence](cleanup.json).
- Removed only owned adb reverse18769, stopped owned backend relay, removed `/sdcard/checkout-email-lifecycle.xml`, and deleted private credentials/preferences/rotation backup after verification. No recorder started. Parent keepalive remains untouched; physical device released.
- Installed build remains baseDebug, DIRECTLY_TO_PLAYGROUND=false, STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true. Tracked diff empty. Both synthetic Sessions remain legitimate paid test objects. No external work item filed.
- Helpers are local exploratory tooling. Reuse requires a private preferences backup at `/tmp/checkout-email-lifecycle/original-settings.xml`; `device.py settings SCENARIO` uses only explicit values plus source defaults. `backend_probe.py serve` binds127.0.0.1:18769; add its reverse mapping before creating a Session. Every new Session replaces latest private credentials. Remove the mapping, private files and relay after use.
