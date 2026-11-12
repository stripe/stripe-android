# Adaptive pricing bug bash — 2026-11-11

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a` on `master`, `~/stripe/stripe-android-2`. Preserved the pre-existing `AGENTS.md` change, earlier `bug-bash/` evidence, and the saved-card billing-address report and swap file. Used the personal `bug-bash` skill and `references/android-checkout.md`.

**No new confirmed bug.** Four live backend controls and 123 focused JVM tests passed. Native end-to-end adaptive-pricing behavior remains untested: the physical Pixel 6a/API 36 stayed PIN locked (`showing=true`, `secure=true`) at both checks. The coordinator had already asked the owner to unlock it. No PIN request, bypass, emulator, device UI interaction, screenshot, or video was attempted in this pass.

## Environment and supported setup

Reused the installed APK built and installed by the immediately preceding customer-logic pass from the same source, with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No rebuild or reinstall was needed for production code. No app preferences, debugger settings, adb mappings, or device settings were changed.

The playground explicitly offers France and Japan Currency Selector scenarios. `AdaptivePricingCountry.testEmail` supplies `test+location_FR@example.com` and `test+location_JP@example.com`; configuring the currency selector makes `CheckoutElementsController.configure` send `adaptive_pricing[allowed]=true`.

All backend cases used `https://stp-mobile-playground-backend-v7.stripedemos.com`, merchant `US`, and session creation version `2026-08-26.preview`. The standalone probe verifies a `pk_test_` key before creating any objects. Shared creation parameters: `ui_mode=mobile_elements`, USD integration currency, card only, guest with the supported test-location email, no automatic tax, and the playground's two line items: Classic T-Shirt USD 35.00 × 2 and Zip-Up Hoodie USD 50.00 × 1. Total USD 120.00. Tax behavior is exclusive; no tax or discount is applied.

## Live backend coverage

Run [live_probe.py](live_probe.py) with `python3 bug-bash/adaptive-pricing/live_probe.py`. It creates fresh test-mode sessions and tokenized Visa PaymentMethods, uses the SDK's Checkout client endpoint parameters, and retains only sanitized evidence. Client secrets and keys remain in process memory. Captcha content is removed from retained responses; this does not change pricing, order items, or status fields.

[backend-results.json](backend-results.json) summarizes the final successful run. Per-operation creation, init, update, retrieve, confirmation request/response, and retrieved PaymentIntent JSON files are alongside it.

| Case | Actual result |
| --- | --- |
| France, selector disallowed | USD 120.00; `adaptive_pricing_info=null`; no payment attempted |
| France, selector allowed | EUR 112.51 initially; uppercase `USD` update/retrieve yields USD 120.00; uppercase `EUR` update/retrieve restores EUR 112.51; direct Checkout client confirmation with `expected_amount=11251` returns complete/paid |
| Japan, selector allowed | JPY 19,951 initially; `USD` update/retrieve yields USD 120.00; `JPY` update/retrieve restores JPY 19,951; direct confirmation with `expected_amount=19951` returns complete/paid |
| Japan, USD selected for payment | JPY → USD → JPY → USD; direct confirmation with `expected_amount=12000` returns complete/paid in session presentment currency USD |

For each active step, the probe checks the USD integration option amount of 12000, selected currency option versus `total_summary.total`, every line item's currency, summed line totals/subtotals, and persistence in a separate retrieve. These are real backend requests, not mocked responses.

The three final-run PaymentIntents all report `status=succeeded`, `currency=usd`, `amount=12000`; the retrieved PI representation does not expose `presentment_details`. Checkout's responses retain the selected EUR, JPY, or USD presentment currency and corresponding total. The SDK's public `Session.currency` is the integration currency; its `presentmentDetails`, formatted totals, and minor-unit divisor describe presentment. A USD PI is therefore not evidence that the session's local-currency display or public session mapping is incorrect. No charge-level presentment object was independently retrieved.

These confirmations were made directly to the live Checkout client endpoint using tokenized test cards. They do **not** establish native Payment Element selection, native confirmation orchestration, 3DS, wallets, or successful completion of a physical-device UI flow.

