# stripe-core: Kotlin Multiplatform Migration Plan

## Scope

Migrate `stripe-core` from an Android-only library to a Kotlin Multiplatform module,
enabling `commonMain` code that can be consumed by a future KMP `payments-core`.

This is a prerequisite for making the `com.stripe.android.networking` package in
`payments-core` available in a common source set.

## Current State

- **Kotlin**: 2.2.21 (post-2.0 — affects Parcelize KMP approach)
- **AGP**: 8.13.x, compileSdk 36, minSdk 23
- **Okio**: 3.16.4 already in dependency catalog (not yet used in stripe-core)
- **Build plugin**: `org.jetbrains.kotlin.multiplatform` is already enabled in `stripe-core/build.gradle`
- **KMP**: `stripe-core` now has `commonMain` and `androidMain`
- **Consumers**: 10 modules directly depend on `stripe-core` (4 via `api`, 6 via `implementation`)

## Progress Snapshot

The following types are already migrated into `commonMain`:

- `ApiKeyValidator`
- `ApiVersion`
- `AppInfo`
- `Logger` interface plus Android logger factory split
- `PlatformContext` via `actual typealias PlatformContext = android.content.Context`
- `StripeError`
- `StripeFile`
- `StripeFilePurpose`
- `StripeModel`
- `version/StripeSdkVersion`
- `networking/AnalyticsFields`
- `networking/AnalyticsEvent`
- `networking/AnalyticsRequestExecutor`
- `networking/ExponentialBackoffRetryDelaySupplier`
- `networking/LinearRetryDelaySupplier`
- `networking/MarkdownParser`
- `networking/MarkdownToHtmlSerializer`
- `networking/NetworkConstants`
- `networking/JsonUtils`
- `networking/RequestId`
- `networking/RetryDelaySupplier`
- `networking/ApiRequest`
- `browser/BrowserCapabilities`
- `model/Country`
- `model/CountryCode`
- `model/serializers/CountryListSerializer`
- `storage/Storage`
- `utils/DateUtils`
- `utils/DefaultDurationProvider` and `DurationProvider`
- `utils/FeatureFlags`
- `utils/ResultUtils`
- `utils/UserFacingLogger` interface
- `utils/IsWorkManagerAvailable` interface

Shared models use `CommonParcelize`, `CommonParcelable`, and
`CommonJavaSerializable` so Android keeps the real `Parcelable`/`Serializable`
behavior without forcing platform imports into `commonMain`.

Two migration decisions are now validated in code:

1. Keep `@RestrictTo` in `commonMain` where it still communicates intended
   visibility. `androidx.annotation` is already a multiplatform dependency, so
   these annotations are not themselves a blocker.
2. Prefer interface- or DI-based seams over `expect`/`actual` when there is an
   existing Android injection path. The retry-delay suppliers now live in
   `commonMain`, while Android Dagger wiring stays in `androidMain`. The same
   pattern now applies to `UserFacingLogger` and `IsWorkManagerAvailable`.
3. Use narrow `expect`/`actual` only for small platform primitives where DI
   would add more ceremony than value. `utils/urlEncode()` now follows this
   pattern so Android keeps `URLEncoder` behavior exactly.

## Official Kotlin Integration Guidance Applied Here

Kotlin's "integrate multiplatform into an existing app" guidance recommends:

1. introducing a shared module,
2. wiring the existing Android app to depend on it,
3. moving reusable business logic into `commonMain`, and
4. replacing JVM/Android dependencies with multiplatform ones before falling
   back to platform-specific hooks.

For Stripe, `stripe-core` is already the lowest reusable Android library and is
the right place to act as that shared module. We should therefore adapt
`stripe-core` in place rather than create a second wrapper module just to host
the shared code. Existing Android consumers keep depending on `stripe-core`
while packages are moved into `commonMain` incrementally.

Kotlin's Android-to-multiplatform migration guidance adds three constraints that
matter here:

1. Build and follow a module dependency graph, typically starting with a module
   that few others depend on.
2. Migrate Android/JVM-only dependencies before moving code into shared source
   sets.
3. Keep the project in working states with small, reversible steps.

Applied to this repo:
- `stripe-core` still goes first because it is the lowest practical foundation
  layer for Stripe networking and model code, even though many sibling modules
  depend on it.
- Downstream modules do **not** need to become KMP immediately; they can remain
  Android-only consumers of `stripe-core` while `commonMain` grows.
- Java/JVM calls in shareable code paths must either move behind interfaces into
  `androidMain` or be rewritten against multiplatform APIs such as Okio and
  coroutines before those files move.
- Where DI already exists, prefer interface-based seams so platform
  implementations can be injected consistently. Where DI does not exist and the
  need is only a tiny platform primitive, a narrow `expect`/`actual` can still
  be the simpler choice.

---

## File-by-File Classification

### Networking (`com.stripe.android.core.networking`) — 32 files

#### Already in `commonMain`

