# PaymentSheet first-presentation fixes: independent implementation guide

This document describes the 16 production changes from the first-load jank
investigation. Each numbered section is an independently implementable task
against the code before these changes. The current working tree already contains
the implementations. Use symbols and behavior, rather than line numbers, to
locate code on another revision. No commit or PR is required by this guide.

The reported workload is the example app transitioning from loading into
horizontal PaymentSheet, especially the first presentation in a fresh process.
Release performance is the priority; debug improvements are useful where cheap.
Preserve test infrastructure, synchronization, and fixtures. Regression tests for
production behavior are appropriate; read the repository's applicable testing
skills before writing them.

## Impact estimates and evidence

Estimates describe the expected effect on the affected path, not additive savings
from the total frame time. Most changes have correctness or structural validation
but no isolated before/after release benchmark. Do not convert these estimates
into claimed milliseconds saved. Several changes remove overlapping work.

| ID | Change | Estimated impact on first presentation | Evidence/confidence |
| --- | --- | --- | --- |
| 1 | Build address controllers only for requested countries | High when an address form previously built every country's controller graph | Large structural reduction; isolated release timing unmeasured |
| 2 | Avoid hidden contact/autocomplete controller construction | Medium for forms hiding contact fields | Removes unused controllers and their initialization; timing unmeasured |
| 3 | Skip duplicate initial interactor form construction | Medium; high with an expensive form factory | Eliminates one redundant initial factory pass |
| 4 | Classify forms without constructing disposable controllers | Medium on initial card/form classification | Avoids classification-only construction; overlaps IDs 1–3 |
| 5 | Resolve the payment loader on a worker | Small to medium main-thread relief | Defers loader dependency construction; total work largely unchanged |
| 6 | Warm localized countries on a worker and publish cache atomically | Medium on a cold locale/cache | Moves localization/sorting off Main; concurrent publication becomes consistent |
| 7 | Defer analytics dependencies and V2 request construction | Small to medium | Moves dependency/request work out of presentation callbacks; timing unmeasured |
| 8 | Lazily resolve saved-payment backends | Small; larger when backend graphs are cold | Avoids unused repository graphs at construction |
| 9 | Lazily construct inline Link analytics | Small | Avoids an unused analytics subcomponent on startup |
| 10 | Supply scoped library baseline profiles | Potentially high for cold release runtime work; negligible direct debug effect | Packaging/R8 verified; isolated release speedup unmeasured |
| 11 | Render dropdown values without an embedded text editor | Medium per affected dropdown, especially cold composition | Removes disabled BasicTextField/editor machinery; snapshots match |
| 12 | Remove a duplicate dropdown subscription | Small | Two selected-index subscriptions become one |
| 13 | Read row divider height during layout | Small to medium for multi-field rows | Removes the composition invalidation caused by first measurement |
| 14 | Capture debug assertion stacks only on failure | Medium in debug forms with many subscriptions; approximately no release benefit | Removes one stack capture per subscription in the normal debug path |
| 15 | Collect the complete form field graph once | Small to medium, depending on form size | Two complete-value graph subscriptions become one |
| 16 | Await asynchronous WebView startup before CAPTCHA setup | High when passive CAPTCHA initializes a cold WebView | Release warmer activity creation observed at 241.79 ms before vs 2.70 ms after; native main task remains |

In an earlier debug test trace, 93.71 ms of a 126.67 ms first Choreographer
callback was class verification. That supports investigating cold runtime costs,
but does not quantify a baseline-profile release benefit. Compose test-clock
composition durations are not production frame deadlines.

The standalone release observations use an API 33 Pixel 2 emulator with software
graphics. In the updated release capture, 10 of 45 submitted PaymentSheet surface
frames in the first 1.2 seconds missed app deadlines. This is neither an exact
skipped-refresh count nor a hardware benchmark. See
[perfetto_analysis_report.md](perfetto_analysis_report.md) for trace identities,
SQL, measurements, and limits. Temporary `/tmp` traces may not be available to a
future agent; reproduce measurements rather than depending on them.

