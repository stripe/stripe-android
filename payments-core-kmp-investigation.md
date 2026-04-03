# `payments-core` Kotlin Multiplatform investigation

## Goal

Investigate what would be required to make `payments-core` support Kotlin Multiplatform, with `com.stripe.android.networking` present in the common source set/module and everything else allowed to remain Android-only where necessary.

Assumption: "common module" means either:

- `payments-core/src/commonMain` after converting `payments-core` into a KMP module, or
- a new internal KMP module such as `payments-core-common`.

The conclusions below apply to both interpretations.

## Executive summary

Short version: this is not a package move. It is a cross-cutting architecture project.

- There is no existing KMP infrastructure in this repo today. `payments-core`, `stripe-core`, and `payments-model` are plain Android library modules, and shared build/publish tooling assumes Android/AAR publication.
- Moving the current `com.stripe.android.networking` package into `commonMain` unchanged is not feasible. The package is tightly coupled to Android/JVM-only networking, Android/JVM-only models, fraud detection, analytics environment collection, file upload, Dagger modules, Alipay, 3DS2, and resource-based error localization.
- The realistic way to satisfy the requirement is to split the package across source sets:
  - `commonMain/com/stripe/android/networking/...` for platform-neutral contracts and types
  - `androidMain/com/stripe/android/networking/...` for Android implementations, adapters, and Android-only features
- If the requirement is weaker than "move the current API unchanged", this is achievable with a staged refactor.
- If the requirement is "make the existing `StripeRepository` and `StripeApiRepository` common without changing their effective API shape", scope expands substantially because a large part of the model/request stack must also become common or be wrapped.

My recommendation is:

1. Do not try to move the current package wholesale.
2. Introduce a common-safe networking surface first.
3. Keep Android implementations and Android-only endpoints in `androidMain`.
4. Prefer interface-based platform seams over broad `expect`/`actual` usage.

## What "have the package in the common module" can realistically mean

In KMP, the same package can exist in both `commonMain` and `androidMain`. That is the right shape here.

What is realistic:

- `commonMain/com/stripe/android/networking` contains platform-neutral enums, interfaces, DTOs, and/or internal repository contracts.
- `androidMain/com/stripe/android/networking` contains `StripeApiRepository`, analytics factories, Dagger modules, error mapping, Alipay, and other Android-only code.

What is not realistic as a first step:

- taking the current contents of `payments-core/src/main/java/com/stripe/android/networking` and moving them unchanged into `commonMain`

If the intent is only that the package namespace and common-safe networking contracts live in common code, that is workable. If the intent is that the current Android implementation and current model-heavy repository contract live in common code unchanged, that is not workable without a much larger migration.

## Current repo state

### No KMP infrastructure exists today

Search for KMP wiring returned no matches for `kotlin("multiplatform")`, `commonMain`, or `androidMain`.

### The relevant modules are Android libraries

- `payments-core/build.gradle:1-145` applies `configs.androidLibrary`, `parcelize`, Compose, Android source sets, and Android test configuration.
- `stripe-core/build.gradle:1-59` applies `configs.androidLibrary` and `parcelize`.
- `payments-model/build.gradle:1-40` applies `configs.androidLibrary` and `parcelize`.

### Shared build and publishing infrastructure is Android-oriented

- `build-configuration/android-library.gradle:6-95` always applies `com.android.library` and `kotlin-android`.
- `build-configuration/android-library.gradle:61-65` assumes Android `sourceSets.main`.
- `build-configuration/android-library.gradle:97-107` suppresses all Dokka source sets except `main`.
- `deploy/deploy.gradle:28-33` builds source jars from `android.sourceSets.main.java.srcDirs`.
- `deploy/deploy.gradle:42-47` publishes `bundleReleaseAar`.
- `deploy/deploy.gradle:55-58` hardcodes packaging as `"aar"`.
- `deploy/deploy.gradle:114-119` builds POM dependencies from Android `api` and `implementation` configurations.
- `build.gradle:86-131` enables binary compatibility validation and Dokka across modules; both would need KMP-aware adjustments.

### Scale signal