## SDK validation

Existing focused SDK tests passed: **119 tests, zero failures/errors/skips**, Gradle exit 0 in 1m 19s. [jvm-existing-results.json](jvm-existing-results.json) records class counts. Coverage includes currency selector absent/present rendering and accessible labels under Robolectric, selected-option no-op, uppercase selector request, controller reload and error preservation, currency option formatting, public amounts, response parsing, and serialized mutations.

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.CurrencySelectorViewModelTest' \
  --tests 'com.stripe.android.elements.CurrencySelectorElementContentUITest' \
  --tests 'com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactoryTest' \
  --tests 'com.stripe.android.checkout.CheckoutSessionMappersTest' \
  --tests 'com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseJsonParserTest' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.*Currency*' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.*adaptive*' \
  --tests 'com.stripe.android.checkout.CheckoutElementsControllerTest.concurrent mutations are serialized' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

Four additional named tests replayed the sanitized live responses through the real `CheckoutSessionResponseJsonParser`, `CheckoutSessionMappers`, currency options factory, and `PaymentIntentJsonParser`: **4 tests, zero failures/errors/skips**, Gradle exit 0 in 41s. They assert EUR/USD round trips, JPY/USD round trips, USD confirmation after Japanese toggles, and disabled adaptive pricing. They verify exact formatted totals (`€112.51`, `¥19,951`, `$120.00`), JPY divisor 1 versus USD/EUR divisor 100, selected option amounts, public integration/presentment currencies, summed order items, and complete/paid public status after applying the actual USD PI using `withIntent`.

The evidence-only [AdaptivePricingLiveResponseBashTest.kt](AdaptivePricingLiveResponseBashTest.kt) and [jvm-live-replay-results.json](jvm-live-replay-results.json) preserve this validation. The temporary SDK test source copy was removed after passing. To replay the recorded responses, copy the test into `paymentsheet/src/test/java/com/stripe/android/checkout/`, run the command below, and remove only that copied file afterwards. The concrete assertions intentionally describe this recorded run; newly provisioned exchange rates may differ.

```sh
./gradlew :paymentsheet:testDebugUnitTest \
  --tests 'com.stripe.android.checkout.AdaptivePricingLiveResponseBashTest' \
  -PDIRECTLY_TO_PLAYGROUND=false \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true
```

Read the required unit/network test skills before creating the replay test and the build-delegation skill before Gradle work. A narrow `gpt-6-luna` build executor was attempted, but its initial task and follow-up arrived empty. It was interrupted without starting any work. The unavailable-executor fallback ran the exact Gradle commands sequentially in this topic agent.

## Investigated leads and limits

- The first direct confirmation probe mistakenly sent `timezone`, which the current SDK sends only for init/retrieve. The backend correctly rejected the unsupported confirmation parameter. Correcting the probe allowed payment; this was a probe error.
- Retrieving a completed session through `/checkout_client/{id}/retrieve` returns `checkout_not_active_session`. Confirmation responses already report complete/paid, and `DefaultCheckoutSessionRefresher.refresh(response)` reloads a supplied response without another fetch. This direct post-completion retrieve result did not establish an SDK bug. An earlier France payment also succeeded before the probe was adjusted to record this response; final-run evidence is explicitly identified above.
- France's backend returns unit amount 3281 for each T-shirt and line subtotal 6563 for quantity 2. The one-cent difference versus multiplying displayed rounded unit prices originates in backend conversion/rounding. The SDK preserves the server line subtotal, and summed line totals, selector amount, session total, and confirmation expected amount all agree at 11251. No SDK rounding defect was established or reported.
- Source tracing confirms the selector's selected code comes from the returned presentment currency; its UI is disabled while controller mutations are in progress. Updates go through the shared mutation coordinator and reload state. The confirmation interceptor derives `expected_amount` from the current Checkout response's item totals. Existing tests and live-response replay cover the relevant mappings, but physical-device selection retention and confirmation transitions remain untested.
- Checked the root local report and GitHub using `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'adaptive pricing'`; GitHub returned no matching issues. No duplicate or speculative report was created.
- No tax, discount, shipping, dynamic cart edits, unsupported currencies, setup-mode adaptive pricing, recurring prices, wallets, LPM changes, 3DS, process restoration, on-device accessibility, latency/error recovery on a real device, or real native payment transition was claimed as covered. Known unfinished preview surfaces were excluded.

