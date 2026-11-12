# Automatic-tax bug bash — 2026-11-11

**No new confirmed SDK bug.** Fresh live backend coverage exercised both billing and shipping tax sources, address replacement and clearing, incomplete-input recovery, and four successful test payments. **78 existing focused JVM tests and 4 captured-response replay tests passed.** This pass did not exercise the native UI: the physical Pixel remained PIN locked at both checks (`showing=true`, `secure=true`, screen off). The coordinator had already asked its owner to unlock it; no PIN, bypass, emulator, or connected-device test was used.

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `master`, in `~/stripe/stripe-android-2`. Preserved the pre-existing modified `AGENTS.md`, billing issue/swap file, and all earlier evidence. Used the personal bug-bash skill with `references/android-checkout.md` and `references/tax-and-saved-cards.md`, plus the required unit/network testing and build-delegation skills.

## Environment and supported configuration

Physical device: Pixel 6a, Android 16/API 36. Reused the current-source debug APK built and installed by the customer-logic pass, with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No production source change, rebuild, install, preference change, or native app interaction was needed in this pass.

All live cases used `https://stp-mobile-playground-backend-v7.stripedemos.com`, merchant `us_tax`, and creation version `2026-08-26.preview`. The probe asserted a `pk_test_` key before provisioning. Shared session parameters: `ui_mode=mobile_elements`, USD, card only, `automatic_tax[enabled]=true`, exclusive taxable prices with `tax_code=txcd_99999999`, two Classic T-Shirts at $35 and one Zip-Up Hoodie at $50. Subtotal $120. Synthetic names, email, addresses, customers, and tokenized Visa PaymentMethods only. Keys and client secrets stayed in memory.

Run the standalone [live_probe.py](live_probe.py):

```sh
python3 bug-bash/automatic-tax/live_probe.py
python3 bug-bash/automatic-tax/live_probe.py --boundaries
```

The helper issues real Checkout client init/update/retrieve/confirm requests using the SDK's parameter shapes. These are direct backend probes, not native SDK confirmation orchestration. Every active update is separately retrieved; line-item totals and taxes are summed against `total_summary`. Confirmation responses and retrieved PaymentIntents are retained with secrets redacted. The four final PaymentIntents all report `succeeded` with the expected USD amount.

## Newly executed live coverage

[backend-results.json](backend-results.json) summarizes five main configurations. Creation and per-operation requests/responses are adjacent files. [boundary-results.json](boundary-results.json) covers a sixth session for invalid/incomplete inputs and confirmation amount protection.

| Configuration / action | Actual result |
| --- | --- |
| Guest, automatic billing collection, no initial address | `address_source=billing`, `requires_address`, tax $0, total $120 |
| Same session: full San Francisco CA/94103 with line 2 | `ready`, tax $10.35, total $130.35 |
| Replace with Portland OR/97204, then Toronto ON/`M5V 3G5`, then country-only France | Each update/retrieve `ready`, tax $0, total $120 |
| Clear billing tax region (`tax_region=`), then restore full California | Clear returns `requires_address`/$120; restoration returns `ready`/$130.35; direct confirmation succeeds for $130.35 |
| Guest, required billing collection | Billing tax source. California → clear → Oregon produces $130.35 → requires-address/$120 → ready/$120; payment succeeds at $120 |
| Guest, shipping countries US/CA/IE/GB and required billing collection | `address_source=shipping`; California → Canada → clear → Oregon produces $130.35 → $120 → requires-address/$120 → ready/$120 |
| Confirm shipping case with **Oregon shipping and California card billing** | Complete/paid at $120; the current shipping tax source controls the amount despite conflicting billing |
| Returning customer with stored full California address, no `customer_update` | `address_source=customer`, already `ready`, tax $10.35; no payment attempted |
| Same customer with `customer_update[address]=auto` | `address_source=billing`, initially requires-address/$120. Explicit Oregon update becomes ready/$120; no payment attempted |
| Fresh billing session: full California → invalid country `XX` | Backend rejects the invalid country. Separate retrieve retains California tax $10.35 and total $130.35 |
| Same session: US country/ZIP only | Backend accepts and calculates $10.35. This does not establish sufficient SDK saved-card billing details; see the pre-existing issue below |
| Replace with country-only US, then US/`not-a-postal` | Both accepted as incomplete tax input, returning `requires_address` and $120; prior California tax does not remain |
| Recover with full Oregon, try stale `expected_amount=13035` | Backend rejects with `checkout_expected_amount_does_not_match_amount_due`; session remains active at $120 |
| Confirm again using current amount | Complete/paid; PaymentIntent succeeded for $120 |