## 1. Lazily create address fields for each requested country

**Root cause:** `AddressElementUiRegistry` eagerly transformed every country's
address schema into mutable field/controller objects when one address form was
created. A form generally initially uses one country.

**Production file:**
`stripe-ui-core/src/main/java/com/stripe/android/uicore/elements/AddressElementUiRegistry.kt`.

**Implementation:**

1. Replace the eager all-countries map and eagerly constructed default fields
   with an instance-local `MutableMap<String?, List<SectionFieldElement>>`.
2. On `get(countryCode)`, return that country's cached fields or obtain its schema
   and call `transformToElementList` on demand. Cache successful results,
   including an empty list. Keep unknown-country results nullable.
3. Preserve `null` as the default-schema lookup. Supply the default schema's
   country code to the transformation when the request is null.
4. Keep the cache per address registry/form. Controllers contain mutable user
   input and must never be globally shared. Reuse the same objects when switching
   back to a previously selected country.
5. For focused tests, a constructor accepting the element-creation function can
   make requests observable; retain the schema-registry constructor for callers.

**Independent validation:** Add/run `AddressElementUiRegistryTest` in the matching
`stripe-ui-core/src/test/java/com/stripe/android/uicore/elements/` package. Cover
zero factory calls at construction, one call per requested country, retained
values after US → CA → US, isolation between two forms, default/null lookup, and
unknown-country behavior.

**Impact:** High structural reduction: creation scales with countries visited
rather than all schemas. No isolated release millisecond saving was measured.
Moving work to the first switch is intentional; do not prebuild other countries.
No dependency on the other fixes.

## 2. Do not construct hidden address contact and autocomplete fields

**Root cause:** `AddressElement` eagerly created email, name, phone, and condensed
autocomplete elements even when the configured form never displayed them. A
country update also accessed the phone controller when phone collection was
hidden. Merely marking the properties lazy would therefore be insufficient.

**Production file:**
`stripe-ui-core/src/main/java/com/stripe/android/uicore/elements/AddressElement.kt`.

**Implementation:**

1. Make `emailElement`, `nameElement`, `phoneNumberElement`, and
   `addressAutoCompleteElement` lazy, preserving their existing initial values,
   optionality, identifiers, and configuration.
2. Guard phone-country synchronization with
   `phoneNumberConfig != AddressFieldConfiguration.HIDDEN` before reading the
   lazy phone property.
3. Use `if (hideName) null else nameElement`; `nameElement.takeUnless { hideName }`
   evaluates the receiver and would still initialize it.
4. Build the condensed autocomplete list only in branches that use it. Do not
   eagerly compute both condensed and expanded lists before selecting a mode.
5. Audit all reads of the lazy properties. Keep visible contact fields,
   autocomplete behavior, and expanded autocomplete configuration intact.

**Independent validation:** `AddressElementTest` in the same module/package's
test source set covers hidden identifiers, initial values for visible contact
fields, and visible phone-country synchronization. Also exercise address modes
with and without autocomplete and required/optional contact fields. Identifier
exclusion alone does not prove allocation savings; inspect property accesses or
profile constructor calls for that part.

**Impact:** Medium when fields are hidden, especially phone initialization.
Visible fields still incur their normal cost. No isolated timing. Independent of
ID 1, though both edit the address initialization path.

## 3. Skip the initial selected-method emission when the form already exists

**Root cause:** `DefaultAddPaymentMethodInteractor` creates its initial state
with form arguments/elements, then collects a StateFlow whose first emission is
the same selected method. Handling that emission rebuilt the form immediately.

**Production file:**
`paymentsheet/src/main/java/com/stripe/android/paymentsheet/ui/AddPaymentMethodInteractor.kt`.

**Implementation:** At the start of the `selectedPaymentMethodCode.collect`
handler, return from that emission when the emitted code equals
`_state.value.selectedPaymentMethodCode`. Place the guard before all form,
US-bank-account argument, and selection-change work. Continue processing actual
method changes with the existing behavior.