Across `payments-core`, `payments-model`, and `stripe-core`, quick grep counts show:

- `org.json` appears in 137 files.
- `Parcelable` / `@Parcelize` appears in 176 files.
- selected Android platform types (`Context`, `Uri`, `Parcelable`, `Build`, `Os`) appear in 242 files.

These are not exact blocker counts, but they confirm the problem is systemic rather than localized to one package.

## Current contents of `com.stripe.android.networking`

Current files:

- `AlipayRepository.kt`
- `DefaultAlipayRepository.kt`
- `PaymentAnalyticsEvent.kt`
- `PaymentAnalyticsRequestFactory.kt`
- `PaymentElementRequestSurfaceModule.kt`
- `RequestSurface.kt`
- `StripeApiRepository.kt`
- `StripeErrorMapping.kt`
- `StripeRepository.kt`

## File-by-file classification

| File | Common-safe as-is? | Notes |
| --- | --- | --- |
| `RequestSurface.kt` | Almost | Logically common, but currently imports `androidx.annotation.RestrictTo` (`payments-core/src/main/java/com/stripe/android/networking/RequestSurface.kt:3-11`). |
| `PaymentAnalyticsEvent.kt` | Almost | Logically common, but currently depends on `AnalyticsEvent` defined in an Android-bound file and imports AndroidX annotations (`payments-core/src/main/java/com/stripe/android/networking/PaymentAnalyticsEvent.kt:3-129`, `stripe-core/src/main/java/com/stripe/android/core/networking/AnalyticsRequestFactory.kt:14-127`). |
| `StripeRepository.kt` | No | Contract is too wide and uses Android/JVM-only request and model types (`payments-core/src/main/java/com/stripe/android/networking/StripeRepository.kt:45-476`). |
| `StripeApiRepository.kt` | No | Depends on `Context`, `HttpResponseCache`, `File`, `HttpURLConnection`, `Locale`, Android defaults, and JSON parser stack (`payments-core/src/main/java/com/stripe/android/networking/StripeApiRepository.kt:3-185`). |
| `PaymentAnalyticsRequestFactory.kt` | No | Depends on `Context`, `PackageManager`, `PackageInfo`, and `NetworkTypeDetector` (`payments-core/src/main/java/com/stripe/android/networking/PaymentAnalyticsRequestFactory.kt:3-79`). |
| `StripeErrorMapping.kt` | No | Depends on `Context` and Android resources (`payments-core/src/main/java/com/stripe/android/networking/StripeErrorMapping.kt:3-109`). |
| `PaymentElementRequestSurfaceModule.kt` | No | Dagger Android module (`payments-core/src/main/java/com/stripe/android/networking/PaymentElementRequestSurfaceModule.kt:3-11`). |
| `AlipayRepository.kt` | No | Depends on `AlipayAuthenticator`, `PaymentIntent`, and Android request/model stack (`payments-core/src/main/java/com/stripe/android/networking/AlipayRepository.kt:3-13`). |
| `DefaultAlipayRepository.kt` | No | Android-specific payment flow behavior (`payments-core/src/main/java/com/stripe/android/networking/DefaultAlipayRepository.kt:3-64`). |

### Practical implication

The only clear common candidates are small, platform-neutral types after cleanup:

- `RequestSurface`
- `PaymentAnalyticsEvent`
- a new, reduced networking contract

The rest should initially remain Android-only, even if they stay under the same package name in `androidMain`.

## Why the current package cannot move unchanged

### 1. `StripeApiRepository` is deeply Android/JVM-specific

The implementation is Android-bound from construction time:

- `Context` constructor parameter (`StripeApiRepository.kt:123`)
- default `DefaultFraudDetectionDataRepository(context, workContext)` (`StripeApiRepository.kt:136-137`)
- default `DefaultCardAccountRangeRepositoryFactory(context, ...)` (`StripeApiRepository.kt:138-139`)
- default `PaymentAnalyticsRequestFactory(context, ...)` (`StripeApiRepository.kt:140-141`)
- `HttpResponseCache.install(...)` using `File(context.cacheDir, ...)` (`StripeApiRepository.kt:178-185`)
- direct imports of `File`, `HttpURLConnection`, `Security`, and `Locale` (`StripeApiRepository.kt:109-113`)

