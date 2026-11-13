# Migration from Payment Intents — docs-driven bug bash

## Outcome

One new documentation contract issue: [payment_intent_data rejected](../../ISSUE_migration_payment_intent_data_rejected.md). Also extended the existing [Android API drift report](../../ISSUE_docs_foundation_android_guide_does_not_compile.md) with migration's compiler-proven paymentOptionDisplayData mismatch. No new SDK behavioral defect or fix.

## Environment and review

- SDK HEAD8cbb695eabcafa957a86c24e1f025643979a500a, tracked tree unchanged. Read root AGENTS, bug-bash skill and Android reference, shared brief, docs index/migration excerpts/backend routes, all six existing ISSUE reports, foundation/saved-methods/email-lifecycle handoffs. Read unit-test and build-delegation skills before compiler probes. No Gradle command, build, install, tracked test/source mutation or regression patch needed.
- Docs revision b4da179f9b139180a428c5a91a3d543c150a77c5, [contracts](../docs-driven/MIGRATION_DOCS_EXCERPTS.md). Android source already verified by parent. No browser used.
- Physical device assigned Pixel6a2B211JEGR05252/API36; no meaningful UI needed for these new migration gaps. Initial sandboxed adb devices could not access/start daemon (listener Operation not permitted); no device command was executed successfully and no emulator fallback used. Device identity/build below are inherited from the immediately prior appearance handoff, not newly queried.
- Installed build remains baseDebug, DIRECTLY_TO_PLAYGROUND=false, STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true. Prior app stopped/preferences and system settings restored; this pass made no device changes.

## Actual new checks

[Backend script](backend_contract_probe.py) creates one fresh synthetic-email test Customer and three otherwise identical creation requests through verified fixture routes. Publishable key pk_test_ and customer/session livemode=false asserted. Keys/IDs/secrets remain in process memory; output retains only hashes and safe fields. No relay, mapping or on-disk credential file.

Base: US merchant, version2026-08-26.preview, ui_mode=mobile_elements, no mode, valid existing Customer (synthetic email migration.bugbash@example.com), one USD1099 T-shirt through items/one_time_price, no optional shipping/tax/discounts/adaptive pricing.

| Difference | Result |
| --- | --- |
| No payment settings | HTTP200, mode none, mobile_elements, open/unpaid. |
| Only payment_intent_data.setup_future_usage=off_session | HTTP400: cannot pass payment_intent_data in modeless mode. |
| Only payment_settings.setup_future_usage=off_session | HTTP200, mode none, open/unpaid, response echoes off_session. |

[Sanitized exact requests/results](backend-contract-results.json); script exit0 means assertions of this observed failure and working controls passed. No payment attempted for these Sessions. Prior [saved-methods](../saved-methods/RUN_NOTES.md) confirms current-container off_session with real paid test payment and limited redisplay, so the comparison uses a known-supported parameter, not an invalid one. GitHub [duplicate search](github-duplicate-search.json), --state all, returned none; local root causes reviewed.

[Compiler probe](compile_docs_probe.py), command python3 bug-bash/migration/compile_docs_probe.py, uses cached Kotlin compiler plus built SDK classes.jar. ActualMigrationApi.kt paymentOption control exit0; DocumentedMigrationPaymentOption.kt paymentOptionDisplayData exit1/unresolved reference; overall probe exit0, [output](docs-compiler-results.txt). This isolates the doc predicate after correcting obsolete controller name. No passing compilation of the complete stale guide is claimed. Scratch compiler outputs removed after validation.

## Mapping review and reused coverage

- Actual CheckoutElementsController.Builder uses SavedStateHandle; configure returns OperationResult; createPresenter(activity) and destroy exist. Example ViewModel calls destroy onCleared, Activity creates presenter during onCreate and retains paymentElement; Content and present entrypoints exist. Result Completed/Canceled/Failed variants match migration's callback mapping once controller name is corrected.
- Actual Session exposes paymentOption, shippingAddress, totals, email and orderSummaryItems. Pay predicate must use paymentOption and condition shipping on configured collection. No new defect in those corrected mappings found by source review.
- Foundation exercised corrected setup, inline/sheet card selection, decline retry, cancellation/retry. Email/lifecycle subsequently independently proved backend-paid completion and same-Session identity across process recreation plus callback non-replay after rotation. Saved-methods independently proved supported current save parameter. Shipping topic owns optional/required shipping. These earlier results are reused, not claimed as rerun here.
- Migration's optional shipping and fixed-email recipe add no new runtime wiring versus completed foundation/shipping coverage. The new backend control exercises exact nested item shape/amount with valid Customer email and no mode; it does not separately re-provision a guest+shipping variant.
- Source-reviewed fulfillment guidance correctly distinguishes checkout.session.completed from delayed payment success, recommends signed webhooks/idempotence and creates new Sessions on expiry. No signed webhook receipt, fulfillment implementation, forced expiry or async failure notification was available or tested.

## Rejected leads, limits, and cleanup

- Controller name/Result<Unit> and new paymentOptionDisplayData discrepancy are one existing API-drift root cause, not another report. Prior fixed-email, discounts prerequisites, selection bugs and BLIK transport gap not reopened.
- No unsupported subscription/setup/OBO/manual/server-line-item/preview-excluded behavior tested. off_session is a payment-save setting in a one-time modeless payment, not setup mode.
- Legacy error is direct real API evidence; no inference that all legacy fields or merchants behave identically. No backend source fix or gating diagnosis required for the bounded documented-contract failure.
- No screenshots/video because no UI changes/interactions; no signed-webhook or new payment claims. No device prefs/system changes, relays, reverse mappings, recorder or scratch files created. Parent keepalive untouched; device released immediately.
- Removed /tmp/checkout-migration compiler scratch. New synthetic Customer and two open test Sessions remain backend test objects. No keys/client secrets persisted. No tracked changes, staging, commits, push, remote issues/messages or fixes. Parent performs final overall cleanup/report audit.

Parent subsequently verified the exact documentation sentence, modeless validator, and backend payment-settings parity test: [SOURCE_CONFIRMATION.md](SOURCE_CONFIRMATION.md).