**Independent validation:** In
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/ui/DefaultAddPaymentMethodInteractorTest.kt`,
count initial factory calls and verify the initial emission adds none. Verify a
different method constructs and publishes its form and arguments. Retain existing
selection/event tests.

**Impact:** Removes one redundant initial form-factory pass. Medium in typical
forms and potentially high before ID 1. Does not remove legitimate rebuilding
after method changes. Independent of ID 4; their savings overlap.

## 4. Determine form type without constructing disposable forms

**Root cause:** `DefaultFormDefinitionFactory.formTypeForCode` constructed form
elements just to classify the form, even if those controllers were not used for
display or had already been constructed elsewhere.

**Production file:**
`paymentsheet/src/main/java/com/stripe/android/paymentsheet/FormDefinitionFactory.kt`.

**Implementation:**

1. Add an instance-local map from payment-method code to `FormHelper.FormType`.
2. Whenever `formElementsForCode` creates elements, derive and cache their type
   before returning them.
3. In `formTypeForCode`, use a cached classification first. Return
   `UserInteractionRequired` directly for US bank account and Link, and for card
   when metadata reports card as a supported method.
4. For other uncached types, retain the existing element-based classification
   fallback, and cache its result.
5. Cache classification only. Continue creating controllers using the current
   selection/default values when actual form elements are requested.

**Independent validation:** Extend
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/FormHelperTest.kt`.
Verify known-type classification avoids construction, classification from an
already built form is reused, and requesting form elements again observes an
updated selection. Retain no-form and user-interaction classification coverage.

**Impact:** Medium where classification would build an unused form; little when
the cache/path is unused. No isolated timing. Do not estimate savings by adding
an entire form-construction duration to ID 3's estimate. No prerequisite fix.

## 5. Resolve the payment-element loader inside the worker context

**Root cause:** Injecting `PaymentElementLoader` directly into the ViewModel
constructed its dependency graph during main-thread ViewModel creation, before
the loader's existing worker-context load call.

**Production file:**
`paymentsheet/src/main/java/com/stripe/android/paymentsheet/PaymentSheetViewModel.kt`.

**Implementation:** Change the injected loader to
`dagger.Lazy<PaymentElementLoader>`. Resolve it with `.get()` inside
`withContext(workContext)` in `loadPaymentSheetState`, immediately before calling
`load`. Preserve all load arguments, error handling, state transitions, and DI
scope. A `.get()` in a property initializer or before `withContext` defeats this
change.