Even if this class were moved into `commonMain`, its default dependencies are not common-safe today.

### 2. `StripeRepository` is too wide for a first common contract

The interface mixes common-ish payment APIs with clearly Android/JVM-only concerns. Examples:

- file upload: `createFile(...)` (`StripeRepository.kt:289-292`)
- arbitrary object fetch: `retrieveObject(...)` (`StripeRepository.kt:295-298`)
- issuing PIN APIs: `retrieveIssuingCardPin(...)` and `updateIssuingCardPin(...)` (`StripeRepository.kt:251-265`)
- 3DS2 APIs: `start3ds2Auth(...)` and `complete3ds2Auth(...)` (`StripeRepository.kt:277-286`)
- hCaptcha challenge cancellation: `cancelPaymentIntentCaptchaChallenge(...)` and `cancelSetupIntentCaptchaChallenge(...)` (`StripeRepository.kt:456-467`)

Even the "normal" methods are blocked because the request/response types are not common-safe yet.

### 3. `stripe-core` networking foundations are not common-safe

The repository depends on `stripe-core` abstractions that are Android/JVM-shaped:

- `StripeModel` extends `Parcelable` (`stripe-core/src/main/java/com/stripe/android/core/model/StripeModel.kt:3-8`)
- `ApiRequest.Options` is `@Parcelize` and `Parcelable` (`stripe-core/src/main/java/com/stripe/android/core/networking/ApiRequest.kt:97-138`)
- `ApiRequest.writePostBody()` writes to `OutputStream` (`stripe-core/src/main/java/com/stripe/android/core/networking/ApiRequest.kt:13-18`, `ApiRequest.kt:86-90`)
- `DefaultStripeNetworkClient` uses `File` and JVM I/O (`stripe-core/src/main/java/com/stripe/android/core/networking/DefaultStripeNetworkClient.kt:10-35`)
- `ConnectionFactory` is built on `HttpURLConnection`, `HttpsURLConnection`, `URL`, and `File` (`stripe-core/src/main/java/com/stripe/android/core/networking/ConnectionFactory.kt:5-99`)
- `StripeConnection` is built on `InputStream`, `FileOutputStream`, scanners, and `HttpsURLConnection` (`stripe-core/src/main/java/com/stripe/android/core/networking/StripeConnection.kt:4-116`)
- `responseJson()` returns `org.json.JSONObject` (`stripe-core/src/main/java/com/stripe/android/core/networking/ResponseJson.kt:5-27`)

As long as the core request/transport stack is JVM-only, a common `StripeApiRepository` cannot exist.

### 4. Analytics is Android-specific today

`PaymentAnalyticsRequestFactory` is Android-specific:

- uses `Context`, `PackageManager`, `PackageInfo` (`payments-core/src/main/java/com/stripe/android/networking/PaymentAnalyticsRequestFactory.kt:3-79`)
- uses `NetworkTypeDetector(context)` (`PaymentAnalyticsRequestFactory.kt:64-65`, `77-78`)

The lower-level analytics stack is also Android-shaped:

- `AnalyticsRequestFactory` uses `PackageManager`, `PackageInfo`, `Build`, `Locale`, and plugin detection (`stripe-core/src/main/java/com/stripe/android/core/networking/AnalyticsRequestFactory.kt:3-127`)
- `RequestHeadersFactory` uses `Build`, `Os.getenv`, and `Locale` (`stripe-core/src/main/java/com/stripe/android/core/networking/RequestHeadersFactory.kt:3-220`)

Even `PaymentAnalyticsEvent` is only common-safe after extracting or recreating the tiny `AnalyticsEvent` contract outside the current Android-bound file.

### 5. Fraud detection is Android-specific today

The default fraud detection path depends on Android at every layer:

