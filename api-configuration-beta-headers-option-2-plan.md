# Android beta headers — option 2: replace ApiRequest.Options

## Decision and feasibility

A literal replacement of every `ApiRequest.Options` argument with today's `ApiConfiguration.State` is **not sufficient**. Options represents an effective API key (including ephemeral/user/custom keys), an account override, and per-request idempotency. State represents merchant/session configuration with a field explicitly named `publishableKey`. Dropping the extra semantics would break existing calls; storing ephemeral keys or idempotency on the public configuration would change what that API means.

Complete removal of the Options class is technically feasible if session configuration is paired with explicit credential overrides and per-request idempotency at the networking boundary. The plan below describes that migration, including a small prototype/decision milestone before committing to the SDK-wide change. It does not claim that replacing one type automatically removes the request-level information.

Recommendation: use this design only as a separately justified networking refactor. It can improve MPE-facing repository signatures, but it is not an improvement for beta headers alone. Option 3 preserves an already useful abstraction and is the lower-risk choice for this feature.

| Alternative | Session data | Effective credentials/idempotency | Main tradeoff |
| --- | --- | --- | --- |
| 1 | State forwarded alongside options | Existing Options | Parallel inputs, smallest conceptual change |
| 2 | State is the operation's configuration | Explicit override + per-request idempotency | Broader signatures/migration; removal does not eliminate this information |
| 3 | State converted to request snapshot | Options gains beta strings | Retains compact transport semantics |

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

## Evidence that makes replacement non-trivial

- `CustomerApiRepository` calls `retrieveCustomer` and `getPaymentMethods` with an ephemeral key while still needing the merchant publishable key for analytics. A single State with `publishableKey = ephemeralKey` would conflate two identities; `StripeRepository.getPaymentMethods` already takes both options and state for this reason.
- `LinkApiRepository.buildRequestOptions` substitutes a custom key and explicitly removes the account. 3DS2 next-action handling also substitutes a key without the merchant account. Null account is a real override, not “fall back to the session account.”
- `ApiRequest.Options` validates keys with `ApiKeyValidator`, masks its `toString()`, and exposes user-key/live-mode behavior. `ApiConfiguration.State.isLiveMode()` is a different implementation and is not a drop-in replacement for all option capabilities.
- StripeApiRepository branches on `apiKeyIsUserKey` to remove client-secret parameters, handles API errors using request options, and intentionally clears accounts for FPX/card metadata. Those behaviors must use the effective request identity.
- `StripePaymentControllerTest` explicitly verifies confirmation idempotency survives while next-action authentication gets no idempotency key. Session-wide storage of idempotency would give unrelated requests the same key.
- Options appears in shared core API/network/upload code, ConsumersApiService, Financial Connections, Identity, Crypto Onramp, card metadata, Google Pay, legacy Stripe/StripeKtx/issuing/customer operations, and tests/fakes. The restricted API dump excludes Options itself; this does not make its JVM/parcel/signature migration free of compatibility impact.

## Proposed target shape

Use restricted immutable State for **session policy**, with betas. Use a narrowly named internal `RequestCredentials` for an explicit effective `apiKey` and nullable `stripeAccount`; it contains no betas and no idempotency. A nullable override means “use the session credentials.” A non-null override with a null account means “use no account.” Keep `idempotencyKey: String?` as request-level input, snapshotting it with the actual request.

At an MPE-facing boundary, conceptually:

```text
repository.operation(params, apiConfiguration, credentialsOverride, idempotencyKey)
  -> resolve effective credentials once
  -> ApiRequest(configuration's copied beta set, effective credentials,
                request idempotency, producer base version)
```

New internal parameters are explicit, including `null` when no override/idempotency is needed. Keep the public ApiConfiguration builder free of `apiKey`, ephemeral-key, and idempotency setters. Public Stripe methods preserve their existing key/account/idempotency arguments and adapt them internally.

For generic shared transport paths lacking an MPE session (for example an Identity ephemeral-key request), allow an explicitly absent session configuration **only at the generic networking boundary** and require explicit effective credentials. No session means no configured beta tokens. MPE repository operations require state and cannot silently take that path. Normal legacy Stripe calls can construct state from their merchant credentials while their existing enum betas remain a factory concern. Do not fabricate a merchant publishable key from an ephemeral key just to satisfy a non-null State parameter.

This removes the Options class but retains a credential value object and a request-level idempotency field. That is unavoidable useful information, not a claim of reducing networking to one object. If a prototype finds it better to group those fields again, retain Options (option 3) rather than introducing a renamed equivalent without benefit.

## Refactors and implementation order