| File | Notes |
|------|-------|
| `RequestId.kt` | Pure value wrapper |
| `AnalyticsFields.kt` | Pure constants |
| `RetryDelaySupplier.kt` | Pure interface |
| `ExponentialBackoffRetryDelaySupplier.kt` | Shared implementation; Android Dagger now provides instances from `RetryDelayModule` |
| `LinearRetryDelaySupplier.kt` | Shared implementation; Android Dagger now provides instances from `RetryDelayModule` |
| `MarkdownParser.kt` | Pure regex/string logic |
| `MarkdownToHtmlSerializer.kt` | `kotlinx.serialization` serializer; common-safe |
| `NetworkConstants.kt` | Shared HTTP/header constants |
| `StripeRequest.kt` | Now in `commonMain`; `writePostBody()` uses `okio.BufferedSink` |
| `StripeResponse.kt` | Now in `commonMain`; uses inline HTTP status thresholds instead of `HttpURLConnection` constants |
| `StripeNetworkClient.kt` | Now in `commonMain`; file execution uses `okio.Path` |
| `QueryStringFactory.kt` | Now in `commonMain`; uses the existing `urlEncode` expect/actual seam |
| `AnalyticsRequest.kt` | Now in `commonMain`; pure GET request on top of shared query-string building |
| `AnalyticsRequestV2.kt` | Now in `commonMain`; uses `kotlin.uuid` for event IDs and `kotlin.time.Clock.System` for wall-clock timestamps |
| `AnalyticsRequestExecutor.kt` | Now in `commonMain`; pure interface over shared `AnalyticsRequest` |
| `ApiRequest.kt` | Now in `commonMain`; `Options` uses `CommonParcelize` / `CommonParcelable` and header generation now routes through the shared `RequestHeadersFactory` |
| `RequestHeadersFactory.kt` | Now in `commonMain`; `Locale` usage was reduced to `languageTag: String?`, and Android platform data comes from an internal `RequestHeadersPlatform` expect/actual helper |
| `StripeClientUserAgentHeaderFactory.kt` | Now in `commonMain`; uses `kotlinx.serialization.json` plus the shared `RequestHeadersPlatform` helper |

#### Can move to `commonMain` after targeted refactors

| File | Blockers | Fix |
|------|----------|-----|
| `JsonUtils.kt` | None after removing JVM-only type-name lookup | Already in `commonMain` |
| `RequestExecutor.kt` | Uses `responseJson()` → `JSONObject` and Android-only request implementations | Handle JSON abstraction and commonize request types first |
| `FileUploadRequest.kt` | `StripeFileParams.file: java.io.File` and `java.net.URLConnection.guessContentTypeFromName` | Keep Okio body writing; replace input file type/content-type resolution |

#### Must stay in `androidMain` (or be restructured)

| File | Why |
|------|-----|
| `DefaultStripeNetworkClient.kt` | Uses `ConnectionFactory` → `HttpsURLConnection`. Move to `androidMain` as the Android `StripeNetworkClient` implementation. |
| `ConnectionFactory.kt` | `java.net.URL`, `javax.net.ssl.HttpsURLConnection` — the actual HTTP transport. Android-only. |
| `StripeConnection.kt` | `HttpsURLConnection`, `java.io.InputStream/Scanner`, and Android file persistence via Okio `FileSystem` — Android HTTP response reader. |
| `AnalyticsRequestFactory.kt` | `android.content.pm.PackageInfo/PackageManager`, `android.os.Build` |
| `AnalyticsRequestV2Factory.kt` | `android.content.Context`, `android.os.Build`, `android.provider.Settings.Secure.ANDROID_ID` |
| `NetworkTypeDetector.kt` | `android.net.ConnectivityManager`, `android.telephony.TelephonyManager` |
| `DefaultAnalyticsRequestExecutor.kt` | Not Android-bound by imports, but its injected convenience constructor currently hardwires `DefaultStripeNetworkClient` | Keep in `androidMain` until the default-construction path is removed or split from the common executor |
| `AnalyticsRequestV2Executor.kt` | Uses `AnalyticsRequestV2Storage` which uses SharedPreferences |
| `AnalyticsRequestV2Storage.kt` | `android.content.Context`, `SharedPreferences` |
| `SendAnalyticsRequestV2Worker.kt` | `androidx.work.CoroutineWorker`, `WorkManager` — fully Android |
| `ResponseJson.kt` | `org.json.JSONObject` — parses response body to JSONObject |

### Models (`com.stripe.android.core.model`) — ~8 files

| File | Blockers | Fix |
|------|----------|-----|
| `StripeModel.kt` | Already in `commonMain` | Done via `CommonParcelable` |
| `StripeFile.kt` | Already in `commonMain` | Done via `CommonParcelize` |
| `StripeFileParams.kt` | `@Parcelize`, `java.io.File` | Parcelize KMP; replace `File` with `okio.Path` |
| `StripeError.kt` | Already in `commonMain` | Done via `CommonParcelize` + `CommonJavaSerializable` |
| `parsers/ModelJsonParser.kt` | `org.json.JSONObject` | Phase 2 — JSON abstraction |
| `parsers/StripeErrorJsonParser.kt` | `org.json.JSONObject` | Phase 2 — JSON abstraction |
| Other parsers | `org.json.JSONObject` | Phase 2 |