- `PaymentsFraudDetectionDataRepositoryFactory` builds the default repository from `Context` (`payments-core/src/main/java/com/stripe/android/PaymentsFraudDetectionDataRepositoryFactory.kt:5-28`)
- `DefaultFraudDetectionDataRequestFactory(context)` (`stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionDataRequestFactory.kt:13-27`)
- `FraudDetectionDataRequestParamsFactory` uses `Context`, `DisplayMetrics`, `Build`, `Locale`, `TimeZone` (`stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionDataRequestParamsFactory.kt:3-93`)
- `DefaultFraudDetectionDataStore` uses `Context`, `SharedPreferences`, and `org.json` (`stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionDataStore.kt:3-47`)
- `FraudDetectionData` itself is `@Parcelize`, `StripeModel`, and serializes to `JSONObject` (`stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionData.kt:4-44`)

This is a strong argument for interface-based platform providers rather than `expect`/`actual` across the fraud stack.

### 6. The model layer used by networking is not common-safe

The largest blocker is not the package itself. It is the model graph that the package exposes.

### Base model contracts

- `StripeParamsModel` extends `Parcelable` (`payments-model/src/main/java/com/stripe/android/model/StripeParamsModel.kt:3-10`)
- `TokenParams` extends `StripeParamsModel, Parcelable` (`payments-model/src/main/java/com/stripe/android/model/TokenParams.kt:3-16`)

### Payment intents and next actions

- `StripeIntent` imports `android.net.Uri` and `Parcelable` through `StripeModel` (`payments-core/src/main/java/com/stripe/android/model/StripeIntent.kt:3-15`)
- `StripeIntent.NextActionData.RedirectToUrl` stores `Uri` (`StripeIntent.kt:252-264`)
- `StripeIntent.NextActionData.AlipayRedirect` stores `Uri` and parses URLs with `Uri.parse(...)` (`StripeIntent.kt:266-290`)
- `NextActionDataParser` parses URLs into `Uri` and depends on `org.json.JSONObject` (`payments-core/src/main/java/com/stripe/android/model/parsers/NextActionDataParser.kt:3-220`)

### Payment method and related types

- `PaymentMethod` is `@Parcelize`, depends on `JSONObject`, and imports `PaymentFlowResultProcessor` constants from payment-flow code (`payments-core/src/main/java/com/stripe/android/model/PaymentMethod.kt:3-14`, `28-30`)
- `CardBrand` depends on Android drawable resources and `java.util.regex.Pattern` (`payments-model/src/main/java/com/stripe/android/model/CardBrand.kt:3-8`, `13-255`)

### Source and confirm parameter models

- `SourceParams` uses `Parcel`, `Parcelable`, `JSONObject`, and `StripeJsonUtils` JSON conversions (`payments-core/src/main/java/com/stripe/android/model/SourceParams.kt:3-15`, `19-21`, `314-338`)
- `ConfirmPaymentIntentParams` is `@Parcelize` and depends on many Android-shaped param models (`payments-core/src/main/java/com/stripe/android/model/ConfirmPaymentIntentParams.kt:3-18`, `23-25`)

### Utility coupling

- `StripeUrlUtils` uses `android.net.Uri` (`payments-core/src/main/java/com/stripe/android/utils/StripeUrlUtils.kt:3-13`)

### Important consequence

If the goal is to make the existing repository contract common, then the current model/request types must also become common or be replaced with common DTOs plus Android adapters.

That is the real scope driver.

### 7. Android-only features inside or adjacent to networking should stay Android-only

These features should not drive the first common split:

- localized error mapping via Android resources (`StripeErrorMapping.kt:11-109`)
- Dagger provisioning modules (`PaymentElementRequestSurfaceModule.kt:7-11`, `payments-core/src/main/java/com/stripe/android/payments/core/injection/StripeRepositoryModule.kt:3-54`)
- Alipay (`AlipayRepository.kt:3-13`, `DefaultAlipayRepository.kt:3-64`)
- file upload (`StripeRepository.kt:289-292`, `stripe-core/src/main/java/com/stripe/android/core/model/StripeFileParams.kt:3-70`)
- 3DS2 methods (`StripeRepository.kt:277-286`)
- issuing PIN methods (`StripeRepository.kt:251-265`)
- hCaptcha cancel methods (`StripeRepository.kt:456-467`)