**Independent validation:** Update manual ViewModel constructions to wrap their
existing loader fakes in Dagger Lazy. Relevant tests are `PaymentSheetViewModelTest`
and `PaymentSheetActivityTest` under
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/`. Verify loader
resolution is deferred until loading and occurs in the supplied work context,
and existing success/failure/reload cases still work.

**Impact:** Small to medium main-thread relief from dependency creation; total
work remains and can still influence time-to-ready. Isolated cost unmeasured.
Independent of ID 6, which edits the same load block.

## 6. Prepare localized countries on a worker with atomic cache publication

**Root cause:** The first country dropdown paid locale display-name generation,
collation, and sorting on Main. The existing cache published locale and country
list in separate volatile fields, which is unsafe to rely on when warming it
concurrently: a reader can observe a mismatched pair.

**Production files:**

- `paymentsheet/src/main/java/com/stripe/android/paymentsheet/PaymentSheetViewModel.kt`
- `stripe-core/src/main/java/com/stripe/android/core/model/CountryUtils.kt`

**Implementation:**

1. In `CountryUtils`, replace the two cache fields with one volatile nullable
   `Pair<Locale, List<Country>>` or equivalent immutable holder.
2. Read the holder once into a local variable. Return its list only if its locale
   matches. Compute a new list locally and publish the completed pair in one
   assignment. Preserve ordering, current-country priority, and locale changes.
3. Inside the ViewModel's existing `withContext(workContext)` load block, use a
   structured `coroutineScope` and launch
   `CountryUtils.getOrderedCountries(Locale.getDefault())` concurrently with the
   loader. Complete that scope before publishing loaded state, so form creation
   can consume the warmed cache.
4. Keep both parts in this task. Do not introduce background warming while still
   publishing locale/list independently. Duplicate concurrent computation is
   acceptable; inconsistent publication is not.

**Independent validation:** Check locale switching and country ordering, cold
and cached reads, concurrent reads across locales, and existing ViewModel loading
tests. Inspect a trace to confirm sorting is on a worker before form creation.
No isolated concurrent-cache regression test was added in this investigation;
the behavior above is an acceptance criterion for an independent implementation.

**Impact:** Medium on a cold locale; warm cache reads gain little. Work is moved
and overlapped, not eliminated. If warmup outlasts loading it can delay readiness;
measure that tradeoff. No dependency on ID 5.

## 7. Defer analytics dependencies and build V2 requests on the worker

**Root cause:** Analytics dependencies were constructed with the event reporter,
and V2 request construction ran synchronously in presentation/event callbacks.

**Production file:**
`paymentsheet/src/main/java/com/stripe/android/paymentsheet/analytics/DefaultEventReporter.kt`.

**Implementation:**

1. Inject `AnalyticsRequestV2Executor` and `PaymentAnalyticsRequestFactory` as
   `dagger.Lazy` and resolve them only at their existing worker-side uses.
2. Make the internally created `AnalyticsRequestV2Factory` lazy as well.
3. In `fireV2Event`, compute the event's additional-parameter snapshot on the
   caller, then launch on `workContext` to build the request and resolve/enqueue
   through the V2 executor.
4. Preserve event-time metadata and publishable-key overrides. Delaying request
   creation must not silently substitute metadata from a later configuration.
   Keep event-specific parameter precedence and existing callback behavior.

**Independent validation:** In
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/analytics/DefaultEventReporterTest.kt`,
cover lazy resolution, request work after advancing the worker scheduler,
event-time metadata despite a later metadata update, parameter precedence,
publishable-key overrides, and unchanged events. Update manual constructor
arguments with lazy wrappers rather than eagerly resolving them.

**Impact:** Small to medium main-thread relief; repeated callbacks can benefit
too. Network traffic and event count are unchanged. No isolated release timing;
no dependency on IDs 8 or 9.

## 8. Lazily resolve saved-payment-method repository backends

**Root cause:** Constructing `DefaultSavedPaymentMethodRepository` eagerly
constructed both customer and checkout-session backend graphs, even when no saved
payment method operation used them.

**Production file:**
`paymentsheet/src/main/java/com/stripe/android/paymentsheet/repositories/SavedPaymentMethodRepository.kt`.

**Implementation:** Inject `CustomerRepository` and `CheckoutSessionRepository`
as `dagger.Lazy`. Resolve only the backend selected by each operation's
`CustomerMetadata` branch: detach, update, set default, or retrieve. Preserve
unsupported-operation behavior, request parameters, API configuration, error
mapping, and DI scopes. Do not resolve either backend in the constructor.

