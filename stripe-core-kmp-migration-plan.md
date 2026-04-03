# `stripe-core` Kotlin Multiplatform migration plan

## Goal

Turn `stripe-core` into the first Kotlin Multiplatform-ready foundation for the payments stack, with these constraints:

- `stripe-core` becomes the lowest shared layer that later `payments-model`, `payments-core`, and `paymentsheet` can build on.
- Parcelization is treated as an Android concern, not as a blocker for common code.
- Okio is the replacement path for `OutputStream`, and likely for `File` as well.
- Prefer interfaces over broad `expect` / `actual` usage.

## Working assumptions

### 1. `stripe-core` is the right first module

The dependency direction is:

- `paymentsheet` -> `payments-core`
- `payments-core` -> `stripe-core`, `payments-model`
- `payments-model` -> `stripe-core`

So the lowest reusable layer is `stripe-core`, not `payments-core`.

Relevant files:

- `paymentsheet/build.gradle:11`
- `payments-core/build.gradle:11-12`
- `payments-model/build.gradle:17`

### 2. Parcelize should not define the common model shape

Current state:

- `stripe-core` is still a plain Android library with the Parcelize plugin enabled in `stripe-core/build.gradle:1-59`.
- root Gradle is on Kotlin `2.2.21` in `build.gradle:29-37`.
- there are currently 15 Parcelize/Parcelable usages under `stripe-core/src/main/java`.

Plan implication:

- Parcelize should be treated as Android compatibility glue only.
- Shared `stripe-core` types should become common Kotlin types first.
- Android parcelability should either:
  - be provided only in `androidMain`, or
  - use the supported KMP Parcelize bridge only where Android API compatibility really requires it.

### 3. Okio should be the I/O abstraction

Current state:

- `StripeRequest` writes POST bodies through `OutputStream` in `stripe-core/src/main/java/com/stripe/android/core/networking/StripeRequest.kt:46-49`.
- `ApiRequest` writes through `OutputStream` in `stripe-core/src/main/java/com/stripe/android/core/networking/ApiRequest.kt:86-90`.
- `FileUploadRequest` writes multipart bodies and file contents through `OutputStream` in `stripe-core/src/main/java/com/stripe/android/core/networking/FileUploadRequest.kt:47-68`.
- `StripeNetworkClient`, `DefaultStripeNetworkClient`, `ConnectionFactory`, `StripeConnection`, and `StripeResponse` all use `java.io.File` in their file-download path.
- Okio is already declared in the repo dependency catalog in `dependencies.gradle:58` and `dependencies.gradle:184`.

Plan implication:

- replace `OutputStream` with Okio sinks
- replace `File` with Okio `Path` plus `FileSystem`, or a small upload/download abstraction built on top of them

## Migration principles

### 1. Build common code around pure Kotlin contracts

Common code should depend on:

- Kotlin stdlib
- coroutines
- kotlinx.serialization
- Okio

Common code should not depend on:

- `android.os.Parcelable`
- `android.content.Context`
- Android resources
- `java.io.File`
- `java.io.OutputStream`
- `HttpURLConnection`

### 2. Prefer interface seams

Use interfaces for:

- logging
- HTTP transport
- file system and sink/source access
- clock / UUID / session-id providers
- analytics environment info

Use `expect` / `actual` only for tiny leaf compatibility shims when an interface would add no value.

### 3. Keep Android behavior in `androidMain`

The first goal is not “move everything.”

The first goal is:

- make `stripe-core` structurally KMP
- move common-safe types and logic into `commonMain`
- keep Android runtime details in `androidMain`

## Recommended target structure

Use the existing `stripe-core` module and convert it to KMP.

Recommended source-set shape:

- `stripe-core/src/commonMain/kotlin/com/stripe/android/core/...`
- `stripe-core/src/androidMain/kotlin/com/stripe/android/core/...`
- `stripe-core/src/commonTest/kotlin/...`
- `stripe-core/src/androidUnitTest/kotlin/...`

Pragmatic canary target:

- start with `androidTarget()`
- add one cheap non-Android compile target early, preferably plain JVM, so common code is forced to stay Android-free before adding iOS/Native targets

## Scope split

## Move early to `commonMain`

### Foundation types

- `ApiVersion`
- `ApiKeyValidator`
- `RequestId`
- `NetworkConstants`
- `AnalyticsFields`
- `RetryDelaySupplier`
- `ExponentialBackoffRetryDelaySupplier`
- `LinearRetryDelaySupplier`
- `StripeSdkVersion`

These are the easiest files to move because they are mostly pure Kotlin already and mainly need AndroidX annotation cleanup.

### Commonized after small cleanup

- `Logger` interface only
- `AppInfo`
- `Country`
- `CountryCode`
- `StripeFile`
- `StripeFilePurpose`
- `StripeResponse`
- `StripeRequest`
- `ApiRequest.Options`
- `QueryStringFactory`
- `AnalyticsRequestV2`

These are good medium-term candidates once Parcelize, URL encoding, and Okio changes are in place.

## Keep in `androidMain` initially

### Android transport/runtime layer

