# Docs-driven Checkout Elements bug bash

Completed: all eleven sequential Astra investigations. Seven confirmed reports: two SDK defects, one client/server integration defect, and four documentation issues. A completed investigation does not imply every flow passed; prerequisites and coverage limits are recorded below and in each topic handoff.

SDK revision tested: `8cbb695eabcafa957a86c24e1f025643979a500a`. Initial working tree clean; tracked source and tests restored after temporary verification. The investigation produced reports and evidence without production fixes, remote issue filing, or messages to others. This branch packages those reports and evidence for GitHub review at the user's request; cleanup and working-tree statements below describe the investigation's completion before publication.

The [documentation inventory](DOCS_INDEX.md) covers both requested roots and all nine routes, including the Express subpage. Suspected documentation errors were checked against Android/common Markdoc source, because indexed content merges platform branches. Each topic ran with a fresh Astra investigator; device access was sequential.

| Completed topic | Main coverage and result | Detailed handoff |
| --- | --- | --- |
| Foundation | Corrected integration, cards, decline/retry, 3DS cancel/retry; Android API drift and fixed-email docs findings | [Foundation](../docs-foundation/RUN_NOTES.md) |
| Express Checkout | Shared and standalone Link paid; availability/configuration; expanded API-drift evidence | [Express](../express-checkout/RUN_NOTES.md) |
| Shipping | Save/edit/cancel, country restrictions, shipping-based tax refresh; no new confirmed defect | [Shipping](../shipping/RUN_NOTES.md) |
| Email/lifecycle | Mutable/default/fixed email, rotation, actual process restoration; email update clears selected card | [Email/lifecycle](../email-lifecycle/RUN_NOTES.md) |
| Saved methods | Optional/unconditional saving, always/limited redisplay, saved-card payment/removal; no new confirmed defect | [Saved methods](../saved-methods/RUN_NOTES.md) |
| Discounts | Real creation/apply/remove contract probes; documented flows blocked by backend prerequisites | [Discounts](../discounts/RUN_NOTES.md) |
| Tax | Billing-based exclusive/inclusive taxed payments; saved-card editor omits required tax fields | [Tax](../tax/RUN_NOTES.md) |
| Adaptive Pricing | EUR payment, JPY precision, currency round trips and availability boundaries; no new confirmed defect | [Adaptive Pricing](../adaptive-pricing/RUN_NOTES.md) |
| Other mobile methods | UnionPay, Bancontact, iDEAL, Pay by Bank controls/recovery; SEPA eventual paid and failed outcomes; BLIK code transport defect | [Payment methods](../standard-methods/RUN_NOTES.md) |
| Appearance | Palettes, errors, titles, forced themes, 1.3 font scale/landscape, currency labels/fonts; no new confirmed defect | [Appearance](../appearance/RUN_NOTES.md) |
| Migration | Identical-request creation comparisons and compiler probe; documented payment_intent_data container rejected | [Migration](../migration/RUN_NOTES.md) |

## Confirmed reports