These are observed merchant/test-backend tax outcomes. In particular, the $0 Canada/France results do not establish general tax applicability or legal rate correctness for other merchants. France used country-only input intentionally. Ireland/UK were allowed shipping configurations but were not entered in this pass.

## SDK validation and inspected paths

Existing tests passed: **78 tests, no failures/errors/skips**, Gradle exit 0 in 24s. [jvm-existing-results.json](jvm-existing-results.json) records the actual classes and counts. Coverage includes initial shipping/billing tax selection and defaults, persisted saved selection, wrong-source/no-tax no-op, clearing billing selection and shipping, failed-update state preservation, country validation, saved-card filtering, and confirmation's tax refresh/amount guard and shipping parameters.

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.CheckoutSessionTaxRegionUpdaterTest' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.*tax*' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.*Tax*' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.*ShippingAddress*' \
  --tests 'com.stripe.android.paymentsheet.repositories.CheckoutSessionRepositoryTest.*tax*' \
  --tests 'com.stripe.android.paymentsheet.state.DefaultPaymentMethodFilterTest' \
  --tests 'com.stripe.android.checkout.ShippingAddressElementStateHolderTest' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.*tax*' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.*expectedAmount*' \
  --tests 'com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationInterceptorTest.*shipping*' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

The repository selector with lowercase `*tax*` matched no methods; do not count it as separate repository-suite coverage. The controller/updater network tests exercise tax request parameters through that repository.

Four additional named tests replayed the sanitized live responses through `CheckoutSessionResponseJsonParser`, `CheckoutSessionMappers`, and `PaymentIntentJsonParser`: **4 passed**, Gradle exit 0 in 41s. They assert billing/shipping requires-address versus ready states, correct source flags, exact formatted totals, summed order-item amounts, customer-source behavior, and complete/paid status with the actual retrieved PaymentIntent.

The evidence-only [AutomaticTaxLiveResponseBashTest.kt](AutomaticTaxLiveResponseBashTest.kt) and [jvm-live-replay-results.json](jvm-live-replay-results.json) preserve the test and outcome. Copy the test into `paymentsheet/src/test/java/com/stripe/android/checkout/`, run this command, then remove only that temporary copy:

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.AutomaticTaxLiveResponseBashTest' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

Source inspection additionally traced new-card billing through `PaymentMethodMetadata.requiresBillingAddressForAutomaticTax`, `BillingAddressFormElementsBuilder`, `SheetTaxRegionUpdater`, `DefaultSheetActivityContinueCoordinator`, and `CheckoutSheetLauncher`. Shipping Save uses the address-element updater and controller commit path. `CheckoutSessionTaxRegionUpdater` sends an update only for the matching configured tax source; null address serializes an empty `tax_region`. The confirmation interceptor uses the current response's summed line totals for `expected_amount`; saved-card confirmation also rejects a changed estimate. The source inspection is not fresh on-device form/Continue coverage.

The low-cost build executor was attempted with a narrow no-edit task, but both initial and follow-up instructions arrived empty. It was stopped without executing anything. The skill's unavailable-executor fallback ran both exact commands sequentially in this topic agent.

## Pre-existing evidence, rejected leads, and limits

- The earlier [billing-address report](../../ISSUE_billing_address_saved_card_edit_omits_tax_fields.md) remains the **pre-existing confirmed issue** for saved French card → US editor omitting tax fields, then filtering out the card. Read it and its evidence, but did not rerun or duplicate it. This prior report is now included in the sharing snapshot. The live ZIP-only tax acceptance above is a backend input control and is not a new saved-card disappearance reproduction.
- Earlier local shipping notes contain same-revision native controls for California/Oregon tax changes and clearing, saved-card selection retention, native confirmation, and Canadian address entry; those notes remain outside the sharing snapshot. [Earlier billing notes](../billing-address/RUN_NOTES.md) contain native billing controls and the known issue. Both are **inherited evidence**, not newly executed native coverage here.
- The first direct confirmation omitted required email. The backend returned `checkout_email_missing`. Correcting the probe to send synthetic `collected_information[email]`, as the SDK supports, allowed successful payment. This was probe setup, not an SDK finding. Main evidence files represent the final successful rerun; that abandoned first session was not completed.
- `address_source=customer` with a stored customer address is expected under the tested creation parameters. It is not evidence that billing updates are broken. Likewise, incomplete US input returning requires-address and invalid-country rejection are expected controls.
- Some zero-tax backend responses retain an 8.625% Sales Tax row with amount zero after leaving California. The SDK preserves server data; totals agree. This did not establish an SDK calculation defect.
- Searched GitHub with `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'automatic tax'`; no issues returned. Rechecked the local root report. No new `ISSUE_automatic_tax_*.md` was warranted.
- Native new-card form collection, interactive address switches/clearing, native confirmation/selection retention, UI error recovery, and rendered totals were not exercised during this pass because the physical device stayed locked. No screenshot or video was captured.
- No claim for ECE, Google Pay, Link, other payment methods, 3DS, setup mode, recurring prices, tax exemptions/IDs, inclusive tax, discounts, adaptive pricing plus tax, shipping rates, merchant tax registration correctness, process death, accessibility, locales, or concurrent UI/merchant updates. Known unfinished preview flows were excluded.
- Final persisted Checkout Session fulfillment/customer address fields remain unverified. The earlier shipping pass could not retrieve completed Checkout Sessions with the available backend; this pass did not repeat that known inaccessible retrieval. Successful direct confirmation and PaymentIntent amounts do not close the fulfillment-persistence gap.