1. **Characterize effective request identity first.** Add baseline tests for ephemeral/custom/user keys, explicit null-account overrides, key validation, masked diagnostics, request error handling, card-metadata account clearing, and idempotent confirmation/retries. Keep existing controller/3DS2 tests. Audit public and restricted signatures before changing them.
2. **Perform a bounded prototype.** Migrate three representative operations on a temporary implementation branch: a normal confirm carrying idempotency; customer payment-method retrieval with ephemeral credentials plus merchant analytics; and 3DS2 authenticate/complete with a custom key and null account. Include a Link call and an upload at the transport boundary. Compare signature clarity, test setup, duplicate values, and Java/Kotlin adapters with option 3. The prototype succeeds only if no key/account/idempotency/beta semantics are lost.
3. **Decision milestone.** Choose complete removal only if callers reliably know their session state, the effective identity rules are explicit, and the extra override/idempotency parameters actually simplify MPE code. Otherwise stop the migration and implement option 3. This is a technical decision based on the prototype, not a requirement for another user permission step. Do not make the beta rollout depend on completing this broader migration if it has no demonstrated benefit.
4. **Introduce the explicit boundary.** Add RequestCredentials and a resolver for session-default versus explicit credentials. Move validator/user-key/live-mode capability and masked diagnostic behavior to that boundary. Add temporary Options-to-boundary adapters preserving behavior so modules can move in small PRs. No beta header behavior changes yet.
5. **Migrate low-level networking.** Change ApiRequest, its Factory, BaseApiHeadersFactory, FileUploadRequest, and repository error handling to use resolved credentials and per-request idempotency. Both normal/file headers use the shared beta/version formatter; retrying uses the same immutable request. Legacy adapters provide explicit empty configured flags when there is no session.
6. **Migrate repository contracts and MPE callers.** Change StripeRepository/StripeApiRepository and fakes, ConsumersApiService, ElementsSessionRepository (including fallback), CheckoutSessionRepository, CustomerApiRepository, LinkApiRepository, autocomplete, and PaymentLauncher. Carry state to repository operations; resolve overridden credentials without mutating it. Use an explicit idempotency argument only for the operation that owns it. Remove ApiRequestOptionsModule once its consumers have migrated; replace it with scoped state/credential resolution as needed, without inventing a DI-scoped idempotency key.
7. **Migrate subflows and legacy/public adapters.** Finish Google Pay, cards, polling, Tap to Add, bank-account/Financial Connections, 3DS2 launch/result completion, legacy StripePaymentController/Stripe/StripeKtx, issuing/card widgets and customer-session helpers. For each public API preserve parameters and externally observable behavior. For Identity/Crypto/standalone Financial Connections retain their generic authentication and own API versions/built-in tokens; they are transport migration work, not new public beta integrations.
8. **Remove Options last.** Update parcelled transaction/activity contracts, secondary-module imports, request helper signatures, tests/fakes, and API dumps where applicable. Search all source sets for `ApiRequest.Options`, its properties, constructors, `copy`, and `Provider<Options>`. Delete adapters only when there are no references and every module compiles. Release mutually dependent SDK modules together; do not assume restricted binary compatibility or cross-version parcel compatibility.
9. **Deliver public betas and network coverage.** Complete the shared public-configuration work and every request producer, run the full compatibility/behavior matrix, regenerate public API files, update preview examples/changelog, and run the wider validation below.

## Test plan and improvement criteria

In addition to shared all-request network validation, explicitly test:

| Contract | Required proof |
| --- | --- |
| Credential resolver | Normal state key/account; ephemeral/user/custom keys; override with null account stays null; absent state requires explicit credentials; invalid keys rejected; no secret/key leaks in request diagnostics. |
| Request metadata | Different requests can have different idempotency keys in the same session; reads/authentication have null where they previously did; retries keep the original key; configurations do not acquire mutable idempotency state. |
| Existing user-key behavior | Client-secret removal and live-mode header behavior remain based on the effective key. Keep legacy exception mapping/error parsing unchanged. |
| Merchant versus request identity | Customer calls use ephemeral authorization while merchant analytics use the merchant key. Link/3DS2 overrides keep session beta flags and their established account policy. |
| Migration boundaries | Equivalent old/new requests during staged migration; explicit empty configured flags for standalone non-MPE requests; normal/upload/consumer/FC producer versions remain correct; Java/public Stripe adapters remain callable. |
| End-to-end flows | All beta, empty-beta, reconfiguration, concurrent-flow and recreation tests shared with options 1/3, including initial elements session, fallback, confirmation, polling, customer, Link, Google Pay and FC. |
| Completion | Zero Options references across production/test source sets; all affected modules compile and pass relevant tests; API review/dumps show only intended public beta API changes. |

An improvement means MPE repositories consistently receive the originating state, shared repositories no longer read a competing global configuration, and credential/idempotency rules remain understandable. Removal merely producing longer signatures plus a differently named Options-like object is not an improvement. Expanding public ApiConfiguration to cover transport authentication is outside the proposed review and is not recommended.

## Pros and cons

Pros: clearer session configuration at MPE-facing operations; less reconstructing configuration from partial options; explicit per-request idempotency; a chance to separate merchant analytics identity from effective authentication consistently.

Cons: SDK-wide migration beyond the product feature; extra override/idempotency parameters; greater compatibility risk; lower-level services cannot universally require merchant State; broad changes to fakes/parcelled contracts; new credential resolver still embodies much of Options' existing job. Completing the feature takes longer and has more regression surface than either other option.

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

For option 2 additionally build/test every migrated shared consumer, including `identity`, `crypto-onramp`, `financial-connections`, `financial-connections-lite`, `financial-connections-core`, `payments-ui-core`, `payments-core-testing`, `payments-model`, and example/instrumentation source sets. Use module-specific tasks first, then a repository-wide compile/API validation pass once the staged migration is complete. This expanded cost is part of the decision, not required for options 1/3 solely because these modules share networking.
