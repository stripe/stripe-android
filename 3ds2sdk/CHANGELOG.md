# CHANGELOG

## 6.2.0 - 2024-11-19
- [#9639](https://github.com/stripe/stripe-android/pull/9639) Update 3DS spec to 2.2

## 6.1.8 - 2024-01-25
- [#11535](https://git.corp.stripe.com/stripe-internal/android/pull/11535) Add tink to proguard rules.

## 6.1.7 - 2023-02-27
- [#8235](https://git.corp.stripe.com/stripe-internal/android/pull/8235) Minimize proguard rules.

## 6.1.6 - 2023-01-30
- [#7995](https://git.corp.stripe.com/stripe-internal/android/pull/7995) Upgrade license file referenced from release to make it easier for external tools to process.

## 6.1.3 - 2021-12-08
- [#4502](https://git.corp.stripe.com/stripe-internal/android/pull/4502) Upgrade nimbus-jose-jwt to 9.15.2 
- [#4741](https://git.corp.stripe.com/stripe-internal/android/pull/4741) Add support for new languages: HR, ID, MS, TH, FIL, VI

## 6.1.2 - 2021-08-30
- [#3979](https://git.corp.stripe.com/stripe-internal/android/pull/3979) Use postValue in `ChallengeActivityViewModel`

## 6.1.1 - 2021-07-15
- [#3581](https://git.corp.stripe.com/stripe-internal/android/pull/3581) Upgrade `nimbus-jose-jwt` to `9.11.1`
- [#3588](https://git.corp.stripe.com/stripe-internal/android/pull/3588) Add `android:exported` property to `ChallengeActivity` manifest entry

## 6.1.0 - 2021-07-13
- [#3543](https://git.corp.stripe.com/stripe-internal/android/pull/3543) Upgrade `nimbus-jose-jwt` to `9.11`
- [#3544](https://git.corp.stripe.com/stripe-internal/android/pull/3544) Upgrade coroutines to `1.5.1`
- [#3549](https://git.corp.stripe.com/stripe-internal/android/pull/3549) Upgrade `core-ktx` to `1.6.0`
- [#3551](https://git.corp.stripe.com/stripe-internal/android/pull/3551) Upgrade `material-components` to `1.4.0`
- [#3564](https://git.corp.stripe.com/stripe-internal/android/pull/3564) Add UnionPay support

## 6.0.2 - 2021-07-01
- [#3461](https://git.corp.stripe.com/stripe-internal/android/pull/3461) Upgrade `nimbus-jose-jwt` to `9.10.1`
- [#3471](https://git.corp.stripe.com/stripe-internal/android/pull/3471) Pass `SdkTransactionId` into `StripeThreeDs2Service.createTransaction()`
- [#3488](https://git.corp.stripe.com/stripe-internal/android/pull/3488) Create `InitChallengeRepository`

## 6.0.1 - 2021-06-24
- [#3439](https://git.corp.stripe.com/stripe-internal/android/pull/3439) Change `Transaction.createIntent()` to `createTransactionArgs()`

## 6.0.0 - 2021-06-24
- [#2931](https://git.corp.stripe.com/stripe-internal/android/pull/2931) Clear `ChallengeStatusReceiverProvider` when `StripeTransaction` is closed
- [#3289](https://git.corp.stripe.com/stripe-internal/android/pull/3289) Upgrade `nimbus-jose-jwt` to `9.10.0`
- [#3298](https://git.corp.stripe.com/stripe-internal/android/pull/3298) Add Cartes Bancaires support to 3DS2 loading screen
- [#3318](https://git.corp.stripe.com/stripe-internal/android/pull/3318) Create `ChallengeResult` and set result on `ChallengeActivity`
- [#3319](https://git.corp.stripe.com/stripe-internal/android/pull/3319) Refactor `Stripe3ds2ActivityStarterHost`
- [#3321](https://git.corp.stripe.com/stripe-internal/android/pull/3321) Inject `CoroutineContext` to `DefaultAuthenticationRequestParametersFactory`
- [#3332](https://git.corp.stripe.com/stripe-internal/android/pull/3332) Cleanup `ChallengeProgressActivity`
- [#3336](https://git.corp.stripe.com/stripe-internal/android/pull/3336) Create `ChallengeProgressDialogFragment`
- [#3345](https://git.corp.stripe.com/stripe-internal/android/pull/3345) Upgrade `org.bouncycastle:bcprov-jdk15to18` to `1.69`
- [#3348](https://git.corp.stripe.com/stripe-internal/android/pull/3348) Remove UL certification test interfaces
- [#3432](https://git.corp.stripe.com/stripe-internal/android/pull/3432) Finalize migrating 3DS2 challenge flow to use Activity Result API

## 5.3.1 - 2021-04-27
- [#2904](https://git.corp.stripe.com/stripe-internal/android/pull/2904) Add Cartes Bancaires Directory server info
- [#2905](https://git.corp.stripe.com/stripe-internal/android/pull/2905) Use a default KeyUse value for unknown 3DS2 directory servers

## 5.3.0 - 2021-04-23
- [#2856](https://git.corp.stripe.com/stripe-internal/android/pull/2856) Update 3DS2 SDK's brand lookup logic
- [#2857](https://git.corp.stripe.com/stripe-internal/android/pull/2857) Upgrade AndroidX Fragment to 1.3.3

## 5.2.2 - 2021-04-16
- [#2769](https://git.corp.stripe.com/stripe-internal/android/pull/2769) Correctly set 3DS2 SDK release artifact's POM details
- [#2808](https://git.corp.stripe.com/stripe-internal/android/pull/2808) Migrate 3DS2 SDK's Nexus publishing plugin

## 5.2.1 - 2021-04-13
- [#2581](https://git.corp.stripe.com/stripe-internal/android/pull/2581) Upgrade AndroidX dependencies
  - `activity-ktx` to `1.2.2`
  - `fragment-ktx` to `1.3.2`
  - `lifecycle-livedata-ktx` to `2.3.1`
- [#2718](https://git.corp.stripe.com/stripe-internal/android/pull/2718) Upgrade `nimbus-jose-jwt` to `9.8.1`
- [#2744](https://git.corp.stripe.com/stripe-internal/android/pull/2744) Upgrade Kotlin to `1.4.32`

## 5.2.0 - 2021-02-11
- [#2231](https://git.corp.stripe.com/stripe-internal/android/pull/2231) Upgrade `nimbus-jose-jwt` to `9.5`
- [#2238](https://git.corp.stripe.com/stripe-internal/android/pull/2238) Upgrade Kotlin to `1.4.30`
- [#2246](https://git.corp.stripe.com/stripe-internal/android/pull/2246) Upgrade `material-components` to `1.3.0`
- [#2255](https://git.corp.stripe.com/stripe-internal/android/pull/2255) Upgrade `activity-ktx` to `1.2.0` and `fragment-ktx` to `1.3.0`
- [#2256](https://git.corp.stripe.com/stripe-internal/android/pull/2256) Upgrade `androidx.lifecycle` to `2.3.0`
- [#2260](https://git.corp.stripe.com/stripe-internal/android/pull/2260) Migrate `ProgressBar` to `CircularProgressIndicator`

## 5.1.1 - 2021-01-29
- [#2209](https://git.corp.stripe.com/stripe-internal/android/pull/2209) Refactor TransactionTimer logic
- [#2218](https://git.corp.stripe.com/stripe-internal/android/pull/2218) Upgrade `nimbus-jose-jwt` to `9.4.2`

## 5.1.0 - 2021-01-26
This release represents a refactor of the challenge screen logic. Previously, each step of the
challenge flow was represented by a `ChallengeActivity` instance. This has been replaced with a
single `ChallengeActivity`. Each screen of the challenge flow is represented by a
`ChallengeFragment` instance.

Additionally, `ChallengeStatusReceiver` has been modified to remove the callback lambda that was
used to start the challenge completion intent. Now, the Payments SDK is responsible for this.

- [#2125](https://git.corp.stripe.com/stripe-internal/android/pull/2125) Upgrade `nimbus-jose-jwt` to `9.4.1`

## 5.0.1 - 2020-01-08
5.0.0 was released without sources. 5.0.1 is identical to 5.0.0 but will include sources.

## 5.0.0 - 2020-01-07
This release open-sources the 3DS2 SDK.

- [#1994](https://git.corp.stripe.com/stripe-internal/android/pull/1994) Set Nexus `packageGroup`
- [#2066](https://git.corp.stripe.com/stripe-internal/android/pull/2066) Update `ProgressViewFactory.Brand.lookup()`
- [#2067](https://git.corp.stripe.com/stripe-internal/android/pull/2067) Use `viewModels()` ktx
- [#2074](https://git.corp.stripe.com/stripe-internal/android/pull/2074) Strip JWK from `JWSHeader` before verifying JWS
- [#2090](https://git.corp.stripe.com/stripe-internal/android/pull/2090) Upgrade `nimbus-jose-jwt` to `9.4`
- [#2093](https://git.corp.stripe.com/stripe-internal/android/pull/2093) Upgrade bcprov-jdk15to18 to 1.68
- [#2110](https://git.corp.stripe.com/stripe-internal/android/pull/2110) Include sources in 3DS2 SDK release

## 4.1.2 - 2020-11-24
- [#1926](https://git.corp.stripe.com/stripe-internal/android/pull/1926) Improve `JwsValidator` error reporting
- [#1937](https://git.corp.stripe.com/stripe-internal/android/pull/1937) Upgrade Android Gradle Plugin to `4.1.1`
- [#1968](https://git.corp.stripe.com/stripe-internal/android/pull/1968) Expose `sdkTransactionId` on `Transaction`
- [#1962](https://git.corp.stripe.com/stripe-internal/android/pull/1962) Upgrade Gradle to `6.7.1`
- [#1974](https://git.corp.stripe.com/stripe-internal/android/pull/1974) Upgrade Kotlin to `1.4.20`
- [#1975](https://git.corp.stripe.com/stripe-internal/android/pull/1975) Migrate to Kotlin Parcelize plugin
- [#1991](https://git.corp.stripe.com/stripe-internal/android/pull/1991) Properly handle exception in `DefaultErrorReporter`

## 4.1.1 - 2020-11-04
- [#1749](https://git.corp.stripe.com/stripe-internal/android/pull/1749) Catch null `directoryServerName` earlier in `ChallengeProgressActivity`
- [#1913](https://git.corp.stripe.com/stripe-internal/android/pull/1913) Upgrade coroutines to `1.4.1`
- [#1914](https://git.corp.stripe.com/stripe-internal/android/pull/1914) Upgrade `nimbus-jose-jwt` to `9.1.2`
- [#1915](https://git.corp.stripe.com/stripe-internal/android/pull/1915) Upgrade `bcprov-jdk15to18` to `1.6.7`

## 4.1.0 - 2020-09-18
- [#1426](https://git.corp.stripe.com/stripe-internal/android/pull/1426) Update `buildToolsVersion` to `30.0.2`
- [#1438](https://git.corp.stripe.com/stripe-internal/android/pull/1438) Upgrade coroutines to `1.3.9`
- [#1516](https://git.corp.stripe.com/stripe-internal/android/pull/1516) Upgrade to Gradle `6.6.1`
- [#1556](https://git.corp.stripe.com/stripe-internal/android/pull/1556) Upgrade `nimbus-jose-jwt` to `9.0`
- [#1566](https://git.corp.stripe.com/stripe-internal/android/pull/1566) Integrate `AnalyticsReporter`
- [#1571](https://git.corp.stripe.com/stripe-internal/android/pull/1571) Upgrade Kotlin to `1.4.10`
- [#1592](https://git.corp.stripe.com/stripe-internal/android/pull/1592) Upgrade `material-components` to `1.2.1`
- [#1595](https://git.corp.stripe.com/stripe-internal/android/pull/1595) Update `DefaultAnalyticsReporter` to use Sentry
- [#1620](https://git.corp.stripe.com/stripe-internal/android/pull/1620) Upgrade `com.nimbusds:nimbus-jose-jwt` to `9.0.1`
- [#1622](https://git.corp.stripe.com/stripe-internal/android/pull/1622) Make `Transaction.doChallenge()` a `suspend fun`
- [#1624](https://git.corp.stripe.com/stripe-internal/android/pull/1624) Properly handle unavailable `TransactionTimer` in `ChallengeActivity`

## 4.0.5 - 2020-08-13
- [#1411](https://git.corp.stripe.com/stripe-internal/android/pull/1411) Fix crash related to `SdkAppIdSupplierImpl`
- [#1414](https://git.corp.stripe.com/stripe-internal/android/pull/1414) Upgrade `com.nimbusds:nimbus-jose-jwt` to `8.20`
- [#1416](https://git.corp.stripe.com/stripe-internal/android/pull/1416) Make `AuthenticationRequestParametersFactory.create` a suspend function

## 4.0.4 - 2020-07-30
- [#1337](https://git.corp.stripe.com/stripe-internal/android/pull/1337) Capture debug details when JSON parsing fails in `AcsData`

## 4.0.3 - 2020-07-09
- [#1033](https://git.corp.stripe.com/stripe-internal/android/pull/1033) Target API 30

## 4.0.2 - 2020-06-18
- [#1013](https://git.corp.stripe.com/stripe-internal/android/pull/1013) Observe `LiveData` on main thread in `StripeChallengeRequestExecutor`

## 4.0.1 - 2020-06-18
- [#1004](https://git.corp.stripe.com/stripe-internal/android/pull/1004) Remove custom SSLSocketFactory from 3DS2 SDK
- [#1005](https://git.corp.stripe.com/stripe-internal/android/pull/1005) Migrate ChallengeResponseProcessor to use LiveData
- [#1007](https://git.corp.stripe.com/stripe-internal/android/pull/1007) Observe ChallengeRequestTimer on main thread

## 4.0.0 - 2020-06-17
- [#927](https://git.corp.stripe.com/stripe-internal/android/pull/927) Use `Dialog` instead of `ProgressDialog` in 3DS2 SDK
- [#948](https://git.corp.stripe.com/stripe-internal/android/pull/948) Add `stripe` prefix to 3DS2 SDK layouts
- [#963](https://git.corp.stripe.com/stripe-internal/android/pull/963) Bump SDK min version to 21
- [#975](https://git.corp.stripe.com/stripe-internal/android/pull/975) Refactor `StripeChallengeRequestExecutor`
- [#986](https://git.corp.stripe.com/stripe-internal/android/pull/986) Improve 3DS2 SDK's image fetching logic
- [#987](https://git.corp.stripe.com/stripe-internal/android/pull/987) Improve `ChallengeRequestTimer`

## 3.0.3 - 2020-06-08
- [#919](https://git.corp.stripe.com/stripe-internal/android/pull/919) Fix `DiskReadViolation` in 3DS2 SDK

## 3.0.2 - 2020-05-29
- [#871](https://git.corp.stripe.com/stripe-internal/android/pull/871) Make ChallengeParameters properties optional and mutable
- [#872](https://git.corp.stripe.com/stripe-internal/android/pull/872) Fix 3DS2 SDK start activity logic

## 3.0.1 - 2020-05-28
- [#858](https://git.corp.stripe.com/stripe-internal/android/pull/858) Make `CompletionEvent` and `RuntimeErrorEvent` have public constructors

## 3.0.0 - 2020-05-28
- [#757](https://git.corp.stripe.com/stripe-internal/android/pull/757) Update Kotlin Coroutines to 1.3.7
- [#810](https://git.corp.stripe.com/stripe-internal/android/pull/810) Use `runCatching` to simplify 3DS2 SDK codebase
- [#849](https://git.corp.stripe.com/stripe-internal/android/pull/849) Fix 3DS2 SDK for Discover certification
- Convert simple interfaces to data classes (breaking changes)
- Migrate Handlers to Coroutines

## 2.8.1 - 2020-05-15
- [#771](https://git.corp.stripe.com/stripe-internal/android/pull/771) Add support for starting 3DS2 challenge from a fragment

## 2.7.9 - 2020-05-08
- [#733](https://git.corp.stripe.com/stripe-internal/android/pull/733) Update `com.nimbusds:nimbus-jose-jwt` to `8.16`
- [#748](https://git.corp.stripe.com/stripe-internal/android/pull/748) Update Discover certificate for 3DS2

## 2.7.8 - 2020-04-30
- [#688](https://git.corp.stripe.com/stripe-internal/android/pull/688) Downgrade BouncyCastle to 1.64

## 2.7.7 - 2020-04-28
- [#664](https://git.corp.stripe.com/stripe-internal/android/pull/664) Remove all unused device data collection from 3DS2 SDK

## 2.7.6 - 2020-04-28
- [#303](https://git.corp.stripe.com/stripe-internal/android-archive/pull/303) Remove phone state and phone number access in DeviceDataFactoryImpl
- [#665](https://git.corp.stripe.com/stripe-internal/android/pull/665) Update 3DS2 SDK dependencies
  - Change `androidx.legacy:legacy-support-v4` to `androidx.localbroadcastmanager:localbroadcastmanager`

## 2.7.5 - 2020-04-23
- [#298](https://git.corp.stripe.com/stripe-internal/android-archive/pull/298) Disable default system back button behavior on 3DS2 challenge screen

## 2.7.4 - 2020-04-20
- Don't gather list of installed apps in 3DS2 SDK
- Update `com.nimbusds:nimbus-jose-jwt` to `8.15`

## 2.7.3 - 2020-04-16
- Add translations for 3DS2 SDK strings
- Update `org.bouncycastle:bcprov-jdk15on` to `1.65`
- Update `com.nimbusds:nimbus-jose-jwt` to `8.14.1`
- Update `org.ow2.asm:asm` to `8.0.1`

## 2.5.4 - 2020-02-18
  - Rename `BaseStripe3DS2EditTextTheme` to `BaseStripe3DS2EditText`

## 2.5.3 - 2020-02-14
- Update 3DS2 styles for consistency
  - Rename `Stripe.3ds2.TextInputLayout` to `Stripe3DS2TextInputLayout`
  - Rename `Stripe.Base.3ds2.TextInputLayout` to
    `BaseStripe3DS2TextInputLayout`
  - Remove `Stripe3DS2TextInput` and use `Stripe3DS2TextInputLayout`
    instead
  - Create `BaseStripe3DS2EditTextTheme` with parent
    `Widget.MaterialComponents.TextInputEditText.OutlinedBox`
  - Rename `Stripe3DS2EditTextTheme` to `Stripe3DS2EditText` and
    change its parent to `BaseStripe3DS2EditTextTheme`

## 2.4.3 - 2020-02-07
- No material changes since 2.4.2; we're testing out the new deploy process

## 2.4.2 - 2020-02-07
- No material changes since 2.4.1; we're testing out the new deploy process