## Cleanup and environment handoff

The temporary replay test copy was removed from the SDK source tree. No production SDK/app source changes remain. Git status is unchanged outside new files under `bug-bash/automatic-tax/`; pre-existing reports/settings/source changes were preserved. Both Gradle runs and both final backend probe processes completed, and the unused executor was stopped. Temporary Gradle logs were removed after preserving test summaries. All 91 evidence JSON files parse; retained JSON contains no actual publishable/secret keys or client secrets. The Python probe syntax was checked.

No relay, recorder, private credential file, device scratch file, or adb mapping was created in this pass. Final `adb reverse --list` is empty. Original playground preferences were absent and remain absent. No app, debugger, or device settings were changed. The Pixel remains locked; the installed current-source build still has `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`.

Only local notes, probe, evidence-only replay test, sanitized backend responses, and test-result summaries are retained. Test-mode backend objects remain. No commit/amend, push, PR, remote issue, Slack, or email was created.

**Automatic tax has released the device/build/backend environment to the coordinator for the next sequential topic.**


## Native follow-up after device unlock — 2026-11-11

**No new confirmed SDK bug.** Two focused native automatic-tax payments completed on the physical Pixel 6a (`REDACTED_DEVICE_SERIAL`, Android 16/API 36), closing the representative shipping-source and billing-source UI gaps from the first pass. This was sequential exclusive use after Adaptive Pricing. The earlier 82 JVM tests and broader direct-backend matrix above are inherited results and were not rerun. Earlier shipping/billing native runs remain separate inherited evidence.

Reused the unchanged current-source APK at `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, `DIRECTLY_TO_PLAYGROUND=false`, `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No SDK/app hooks, production source edits, new regression tests, build, or install were needed. Used the bug-bash skill and both Android setup/tax references. All backend objects were newly created in test mode under `us_tax`; asserted `pk_test_` before provisioning. Real creation requests used `2026-08-26.preview`, `mobile_elements`, USD, card only, automatic tax, two $35 shirts plus one $50 hoodie, exclusive tax and `txcd_99999999`. See [shipping creation](native/shipping-creation.json), [billing creation](native/billing-creation.json), and [native result summary](native/native-results.json).

[The native helper](native_probe.py) reuses the existing customer helper transport and provides a logging-only loopback relay: **no request parameters or backend responses are rewritten**. Both cases use current supported playground settings. Start with a fresh private `TAX_NATIVE_STATE` and the explicit physical `ANDROID_SERIAL`; `serve`, `prepare shipping` / `prepare billing`, observed `tree`/`tap`/`enter`/`shot`/`snapshot`/`poll`, then `restore`. Credentials remain only in owned private temporary state. The source itself supplied `customer_update[shipping]=auto` for the returning shipping customer. The guest billing case needed no customer update override; its ordinary billing collection setting was automatic. Actual backend `automatic_tax.address_source`, rather than the flag alone, established both sources.