### Exceptions (`com.stripe.android.core.exception`) — 13 files

| File | Blockers | Fix |
|------|----------|-----|
| `StripeException.kt` | `org.json.JSONException`, `java.io.IOException`, `.javaClass` usage | Replace `JSONException` ref with general approach; `IOException` → `okio.IOException` (re-exported by Okio); `.javaClass` → platform-specific analytics |
| All other exceptions | `@RestrictTo` only | Drop annotations — all are pure Kotlin classes extending `StripeException` |

### Core Utilities

| File | Blockers | Disposition |
|------|----------|-------------|
| `Logger.kt` | `android.util.Log` | Already split: interface/common factory surface in `commonMain`, Android logger implementation in `androidMain` |
| `ApiVersion.kt` | None | Already in `commonMain` |
| `AppInfo.kt` | `@Parcelize`, `Parcelable` | Already in `commonMain` via `CommonParcelize` + `CommonParcelable` |
| `StripeError.kt` | See models above | Already in `commonMain`; `Serializable` behavior preserved via shim |
| `ApiKeyValidator.kt` | None likely | Already in `commonMain` |
| `version/StripeSdkVersion.kt` | None likely | Already in `commonMain` |

### Storage (`com.stripe.android.core.storage`)

| File | Blockers | Disposition |
|------|----------|-------------|
| `Storage.kt` | None after split | Already in `commonMain` |
| `StorageFactory` | `android.content.Context` | `androidMain` |
| `SharedPreferencesStorage` | `android.content.SharedPreferences`, `android.util.Log` | `androidMain` |

### Android-Only Utilities (stay in `androidMain` entirely)

- `utils/ContextUtils.kt` — `Context.packageInfo` extension
- `utils/StatusBarCompat.kt` — Status bar color (uses `Activity`, `Build.VERSION`)
- `utils/CreationExtrasKtx.kt` — ViewModel factory (AndroidX)
- `utils/PluginDetector.kt` — reflection plus Android logging
- `browser/BrowserCapabilitiesSupplier.kt` — Chrome Custom Tabs
- `reactnative/*` — React Native bridge
- `injection/*` — Dagger modules

### Fraud Detection (`com.stripe.android.core.frauddetection`)

| File | Blockers | Disposition |
|------|----------|-------------|
| `FraudDetectionDataRepository.kt` (interface) | `@RestrictTo` | Interface → `commonMain` |
| `FraudDetectionData.kt` | `@Parcelize`, `StripeModel`, `org.json.JSONObject`, `TimeUnit` | Not first-wave common; move only after Parcelize and JSON cleanup |
| `FraudDetectionDataStore.kt` | `SharedPreferences`, `org.json.JSONObject` | `androidMain` |
| Other impls | `Context` | `androidMain` |

---

## Migration Phases

### Phase 1: Gradle Setup + Parcelize KMP + StripeModel

**Goal**: Convert `stripe-core` into the shared KMP module for existing Android
consumers, then start moving the lowest-risk business logic into `commonMain`.

#### 1a. New build configuration

Replace the current `android-library.gradle` approach with a KMP build script.

```kotlin
// stripe-core/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.parcelize")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("dev.drewhamilton.poko")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            // Kotlin 2.0+ Parcelize KMP: register custom annotation as Parcelize trigger
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.stripe.android.core.model.CommonParcelize"
            )
        }
    }
    // Future: iosArm64(), iosSimulatorArm64(), iosX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization)
            api(libs.okio)
        }
        androidMain.dependencies {
            implementation(libs.dagger)
            implementation(libs.androidx.annotation)
            implementation(libs.androidx.browser)
            implementation(libs.androidx.activity)
            // ... other Android-specific deps
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.truth)
            // ... existing test deps
        }
    }
}

android {
    namespace = "com.stripe.android.core"
    compileSdk = 36
    defaultConfig { minSdk = 23 }
    // ... rest of android config
}
```

Keep the KMP Android minSdk aligned with the current Android library minimum
(`23` today), matching Kotlin's existing-app integration guidance.

#### 1b. Parcelize KMP (Kotlin 2.0+ approach)

Since Kotlin 2.2.21 does not support `typealias` triggering compiler plugins,
use the `additionalAnnotation` approach:

**`commonMain`** — define annotation and interface:

```kotlin
// com.stripe.android.core.model.CommonParcelize
package com.stripe.android.core.model

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CommonParcelize

// com.stripe.android.core.model.CommonParcelable
package com.stripe.android.core.model

expect interface CommonParcelable

// com.stripe.android.core.model.CommonIgnoredOnParcel
package com.stripe.android.core.model

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
expect annotation class CommonIgnoredOnParcel()
```

**`androidMain`** — actual declarations:

