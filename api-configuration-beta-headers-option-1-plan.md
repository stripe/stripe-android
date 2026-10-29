# Android beta headers — option 1: explicit configuration forwarding

## Decision

Implement the smallest design change: retain `ApiRequest.Options` exactly as the authentication/idempotency carrier, pass `ApiConfiguration.State` through the configured payment-flow calls, and derive extra beta tokens at request construction. Reuse the existing API-version/header machinery. Do not add a new request-context abstraction, rename repositories, remove options, or add a global network interceptor.

This is the preferred option when the immediate priority is shipping the review's preview API with the least change to authentication semantics. It still requires auditing every request-producing path; adding a constructor beta set to StripeApiRepository alone is not sufficient.

| Alternative | Advantage | Cost |
| --- | --- | --- |
| 1: forward state alongside existing options | Simple data flow; credential behavior stays intact | Parallel arguments and overloads; some duplicate key/account data |
| 2: remove options | MPE-facing methods speak in session configuration | Generic credentials/idempotency still need explicit representation; broad migration |
| 3: extend options with flags | Small central header change; existing request methods carry flags naturally | Must audit every options construction and credential replacement |

## Shared requirements and decisions

Reference: [Mobile API Review: Beta Headers for MPE](https://docs.google.com/document/d/1i5jhlsr_ki0OqFmlDVlBdg5nI-whDVwQfd6cyOLHD6w/edit?tab=t.0), read on 2026-10-14. The document proposes a public-preview `ApiConfiguration.betas(Set<String>)` API for PaymentSheet, FlowController, and EmbeddedPaymentElement. It calls out Vipps as the motivating integration, arbitrary strings rather than a maintained enum, and a minor SDK release. The review outcome is not filled in; this plan does not claim API approval.

All three alternatives start from `refactor/next-action-api-configuration` at `a7447f8ec14b7205e22f09fff05806d7214fdbb7`. They are competing designs, not a sequence of implementations. This branch now includes the implementation described below.

The requested behavior is: every Stripe API request belonging to a payment flow with an explicit configuration receives that configuration's beta flags. This includes auxiliary requests, credential overrides, authentication, retries, and activities launched by the flow. It does not mean a process-wide beta setting: a different payment flow must retain its own flags, and a flow with no flags must not inherit another flow's flags. Non-payment-element public integrations remain outside the API review's product scope, but shared networking must keep working for those integrations.

“Beta headers” are tokens in one `Stripe-Version` HTTP header, not separate HTTP header names. For example, the existing formatter produces `2020-03-02;test_beta_header=v1`. Preserve each request producer's base API version and built-in betas. Union configured flags with built-in flags, deduplicate exact strings, and use deterministic ordering for added flags. With an empty configured set, preserve the existing header exactly. Do not replace a pinned Financial Connections or consumer API version with a generic SDK version. Do not invent enum entries or strip the `=v1` portion. Different strings with the same beta name are not automatically equivalent; conflicting-version validation would need a documented policy, rather than silently overriding a token.

Do not attach this API negotiation header to analytics (`q.stripe.com`), fraud telemetry (`m.stripe.com`), Google APIs, bank websites, or browser/ACS requests. Those are not Stripe API calls governed by this configuration. Include authenticated Stripe API requests on secondary hosts when the configured flow uses them; tests must explicitly track those hosts.

## What the current code establishes

| Source | Finding and consequence |
| --- | --- |
| [ApiConfiguration.kt](stripe-core/src/main/java/com/stripe/android/core/ApiConfiguration.kt) | The preview builder and restricted, parcelable `State` currently contain only the publishable key and account. Add strings to both; keep `State` and `build()` restricted. |
| [StripeApiRepository.kt](payments-core/src/main/java/com/stripe/android/networking/StripeApiRepository.kt) | Existing constructor `betas: Set<StripeApiBeta>` is formatted through `ApiVersion` into an `ApiRequest.Factory` at repository construction. The injected constructor receives a state provider but forwards only its publishable key to the primary constructor. Configuring this constructor alone misses other API producers and per-call configurations. |
| [ApiVersion.kt](stripe-core/src/main/java/com/stripe/android/core/ApiVersion.kt) | Already formats a set of strings as semicolon-delimited beta tokens after the API version. Reuse/extend this formatting rather than creating a second convention. |
| [ApiRequest.kt](stripe-core/src/main/java/com/stripe/android/core/networking/ApiRequest.kt) and [RequestHeadersFactory.kt](stripe-core/src/main/java/com/stripe/android/core/networking/RequestHeadersFactory.kt) | GET/POST/DELETE share factory/header code. `BaseApiHeadersFactory` also serves uploads; this is the common formatting point. Headers are materialized on request construction, so snapshot the flags before constructing the request. |
| [FileUploadRequest.kt](stripe-core/src/main/java/com/stripe/android/core/networking/FileUploadRequest.kt) | Does not currently receive `StripeApiRepository`'s constructor API version. A repository-only change would not cover uploads. Preserve legacy upload behavior when no new configuration is supplied; cover configured uploads explicitly. |
| [ElementsSessionRepository.kt](paymentsheet/src/main/java/com/stripe/android/paymentsheet/repositories/ElementsSessionRepository.kt), [CheckoutSessionRepository.kt](paymentsheet/src/main/java/com/stripe/android/paymentsheet/repositories/CheckoutSessionRepository.kt), [ConsumersApiService.kt](payments-model/src/main/java/com/stripe/android/repository/ConsumersApiService.kt) | Each constructs its own request factory. Initial elements-session loading happens before payment-method metadata is available; obtaining flags only from a later DI binding is too late. |
| [CustomerApiRepository.kt](paymentsheet/src/main/java/com/stripe/android/paymentsheet/repositories/CustomerApiRepository.kt) and [LinkApiRepository.kt](paymentsheet/src/main/java/com/stripe/android/link/repositories/LinkApiRepository.kt) | Customer calls use ephemeral credentials. Link can use a custom API key with a null account. Session betas must survive those authentication changes. |
| [DefaultCardAccountRangeRepositoryFactory.kt](payments-core/src/main/java/com/stripe/android/cards/DefaultCardAccountRangeRepositoryFactory.kt) | Its remote source constructs a separate `StripeApiRepository` and options from just a key. `getCardMetadata` intentionally clears the account. Carry flags without changing that existing account policy. |
| [Stripe3ds2TransactionViewModel.kt](payments-core/src/main/java/com/stripe/android/payments/core/authentication/threeds2/Stripe3ds2TransactionViewModel.kt) and [Stripe3ds2ChallengeResultProcessor.kt](payments-core/src/main/java/com/stripe/android/payments/core/authentication/threeds2/Stripe3ds2ChallengeResultProcessor.kt) | 3DS2 can substitute a next-action key; challenge completion reconstructs options from result key/account fields. Those reconstruction points must preserve the originating configuration's flags. |
| [ApiConfigurationFromPaymentConfigurationModule.kt](payments-core/src/main/java/com/stripe/android/payments/core/injection/ApiConfigurationFromPaymentConfigurationModule.kt) | Contains both the legacy configuration adapter and `ApiRequestOptionsModule`. Explicitly choose empty flags for the legacy adapter; extend the configuration-to-request conversion for the selected design. |

### Public configuration and prerequisite refactors, before header delivery

1. Build on/merge the next-action state refactor (#15001). Its polling and authentication contracts already carry `ApiConfiguration.State`; do not reverse that change. Voucher tests are merged into master, and the polling prerequisite is #15005. Coordinate matching SDK module versions because restricted JVM signatures and parcel shapes can change.
2. Add `ApiConfiguration.betas(betas: Set<String>)`, an empty builder default, and `betas: Set<String> = emptySet()` on internal `State`. Defensively copy the supplied set when setting/building; mutations of the caller's set or later builder reuse must not alter an already configured flow. State construction can omit flags when none are configured; child flows explicitly forward inherited flags.
3. Fix reconstruction/copy points before enabling headers: `PaymentSheet.Configuration.newBuilder()`, `EmbeddedPaymentElement.Configuration.newBuilder()`, and `ApiConfigurationTestType.withPublishableKey()` currently rebuild a configuration from only key/account. Preserve flags in these methods, configuration/common-model conversions, metadata, resolver output, and parcelled activity state. `DefaultApiConfigurationResolver` must preserve explicit state and give the PaymentConfiguration fallback an empty set.
4. Expose `PaymentSheet.Configuration.Builder.apiConfiguration(...)` with `@ApiConfigurationPreview` instead of its current library-group restriction. FlowController uses that configuration; EmbeddedPaymentElement already exposes its preview setter. Extend KDoc and integration examples. Keep explicit annotation opt-ins in example applications; SDK/testing modules may use the established Gradle opt-ins. Update `stripe-core/api/stripe-core.api` and `paymentsheet/api/paymentsheet.api`. The planned changelog is the review's `[ADDED]` public-preview support for all three payment elements.
5. Audit and fix configuration loss in payment-flow subgraphs before claiming “all requests”: card metadata, Google Pay payment-method creation, address autocomplete, ACH/Financial Connections, Link, customer operations, Tap to Add, and 3DS2 completion. Use existing state-bearing internal contracts; no new standalone Financial Connections/Identity public beta API is required. For Financial Connections retain the built-in `financial_connections_client_api_beta=v1` token. Distinguish standalone legacy entry points, which explicitly start with empty configured flags, from child flows that inherit state.

Also preserve flags when Link's `WalletViewModel.presentAddBankAccount()` constructs a FinancialConnectionsSheetConfiguration with the consumer publishable key and null account. `StripePaymentLauncher.handleNextActionForHashedPaymentIntent()` / `PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs` currently reconstruct configuration from a decoded key and a separately passed account; add an explicit originating beta snapshot to that internal boundary. Neither key replacement nor hashed next-action handling should strip session policy.

Tap to Add delegates retrieve/collect/confirm to a separate Terminal SDK, in addition to making owned StripeRepository update/retrieve calls. The latter are covered by the transport design. Before claiming beta delivery for Terminal-owned API calls, inspect its supported configuration/header extension and add a testable propagation hook if available. If the Terminal SDK cannot accept these flags, document that concrete limitation and resolve the supported combination with the API owners before claiming universal coverage for that subflow. Do not suggest that changing android ApiRequest will affect a separate SDK's HTTP requests.

Audit object lifetime as well as fields: DefaultCardAccountRangeRepositoryFactory lazily caches its repository, and injected clients can outlive a configure call. Either scope a client/remote source to a configuration snapshot or rebuild/read per-operation options when configuration changes. Do not freeze the first set of flags in a long-lived factory. Where response caches depend on beta-enabled response shape, partition or invalidate them by the relevant configuration instead of assuming a BIN-only cache represents every beta session. Add a reconfiguration test using a previously initialized auxiliary client.

These prerequisites can be separate reviewable refactor/test PRs. Preserve credentials, null-account semantics, idempotency, and existing API versions while refactoring; header behavior can then land as a focused follow-up.

## Request-producer completion checklist

The implementation should attach an owner and a passing HTTP assertion to every row. Run an additional constructor/factory search at implementation time; this table records the inspected base, not a permanently exhaustive inventory.

| Request family | Configuration/credential boundary to preserve | HTTP proof |
| --- | --- | --- |
| Initial elements session and fallback | Per-call state in ElementsSessionRepository before metadata/DI readiness | Initial load, failed load fallback, and later reconfiguration |
| Intent/payment method/confirmation token/confirm/refresh/cancel | PaymentLauncher/StripeRepository with operation idempotency | Payment and setup paths; requests before and after next action |
| Checkout-session API | Its own request factory and options provider | Init, confirm, and update |
| Customer saved methods, update/detach and Tap to Add's owned API work | Ephemeral key plus originating merchant policy | Fixture ephemeral bearer and correct account plus all beta tokens |
| Link consumer operations | ConsumersApiService's own factory; custom consumer key and null account | Lookup and payment details, followed by confirmation |
| Card metadata / autocomplete / promotions | Separate repository/factory or key-only reconstruction | Beta tokens on auxiliary calls; intentional metadata account removal preserved |
| Google Pay creation and bank-account/Financial Connections | Internal activity/launcher state, direct options providers, FC built-in beta | Payment-method creation and FC create/attach/verification API calls |
| Polling, 3DS2 initialization/completion, hashed next action | Activity state and result/key reconstruction | Real polling request, overridden 3DS2 key, completion retries, and hashed contract propagation |
| Files and secondary authenticated Stripe API hosts | Upload/shared header path; host-specific base versions and auth | Explicit host tracking plus auth/account/version expectations |
| Telemetry, external websites/SDK transport | Separate protocol or transport ownership | No Stripe-Version injection into analytics/fraud/browser calls; explicit review of delegated Terminal API support |

## Existing network validation and the extension to build first

[NetworkRule](network-testing/src/main/java/com/stripe/android/networktesting/NetworkRule.kt) adds its default matcher to every `enqueue`/`enqueueRaw` call unless explicitly disabled. [RequestMatchers.stripeApiKey](network-testing/src/main/java/com/stripe/android/networktesting/RequestMatchers.kt) checks bearer publishable/live/ephemeral keys and `Stripe-Account` on `api.stripe.com`; it determines ephemeral-key requests from method/path/body. It checks the analytics publishable-key query on `q.stripe.com`, and currently returns true for other hosts. It does **not** validate `Stripe-Version`.

Reuse this validation; do not replace it with a beta matcher and do not set `applyDefaultAuthorization = false` to make beta tests pass. JVM tests use module-local `ApiKeyFixtures`; instrumentation uses `TestApiKeys`. [PollingComponentTest](paymentsheet/src/test/java/com/stripe/android/paymentsheet/paymentdatacollection/polling/PollingComponentTest.kt) already verifies a real Dagger-to-network path using the fixture key/account and default authorization validation. [PaymentSheetTest](paymentsheet/src/androidTest/java/com/stripe/android/paymentsheet/PaymentSheetTest.kt), [PaymentSheetDeferredTest](paymentsheet/src/androidTest/java/com/stripe/android/paymentsheet/PaymentSheetDeferredTest.kt), [FlowControllerTest](paymentsheet/src/androidTest/java/com/stripe/android/paymentsheet/FlowControllerTest.kt), and [EmbeddedPaymentElementTest](paymentsheet/src/androidTest/java/com/stripe/android/paymentelement/EmbeddedPaymentElementTest.kt) already exercise real payment flows. [ApiConfigurationTestType](paymentsheet/src/androidTest/java/com/stripe/android/paymentsheet/utils/ApiConfigurationTestType.kt) covers legacy global configuration, explicit configuration, and (for embedded tests) overriding unrelated global credentials.

Add an opt-in per-rule expected-version matcher so **every** queued API request in a beta flow is checked, including unexpected auxiliary requests. Prefer an explicit expected complete version or a per-host policy that validates the base version and exact token set, rather than accepting any header merely containing one beta. A proposed testing API is `NetworkRule(expectedStripeVersions = ...)`: add it to both enqueue variants alongside the existing authorization matcher, restricted to the configured Stripe API hosts. Default null expectation leaves existing tests unchanged. For Financial Connections use its built-in beta plus configured flags; for the normal API use the normal version plus configured flags. Secondary API hosts need explicit tracking and their own credential matcher because the current default does not validate them. Intentional null-account/key overrides need precise expectations rather than bypassing authorization: extend the default credential matcher with explicit per-route expectations (including nullable account support) when introducing the testing API. Keep the current fixture policy as its default; a route-specific expected key/account is still authorization validation, not permission to omit it. Test the testing helper itself against missing flags, extra flags, wrong version, wrong key, and wrong account; its purpose is to catch lost flags anywhere in the flow.

Add focused beta variants, rather than multiplying every existing instrumentation case. Keep an empty-flags control. Preserve flags when `ApiConfigurationTestType.withPublishableKey()` selects the live fixture. Use real repository/network tests for plumbing and unit tests for formatter/credential policy; fake-repository unit tests alone cannot establish HTTP delivery.

## Option-specific design

```text
Payment element configuration / fallback resolver
  -> immutable ApiConfiguration.State(key, account, betas)
  -> configured repository / launcher operation(state, existing options)
  -> ApiRequest.Factory configured overload(state, existing options)
  -> request's copied beta set
  -> BaseApiHeadersFactory(base API version + configured beta tokens)
```

`ApiRequest.Options` remains authoritative for the actual bearer key, account header, and idempotency key. State supplies session policy (betas); its merchant key must not override a customer's ephemeral key or a 3DS2/Link key. This apparent duplication is deliberate in this option and must be stated in naming/KDoc/tests.

Use configuration-aware overloads on `ApiRequest.Factory.createGet/createPost/createDelete` and request-producing repository methods. New overloads require state explicitly. Existing legacy overloads delegate to empty additional beta tokens and retain constructor-level legacy `StripeApiBeta` handling. Do not make configuration optional in payment-flow call sites: they already have resolved state. At the low-level request/header boundary pass just the copied `Set<String>` needed for formatting, rather than teaching every header class how to choose between two credentials objects.

Introduce one formatter in `ApiVersion` (or a small adjacent internal helper), accepting the existing version string and additional beta strings. If additional flags are empty, return the version unchanged. Otherwise preserve the original version and built-in tokens, union exact strings, and append additional tokens deterministically. Apply it in `BaseApiHeadersFactory`, shared by normal and multipart API headers. Extend `ApiRequest`/`FileUploadRequest` internal construction so both can supply the additional set. Do not set the final header in `executeRequest` or `makeApiRequest`: retries and callers using other repositories must observe the same request snapshot.

## Implementation sequence after shared prerequisites

1. **Baseline and formatter PR.** Extend `ApiVersionTest` beyond its existing single-beta case, and add targeted normal/upload header tests for version preservation and union. Establish fixtures for configured flags in the network test utilities. Keep existing constructor enum behavior covered. Add no production beta behavior until a failing focused test identifies the boundary.
2. **Transport support.** Add the configured request-factory paths and explicit beta-set inputs to ApiRequest/FileUploadRequest/header formatting. Preserve the old no-configuration paths through delegating overloads; new internal parameters are explicit. For files, forward the configuration's flags and the appropriate upload base version; do not accidentally change legacy repository enum behavior that currently does not reach FileUploadRequest.
3. **Main repository and initial loading.** Thread state into the MPE-used StripeRepository/StripeApiRepository operations without changing their options semantics. Cover payment/setup retrieval, payment-method/confirmation-token creation, confirmation, refresh/cancellation, customer operations, financial-connections creation/attachment/verification, and authentication completion. ElementsSessionRepository receives state per `get()` call: pass that exact state to its factory and to fallback retrieval. Do not capture it once when the repository is built. Update fakes and callers alongside each signature change.
4. **Other factories.** Forward state into ConsumersApiService operations called by Link, CheckoutSessionRepository, and autocomplete, and pass it into separately constructed card-range repositories. For provider-based repositories read configuration once when starting the operation and construct a fixed request; do not let a provider change a suspended operation's flags. Keep each producer's existing API version and authentication policy.
5. **Subflows.** Forward state through Google Pay payment-method creation, collect-bank-account domain methods/launchers, Financial Connections internal configuration, Tap to Add API work, polling, and 3DS2. The next-action refactor already transports state for polling. For 3DS2 initial and completion calls pass the originating state for betas even when the actual options use next-action/result credentials; obtain that state from the transaction's DI graph for completion rather than modifying the external challenge-result schema. Keep null-account overrides intact.
6. **End-to-end verification and publication.** Enable the explicit-state API on all three entry points, update generated dumps/docs/examples/changelog, run the matrix below and the shared validation gates. Audit remaining legacy overload calls inside payment-flow source before declaring completion.

Refactors to do first are state preservation, the per-operation configuration path into initially loaded/shared repositories, and the auxiliary factories that currently accept only a key. Removing options, global configuration storage, and broad analytics refactoring are not prerequisites.

## Tests specific to option 1

| Layer | Scenarios |
| --- | --- |
| Core formatter and headers | Empty/one/multiple flags; exact-string deduplication against enum/built-in flags; deterministic result; custom base version preserved; GET/POST/DELETE and multipart agree; request snapshot remains unchanged after input mutation. |
| StripeApiRepository | Capture an actual ApiRequest for retrieve, confirm, delete, and upload; assert the complete version plus existing auth/account/idempotency. Legacy Stripe(enum betas) still behaves as before; configured flags do not replace enum flags where both are supported. |
| Credential split | State uses fixture merchant key; options use fixture ephemeral key/custom key and explicit account/null. Header uses options credentials and state flags. This proves we are not accidentally using state's publishable key as authorization. |
| Initial/fallback loading | ElementsSessionRepository includes flags in initial request and fallback intent retrieval. Test reconfiguration from beta set A to set B and then empty using the same shared repository. |
| Existing polling integration | Extend PollingComponentTest with flagged state; default authorization validates fixture key/account and the version matcher validates every polling request. Preserve independent IO/test schedulers and ViewModelStoreTestRule. |
| Authentication | 3DS2 next-action key null/empty/custom plus challenge-completion result reconstruction and retry. Verify credentials retain existing precedence while all requests keep session flags. Voucher delegation tests assert full state forwarding after its extra field is added. |
| Payment flows | Focused PaymentSheet card flow, FlowController configure/select/confirm, embedded configure/update/confirm, customer ephemeral calls, Link lookup/payment details, Google Pay payment-method creation, and bank-account/FC flow. All API mocks add version expectations while keeping default key/account checks. |
| Isolation and exclusions | Two concurrently configured flows with different flags; reconfigure flags off; activity recreation preserves the state; analytics/fraud/browser requests do not receive Stripe API beta negotiation. Files/secondary API hosts have explicit host/auth/version assertions. |

## Pros, cons, and recommendation

Pros: no new authentication model; easy to explain and review; existing request options remain intact; full state stays available for future session fields; use of existing formatter and factory avoids per-endpoint header strings.

Cons: methods can take both state and options, and the relationship must remain explicit. Many shared repository signatures gain overloads or parameters. A missed legacy overload silently produces no configured flags, so the all-request network matcher and request-producer audit are necessary. It is not sufficient to bind betas at repository construction because repositories can serve later configurations or custom keys.

Choose this option for a straightforward first implementation if the extra argument plumbing is acceptable. Option 3 is a stronger long-term representation of a complete request. A repository-only constructor tweak is a possible smaller spike, but it cannot meet the stated “all API requests” acceptance condition and is not a release plan.

## Validation and release gates for the implementation

These are the implementation validation gates. See the PR description for checks actually run. Invoke repository testing skills before writing tests and delegate routine Gradle commands according to `AGENTS.md`.

Start with focused tests in the touched packages; then run affected-module suites and checks:

```sh
./gradlew :stripe-core:testDebugUnitTest :payments-model:testDebugUnitTest \
  :payments-core:testDebugUnitTest :paymentsheet:testDebugUnitTest \
  :wechatpay:testDebugUnitTest :network-testing:testDebugUnitTest --continue
./gradlew :stripe-core:apiDump :paymentsheet:apiDump
./gradlew :stripe-core:apiCheck :paymentsheet:apiCheck \
  :payments-core:apiCheck :wechatpay:apiCheck \
  :stripe-core:detekt :payments-core:detekt :paymentsheet:detekt \
  :wechatpay:detekt :network-testing:detekt --continue
./gradlew :paymentsheet:compileDebugAndroidTestKotlin
```

Also compile/run relevant tests, Detekt, and API checks for the touched Financial Connections, Google Pay/card metadata, payments-ui-core, and other shared-networking modules; option 2 requires all migrated modules. Compile example flavors affected by the new preview setter and sample code with explicit source opt-ins. Review generated API changes and the changelog. A passing API check does not guarantee compatibility for signatures excluded by `@RestrictTo`.

Run the selected payment-flow instrumentation tests using `./gradlew :paymentsheet:pixel2api33DebugAndroidTest` with an instrumentation runner class filter for the focused beta cases. The `pixel2api33` managed device is registered in `build-configuration/android-library.gradle`; the flow tests listed above live in `paymentsheet`. Confirm task availability when executing; if unavailable/failing, report the issue instead of using a connected device or the user's emulator. Use `pixel2api33imeDebugAndroidTest` only for a scenario specifically needing the IME. Filter to focused beta cases before broadening the suite. No screenshots should change for header-only behavior.

Acceptance: empty-flags behavior is unchanged; explicit flags appear in all inventoried API requests; built-in betas/base versions remain correct; alternate keys/accounts and idempotency survive; retries preserve a fixed snapshot; reconfiguration/concurrent sessions do not leak flags; builder/state/activity round trips preserve flags; all three payment-element entry points work; standalone legacy integrations retain their behavior. Use a request-producer checklist as a release gate, not a claim based on one successful confirm request.
