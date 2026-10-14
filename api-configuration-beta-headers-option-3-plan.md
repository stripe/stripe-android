# Android beta headers — option 3: carry betas in request options

## Decision

Keep `ApiRequest.Options` as an immutable snapshot of the actual request authentication/idempotency and extend it with `betas: Set<String>`. Convert the originating `ApiConfiguration.State` to options at a small, explicit set of boundaries. All existing request factories and repositories then carry beta flags with the request they already construct. Format the union of options flags and producer API-version tokens in the common header factory.

This is the recommended complete implementation. It preserves the distinction between merchant/session configuration and effective credentials, fits existing ephemeral-key/idempotency behavior, and makes flags inspectable in repository and network tests. It avoids passing competing state/options arguments throughout generic networking and avoids removing an abstraction still needed by legacy consumers.

| Alternative | Main benefit | Main drawback |
| --- | --- | --- |
| 1: forward state alongside options | Small conceptual change; state visible at every call | More parallel arguments and legacy-overload mistakes |
| 2: replace options | Session-first MPE repository APIs | Generic credentials/idempotency still need types; broad migration |
| 3: state-derived options with flags | Existing GET/POST/DELETE/upload/consumer paths naturally carry flags | Every fresh options construction and override must be audited |

## Shared requirements and decisions