```kotlin
package com.stripe.android.core.model

actual typealias CommonParcelable = android.os.Parcelable
actual typealias CommonIgnoredOnParcel = kotlinx.parcelize.IgnoredOnParcel
```

Note: `CommonParcelize` has NO `expect`/`actual`. Instead, the
`additionalAnnotation` compiler argument tells the Parcelize plugin to treat
`@CommonParcelize` the same as `@Parcelize` on Android. On non-Android targets,
it's inert.

If and when non-Android targets are added, they need empty/no-op `actual`
declarations for `CommonParcelable` and `CommonIgnoredOnParcel`. Parcelize
itself remains Android-only.

#### 1c. Migrate `StripeModel`

```kotlin
// commonMain
package com.stripe.android.core.model

interface StripeModel : CommonParcelable
```

On Android, `CommonParcelable` resolves to `android.os.Parcelable`, so
`StripeModel` still implements `Parcelable`. On other platforms,
`CommonParcelable` is an empty interface.

All existing model classes change `@Parcelize` → `@CommonParcelize` and
`Parcelable` → `CommonParcelable`. Same for `@IgnoredOnParcel` →
`@CommonIgnoredOnParcel`.

**Impacted files in stripe-core**:
- `StripeModel.kt`
- `StripeFile.kt`
- `StripeFileParams.kt` (also needs `java.io.File` → `okio.Path`)
- `StripeError.kt` (handle `java.io.Serializable` as a compatibility decision)
- `AppInfo.kt`
- `ApiRequest.Options`

### Phase 2: Okio Integration + Core Networking Abstractions

**Goal**: Replace `java.io.*` and `java.net.*` types in common networking interfaces with
Okio equivalents and platform-neutral constants.

This seam is now complete for the transport contracts:
- `StripeRequest` moved to `commonMain` with `writePostBody(sink: BufferedSink)`
- `StripeResponse` moved to `commonMain` with inline status thresholds (`200`, `300`)
- `StripeNetworkClient` moved to `commonMain` with `executeRequestForFile(..., outputFile: Path)`
- `QueryStringFactory` moved to `commonMain` using the existing `urlEncode` expect/actual seam
- `AnalyticsRequest` moved to `commonMain` on top of the shared query-string builder
- Android transport implementations (`ConnectionFactory`, `StripeConnection`, `DefaultStripeNetworkClient`) now adapt `HttpsURLConnection` to Okio
- Downstream Android consumers (`identity`, `network-testing`, and affected unit tests) were updated to the new Okio seam

The next meaningful networking boundary is no longer the transport contract itself.
`ApiRequest` is now shared as well, so the remaining request-building boundary is
`FileUploadRequest`, `AnalyticsRequestV2`, and then `RequestExecutor` once the
JSON dependency is abstracted.

#### 2a. Replace `java.io.OutputStream` with `okio.BufferedSink`

`StripeRequest.writePostBody()` is the key method:

```kotlin
// Previous
open fun writePostBody(outputStream: OutputStream) {}

// Current (commonMain)
open fun writePostBody(sink: okio.BufferedSink) {}
```

`ApiRequest`, `FileUploadRequest`, `AnalyticsRequestV2`, `FraudDetectionDataRequest`,
and Identity’s `IdentityFileUploadRequest` now override the sink-based method.

The Android `ConnectionFactory` now wraps `HttpsURLConnection.outputStream` in
`outputStream.sink().buffer()`.

#### 2b. Replace `java.io.File` with `okio.Path`

Completed transport signatures:
- `StripeNetworkClient.executeRequestForFile(request, outputFile: File)` → `outputFile: okio.Path`
- `DefaultStripeNetworkClient.executeRequestForFile` → `StripeResponse<okio.Path>`
- `ConnectionFactory.createForFile(...)` → `StripeConnection<okio.Path>`
- `StripeConnection.FileConnection` now persists to `okio.Path` via `okio.FileSystem`

Still pending:
- `StripeFileParams.file: File` → `file: okio.Path`
- `FileUploadRequest` input file access should move fully to `okio.FileSystem.SYSTEM.source(path)`

#### 2c. Replace `HttpURLConnection` constants in `StripeResponse`

```kotlin
// Previous
val isOk: Boolean = code == HttpURLConnection.HTTP_OK
val isError: Boolean = code < HttpURLConnection.HTTP_OK || code >= HTTP_MULT_CHOICE

// Current (commonMain)
val isOk: Boolean = code == 200
val isError: Boolean = code < 200 || code >= 300
```

This is now done in `commonMain`.

#### 2d. `QueryStringFactory` — URL encoding

`java.net.URLEncoder.encode()` is JVM-only. Options:
1. Use a tiny `expect` declaration in `commonMain`
2. Keep the Android `actual` backed by `java.net.URLEncoder.encode()`

Recommended: use `expect/actual` here instead of a manual common implementation.
The encoding must stay byte-for-byte compatible with the existing Android/JVM
form-encoding behavior, and this seam is small enough that DI would not help.

This is now done by routing `QueryStringFactory` through the existing shared
`urlEncode()` expect/actual function.