These belong in `androidMain` or in Android-only adapter layers even after the common networking layer exists.

## Required changes outside `com.stripe.android.networking`

This requirement cannot be achieved while leaving the rest of the codebase literally untouched. A common networking package needs at least some common-safe support types outside the package.

Minimum categories of external work:

### Build and publication

- add KMP plugin/application strategy for the target module
- define `commonMain`, `androidMain`, `commonTest`, and Android test source sets
- create KMP-capable shared Gradle config rather than reusing `android-library.gradle` unchanged
- teach `deploy/deploy.gradle` how to publish KMP components instead of only AARs
- teach Dokka not to suppress non-`main` source sets
- update binary compatibility validation for KMP APIs if the public surface changes

### Core request and transport abstractions

- create a platform-neutral request options type to replace or wrap `ApiRequest.Options`
- create a platform-neutral transport abstraction instead of exposing `HttpURLConnection`, `OutputStream`, `File`, and `StripeResponse<String>` as the core contract
- decide whether common code talks in raw JSON trees, typed DTOs, or both

### Model strategy

One of these must happen:

- commonize the existing public model types, or
- introduce internal common DTOs and keep Android public models as adapters/wrappers in `androidMain`

The second option is much lower risk and better aligned with the "others can remain Android-only" constraint.

### Parsing and utilities

- replace `org.json`-centric parsing with `kotlinx.serialization.json` or another common JSON abstraction
- replace `android.net.Uri` with `String` or a small common URI value type
- replace `java.util.regex.Pattern` with Kotlin `Regex` where the code moves into common
- remove or replace AndroidX annotations from common code

## Interface vs `expect` / `actual`

I agree with the constraint that interfaces are often better than `expect` / `actual` here. This codebase is a good example of that.

### Where interfaces are the better seam

Prefer interfaces for platform behavior that has lifecycle, I/O, configuration, or test-double needs:

- HTTP transport / request execution
- analytics environment data collection
- app/package metadata lookup
- network type lookup
- device/build information lookup
- fraud detection signal collection
- fraud detection storage
- cache installation / cache policy
- clock, UUID, session ID, and other runtime providers

Why interfaces fit better here:

- they keep common code independent of platform types
- they are easier to fake in tests
- they avoid spreading `actual` implementations across large parts of the graph
- they work naturally with DI and constructor injection

### Where `expect` / `actual` may still make sense

Reserve `expect` / `actual` for tiny leaf utilities where there is a very small, stable platform difference and no meaningful service contract to inject.

Examples of acceptable candidates:

- a minimal default locale tag helper
- a minimal timezone snapshot helper
- a tiny monotonic/system clock utility

### Where I would avoid `expect` / `actual`

I would not use `expect` / `actual` to drag these Android concepts into common code:

- `Context`
- `Uri`
- `Parcelable`
- `File`
- `HttpURLConnection`
- Android resources
- Dagger modules
- the whole repository implementation stack

For this migration, the default should be:

- interfaces for behavior
- plain common value types for data
- Android adapters in `androidMain`
- `expect` / `actual` only for small leaf utilities if an interface would be pure ceremony

## Recommended target architecture

### Option A: convert `payments-core` itself into a KMP module

This is the direct interpretation of "make `payments-core` support Kotlin Multiplatform."

Recommended shape:

- `payments-core/src/commonMain/kotlin/com/stripe/android/networking/...`
  - common enums and contracts
  - common DTOs or internal request/response types
  - platform-neutral service interfaces
- `payments-core/src/androidMain/kotlin/com/stripe/android/networking/...`
  - `StripeApiRepository`
  - `PaymentAnalyticsRequestFactory`
  - `StripeErrorMapping`
  - `PaymentElementRequestSurfaceModule`
  - `AlipayRepository`
  - Android adapters to current public models if needed

Pros:

- directly satisfies the artifact-level goal

Cons:

- forces build/publish/tooling changes immediately
- forces a decision on how much of the current public API becomes common

### Option B: introduce a new internal `payments-core-common` KMP module first

This is the lower-risk migration path if the real goal is reuse of networking/common logic rather than immediate artifact-level KMP support for the existing module.