**Independent validation:** Update
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/repositories/DefaultSavedPaymentMethodRepositoryTest.kt`.
Assert construction resolves neither backend, a customer operation resolves
only the customer backend, and a checkout operation resolves only checkout.
Retain operation result/request coverage across customer metadata variants.

**Impact:** Small startup improvement, potentially larger with cold repository
graphs. The cost moves to the first relevant operation and may disappear entirely
for sessions without such operations. No isolated timing. Independent task.

## 9. Defer the inline Link analytics subcomponent

**Root cause:** Creating the inline-signup confirmation definition also created
a Link analytics subcomponent, although its helper is only used on particular
confirmation branches.

**Production files:**

- `paymentsheet/src/main/java/com/stripe/android/paymentelement/confirmation/linkinline/LinkInlineSignupConfirmationDefinition.kt`
- `paymentsheet/src/main/java/com/stripe/android/paymentelement/confirmation/linkinline/LinkInlineSignupConfirmationModule.kt`

**Implementation:** Accept a Kotlin `Lazy<LinkAnalyticsHelper>` in the
definition. In the module, pass
`lazy { linkAnalyticsComponentFactory.create().linkAnalyticsHelper }`. Resolve
`.value` only in the branches currently emitting `onLinkPopupSkipped`. Preserve
the helper's lifetime, confirmation decisions, and event count. This is Kotlin
Lazy, unlike the injected Dagger Lazy used in other tasks.

**Independent validation:** Update
`paymentsheet/src/test/java/com/stripe/android/paymentelement/confirmation/linkinline/LinkInlineSignupConfirmationDefinitionTest.kt`
and its constructor helpers in
`paymentsheet/src/test/java/com/stripe/android/paymentelement/confirmation/ConfirmationUtils.kt`.
Verify no eager resolution and exactly the existing analytics on relevant
sign-in/verification branches. Run the existing Link confirmation cases.

**Impact:** Small; avoids startup work for an unused subcomponent, moving it to
the first relevant event. No isolated timing. Independent of ID 7.

## 10. Package scoped baseline profiles for the first-presentation path

**Root cause:** Libraries had no Stripe-specific baseline profiles for these
cold UI paths. Cold class verification and unoptimized execution remained costly
after unnecessary work was removed. Compose already supplies its own profile;
Stripe code needs complementary coverage.

**Files to add:**

- `paymentsheet/src/main/baseline-prof.txt`
- `payments-ui-core/src/main/baseline-prof.txt`
- `stripe-ui-core/src/main/baseline-prof.txt`

**Implementation:**

1. Prefer generating rules from a representative first-presentation workload
   when that infrastructure is available. The current implementation uses scoped,
   manually curated class and method rules; do not claim they are generated.
2. Cover PaymentSheet activity/ViewModel, form factory/helper/interactor,
   horizontal screen, bottom sheet, primary button, Link UI, and field UI paths.
   In `payments-ui-core`, cover FormUI and billing/card controllers. In
   `stripe-ui-core`, cover address/country/dropdown, section/row field UI,
   text-field controllers/layout, bottom sheet, and StateFlow helpers.
3. Use owning-module classes only, including nested/generated callbacks where
   needed. Current rule shape is `L<internal-class-name>**;` and
   `HSPL<internal-class-name>**->**(**)**`. Keep coverage tied to presentation;
   do not replace it with a whole-SDK/package wildcard. Current files provide
   the exact 44 class-family rules across the three modules.
4. Verify AGP packages the profiles and R8 retains/remaps them. A source file
   existing in the tree does not prove usable compilation coverage.

**Independent validation:** Build the three modules' `bundleReleaseAar` tasks
and `:paymentsheet-example:assembleBaseRelease`. Inspect each AAR for its source
profile and the minified APK for `assets/dexopt/baseline.prof` and
`baseline.profm`. Match rule families to module classes and inspect R8 output.
For performance, use repeated fresh-process release launches, explicitly verify
package compilation state, and compare profiles enabled/disabled. See
`paymentsheet/PERFORMANCE.md` for the maintenance procedure.

**Impact:** Potentially high for cold release managed-code work; installation
and ART compilation determine the benefit. No isolated profile speedup was
established. Debug verification and native Chromium startup remain. Independent
of the other fixes, though generated profiles should match the final code.

## 11. Display dropdown values without constructing a text editor

**Root cause:** The large dropdown label used a disabled `CompatTextField`,
creating `BasicTextField` editor machinery for text that cannot be edited.

**Production files:**

- `stripe-ui-core/src/main/java/com/stripe/android/uicore/elements/DropdownFieldUI.kt`
- `stripe-ui-core/src/main/java/com/stripe/android/uicore/elements/compat/CompatTextField.kt`

**Implementation:** Add an internal `CompatTextFieldDisplay` composable using
static `Text` inside the existing `InsetDecorationBox`. Preserve label, trailing
icon, content padding, text style/color fallback, disabled decoration behavior,
error semantics, and indicator line. Use it in `LargeDropdownLabel` instead of
the disabled editor. Leave actual editable fields unchanged. Retain the dropdown
container's click, selection, accessibility, and autofill behavior.

**Independent validation:** Extend
`stripe-ui-core/src/test/java/com/stripe/android/uicore/elements/DropdownFieldUITest.kt`
to assert no editable/set-text node and verify opening/selecting an item.
Retain autofill tests. Verify existing dropdown and horizontal PaymentSheet
screenshots without updating baselines; five dropdown and six initial horizontal
screen snapshots matched during this investigation.

**Impact:** Medium for each affected dropdown, especially during cold text-field
initialization. No isolated timing. Independent of ID 12, despite the shared file.

## 12. Subscribe to the dropdown selected index once

**Root cause:** `DropDown` collected `controller.selectedIndex` twice, once for
the selected index and again for the selected label's remember key.

**Production file:**
`stripe-ui-core/src/main/java/com/stripe/android/uicore/elements/DropdownFieldUI.kt`.

**Implementation:** Retain one `selectedIndex` collection. Remove the duplicate
`selectedItemIndex` collection and derive the label with
`remember(controller, selectedIndex) { controller.getSelectedItemLabel(selectedIndex) }`.
Include the controller key so replacing a controller at the same index refreshes
the label. Preserve the selected-state and menu behavior.

**Independent validation:** Use `DropdownFieldUITest` in the matching test
package to verify label updates after selection and when the controller changes.
Inspect or instrument subscriptions to confirm one selected-index collection.

**Impact:** Small; removes one collection/effect per dropdown rather than half
of all dropdown work. No measured millisecond saving. Does not require ID 11.

## 13. Update row dividers in layout without recomposing the fields

**Root cause:** `RowElementUI` wrote measured field height in `onSizeChanged`
and read it through `.height(...)` during composition. First measurement then
invalidated the row's composition, including its text fields. Conversion also
used system display density rather than local layout units.

**Production file:**
`stripe-ui-core/src/main/java/com/stripe/android/uicore/elements/RowElementUI.kt`.

**Implementation:** Store measured height as pixels with `mutableIntStateOf(0)`.
Read that state inside the divider's `Modifier.layout` measurement block rather
than composition. Clamp to incoming height constraints, measure the divider at
the resulting fixed height, and place it normally. Preserve divider width,
color, field weights, visibility filtering, and the existing height-update rule.
Do not add an intrinsic-measure pass just to size the divider.

**Independent validation:** Verify the existing multi-field row/horizontal form
screenshots, including hidden fields and density changes. Inspect composition
tracing to confirm the first size update invalidates layout without recomposing
the row's field subtree. No dedicated recomposition-count test was added in this
investigation.

**Impact:** Small to medium for multi-field rows; avoids a size-induced
composition invalidation, not all subsequent layout work. No isolated timing.
Independent task.

## 14. Construct debug StateFlow assertion stacks only on failure

**Root cause:** The custom `collectAsState` helper constructed and remembered
an `AssertionError` per StateFlow subscription in debug builds, solely to retain
a composition-site stack in case an off-main update occurred later. Forms have
many subscriptions, making successful initialization pay repeated stack walks.

**Production file:**
`stripe-ui-core/src/main/java/com/stripe/android/uicore/utils/StateFlowsCompose.kt`.

**Implementation:** Remove the eagerly remembered exception. Keep the existing
debug main-thread check in the collector, and construct/throw `AssertionError`
only when it fails. Include the flow class in the error message for attribution.
Preserve initial-value production and explicit coroutine-context behavior.

**Independent validation:** Run the existing StateFlow Compose helper tests;
verify ordinary Main collection and the off-main assertion path. Confirm the
normal debug path allocates no assertion stack per subscription.

**Impact:** Medium in debug forms with many fields/subscriptions; approximately
zero release benefit because release already skipped the eager exception.
The diagnostic tradeoff is loss of the original composition-site stack; the
failure still carries its failure-site stack and flow class. Independent task.

## 15. Derive save preference from the same single form-value snapshot

**Root cause:** `FormViewModel` invoked/collected the complete field-value graph
once for values and again to derive save-for-future-use preference. Combining
these branches could also temporarily pair values with preference from a
different update.

**Production files:**

- `paymentsheet/src/main/java/com/stripe/android/paymentsheet/forms/FormViewModel.kt`
- `paymentsheet/src/main/java/com/stripe/android/paymentsheet/forms/CompleteFormFieldValueFilter.kt`

**Implementation:**

1. Remove the separate `userRequestedReuse` flow in `FormViewModel` and that
   constructor argument from `CompleteFormFieldValueFilter`.
2. Combine the current field-value map with hidden identifiers once. Derive
   preference from `idFieldSnapshotMap[FormFieldId.SaveForFutureUse]` in that
   same combine callback: absent → `NoRequest`, true → `RequestReuse`, otherwise
   → `RequestNoReuse`, preserving existing Boolean conversion semantics.
3. Derive this from the unfiltered snapshot before hiding identifiers or applying
   default values. A hidden save field must still affect preference; a default
   value must not invent a user's explicit save request.
4. Pass that preference into the existing completion/filtering function. Preserve
   validation, default filling, and hidden-field behavior. Update direct callers
   of the changed internal constructor.

**Independent validation:** Use `FormViewModelTest` and
`CompleteFormFieldValueFilterTest` in
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/forms/`. Cover one
upstream field-value subscription, true/false updates, hidden save preference,
absent save field, defaults, incomplete values, and hidden identifiers.