#### 2e. `ApiRequest` — current state

This step is now done:
- `ApiRequest.kt` moved to `commonMain`
- `ApiRequest.Options` now uses `@CommonParcelize` / `CommonParcelable`
- the provider-based secondary constructor remains for API continuity, but Dagger
  creation moved into Android `CoreCommonModule`
- `RequestHeadersFactory` and `StripeClientUserAgentHeaderFactory` also moved to
  `commonMain`
- `Locale` handling in request headers was reduced to a `languageTag: String?`
  input instead of carrying `Locale` through the shared API
- Android-only platform reads now live behind the internal
  `RequestHeadersPlatform` expect/actual helper

What still remains around this area is downstream cleanup, not `ApiRequest`
itself:
- `FileUploadRequest` still depends on `java.io.File` and content-type guessing
- analytics factories still read Android platform/app state directly

### Phase 3: Logger, Storage, and Platform Info Interfaces

**Goal**: Extract interfaces for platform-specific services into `commonMain`,
implementations into `androidMain`.

#### 3a. Logger

Already well-structured. `Logger` is an interface with `debug/info/warning/error`.

Move to `commonMain`:
```kotlin
// commonMain
interface Logger {
    fun debug(msg: String)
    fun info(msg: String)
    fun warning(msg: String)
    fun error(msg: String, t: Throwable? = null)

    companion object {
        fun noop(): Logger = NoopLogger
    }
}

private object NoopLogger : Logger {
    override fun debug(msg: String) {}
    override fun info(msg: String) {}
    override fun warning(msg: String) {}
    override fun error(msg: String, t: Throwable?) {}
}
```

Move to `androidMain`:
```kotlin
// androidMain
fun Logger.Companion.real(): Logger = AndroidLogger
fun Logger.Companion.getInstance(enableLogging: Boolean): Logger =
    if (enableLogging) real() else noop()

private object AndroidLogger : Logger {
    private const val TAG = "StripeSdk"
    override fun debug(msg: String) { Log.d(TAG, msg) }
    override fun info(msg: String) { Log.i(TAG, msg) }
    override fun warning(msg: String) { Log.w(TAG, msg) }
    override fun error(msg: String, t: Throwable?) { Log.e(TAG, msg, t) }
}
```

#### 3b. Storage

Completed.

- `Storage` interface now lives in `commonMain`
- `StorageFactory` stays in `androidMain`
- `SharedPreferencesStorage` stays in `androidMain`

#### 3c. PlatformInfo interface (NEW)

Consolidates scattered `Build.*`, `Context.packageManager`, `Settings.Secure`,
and `Os.getenv()` calls into a single injectable interface:

This is no longer required for request-header generation. `RequestHeadersFactory`
and `StripeClientUserAgentHeaderFactory` already moved to `commonMain` using a
much narrower internal `RequestHeadersPlatform` expect/actual helper, which fits
this low-level code better than introducing DI just to read a few platform
fields.

```kotlin
// commonMain
interface PlatformInfo {
    /** e.g. "35" (SDK_INT) on Android */
    val osVersion: String

    /** e.g. "Android" */
    val osName: String

    /** e.g. "samsung" */
    val deviceManufacturer: String?

    /** e.g. "SM-G991B" */
    val deviceModel: String?

    /** Persistent device identifier, null if unavailable */
    val deviceId: String?

    /** App package name / bundle ID */
    val packageName: String?

    /** App version string */
    val packageVersion: String?

    /** e.g. "wifi", "4g", null if unavailable */
    val networkType: String?

    /** Stripe-Livemode environment variable, null if not set */
    val stripeLivemodeEnv: String?
}
```

**`androidMain`** implementation:
```kotlin
// androidMain
class AndroidPlatformInfo(context: Context) : PlatformInfo {
    private val appContext = context.applicationContext
    override val osVersion = Build.VERSION.SDK_INT.toString()
    override val osName = "Android"
    override val deviceManufacturer = Build.MANUFACTURER
    override val deviceModel = Build.MODEL
    override val deviceId: String? = Settings.Secure.getString(
        appContext.contentResolver, Settings.Secure.ANDROID_ID
    )
    override val packageName = appContext.packageName
    override val packageVersion = appContext.packageInfo?.versionName
    override val networkType: String? = NetworkTypeDetector(appContext).invoke()
    override val stripeLivemodeEnv: String? = Os.getenv("Stripe-Livemode")
}
```

This is still most relevant for:
- `AnalyticsRequestFactory` — package info, OS version
- `AnalyticsRequestV2Factory` — device ID, device info
- `NetworkTypeDetector` — wrapped into `PlatformInfo.networkType`

#### 3d. ContentTypeResolver interface (NEW)

`FileUploadRequest` uses `java.net.URLConnection.guessContentTypeFromName()`:

```kotlin
// commonMain
fun interface ContentTypeResolver {
    fun resolveContentType(fileName: String): String?
}

// androidMain
object AndroidContentTypeResolver : ContentTypeResolver {
    override fun resolveContentType(fileName: String): String? =
        java.net.URLConnection.guessContentTypeFromName(fileName)
}
```