## Environment handoff

The physical device remains PIN locked. The existing current-source debug build remains installed with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. The original playground preferences were absent and remain absent; `adb reverse --list` is empty. No relay, recorder, private credential files, or device scratch files were created in this pass. Both Gradle executions and all backend requests finished, and the unused executor is stopped. Owned temporary source and `/tmp` Gradle logs were removed.

Only local files under `bug-bash/adaptive-pricing/` are retained: these notes, standalone probe, evidence-only replay test, sanitized backend responses, and validation summaries. No production SDK/app source remains changed. No commit, push, PR, remote issue, or external message was created. Test-mode backend objects remain retained.

**Adaptive pricing has released the device/build/backend environment to the coordinator for the next sequential topic.**

## Native follow-up — 2026-11-11

**No new confirmed bug.** The unlocked physical-device follow-up closes the native France/Japan selector rendering, same-session EUR↔USD and JPY↔USD amount updates, payment-selection-sheet retention, and representative native EUR/JPY confirmation gaps above. The prior four backend controls and 123 JVM results remain separate evidence. Other native gaps remain untested.

Reused the clean debug APK from `cf7dd4b99965a4e754abdadacf4f337e764f6e6a`, with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. Physical Pixel 6a `REDACTED_DEVICE_SERIAL`, Android 16/API 36 was discovered awake/unlocked at the main menu. Every adb command selected that serial. The 5-minute device-admin screen timeout and stay-awake value `7` were left unchanged. No build, SDK/app source hook, test-source change, or JVM rerun was needed. Read the bug-bash Android/saved-card references and unit/network test skills before creating the local evidence helper.

[native_probe.py](native_probe.py) reuses customer native helper primitives but owns `ADAPTIVE_BASH_STATE`, its own evidence directory, relay, and adb mapping. The relay records actual session creation and forwards the original request and response bytes unchanged. No adaptive-pricing parameters are injected: the playground's `session.adaptive_pricing_country=FR/JP` and `controller.currency_selector.should_set_configuration=true` provide the supported configuration. All SDK requests go directly to Stripe. Independent active-session retrieves and completion polls establish fresh backend state; they do not replace SDK responses. The helper verifies test-mode publishable keys before provisioning. Private credentials/backups are removed during cleanup.

Shared setup: backend `https://stp-mobile-playground-backend-v7.stripedemos.com`, merchant `US`, version `2026-08-26.preview`, USD integration currency, card only, two default line items (T-shirt USD 35 × 2, hoodie USD 50 × 1), no tax/shipping/full-billing collection, Link/Google Pay hidden. Saved Visa 4242 is selected. France uses a fresh synthetic customer with `test+location_FR@example.com` and a tokenized test Visa whose billing country is FR. For Japan, the native Customer ID field was cleared, native Adaptive pricing country set to Japan, and the built-in Returning flow created a fresh `test+location_JP@example.com` customer with the playground's synthetic saved Visa. Country/settings changes after the initial France setup used native controls without restarting the activity/controller.