**Impact:** Small to medium depending on graph size: removes one of two
complete-value subscriptions, not half the entire form's work. Applies to
subsequent field updates too. No isolated frame-time improvement established.
Independent task.

## 16. Await asynchronous WebView startup before passive CAPTCHA setup

**Root cause:** Passive CAPTCHA warmup creates a transparent activity and a
WebView during presentation. First WebView use synchronously initialized
Chromium on Main; later presentations reused the initialized process. Earlier
form tests without CAPTCHA did not exercise this cold path.

**Production files:**

- `payments-core/build.gradle`
- `payments-core/src/main/java/com/stripe/android/hcaptcha/HCaptchaWebViewStartup.kt` (new)
- `payments-core/src/main/java/com/stripe/android/hcaptcha/DefaultHCaptchaService.kt`
- `payments-core/src/main/java/com/stripe/android/hcaptcha/HCaptchaModule.kt`

**Implementation:**

1. Add `implementation libs.androidx.webkit` to payments-core, using the existing
   WebKit 1.15 dependency. Its asynchronous startup API is experimental; WebKit
   1.16's stable replacement requires minSdk 24 while this SDK supports 23.
2. Add an internal suspending startup wrapper around
   `WebViewCompat.startUpWebView(context, WebViewStartUpConfig.Builder(executor).build(), callback)`.
   Opt into `WebViewCompat.ExperimentalAsyncStartUp` locally.