- `DefaultStripeNetworkClient`
- `ConnectionFactory`
- `StripeConnection`
- `RequestHeadersFactory`
- `AnalyticsRequestFactory`
- `DefaultAnalyticsRequestExecutor`
- `NetworkTypeDetector`

### Android-only features

- `frauddetection/*`
- `strings/*`
- `browser/*`
- `reactnative/*`
- Android Dagger modules and AndroidX integration classes
- WorkManager-backed analytics executor/storage classes

### Important example

`ResolvableString` is explicitly Android-only today because it requires `Context` and `Parcelable`:

- `stripe-core/src/main/java/com/stripe/android/core/strings/ResolvableString.kt:3-12`

Do not try to commonize that in the first pass.

## Phase plan

## Phase 0: module conversion and scaffolding

1. Convert `stripe-core/build.gradle` from Android-library-only to Kotlin Multiplatform with an Android target.
2. Add source sets for `commonMain`, `androidMain`, `commonTest`, and Android unit tests.
3. Update shared build logic so `stripe-core` is not forced through `build-configuration/android-library.gradle` unchanged.
4. Keep publication Android-first at first if needed, but make source sets and compilation KMP-aware.

Deliverable:

- `stripe-core` builds with KMP source sets
- Android artifact still works
- common code can compile without Android imports

## Phase 1: establish parcelize policy

### Policy

Parcelize is only required for Android. It should be a no-op everywhere else.

### Practical plan

1. Stop using `Parcelable` as the base shape of common models.
   - `StripeModel` currently extends `Parcelable` in `stripe-core/src/main/java/com/stripe/android/core/model/StripeModel.kt:3-8`.
   - change the common model contract to a plain marker interface or remove the marker entirely where it adds no value.
2. For classes that must remain parcelable in Android public API, use a compatibility bridge only.
3. Do not let `Parcel`, `Parceler`, or Android-only custom parceling rules into common code.
4. Because the repo is already on Kotlin `2.2.21`, do not use the older annotation alias trick as the long-term setup. If the compatibility bridge is needed, use the Kotlin 2.x Parcelize setup with a custom annotation recognized by the Android target.

### What this means in practice

There are two acceptable approaches:

- Preferred:
  - common data classes are plain Kotlin types
  - Android wrappers/adapters remain parcelable in `androidMain`
- Compatibility bridge:
  - define a custom common annotation and marker interface for parcelability
  - configure the Android target’s Parcelize plugin to recognize the custom annotation
  - non-Android targets treat it as a no-op

Use the bridge only where keeping Android API compatibility is important enough to justify it.

### Candidate files affected early

- `StripeModel.kt`
- `AppInfo.kt`
- `StripeError.kt`
- `Country.kt`
- `CountryCode.kt`
- `StripeFile.kt`
- `ApiRequest.kt`
- `StripeFileParams.kt`

## Phase 2: Okio transport refactor

This is the key enabling phase.

### 2A. Replace request-body writes with Okio sinks

Current blocking API:

- `StripeRequest.writePostBody(outputStream: OutputStream)`

Change to one of these:

- `writePostBody(sink: BufferedSink)`, or
- `bodyWriter: (BufferedSink) -> Unit`

Files:

- `StripeRequest.kt`
- `ApiRequest.kt`
- `FileUploadRequest.kt`
- `AnalyticsRequestV2.kt`

### 2B. Replace file paths with Okio `Path` and `FileSystem`

Current blocking API:

- `StripeNetworkClient.executeRequestForFile(request, outputFile: File): StripeResponse<File>`
- `ConnectionFactory.createForFile(..., outputFile: File)`
- `StripeFileParams.file: File`

Recommended replacement:

- downloads: `Path` + `FileSystem`
- uploads: either
  - `Path` + `FileSystem` for least churn, or
  - a new `UploadContent` abstraction if you want future non-file uploads

My recommendation:

- start with `Path` + `FileSystem` because it is the closest equivalent to the current `File`-based API
- only introduce a more abstract upload body later if there is a real need

Files:

- `StripeNetworkClient.kt`
- `DefaultStripeNetworkClient.kt`
- `ConnectionFactory.kt`
- `StripeConnection.kt`
- `StripeResponse.kt`
- `StripeFileParams.kt`
- `FileUploadRequest.kt`

### 2C. Keep the HTTP engine Android-only at first

Do not try to make `HttpURLConnection` common.

Instead:

- commonize request/response shapes
- keep the Android engine in `androidMain`
- later add other engines behind the same interface

## Phase 3: commonize the request/response layer

After Okio refactor, move these into `commonMain`:

- `StripeRequest`
- `StripeResponse`
- `StripeNetworkClient` interface
- `ApiRequest.Options`
- `ApiVersion`
- `AppInfo`
- `RequestId`
- `NetworkConstants`
- retry suppliers

Required changes:

1. `StripeResponse`
   - remove `File`
   - stop depending on `HttpURLConnection.HTTP_OK`
   - use plain numeric ranges for `isOk` / `isError`
2. `ApiRequest.Options`
   - remove direct `Parcelable` dependency from the shared type