Recommended shape:

- new `payments-core-common` holds common networking contracts and common DTOs under `com.stripe.android.networking`
- existing Android `payments-core` depends on it and continues to expose Android public APIs
- later, if desired, fold the common layer back into a KMP `payments-core`

Pros:

- much safer rollout
- smaller publishing blast radius at the beginning
- lets Android keep public parcelable models while common code stays clean

Cons:

- does not, by itself, make the existing `payments-core` artifact a KMP artifact

### My recommendation

If "support KMP" must apply to the existing `payments-core` artifact immediately, choose Option A.

If the actual business goal is "get common networking logic and package presence into shared code with the least disruption", start with Option B, then converge later.

Either way, do not start from the current `StripeRepository` API unchanged.

## Recommended migration plan

### Phase 0: make two architectural decisions up front

Decide these before writing production code:

1. Must the existing `payments-core` artifact itself become KMP now, or is a new internal common module acceptable as a stepping stone?
2. Must the current public model/repository API remain unchanged, or can common code use new internal DTOs plus Android adapters?

If the answer to both is "must stay unchanged", expect a much larger project.

### Phase 1: add KMP scaffolding

- create a KMP-capable shared Gradle convention instead of reusing `android-library.gradle` unchanged
- add `commonMain` / `androidMain`
- update publication logic in `deploy/deploy.gradle`
- update Dokka configuration
- update binary compatibility validation and CI tasks as needed

### Phase 2: define the common-safe networking surface

In `commonMain/com/stripe/android/networking`, introduce:

- `RequestSurface` after annotation cleanup
- `PaymentAnalyticsEvent` after extracting or recreating a tiny common `AnalyticsEvent`
- new platform-neutral contracts for networking

I would not start with the current `StripeRepository`. I would start with smaller common contracts, for example:

- intent retrieval/confirmation contract
- customer payment method contract
- request options/value types
- transport abstraction

This keeps common code from inheriting every Android-only endpoint on day one.

### Phase 3: split common vs Android-only repository behavior

Separate the current interface into:

- a common-safe subset
- an Android-only extension or adapter layer for:
  - file upload
  - arbitrary object retrieval
  - 3DS2
  - issuing PIN
  - hCaptcha cancellation
  - Alipay
  - localized error handling

This is also where `AbsFakeStripeRepository` and test doubles need to split.

### Phase 4: create platform-neutral data and parsing seams

Before real repository logic can move to common code, introduce:

- common request option types
- common response types
- common JSON abstraction
- common DTOs for the subset of APIs that truly need to be shared

Lower-risk strategy:

- keep current Android public models in `androidMain`
- map them to/from internal common DTOs at the Android boundary

Higher-risk strategy:

- make existing public models themselves common

I recommend the lower-risk strategy first.

### Phase 5: move the first common-safe logic

After the seams exist, move only the logic that is actually platform-neutral:

- endpoint selection
- URL path construction
- request parameter shaping
- response interpretation on common DTOs
- retry/call orchestration that does not require platform APIs

Keep Android implementation details in `androidMain`:

- HTTP engine choice
- cache install
- package/app info lookup
- network type lookup
- localized resources
- Dagger provisioning

### Phase 6: update consumers and tests

Impacted consumers include:

- `payments-core`
- `paymentsheet`
- fake repositories in `payments-core-testing`

Evidence of current blast radius:

- `payments-core-testing/src/main/java/com/stripe/android/testing/AbsFakeStripeRepository.kt:42-515`
- many usages across `payments-core` and `paymentsheet` from `rg -n "StripeRepository" payments-core paymentsheet payments-core-testing`

Common code will need common tests, while Android-specific behavior keeps Android unit/instrumentation tests.

## Risk assessment

### Lower-risk version of the project

Definition:

- package namespace exists in common code
- common-safe contracts are added
- Android public APIs stay in Android source sets or are adapted at the boundary

Risk:

- large but manageable
- still requires build, publication, parsing, and model-boundary work

### Higher-risk version of the project

Definition:

- the current `StripeRepository` API and existing public models become truly common

Risk:

- very high
- likely source/binary compatibility pressure due to current `Parcelable`-based public model design
- much larger transitive blast radius across `stripe-core`, `payments-model`, `payments-core`, and consumers

## Bottom line

`payments-core` can support KMP with `com.stripe.android.networking` present in common code, but not by moving the current package wholesale.

The viable plan is:

1. make the module or a new internal module KMP-capable
2. put only common-safe networking contracts in `commonMain`
3. keep Android implementations and Android-only endpoints in `androidMain`
4. introduce interface-based seams for platform services
5. use common DTOs/adapters instead of trying to make the current Android public model graph common on day one

If the requirement is interpreted as "the package namespace and shared networking abstractions must exist in common code", this is realistic.

If it is interpreted as "move the current networking API and implementation into common code unchanged", that is not a contained package refactor; it is a broad redesign of the core request/model stack.

## Key source references

- `payments-core/build.gradle:1-145`
- `stripe-core/build.gradle:1-59`
- `payments-model/build.gradle:1-40`
- `build-configuration/android-library.gradle:6-113`
- `deploy/deploy.gradle:28-149`
- `build.gradle:86-131`
- `payments-core/src/main/java/com/stripe/android/networking/StripeApiRepository.kt:3-185`
- `payments-core/src/main/java/com/stripe/android/networking/StripeRepository.kt:45-476`
- `payments-core/src/main/java/com/stripe/android/networking/PaymentAnalyticsRequestFactory.kt:3-79`
- `payments-core/src/main/java/com/stripe/android/networking/StripeErrorMapping.kt:11-109`
- `payments-core/src/main/java/com/stripe/android/networking/PaymentElementRequestSurfaceModule.kt:7-11`
- `payments-core/src/main/java/com/stripe/android/networking/DefaultAlipayRepository.kt:3-64`
- `stripe-core/src/main/java/com/stripe/android/core/model/StripeModel.kt:3-8`
- `payments-model/src/main/java/com/stripe/android/model/StripeParamsModel.kt:3-10`
- `stripe-core/src/main/java/com/stripe/android/core/networking/ApiRequest.kt:23-180`
- `stripe-core/src/main/java/com/stripe/android/core/networking/AnalyticsRequestFactory.kt:15-127`
- `stripe-core/src/main/java/com/stripe/android/core/networking/RequestHeadersFactory.kt:11-220`
- `stripe-core/src/main/java/com/stripe/android/core/networking/DefaultStripeNetworkClient.kt:14-99`
- `stripe-core/src/main/java/com/stripe/android/core/networking/ConnectionFactory.kt:16-99`
- `stripe-core/src/main/java/com/stripe/android/core/networking/StripeConnection.kt:18-116`
- `stripe-core/src/main/java/com/stripe/android/core/networking/ResponseJson.kt:8-27`
- `payments-core/src/main/java/com/stripe/android/PaymentsFraudDetectionDataRepositoryFactory.kt:15-28`
- `stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionDataRequestFactory.kt:7-27`
- `stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionDataRequestParamsFactory.kt:15-93`
- `stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionDataStore.kt:10-47`
- `stripe-core/src/main/java/com/stripe/android/core/frauddetection/FraudDetectionData.kt:9-44`
- `payments-core/src/main/java/com/stripe/android/model/StripeIntent.kt:15-340`
- `payments-core/src/main/java/com/stripe/android/model/parsers/NextActionDataParser.kt:12-220`
- `payments-core/src/main/java/com/stripe/android/model/PaymentMethod.kt:1-141`
- `payments-model/src/main/java/com/stripe/android/model/CardBrand.kt:13-255`
- `payments-core/src/main/java/com/stripe/android/model/SourceParams.kt:19-338`
- `payments-core/src/main/java/com/stripe/android/model/ConfirmPaymentIntentParams.kt:23-280`
- `payments-model/src/main/java/com/stripe/android/model/TokenParams.kt:6-16`
- `payments-core/src/main/java/com/stripe/android/utils/StripeUrlUtils.kt:5-13`
- `payments-core-testing/src/main/java/com/stripe/android/testing/AbsFakeStripeRepository.kt:42-515`