3. Supply an executor backed by `Dispatchers.IO.asExecutor()`. Wrap execution of
   the provider's Runnable so initialization exceptions resume the suspended
   caller with failure. WebKit 1.15's completion callback has no failure method.
   Resume normally on startup completion; ignore callbacks after cancellation
   or completion. Use application context to avoid retaining the activity.
4. Inject a required suspend `prepareWebView: (FragmentActivity) -> Unit` into
   `DefaultHCaptchaService`. Inside the existing `runCatching` in
   `performPassiveHCaptchaHelper`, await it immediately before `startVerification`.
   Both warmup and direct confirmation must use this helper.
5. Wire the hook to the startup wrapper from `HCaptchaModule` without changing
   its externally visible provider signature. Startup failures must enter the
   existing CAPTCHA failure/reporting path.
6. Preserve the existing cached Loading state before warmup awaits startup, so
   confirmation waits for/reuses that warmup's result. Preserve challenge
   configuration, token lifetime, reset, timeout, and verification behavior.
   Do not probe WebView features before asynchronous startup, construct a
   WebView on a worker, or disable CAPTCHA to hide the stall.

**Independent validation:** In
`payments-core/src/test/java/com/stripe/android/hcaptcha/DefaultHCaptchaServiceTest.kt`,
inject a controllable startup gate. Verify no CAPTCHA setup before completion,
confirmation waits for pending warmup and reuses its token, and startup failure
returns a CAPTCHA failure without constructing its view. Run the service,
`PassiveChallengeWarmerActivityTest`, `PassiveChallengeWarmerViewModelTest`, and
`DefaultPassiveChallengeWarmerTest`; 34 selected tests passed here. Payments-core
detekt and the minified example release build also passed.