3. `AppInfo`
   - remove direct `Parcelable` dependency from the shared type

Deliverable:

- common request metadata and response types usable by higher layers

## Phase 4: commonize common-safe parsers and serializers

### 4A. Query/form encoding

`QueryStringFactory` is close, but still uses JVM `URLEncoder`:

- `stripe-core/src/main/java/com/stripe/android/core/networking/QueryStringFactory.kt:5-6`

Plan:

- replace that with a common percent-encoding implementation
- keep behavior byte-for-byte compatible with current Stripe server expectations

### 4B. Analytics request shaping

`AnalyticsRequestV2` is a good candidate after the Okio and URL-encoding work:

- it already uses `kotlinx.serialization`
- it only needs OutputStream and URLEncoder removed

Plan:

- commonize `AnalyticsRequestV2`
- keep environment capture and scheduling in Android code

### 4C. Request execution helpers

`RequestExecutor.kt` should move later, not first.

Reason:

- it still depends on current parser stack and error model assumptions

Move it after request/response, JSON, and error contracts are stable.

## Phase 5: decide the common error/model contract

This is the first genuinely cross-cutting decision point.

### Option A: commonize existing core models

Candidates:

- `StripeError`
- `StripeFile`
- `Country`
- `CountryCode`

Use this when:

- Android API compatibility pressure is high
- the models are otherwise platform-neutral

### Option B: use common internal models plus Android adapters

Use this when:

- the current public Android type is too parcelable- or Android-shaped
- the common shape is simpler than the Android API shape

Recommendation:

- use Option A for very simple core value objects
- use Option B for parcel-heavy or Android-behavior-heavy types

## Phase 6: keep Android-only subsystems out of the critical path

Do not block the first migration on these packages:

- `frauddetection`
- `strings`
- `browser`
- `reactnative`
- Android analytics environment capture
- WorkManager-backed storage/execution

Treat them as follow-up slices after the transport and request foundation is stable.

## Phase 7: tests and rollout

### Common tests

Add common tests for:

- form/query encoding
- multipart body formatting
- retry policy
- request/response invariants
- Okio path/file behavior

Use Okio `FakeFileSystem` in common tests for upload/download logic.

### Android tests

Keep Android tests for:

- Parcelize behavior
- HttpURLConnection engine behavior
- Request header environment capture
- WorkManager integrations
- Context/resource-backed code

### Rollout order

1. build scaffolding
2. parcelize policy
3. Okio transport refactor
4. request/response commonization
5. parser/serializer commonization
6. selective model commonization
7. delayed Android-only subsystem follow-ups

## File-level first wave

This is the order I would actually execute in code.

### Wave 1: build + pure common foundations

- `stripe-core/build.gradle`
- shared build config used by `stripe-core`
- `ApiVersion.kt`
- `ApiKeyValidator.kt`
- `RequestId.kt`
- `NetworkConstants.kt`
- `AnalyticsFields.kt`
- `RetryDelaySupplier.kt`
- `ExponentialBackoffRetryDelaySupplier.kt`
- `LinearRetryDelaySupplier.kt`
- `StripeSdkVersion.kt`
- split `Logger.kt` into common interface + Android implementation

### Wave 2: parcelize boundary cleanup

- `StripeModel.kt`
- `AppInfo.kt`
- `Country.kt`
- `CountryCode.kt`
- `StripeFile.kt`
- `ApiRequest.kt`
- `StripeFileParams.kt`

### Wave 3: Okio transport

- `StripeRequest.kt`
- `ApiRequest.kt`
- `FileUploadRequest.kt`
- `AnalyticsRequestV2.kt`
- `StripeResponse.kt`
- `StripeNetworkClient.kt`
- `DefaultStripeNetworkClient.kt`
- `ConnectionFactory.kt`
- `StripeConnection.kt`

### Wave 4: common helpers

- `QueryStringFactory.kt`
- `AnalyticsRequestV2.kt`
- `RequestExecutor.kt` once supporting types are ready

### Wave 5: later Android-only follow-ups

- `frauddetection/*`
- `strings/*`
- `browser/*`
- `reactnative/*`
- Android analytics/storage/injection pieces

## Risks

### Main technical risks

- keeping Android API compatibility while removing `Parcelable` from common contracts
- preserving exact request encoding behavior during Okio migration
- avoiding accidental Android imports in `commonMain`
- publication/tooling churn from converting the existing module to KMP

### Main design trap to avoid

Do not let Parcelize or `HttpURLConnection` dictate the common API shape.

If those remain the center of the design, `stripe-core` will technically be “split into source sets” but not actually become a usable multiplatform foundation.

## Bottom line

The fastest sound plan is:

1. convert `stripe-core` to KMP structure
2. isolate Parcelize as Android-only compatibility glue
3. use Okio to replace `OutputStream` and `File`
4. commonize request/response and pure core types
5. leave fraud, Android strings/resources, browser/runtime integrations, and other Android subsystems for later

That gives `payments-model` and `payments-core` a real common foundation without forcing the entire current Android runtime stack into common code.