| Newly exercised native action | Observed outcome and evidence |
| --- | --- |
| Shipping source with full merchant CA/94103 defaults and fresh saved Visa whose billing is also California | Source `shipping`, `ready`, tax $10.35, total $130.35; native display and backend agree. [UI](native/shipping-ca-initial.png), [backend](native/shipping-ca-initial.json). |
| Open Shipping Address, use Search for `1120 SW 5th Ave Portland`, choose the observed Portland suggestion, Save address | Form becomes `1120 Southwest 5th Avenue`, Portland, Oregon/97204; recipient remains `Tax Native Recipient` and old `Suite 17` clears. Tax becomes $0 and total $120, saved Visa remains selected. [Form](native/shipping-or-form.png), [UI](native/shipping-or-saved.png), [backend](native/shipping-or-saved.json). |
| Reopen the saved shipping form, then close unchanged | Oregon fields persist, with empty line 2. [Reopened form](native/shipping-or-reopened.png). |
| Confirm natively with Oregon shipping and the unchanged saved California-billing Visa | Native **Payment complete!** at displayed $120. Checkout poll reports `complete`, `succeeded`, `paid`, `livemode=false`. [Completion](native/shipping-completed.png), [poll](native/shipping-completed-poll.json). |
| Guest billing-source session with no initial billing; open Card | Backend `address_source=billing`, `requires_address`, $120. Native UI asks for billing to calculate tax; US new-card form includes street, city, state and ZIP despite ordinary automatic collection. [Fields](native/billing-required-tax-fields.png), [backend](native/billing-initial.json). |
| Enter synthetic Visa 4242, 12/30, CVC 123, `510 Townsend St`, San Francisco, California, short ZIP `941` | Continue stays disabled; after attempting it, UI hierarchy reports `Your ZIP code is incomplete.` Form remains open and backend remains `requires_address`/$120. [Disabled control](native/billing-short-zip-validation.png), [backend](native/billing-short-zip.json). |
| Correct ZIP to `94103`, Continue | Continue enables; selection commits and backend becomes billing-source `ready`, tax $10.35, total $130.35, matching UI. [Corrected form](native/billing-corrected-form.png), [totals](native/billing-ca-ready.png), [backend](native/billing-ca-ready.json). |
| Confirm corrected billing session natively | Native **Payment complete!** at displayed $130.35. Poll reports `complete`, `succeeded`, `paid`, `livemode=false`. [Completion](native/billing-completed.png), [poll](native/billing-completed-poll.json). |

All retained screenshots were opened and visually inspected. No video was necessary. Personal autofill was dismissed with **No thanks** and the Google save-card prompt with **Not now**; no personal wallet data was selected or captured in retained evidence. The helper refuses screenshots while system dialogs are present and filters their tree output to dismissal labels. One initial Continue lookup occurred while the keyboard obscured the button; it made no tap, and the check was completed after dismissing the keyboard. One attempted screenshot was rejected while the Google save prompt was present; capture succeeded after dismissal. Neither was a product failure.

The pre-existing saved-card France→US editor issue was read and deliberately not rerun or duplicated. Local reports were rechecked; GitHub issue search used `GH_HOST=github.com`, `--state all`, query `checkout tax`, and returned no matches. No `ISSUE_automatic_tax_*.md` was warranted. The saved card's California billing remaining unchanged during Oregon shipping confirmation is a source-selection control, not coverage of saved-card billing editing.

Fresh coverage here is CA→OR shipping replacement and US new-card short-ZIP correction, followed by one native confirmation per source. Reverse OR→CA, shipping clearing, other countries, required billing collection, other payment methods, wallets, saved-card edit, tax IDs/exemptions, inclusive prices, tax jurisdiction correctness, rates/discounts, adaptive pricing plus tax, accessibility, process death and concurrent edits were not re-exercised. Poll proves Checkout completion/payment status but does not return totals or PaymentIntent details. **Final PaymentIntent amount and Checkout fulfillment/address persistence were not independently retrieved in this native follow-up.** Displayed final totals and the active backend snapshots precede successful native confirmation; they do not close the documented final server-side persistence gap.

The first fresh preference backup found Checkout playground preferences absent, while PaymentConfiguration and DefaultPrefsRepository were present. Cleanup restores those two files byte-for-byte and removes the newly created Checkout preference file. Only this topic's relay, adb reverse mapping, `/sdcard/tax-native-ui.xml`, and private temporary state are owned by the follow-up. No recorder, debugger setting, screen policy, stay-awake setting or other device setting was changed. The inherited device-admin five-minute idle timeout still applies even with `stay_on_while_plugged_in=7`; meaningful UI actions were kept active. Debug settings read `debug_app=null`, `wait_for_debugger=0`.

Retained native JSON parses, expected sources/status/taxes/totals and both successful polls were asserted, helper syntax parses, and retained text was scanned for actual keys/client secrets with no matches. Changes are confined to this native helper, evidence and appended notes. Existing user AGENTS edit, both earlier issue reports, swap file and all other evidence were preserved. No commit/amend, push, PR, remote issue, Slack or email was created. Synthetic test backend objects remain.

Cleanup completed: fresh preferences restored as described, owned reverse mapping and device XML removed, relay stopped and awaited, private credential/state directory deleted. The unchanged current-source cleartext-enabled APK remains installed. Final main-menu/unlocked verification and release are recorded in `native/handoff.json`.