Run the existing managed-device `HCaptchaTokenTest` confirmation cases to check
real token forwarding. In this environment `ApiConfigurationOnly` passed and
`PaymentConfigurationOnly` failed because `js.hcaptcha.com` could not resolve.
Two changed-path runs and a temporary synchronous baseline reproduced that DNS
failure. The async source was restored exactly afterward. This check is partially
blocked, not fully passing; leave the test infrastructure unchanged.

**Impact:** High for cold CAPTCHA-enabled presentation. In separate standalone
profile-compiled release captures, warmer activity creation was 241.79 ms before
and 2.70 ms after. Afterward, provider initialization took 21.31 ms on a worker
and later `WebViewChromium.init` took 20.16 ms. These are single captures with
cache/host differences, not a controlled benchmark distribution or proof that
239 ms of total main-thread work disappeared. The old emulator WebView still
ran a 119.68 ms native `startChromiumLocked` task on Main, spanning roughly seven
60 Hz refresh intervals. This fix cannot claim an entirely smooth transition.
Independent of all other fixes and irrelevant when CAPTCHA/WebView is unused.

## Common acceptance and measurement procedure

Each implementation should include its necessary constructor/call-site updates,
not require another task's refactor to compile, and preserve public API behavior.
IDs 5/6 and 11/12 share files but can be implemented separately. Combining tasks
requires resolving those overlapping edits; it does not create a behavioral
dependency. IDs 1–4 and 10 have overlapping potential benefits.

For functional validation, run the focused module unit tests and relevant
static analysis. Follow `delegate-low-reasoning-work` before routine Gradle work
and the repository testing skills before adding tests. Production UI changes
should retain existing screenshots and accessibility/autofill behavior.

For managed instrumentation, use `pixel2api33DebugAndroidTest` in the owning
module, or `pixel2api33imeDebugAndroidTest` for an IME-specific test. Do not fall
back to the user's running emulator. For filtered runs, use task-scoped `--rerun`
if needed so a prior cached task result cannot masquerade as a fresh check.
Do not change test clocks, Mockito/Terminal setup, fixture responses, or retries
as part of these production fixes.

For an isolated performance estimate, compare only one task enabled/disabled on
the same release build/device/configuration. Restart the process for each cold
presentation, keep installation/profile compilation state consistent, record
WebView version and CAPTCHA configuration, and collect multiple runs. Measure
loading-to-horizontal time, app frame deadlines, and the affected main-thread
scope. Report a distribution and separate time moved to loading or later
interactions from work eliminated. Record debug separately. The prior captures
do not justify claiming release became worse overall or that all jank is fixed.

The main remaining measured issue is old-provider native Chromium startup on
Main. Changing warmup timing/policy or relying on a newer provider is follow-up
work, not an implemented fix in this document. Verify on representative physical
devices before assigning that work an expected frame-time benefit.