### Phase 4: JSON Abstraction Layer

**Goal**: Decouple `ModelJsonParser` and response-parsing from `org.json.JSONObject`.

This is the most impactful change because `org.json.JSONObject` is used in:
- `ResponseJson.kt` — converts `StripeResponse<String>` body to `JSONObject`
- `ModelJsonParser<T>.parse(json: JSONObject)` — every model parser
- `StripeErrorJsonParser` — error response parsing
- `StripeClientUserAgentHeaderFactory` — builds JSON for User-Agent header
- `FraudDetectionDataStore` — serializes/deserializes fraud data

#### Recommended approach: two parallel parser contracts

`RequestExecutor.kt` already has `executeRequestWithKSerializerParser()` using
`kotlinx.serialization`. This is the future direction. But migrating all ~30
`ModelJsonParser` implementations at once is high-risk.

**Strategy**: Support both patterns during migration.

```kotlin
// commonMain — new common parser interface (works with raw String)
interface StripeModelParser<out T : StripeModel> {
    fun parse(jsonString: String): T?
}

// androidMain — bridge to existing ModelJsonParser
class ModelJsonParserAdapter<out T : StripeModel>(
    private val legacyParser: ModelJsonParser<T>
) : StripeModelParser<T> {
    override fun parse(jsonString: String): T? {
        return legacyParser.parse(JSONObject(jsonString))
    }
}
```

`fetchStripeModelResult` in `StripeApiRepository` (payments-core) can then accept
either `StripeModelParser<T>` (common) or `ModelJsonParser<T>` (Android, via adapter).

Over time, individual parsers migrate from `ModelJsonParser` (using `JSONObject`)
to either:
1. `kotlinx.serialization` `@Serializable` + `KSerializer` (preferred for new models)
2. `StripeModelParser` with manual string/kotlinx.serialization.json parsing (for gradual migration)

**`ResponseJson.kt`** stays in `androidMain` since it creates `org.json.JSONObject`.
In `commonMain`, response body stays as `String` and goes to `StripeModelParser.parse(String)`.

### Phase 5: Exception Layer + StripeNetworkClient Contract Finalization

**Goal**: Move exception hierarchy and the network client interface to `commonMain`.

#### 5a. Exceptions

Almost all exceptions are pure Kotlin. Changes needed:

- `StripeException.kt`:
  - Remove `org.json.JSONException` import — replace with a general catch in the
    factory method, or move `create(throwable)` to `androidMain`
  - `java.io.IOException` → `okio.IOException` (Okio re-exports it for KMP)
  - `.javaClass.name` in `analyticsValueForThrowable` → move this helper to `androidMain`

- All other exceptions (`APIException`, `APIConnectionException`, etc.): only need
  `@RestrictTo` removed. They're pure data carriers extending `StripeException`.

#### 5b. Finalize StripeNetworkClient in commonMain

```kotlin
// commonMain
interface StripeNetworkClient {
    suspend fun executeRequest(request: StripeRequest): StripeResponse<String>
    suspend fun executeRequestForFile(request: StripeRequest, outputFile: okio.Path): StripeResponse<okio.Path>
}
```

`DefaultStripeNetworkClient` stays in `androidMain` as the `HttpsURLConnection`-based implementation.

`ConnectionFactory` and `StripeConnection` stay in `androidMain` — they are
implementation details of the Android HTTP transport.

### Phase 6: Move Remaining Android-Only Code

Everything that can't be in `commonMain` goes to `androidMain`:

- `browser/*` — Chrome Custom Tabs
- `injection/*` — Dagger modules
- `reactnative/*` — React Native bridge
- `utils/ContextUtils.kt`
- `utils/StatusBarCompat.kt`
- `utils/CreationExtrasKtx.kt`
- `utils/PluginDetector.kt`
- `frauddetection/` implementations (stores, default repository)
- `SendAnalyticsRequestV2Worker.kt`
- Analytics factories that use `PlatformInfo` Android impl
- `ResponseJson.kt`

---

## Source Set Layout (Final State)