| Native action | Observed UI and independently retrieved backend result |
| --- | --- |
| France initial | EUR selected, exchange-rate caption `1 USD = 0.9376 EUR`, line totals €65.64 + €46.88, subtotal/total/selector €112.52, saved Visa selected. |
| Tap USD | USD selected, bank-rate caption shown, line totals $70.00 + $50.00, subtotal/total/selector $120.00. Visa selection retained. |
| Open Select payment-method sheet → Close | USD choice and all USD totals retained on the same session; fresh retrieve still USD 12000. |
| Tap EUR → Confirm | EUR and line totals restored to initial values. Native `Payment complete!` retained €112.52; independent poll `complete` / `succeeded` / `paid`. |
| Native Japan settings → Create Checkout Session | Fresh session, JPY selected, caption `1 USD = 166.2750 JPY`, line totals ¥11,639 + ¥8,314, subtotal/total/selector ¥19,953, no decimal places. Saved Visa selected. |
| Tap USD → JPY | Each currency selection updates all displayed line totals, subtotal and total; separate retrieves return USD 12000 then JPY 19953. Visa selection retained. |
| Confirm in JPY | Native `Payment complete!` retained ¥19,953; independent poll `complete` / `succeeded` / `paid`. |

[Native results](native/results.json) records the seven paired UI/backend amount states, exact session IDs, selected card IDs, completion checks, and limitations. Checks confirm each country's toggles use the same session, France/Japan use distinct sessions, all line currencies and summed totals agree with the selected backend option, saved Visa remains available, and no keys/client secrets appear in retained JSON/XML. Exchange rates differ slightly from the earlier live probe; these assertions use the fresh native run's actual server amounts.

All ten retained PNGs were opened and visually inspected and have adjacent UIAutomator XML:

- [France initial](native/france-initial.png), [USD selected](native/france-usd.png), [USD retained after selection sheet](native/france-usd-after-selector.png), [EUR before confirmation](native/france-eur-before-confirm.png), [EUR completion](native/france-completed.png).
- [Japan setting](native/japan-settings.png), [JPY initial](native/japan-initial.png), [USD selected](native/japan-usd.png), [JPY before confirmation](native/japan-jpy-before-confirm.png), [JPY completion](native/japan-completed.png).

Redacted creation requests and actual active-session responses are in `native/`, with [France poll](native/france-poll.json) and [Japan poll](native/japan-poll.json). No video was needed for these passing controls. No card-form autofill or wallet-save prompt was triggered; saved synthetic cards avoided personal wallet interaction.

One navigation lesson: UIAutomator exposed the bottom Create button even while the rendered keyboard covered it. The first Japan creation tap therefore entered a space into settings search; the next read-only retrieve still addressed the already-completed France session and correctly returned `checkout_not_active_session`. The observed keyboard was dismissed, Create was tapped again, and the newly created Japan session was verified before proceeding. This was an automation mistake, not an SDK candidate. The helper now checks that the named native case has a matching recorded session before snapshot/poll.

Rechecked local root reports and `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'adaptive pricing'`; GitHub returned no matches. No new candidate reproduced and no report was created. The known billing-tax saved-card editor and SEPA-save issues were not rerun.

Remaining native gaps include a USD-confirmed payment, selector-disabled configuration, new-card form interactions with currency changes, latency/error recovery, rapid concurrent mutations, process/activity recreation, accessibility, appearance variants, tax/discount/shipping/cart updates, unsupported currencies, wallets/LPMs, authentication/3DS, recurring/setup-mode flows, and independent charge/webhook fulfillment inspection. Native EUR/JPY completion is supported by the actual UI plus Checkout poll. No native Intent response body or post-payment charge presentment object was captured, and poll does not return amount/currency. Completed Checkout client retrieve remains unavailable.

Only the appended notes, local helper, and redacted native evidence are retained, uncommitted. No production source, test source, existing report, or user AGENTS edit was changed. No commit, amend, push, PR, remote issue, Slack, or email was created. Test-mode backend objects remain. Cleanup and the final actual device-release check are recorded below.

Cleanup verified `PaymentConfiguration` and `DefaultPrefsRepository` byte-for-byte against fresh backups; Checkout playground preferences are absent as at backup. The owned relay stopped, its port closed, reverse mappings are empty, private credentials/backups are removed, and owned device XML is removed. All native/backend operations have finished; no recorder, build, or test process remains. The current clean APK and flags remain installed. [Cleanup evidence](native/cleanup.json). Final main-menu/unlocked verification is recorded in that evidence immediately before release.

**Native adaptive-pricing follow-up has released the device/build/backend environment to the coordinator for Automatic Tax.**