Reference: [Mobile API Review: Beta Headers for MPE](https://docs.google.com/document/d/1i5jhlsr_ki0OqFmlDVlBdg5nI-whDVwQfd6cyOLHD6w/edit?tab=t.0), read on 2026-10-14. The document proposes a public-preview `ApiConfiguration.betas(Set<String>)` API for PaymentSheet, FlowController, and EmbeddedPaymentElement. It calls out Vipps as the motivating integration, arbitrary strings rather than a maintained enum, and a minor SDK release. The review outcome is not filled in; this plan does not claim API approval.

All three alternatives start from `refactor/next-action-api-configuration` at `a7447f8ec14b7205e22f09fff05806d7214fdbb7`. They are competing designs, not a sequence of implementations. This PR changes documentation only.

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
2. Add `ApiConfiguration.betas(betas: Set<String>)`, an empty builder default, and a required `betas: Set<String>` field on internal `State`. Defensively copy the supplied set when setting/building; mutations of the caller's set or later builder reuse must not alter an already configured flow. New internal fields/parameters have no implicit defaults: construction sites explicitly choose inherited flags or `emptySet()`.
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

## Target data flow and invariants

```text
ApiConfiguration(...).betas(flags).build()
  -> resolved immutable State(key, account, copied flags)
  -> centralized request-options conversion
  -> Options(effective apiKey, effective account, idempotencyKey, copied flags)
  -> existing repositories and ApiRequest / FileUploadRequest
  -> BaseApiHeadersFactory: existing version + options.betas
```

Once options exist, they are the only per-request source for key/account/idempotency/betas. A factory may still own built-in tokens and a base version, but it must not look up a mutable process/session provider to obtain configured flags. A request retains its options snapshot across retries. Credentials can change without dropping flags: `copy` preserves fields not deliberately overridden.

Add a required beta-set field to the restricted Options model and update new internal construction sites explicitly. Prefer an explicit migration of internal calls rather than a new `betas = emptySet()` default that lets a missed MPE path silently discard configured flags. Preserve supported old construction signatures at compatibility boundaries through deliberate delegating adapters/overloads if required by the compatibility audit; those legacy adapters use empty configured flags. Do not turn every newly changed call into an empty-set adapter. Retain validation, key capabilities, parcel support, and masked `toString()`; beta metadata must not make credentials visible.

Create a single internal conversion next to ApiRequestOptionsModule (conceptual signatures):

- `State.toRequestOptions(idempotencyKey: String?)` derives normal session credentials and copies flags.
- An explicit credential-override conversion accepts `apiKey: String`, `stripeAccount: String?`, and `idempotencyKey: String?`, retaining `State.betas`.
- For an existing Options snapshot, use `copy(apiKey = ..., stripeAccount = ...)` when overriding identity. Explicit null account means no connected account; do not use a nullable parameter/default to mean both null override and inherit.

New internal helper parameters have no defaults. Prefer the ordinary helper for most paths, explicit overrides for customer/Link/3DS2/Tap to Add, and explicit empty configuration only at standalone legacy boundaries. Keep idempotency owned by the original operation; authentication options start with null where they do today.

## Refactors and implementation sequence

1. **Centralize conversions first, with no beta behavior change.** Replace repeated state-to-options construction in ApiRequestOptionsModule, ElementsSessionRepository, CustomerApiRepository, LinkApiRepository, PaymentMethodMessagingPromotionsHelper, GooglePayPaymentMethodLauncherViewModel, bank-account domain methods, TapToAddCollectionHandler and 3DS2. Add baseline tests for ordinary conversion, effective key/account overrides, explicit null account, and idempotency. This establishes a small auditable set of places to add the new field.
2. **Preserve full state through auxiliary boundaries.** DefaultCardAccountRangeRepositoryFactory must not reduce the state provider to only a key before creating a remote repository/options. Audit `createWithStripeRepository`, PaymentSheet/embedded builder reconstruction, state-carrying activities, and any key-only PaymentLauncher/bank-account/Google Pay path used by MPE. Keep standalone public adapters starting with empty flags. Bind the originating transaction state in DefaultStripe3ds2ChallengeResultProcessor so completion can derive options with result credentials while retaining flags; the external challenge result need not be changed.
3. **Add public/state/Options fields and formatter support together.** Implement the shared builder/public-preview API, required state/Options beta fields, defensive snapshots, conversion helpers, and deliberate construction-site updates. Update non-MPE internal Options constructors to explicit empty sets where there is no originating configuration. Extend ApiVersion's composition behavior and use it in BaseApiHeadersFactory to merge `options.betas` into the producer's existing version. This covers normal and file headers without changing GET/POST/DELETE repository signatures.
4. **Migrate every options producer.** Extend ApiRequestOptionsModule and the direct Financial Connections options provider. Ensure ElementsSessionRepository uses the state from its current call before the initial request and forwards the same options into fallback. Customer ephemeral calls, Link custom-key calls, 3DS2 start/transaction/completion, PaymentLauncher, polling, Tap to Add, bank-account domains, autocomplete, checkout, and card metadata all derive options with the flags. Audit `options.copy(stripeAccount = null)` in StripeApiRepository: preserving betas is desired, and account removal remains intentional.
5. **Preserve built-in/legacy beta handling.** StripeApiRepository's constructor enum betas and ConsumersApiServiceImpl's/Financial Connections' existing version strings remain factory inputs. At header construction union those tokens with options flags. No replacement of `StripeApiBeta` or public `Stripe(..., betas)` API is necessary. File uploads receive configured flags from options through the shared BaseApiHeadersFactory; legacy calls with empty options flags keep their current upload behavior.
6. **Make losses detectable.** Add the shared per-rule API-version expectation, fixtures, focused JVM integrations, and beta variants of existing instrumented flows. Search source for every fresh Options constructor and State constructor; classify it as inherited configuration, deliberate credential override retaining flags, or standalone empty configuration. Do the same for `copy` and parcel/newBuilder transformations.
7. **Release in reviewed stages.** Land baseline conversion/preservation tests first; then behavior and targeted integration coverage; then examples/API dumps/changelog/public setter exposure if split for review. Do not expose a setter that only affects confirm but misses initial loading. All three entry points must pass the all-request assertions before the beta feature is announced.

The configuration-to-options consolidation and lossless subflow forwarding are prerequisite refactors. Removing Options and changing generic repository interfaces are not needed. A separate analytics API overhaul is also not needed; preserve its existing identities without adding Stripe-Version to telemetry.

## Test plan specific to option 3

| Test boundary | Required scenarios and assertion |
| --- | --- |
| ApiConfiguration builder/State | Empty default, arbitrary strings, multiple flags, set replacement, defensive snapshot against mutable input/later builder use, explicit State construction, and focused configuration/activity recreation. Add a dedicated builder unit test if there is no current class. |
| Existing configuration tests | PaymentSheetConfigurationTest and EmbeddedPaymentElementConfigurationTest must include a non-empty beta set in newBuilder/common-configuration assertions; DefaultApiConfigurationResolverTest proves explicit state and empty fallback. Existing all-property tests currently do not adequately demonstrate flags surviving key/account reconstruction. |
| Options/conversion helper | Normal state conversion, idempotency forwarding, ephemeral/custom/user key override, explicit null-account override, copy retaining flags, masked diagnostics, and parcelled options used by 3DS2. Test every policy rather than merely mirroring each construction call. |
| Shared request headers | ApiVersionTest, RequestHeadersFactoriesTest, ApiRequestTest and FileUploadRequestTest cover empty, multiple, exact duplicates, union with existing constructor/FC tokens, deterministic rendering, custom base version, GET/POST/DELETE, and multipart. A fresh snapshot stays fixed during retry. |
| Real repository requests | StripeApiRepositoryTest captures full headers on normal/ephemeral/confirm/delete requests and a configured upload; existing enum betas remain unchanged when options flags are empty. ElementsSessionRepositoryTest and a focused NetworkRule integration prove both initial load and fallback. LinkApiRepositoryTest and CustomerRepositoryTest verify derived options include flags under alternate authentication. |
| Polling/3DS2 | Extend PollingComponentTest using real Dagger/networking and default fixture auth/account validation plus expected version. Extend transaction/challenge-result tests to verify flags survive key substitution, null account and retries; assert no accidental authentication idempotency. |
| Instrumented payment flows | Add focused beta variants to PaymentSheetTest, FlowControllerTest, and EmbeddedPaymentElementTest, including override of unrelated PaymentConfiguration. The expected-version matcher is applied to every API mock so lookup, elements session, metadata, payment-method/confirmation-token creation, retrieval, confirm and auxiliary calls cannot silently drop flags. |
| Customer/Link/subflows | Saved-method customer ephemeral calls, Link custom credentials, Google Pay payment-method creation, ACH/Financial Connections built-in-plus-configured flags, and autocomplete. Include Tap to Add's Stripe API work if enabled in the supported payment flow; SDK calls outside owned HTTP transport need an explicit supported propagation path, not a false coverage claim. |
| Isolation/negative controls | No flags remains the current version, session A and B use distinct sets (including overlapping keys/accounts), same shared repository reconfigures to empty without leaking flags, recreation preserves state, telemetry/browser/third-party traffic is unchanged. Secondary hosts tracked/authenticated explicitly. |

NetworkRule currently accepts both fixture publishable and live keys and uses a path-based ephemeral-key heuristic. Beta coverage should extend its default validation, not loosen it. For null-account metadata or custom-key 3DS2/Link requests, provide route-specific expected credentials in the test helper while keeping validation enabled. Exact token-set assertions catch both lost and leaked beta flags; “contains beta X” alone misses the latter.

## Advantages, limitations, and final recommendation

Advantages: beta metadata travels with effective authentication through existing repository APIs; request objects expose one coherent snapshot; shared header code covers all request methods and uploads; credential `copy` operations naturally retain flags; fake and real-network tests can inspect the same metadata; session State stays about merchant configuration and Options stays about a request.

Limitations: adding a restricted field/parcel layout is still a coordinated internal migration. Any fresh options object built from only key/account drops flags unless audited, so conversion consolidation must precede release. Existing constructor enum betas and configured strings need a tested union policy. Shared transports require no-beta regression checks for legacy integrations. Passing current key/account tests does not prove flags reached every request until the new matcher is enabled.

Choose option 3 unless the simplicity of explicit State forwarding in option 1 is preferred for the first delivery. Defer option 2 to a separately motivated networking simplification. Do not use a global interceptor or mutable repository beta set: neither expresses request origin reliably across shared instances, concurrent flows, or changed configurations.

## Validation and release gates for the implementation

These are future implementation checks, not checks run for this documentation PR. Invoke repository testing skills before writing tests and delegate routine Gradle commands according to `AGENTS.md`.

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