```
stripe-core/
├── src/
│   ├── commonMain/kotlin/com/stripe/android/core/
│   │   ├── ApiKeyValidator.kt
│   │   ├── ApiVersion.kt
│   │   ├── AppInfo.kt                    (@CommonParcelize)
│   │   ├── Logger.kt                     (interface + noop)
│   │   ├── StripeError.kt                (@CommonParcelize)
│   │   ├── exception/
│   │   │   ├── StripeException.kt
│   │   │   ├── APIException.kt
│   │   │   ├── APIConnectionException.kt
│   │   │   ├── AuthenticationException.kt
│   │   │   ├── InvalidRequestException.kt
│   │   │   ├── PermissionException.kt
│   │   │   ├── RateLimitException.kt
│   │   │   └── ... (all other exceptions)
│   │   ├── model/
│   │   │   ├── CommonParcelize.kt         (annotation)
│   │   │   ├── CommonParcelable.kt        (expect interface)
│   │   │   ├── CommonIgnoredOnParcel.kt   (expect annotation)
│   │   │   ├── StripeModel.kt             (: CommonParcelable)
│   │   │   ├── StripeFile.kt              (@CommonParcelize)
│   │   │   ├── StripeFileParams.kt        (okio.Path instead of File)
│   │   │   ├── StripeFilePurpose.kt
│   │   │   └── StripeModelParser.kt       (new: parse from String)
│   │   ├── networking/
│   │   │   ├── StripeRequest.kt           (okio.BufferedSink)
│   │   │   ├── StripeResponse.kt          (inline constants)
│   │   │   ├── StripeNetworkClient.kt     (okio.Path)
│   │   │   ├── ApiRequest.kt              (@CommonParcelize on Options)
│   │   │   ├── AnalyticsRequest.kt
│   │   │   ├── AnalyticsRequestV2.kt
│   │   │   ├── AnalyticsRequestExecutor.kt (interface)
│   │   │   ├── AnalyticsFields.kt
│   │   │   ├── FileUploadRequest.kt       (okio.Path + okio.FileSystem)
│   │   │   ├── NetworkConstants.kt
│   │   │   ├── QueryStringFactory.kt      (common URL encoding)
│   │   │   ├── RequestHeadersFactory.kt   (shared headers + languageTag)
│   │   │   ├── RequestHeadersPlatform.kt  (internal expect helpers)
│   │   │   ├── RequestId.kt
│   │   │   ├── RetryDelaySupplier.kt
│   │   │   ├── ExponentialBackoffRetryDelaySupplier.kt
│   │   │   ├── LinearRetryDelaySupplier.kt
│   │   │   ├── StripeClientUserAgentHeaderFactory.kt
│   │   │   ├── MarkdownParser.kt
│   │   │   └── MarkdownToHtmlSerializer.kt
│   │   ├── storage/
│   │   │   └── Storage.kt                 (interface only)
│   │   ├── frauddetection/
│   │   │   └── FraudDetectionDataRepository.kt (interface)
│   │   ├── platform/
│   │   │   └── PlatformInfo.kt
│   │   └── version/
│   │       └── StripeSdkVersion.kt
│   │
│   ├── androidMain/kotlin/com/stripe/android/core/
│   │   ├── Logger.android.kt              (real() logger with android.util.Log)
│   │   ├── model/
│   │   │   ├── CommonParcelable.android.kt (actual = Parcelable)
│   │   │   ├── CommonIgnoredOnParcel.android.kt
│   │   │   ├── parsers/                   (all existing ModelJsonParser impls)
│   │   │   │   ├── ModelJsonParser.kt     (uses org.json.JSONObject)
│   │   │   │   ├── StripeErrorJsonParser.kt
│   │   │   │   └── ...
│   │   │   └── ModelJsonParserAdapter.kt  (bridges to StripeModelParser)
│   │   ├── networking/
│   │   │   ├── ConnectionFactory.kt       (HttpsURLConnection)
│   │   │   ├── StripeConnection.kt        (HttpsURLConnection wrapper)
│   │   │   ├── DefaultStripeNetworkClient.kt
│   │   │   ├── DefaultAnalyticsRequestExecutor.kt
│   │   │   ├── RequestHeadersPlatform.android.kt
│   │   │   ├── AnalyticsRequestFactory.kt
│   │   │   ├── AnalyticsRequestV2Factory.kt
│   │   │   ├── AnalyticsRequestV2Executor.kt
│   │   │   ├── AnalyticsRequestV2Storage.kt
│   │   │   ├── NetworkTypeDetector.kt
│   │   │   ├── SendAnalyticsRequestV2Worker.kt
│   │   │   ├── ResponseJson.kt            (org.json.JSONObject)
│   │   │   └── RequestExecutor.kt         (until ModelJsonParser is retired)
│   │   ├── platform/
│   │   │   └── AndroidPlatformInfo.kt
│   │   ├── storage/
│   │   │   ├── StorageFactory.kt
│   │   │   └── SharedPreferencesStorage.kt
│   │   ├── frauddetection/
│   │   │   ├── FraudDetectionData.kt      (until Parcelize + JSON cleanup is done)
│   │   │   ├── FraudDetectionDataStore.kt
│   │   │   ├── FraudDetectionDataParamsUtils.kt
│   │   │   └── DefaultFraudDetectionDataRepository.kt
│   │   ├── injection/                     (all Dagger modules)
│   │   ├── browser/                       (Chrome Custom Tabs)
│   │   ├── reactnative/
│   │   ├── strings/                       (Context/resource-backed string types)
│   │   └── utils/                         (Context-dependent utils)
│   │
│   ├── commonTest/kotlin/                 (pure Kotlin tests)
│   └── androidUnitTest/kotlin/            (existing tests with Robolectric)
```

---

## Interface vs. `expect`/`actual` Decision Matrix