1. SDK: [Updating mutable email clears the selected new card](../../ISSUE_email_lifecycle_update_email_clears_selected_card.md). Awaiting updateEmail does not prevent the reset; unchanged-email control preserves selection. Reselecting the retained card pays the same Session.
2. SDK: [Saved-card editor cannot collect automatic-tax address fields](../../ISSUE_tax_saved_card_edit_omits_tax_fields.md). Editing a France-only saved card to US/ZIP leaves required street/city/state unavailable. The backend retains the card, but the SDK filters it locally and confirmation becomes unavailable.
3. Client/server integration: [BLIK confirmation drops the entered code](../../ISSUE_standard_methods_blik_confirmation_drops_code.md). BLIK is offered and accepts six digits, but Android drops optionsParams and the inspected Checkout Client server schema/builder also lack a carrier. A client-only fix is not established. Same-Session Visa recovery paid.
4. Documentation: [Android guide API examples do not compile](../../ISSUE_docs_foundation_android_guide_does_not_compile.md). Consolidated controller, operation-result, availability, and Session-property drift across the guides.
5. Documentation: [Fixed-email override error contract is incorrect](../../ISSUE_docs_foundation_fixed_email_error_contract.md). Existing SDK behavior returns success and preserves the server-provided email.
6. Documentation: [Discount availability prerequisites are omitted](../../ISSUE_discounts_guide_omits_backend_availability_prerequisites.md). Creation requires a backend gate and Checkout Client update lacks the documented promotion-code protocol. This is a preview prerequisite gap, not an established SDK regression.
7. Documentation: [Migration's payment_intent_data recipe is rejected](../../ISSUE_migration_payment_intent_data_rejected.md). Plain creation succeeds, payment_intent_data.setup_future_usage fails, and identical payment_settings.setup_future_usage succeeds and is echoed by the server.

Each report contains Summary/Repro/Test/Elaboration, concrete evidence, and duplicate-search results. GitHub searches included closed issues via --state all.

## Validation evidence

Three archived regression groups each produced one expected failure and one passing control, with zero errors/skips. These are reproductions of defects, not passing regressions. Tracked test files were restored; all three patches passed git apply --check against the restored source:

- [Email patch](../email-lifecycle/selected-card-email-regression.patch), [results](../email-lifecycle/regression-results.txt).
- [Tax patch](../tax/saved-card-tax-fields-regression.patch), [results](../tax/regression-results.txt).
- [BLIK patch](../standard-methods/blik-confirmation-regression.patch), [results](../standard-methods/blik-regression-results.txt), [wire mismatch evidence](../standard-methods/blik-regression-wire-mismatch.txt). The mock HTTP500 is the assertion mechanism, separate from the real backend missing-code response. The report also links XML results.

Existing fixed-email and clearLocalCustomerData tests passed. Actual-API compiler controls passed and incompatible documentation probes failed as expected. Backend probes asserted both successful controls and observed rejection contracts. No whole-repository test-suite pass is claimed.

Initial build: ./gradlew :paymentsheet-example:assembleBaseDebug -PDIRECTLY_TO_PLAYGROUND=false, successful in 1m16s. Relay-enabled build added -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true, successful in 5s. Routine Luna and Sol executor delegation repeatedly received empty task payloads; parent ran the exact Gradle commands under the build-delegation skill fallback.

## Important limits and exclusions

- Google Pay completion/decline was not exercised because no identifiable synthetic test-card suite was available. No personal wallet submission or retained wallet data.
- Discounted checkout and discount restrictions remain untested beyond the documented prerequisite/protocol failures.
- BLIK success and bank failure simulations stop before authentication because the entered code is missing from confirmation.
- No signed-webhook verification or final PaymentIntent/Charge retrieval. Adaptive Pricing amount evidence uses pre-confirm presentment plus a same-Session paid poll. SEPA terminal paid/failed states were observed separately; immediate Completed was not treated as settlement.
- Shipping's exact persisted backend address object was unavailable. Fast-adb UnionPay truncation remained an unconfirmed input/harness lead and was not reported.
- Alipay, EPS/P24, BECS and vouchers were filtered out of the mobile:true docs branch; they were not required device cases. Native Android was tested; iOS, React Native and web ECE were outside this run.
- Preview exclusions respected: subscription/setup, on_behalf_of, server line-item updates/manual approval, shipping options, custom payment methods/multiprocessor, tax IDs/checkout phone, tap-to-add and card-funding filtering.
- Additional topic-specific limits and rejected leads are in the linked handoffs. No untested surface is claimed to pass.

## Environment handoff

Physical Pixel 6a, API 36, serial 2B211JEGR05252. Emulator emulator-5556 was not used. The installed APK remains paymentsheet-example/build/outputs/apk/base/debug/paymentsheet-example-base-debug.apk with DIRECTLY_TO_PLAYGROUND=false and STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true.

All investigators restored preferences/settings and force-stopped the app. Parent independently confirmed original playground preferences byte-for-byte, font scale 1.0, accelerometer rotation 0, user rotation 0, night mode yes, and no adb reverse mappings: [final device check](FINAL_DEVICE_CHECK.json). Owned relay ports 18767–18775 had no listeners. Private credentials and raw temporary evidence were removed by each topic; synthetic backend test objects remain.

Parent keepalive sent WAKEUP plus inert UNKNOWN every 90 seconds during the run. UNKNOWN was verified to refresh user activity; WAKEUP alone did not. No screen-timeout setting was changed. The verified owned keepalive was stopped with SIGTERM after the investigations; its execution session exited 143.

After the successful preference comparison, the original private baseline backup and five parent raw Gradle logs were removed. Sanitized results and reproduction evidence remain in the topic directories.

Final [artifact audit](FINAL_ARTIFACT_AUDIT.json): seven reports with the required four fields; all relative Markdown targets present; all retained JSON/Python files parse; credential-pattern scan clean. Git HEAD is unchanged, tracked and staged diffs are empty, and only the seven reports plus bug-bash/ are untracked.