| Abstraction | Approach | Rationale |
|------------|----------|-----------|
| `CommonParcelable` | **`expect`/`actual`** | Must be a supertype. Only way to make `StripeModel : Parcelable` work on Android while being empty on other targets. |
| `CommonIgnoredOnParcel` | **`expect`/`actual`** | Annotation — needs to resolve to `@IgnoredOnParcel` on Android. |
| `StripeNetworkClient` | **Interface** | Behavior contract. Android impl uses `HttpsURLConnection`; iOS would use `NSURLSession` or Ktor. |
| `Logger` | **Interface** | Multiple implementations, platform-specific backends. Already an interface. |
| `Storage` | **Interface** | Already an interface. Android uses SharedPreferences, others use platform keychain/files. |
| `PlatformInfo` | **Interface** | Consolidates many scattered platform calls. Injected as a dependency. |
| `FraudDetectionDataRepository` | **Interface** | Already an interface. |
| `ContentTypeResolver` | **Interface** | Simple behavior contract. |
| `StripeModelParser` | **Interface** | Behavior contract for JSON parsing. |
| `AnalyticsRequestExecutor` | **Interface** | Already an interface. |
| `RetryDelaySupplier` | **Interface** | Already an interface. |

Only Parcelize-related types use `expect`/`actual` — everything else uses interfaces.

Practical rule for this repo:
- Prefer interfaces for long-lived service seams (`Logger`, `Storage`,
  `StripeNetworkClient`, `PlatformInfo`).
- Prefer interfaces especially where Dagger already constructs or supplies the
  dependency on Android, because that keeps platform injection using the same
  mechanism instead of mixing DI with ad hoc `expect`/`actual` factories.
- Use `expect`/`actual` only for tiny leaf-level platform hooks where an
  interface would add indirection without buying testability, such as
  Parcelize shims and, if needed later, narrow helpers like `randomUUID()`.

---

## Dependency Changes

### New `commonMain` dependencies
```
kotlinx-coroutines-core    (already KMP — replace coroutinesAndroid in android source set)
kotlinx-serialization-json (already KMP)
okio                       (3.16.4, already in catalog, KMP-ready)
```

### Removed from common (moved to androidMain)
```
dagger / javax.inject       → androidMain only
androidx.annotation         → androidMain only (replace @RestrictTo with `internal` or keep `public` for sibling-module APIs)
androidx.browser            → androidMain only
androidx.activity           → androidMain only
androidx.work               → androidMain only
org.json                    → androidMain only (bundled with Android)
```

### `@RestrictTo` Replacement Strategy

`@RestrictTo(LIBRARY_GROUP)` is used pervasively (~192 times under `stripe-core/src/main/java`). In KMP:
- **Within a single module**: replace with `internal` keyword
- **Cross-module access** (LIBRARY_GROUP allows sister modules to access): the
  class must be `public` in common. Since `payments-core` needs access, these
  types stay `public` but are documented as internal API.
- **Concrete examples**: `RequestId` and `HTTP_TOO_MANY_REQUESTS` stay `public`
  because `payments-core` imports them today.
- **LIBRARY_GROUP_PREFIX**: same treatment — `public` in common, documented as
  internal via KDoc.

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Binary compatibility breakage | High | Run `binary-compatibility-validator` after each phase. API dump will change — review carefully. |
| 10 direct consumer modules break | High | Migrate in a single atomic commit per phase. All consumers still target Android, so `androidMain` symbols remain accessible. |
| `org.json.JSONObject` removal affects parsers | High | Phase 4 uses adapter pattern — existing parsers keep working. Migrate one-by-one. |
| Okio version conflicts | Low | Already in dependency catalog at 3.16.4. Transitive via OkHttp in some modules anyway. |
| Test migration effort | Medium | Most tests use Robolectric → stay in `androidUnitTest`. Pure logic tests can move to `commonTest` over time. |
| `QueryStringFactory` URL encoding | Low | Keep URL encoding behind a narrow `expect`/`actual` seam. Test against existing behavior exhaustively. |
| Dagger injection breaks in common code | Medium | Common code uses constructor parameters and interfaces. Dagger wiring stays in `androidMain`. |

---

## Execution Order

```
Phase 1  ─── Gradle KMP setup + Parcelize KMP + StripeModel ───────── Foundation
   │
Phase 2  ─── Okio integration (BufferedSink, Path, constants) ─────── Completed for transport interfaces
   │
Phase 3  ─── Logger, Storage, PlatformInfo interfaces ─────────────── Unblocks analytics/platform code
   │
Phase 4  ─── JSON abstraction (StripeModelParser, adapter) ────────── Unblocks parser migration
   │
Phase 5  ─── Exceptions + StripeNetworkClient finalization ────────── Networking contract complete
   │
Phase 6  ─── Move remaining Android code to androidMain ───────────── Cleanup
   │
   └──────── payments-core networking can now depend on stripe-core commonMain
```

Each phase is independently shippable and testable. The Android-only consumer
experience should not change at any phase — all existing APIs remain available
through `androidMain`.
