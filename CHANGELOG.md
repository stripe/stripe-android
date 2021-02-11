# CHANGELOG

## 16.3.0 - 2021-02-11
* [#3334](https://github.com/stripe/stripe-android/pull/3334) Upgrade Kotlin to `1.4.30`
* [#3346](https://github.com/stripe/stripe-android/pull/3346) Upgrade Gradle to `6.8.2`
* [#3349](https://github.com/stripe/stripe-android/pull/3349) Upgrade `material-components` to `1.3.0`
* [#3359](https://github.com/stripe/stripe-android/pull/3359) Add `brand` and `last4` properties to `CardParams`
* [#3367](https://github.com/stripe/stripe-android/pull/3367) Upgrade `fragment-ktx` to `1.3.0` and `activity-ktx` to `1.2.0`
* [#3368](https://github.com/stripe/stripe-android/pull/3368) Upgrade `androidx.lifecycle` dependencies to `2.3.0`
* [#3372](https://github.com/stripe/stripe-android/pull/3372) Upgrade 3DS2 SDK to `5.2.0`
    * Upgrade `nimbus-jose-jwt` to `9.5`
    * Upgrade Kotlin to `1.4.30`
    * Upgrade `material-components` to `1.3.0`
    * Upgrade `activity-ktx` to `1.2.0` and `fragment-ktx` to `1.3.0`
    * Upgrade `androidx.lifecycle` to `2.3.0`
    * Migrate `ProgressBar` to `CircularProgressIndicator`

## 16.2.1 - 2021-01-29
* [#3275](https://github.com/stripe/stripe-android/pull/3257) Fix spinner positioning in `CardMultilineWidget`
* [#3275](https://github.com/stripe/stripe-android/pull/3275) Upgrade Android Gradle Plugin to `4.1.2`
* [#3291](https://github.com/stripe/stripe-android/pull/3291) Upgrade Gradle to `6.8.1`
* [#3300](https://github.com/stripe/stripe-android/pull/3300) Upgrade AndroidX fragment dependency to `1.3.0-rc2`
* [#3302](https://github.com/stripe/stripe-android/pull/3302) Add support for creating `afterpay_clearpay` payment methods
* [#3315](https://github.com/stripe/stripe-android/pull/3315) Upgrade 3DS2 SDK to `5.1.1`
    * Upgrade `nimbus-jose-jwt` to `9.4.2`

## 16.2.0 - 2021-01-11
* [#3088](https://github.com/stripe/stripe-android/pull/3088) Mark some builders in `PaymentMethodCreateParams` as deprecated
* [#3134](https://github.com/stripe/stripe-android/pull/3134) Upgrade Kotlin to `1.4.21`
* [#3154](https://github.com/stripe/stripe-android/pull/3154) Fix `CvcEditText` layout issues in `CardMultilineWidget`
* [#3176](https://github.com/stripe/stripe-android/pull/3176) Update `GooglePayConfig` constructor
* [#3205](https://github.com/stripe/stripe-android/pull/3205) Add `androidx.activity:activity-ktx:1.2.0-rc01` as a dependency
* [#3232](https://github.com/stripe/stripe-android/pull/3232) Align card number field icon to end in `CardMultilineWidget`
* [#3237](https://github.com/stripe/stripe-android/pull/3237) Upgrade 3DS2 SDK to `5.0.1`
    * Sources are now included with the 3DS2 SDK
    * Upgrade `bcprov-jdk15to18` to `1.6.8`
    * Upgrade `nimbus-jose-jwt` to `9.4`
* [#3241](https://github.com/stripe/stripe-android/pull/3241) Upgrade Gradle to `6.8`

## 16.1.1 - 2020-11-25
* [#3028](https://github.com/stripe/stripe-android/pull/3028) Upgrade Android Gradle Plugin to `4.1.1`
* [#3035](https://github.com/stripe/stripe-android/pull/3035) Update handling of deeplinks in `PaymentAuthWebViewClient`
* [#3046](https://github.com/stripe/stripe-android/pull/3046) Upgrade Gradle to `6.7.1`
* [#3056](https://github.com/stripe/stripe-android/pull/3056) Upgrade Kotlin to `1.4.20`
* [#3058](https://github.com/stripe/stripe-android/pull/3058) Migrate to Kotlin Parcelize plugin
* [#3072](https://github.com/stripe/stripe-android/pull/3072) Fix crash in card widgets
* [#3083](https://github.com/stripe/stripe-android/pull/3083) Upgrade `stripe-3ds2-android` to `4.1.2`
    * Fix crash

## 16.1.0 - 2020-11-06
* [#2930](https://github.com/stripe/stripe-android/pull/2930) Upgrade Android Gradle Plugin to `4.1.0`
* [#2936](https://github.com/stripe/stripe-android/pull/2936) Upgrade Gradle to `6.7`
* [#2955](https://github.com/stripe/stripe-android/pull/2955) Add support for UPI payment method
* [#2965](https://github.com/stripe/stripe-android/pull/2965) Add support for Netbanking payment method
* [#2976](https://github.com/stripe/stripe-android/pull/2976) Update `ExpiryDateEditText` input allowlist
* [#2977](https://github.com/stripe/stripe-android/pull/2977) Fix crash in `CardNumberTextInputLayout`
* [#2981](https://github.com/stripe/stripe-android/pull/2981) Fix `PaymentMethodCreateParams` annotations on create methods
* [#2988](https://github.com/stripe/stripe-android/pull/2988) Update `PaymentSession.handlePaymentData()` to take a nullable `Intent`
* [#2989](https://github.com/stripe/stripe-android/pull/2989) Handle null `client_secret` in result `Intent`
* [#2995](https://github.com/stripe/stripe-android/pull/2995) Upgrade constraintlayout to `2.0.4`
* [#3006](https://github.com/stripe/stripe-android/pull/3006) Upgrade coroutines to `1.4.1`
* [#3010](https://github.com/stripe/stripe-android/pull/3010) Upgrade `stripe-3ds2-android` to `4.1.1`
    * Upgrade `bcprov-jdk15to18` to `1.6.7`
    * Upgrade `nimbus-jose-jwt` to `9.1.2`

## 16.0.1 - 2020-10-06 
* [#2894](https://github.com/stripe/stripe-android/pull/2894) Make `CardParams` constructor public
* [#2895](https://github.com/stripe/stripe-android/pull/2895) Add support for configuring a footer layout in payment methods screen
* [#2897](https://github.com/stripe/stripe-android/pull/2897) Only allow digits in `CvcEditText`
* [#2900](https://github.com/stripe/stripe-android/pull/2900) Only allow digits in BECS BSB and account number fields
* [#2913](https://github.com/stripe/stripe-android/pull/2913) Add support for Oxxo PaymentMethod

## 16.0.0 - 2020-09-23
This release includes several breaking changes. See the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more details.

This release adds support for 19-digit cards in `CardInputWidget` and `CardMultilineWidget`.

* [#2715](https://github.com/stripe/stripe-android/pull/2715) Add support for GrabPay PaymentMethod
* [#2721](https://github.com/stripe/stripe-android/pull/2721) Upgrade Kotlin coroutines to `1.3.9`
* [#2735](https://github.com/stripe/stripe-android/pull/2735) Upgrade Android Gradle Plugin to `4.0.1`
* [#2766](https://github.com/stripe/stripe-android/pull/2766) Upgrade Gradle to `6.6.1`
* [#2821](https://github.com/stripe/stripe-android/pull/2821) Support pasting a 19 digit PAN in `CardNumberEditText`
* [#2836](https://github.com/stripe/stripe-android/pull/2836) Handle `CustomerSession` failure in `PaymentMethodsActivity`
* [#2837](https://github.com/stripe/stripe-android/pull/2837) Upgrade Kotlin to `1.4.10`
* [#2841](https://github.com/stripe/stripe-android/pull/2841) Add new string translations
    * Adds support for several new languages
* [#2847](https://github.com/stripe/stripe-android/pull/2847) Update `CardInputWidget` text size for `ldpi` screens
* [#2854](https://github.com/stripe/stripe-android/pull/2854) Upgrade `com.google.android.material:material` to `1.2.1`
* [#2867](https://github.com/stripe/stripe-android/pull/2867) Upgrade 3DS2 SDK to `4.1.0`
    * Upgrade `material-components` to `1.2.1`
    * Upgrade `com.nimbusds:nimbus-jose-jwt` to `9.0.1`
    * Guard against crash when `TransactionTimer` is unavailable
* [#2873](https://github.com/stripe/stripe-android/pull/2873) Fix `CardInputWidget` field rendering in RTL
* [#2878](https://github.com/stripe/stripe-android/pull/2878) Remove `Stripe.createToken()`
* [#2880](https://github.com/stripe/stripe-android/pull/2880) Fix date formatting in `KlarnaSourceParams`
* [#2887](https://github.com/stripe/stripe-android/pull/2887) Re-render shipping methods screen when shipping methods change

## 15.1.0 - 2020-08-13
* [#2671](https://github.com/stripe/stripe-android/pull/2671) Add `cardParams` property to `CardInputWidget` and `CardMultilineWidget`
* [#2675](https://github.com/stripe/stripe-android/pull/2675) Add `CardParams` methods to `Stripe`
* [#2677](https://github.com/stripe/stripe-android/pull/2677) Deprecate `Card.create()`
* [#2679](https://github.com/stripe/stripe-android/pull/2679) Add missing `TokenizationMethod` values
    * `TokenizationMethod.Masterpass` and `TokenizationMethod.VisaCheckout`
* [#2692](https://github.com/stripe/stripe-android/pull/2692) Add support for Alipay PaymentMethod
    * See `Stripe#confirmAlipayPayment()`
* [#2693](https://github.com/stripe/stripe-android/pull/2693) Upgrade `androidx.appcompat:appcompat` to `1.2.0`
* [#2696](https://github.com/stripe/stripe-android/pull/2696) Upgrade to Gradle `6.6`
* [#2704](https://github.com/stripe/stripe-android/pull/2704) Deprecate metadata field on retrieved API objects
    * See `MIGRATING.md` for more details
* [#2708](https://github.com/stripe/stripe-android/pull/2708) Bump 3DS2 SDK to `4.0.5`
    * Fix crash related to SDK app id
    * Upgrade `com.nimbusds:nimbus-jose-jwt` to `8.20`

## 15.0.2 - 2020-08-03
* [#2666](https://github.com/stripe/stripe-android/pull/2666) Bump 3DS2 SDK to `4.0.4`
* [#2671](https://github.com/stripe/stripe-android/pull/2671) Add `cardParams` property to `CardInputWidget` and `CardMultilineWidget`
* [#2674](https://github.com/stripe/stripe-android/pull/2674) Add `SourceParams` creation method for `CardParams`
* [#2675](https://github.com/stripe/stripe-android/pull/2675) Add `CardParams` methods to `Stripe` class
* [#2679](https://github.com/stripe/stripe-android/pull/2679) Add missing `TokenizationMethod` values
    * Add `Masterpass` and `VisaCheckout`
* [#2681](https://github.com/stripe/stripe-android/pull/2681) Mark code using `Card` for card object creation as `@Deprecated`

## 15.0.1 - 2020-07-28
* [#2641](https://github.com/stripe/stripe-android/pull/2641) Add support for Bank Account as source on `Customer` object
* [#2643](https://github.com/stripe/stripe-android/pull/2643) Add missing fields to `Customer` model
* [#2646](https://github.com/stripe/stripe-android/pull/2646) Allow `CardMultilineWidget`'s `TextInputLayout`s to be styled
* [#2649](https://github.com/stripe/stripe-android/pull/2649) Add `@JvmOverloads` to `GooglePayJsonFactory` methods
* [#2651](https://github.com/stripe/stripe-android/pull/2651) Update Kotlin coroutines to `1.3.8`
* [#2657](https://github.com/stripe/stripe-android/pull/2657) Fix HTML select option rendering in `WebView`

## 15.0.0 - 2020-07-09 
This release includes several breaking changes.
See the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more details.

* [#2542](https://github.com/stripe/stripe-android/pull/2542) Use `CustomerSession.stripeAccountId` in `AddPaymentMethodActivity`
* [#2543](https://github.com/stripe/stripe-android/pull/2543) Target JVM 1.8
* [#2544](https://github.com/stripe/stripe-android/pull/2544) Remove deprecated code related to `BankAccount` and `ActivityStarter`
* [#2545](https://github.com/stripe/stripe-android/pull/2545) Remove deprecated `AccountParams.create()` method
* [#2546](https://github.com/stripe/stripe-android/pull/2546) Remove deprecated `next_action` data objects from `PaymentIntent` and `SetupIntent`
* [#2547](https://github.com/stripe/stripe-android/pull/2547) Convert `BankAccount.BankAccountType` to `BankAccount.Type` enum
* [#2554](https://github.com/stripe/stripe-android/pull/2554) Update `PaymentAuthWebViewActivity`'s back button behavior
* [#2551](https://github.com/stripe/stripe-android/pull/2551) Fix `StrictMode` `DiskReadViolation` violations
* [#2555](https://github.com/stripe/stripe-android/pull/2555) Improve `PaymentAuthWebViewActivity`
* [#2559](https://github.com/stripe/stripe-android/pull/2559) Represent `PaymentIntent`'s `confirmationMethod` and `captureMethod` as enums
* [#2561](https://github.com/stripe/stripe-android/pull/2561) Make `CardInputListener.FocusField` an enum
* [#2562](https://github.com/stripe/stripe-android/pull/2562) Make `SourceTypeModel.Card.ThreeDSecureStatus` an enum
* [#2563](https://github.com/stripe/stripe-android/pull/2563) Make `PaymentMethod.Card#brand` a `CardBrand`
* [#2566](https://github.com/stripe/stripe-android/pull/2566) Make `Token.Type` an enum
* [#2569](https://github.com/stripe/stripe-android/pull/2569) Refactor `Source` class and related classes
* [#2572](https://github.com/stripe/stripe-android/pull/2572) Fix `StrictMode` `DiskReadViolation` violations when starting 3DS2
* [#2577](https://github.com/stripe/stripe-android/pull/2577) Make `CustomerSource#tokenizationMethod` a `TokenizationMethod?`
* [#2579](https://github.com/stripe/stripe-android/pull/2579) Make `PaymentMethod.Card.Networks` fields public
* [#2587](https://github.com/stripe/stripe-android/pull/2587) Fix BouncyCastle Proguard rule
* [#2594](https://github.com/stripe/stripe-android/pull/2594) Fix vector icon references in layout files
    * Reduce SDK size by ~30kb
    * Fix Google Pay icon in `PaymentMethodsActivity`
* [#2595](https://github.com/stripe/stripe-android/pull/2595) Bump SDK `minSdkVersion` to `21`
* [#2599](https://github.com/stripe/stripe-android/pull/2599) Remove `StripeSSLSocketFactory`
* [#2604](https://github.com/stripe/stripe-android/pull/2604) Add `stripeAccountId` to `PaymentConfiguration` and use in `AddPaymentMethodActivity`
* [#2609](https://github.com/stripe/stripe-android/pull/2609) Refactor `AddPaymentMethodActivity.Result`
* [#2610](https://github.com/stripe/stripe-android/pull/2610) Remove `PaymentSession` and `CustomerSession`'s "Activity" Listeners
* [#2611](https://github.com/stripe/stripe-android/pull/2611) Target API 30
* [#2617](https://github.com/stripe/stripe-android/pull/2617) Convert `CustomizableShippingField` to enum
* [#2623](https://github.com/stripe/stripe-android/pull/2623) Update Gradle to `6.5.1`
* [#2557](https://github.com/stripe/stripe-android/pull/2634) Update 3DS2 SDK to `4.0.3`

## 14.5.0 - 2020-06-04
* [#2453](https://github.com/stripe/stripe-android/pull/2453) Add `ConfirmPaymentIntentParams#receiptEmail`
* [#2458](https://github.com/stripe/stripe-android/pull/2458) Remove INTERAC from `GooglePayJsonFactory.DEFAULT_CARD_NETWORKS`
* [#2462](https://github.com/stripe/stripe-android/pull/2462) Capitalize currency code in `GooglePayJsonFactory`
* [#2466](https://github.com/stripe/stripe-android/pull/2466) Deprecate `ActivityStarter.startForResult()` with no args
* [#2467](https://github.com/stripe/stripe-android/pull/2467) Update `CardBrand.MasterCard` regex
* [#2475](https://github.com/stripe/stripe-android/pull/2475) Support `android:focusedByDefault` in `CardInputWidget`
    * Fixes #2463 on Android API level 26 and above
* [#2483](https://github.com/stripe/stripe-android/pull/2483) Fix formatting of `maxTimeout` value in `Stripe3ds2AuthParams`
* [#2494](https://github.com/stripe/stripe-android/pull/2494) Support starting 3DS2 challenge flow from a Fragment
* [#2496](https://github.com/stripe/stripe-android/pull/2496) Deprecate `StripeIntent.stripeSdkData`
* [#2497](https://github.com/stripe/stripe-android/pull/2497) Deprecate `StripeIntent.redirectData`
* [#2513](https://github.com/stripe/stripe-android/pull/2513) Add `canDeletePaymentMethods` to `PaymentSessionConfig`
    * `canDeletePaymentMethods` controls whether the user can delete a payment method
      by swiping on it in `PaymentMethodsActivity`
* [#2525](https://github.com/stripe/stripe-android/pull/2525) Upgrade Android Gradle Plugin to 4.0.0
* [#2531](https://github.com/stripe/stripe-android/pull/2531) Update 3DS2 SDK to 3.0.2
    * Fix bug in 3DS2 SDK where multi-screen challenges were not correctly returning result to starting Activity/Fragment
* [#2536](https://github.com/stripe/stripe-android/pull/2536) Update Gradle to 6.5

## 14.4.1 - 2020-04-30
* [#2441](https://github.com/stripe/stripe-android/pull/2441) Catch `IllegalArgumentException` in `ApiOperation`
* [#2442](https://github.com/stripe/stripe-android/pull/2442) Capitalize `GooglePayJsonFactory`'s `allowedCountryCodes`
* [#2445](https://github.com/stripe/stripe-android/pull/2445) Bump 3DS2 SDK to `2.7.8`
    * Downgrade BouncyCastle to `1.64`

## 14.4.0 - 2020-04-28
* [#2379](https://github.com/stripe/stripe-android/pull/2379) Add optional `stripeAccountId` param to most `Stripe` methods
    * This enables passing a `Stripe-Account` header on a per-request basis
* [#2398](https://github.com/stripe/stripe-android/pull/2398) Add optional `stripeAccountId` param to `Stripe#confirmPayment()`
* [#2405](https://github.com/stripe/stripe-android/pull/2405) Add optional `stripeAccountId` param to `Stripe#confirmSetupIntent()`
* [#2406](https://github.com/stripe/stripe-android/pull/2406) Add optional `stripeAccountId` param to `Stripe#authenticateSource()`
* [#2408](https://github.com/stripe/stripe-android/pull/2408) Update `PaymentMethod.Type#isReusable` values
* [#2412](https://github.com/stripe/stripe-android/pull/2412) Make `StripeIntentResult` implement `Parcelable`
* [#2413](https://github.com/stripe/stripe-android/pull/2413) Create `Stripe#retrievePaymentIntent()` and `Stripe#retrieveSetupIntent()` async methods
* [#2421](https://github.com/stripe/stripe-android/pull/2421) Capitalize `countryCode` in `GooglePayJsonFactory`
* [#2429](https://github.com/stripe/stripe-android/pull/2429) Fix SDK source paths and source publishing
* [#2430](https://github.com/stripe/stripe-android/pull/2430) Add `GooglePayJsonFactory.isJcbEnabled`
    * Enables JCB as an allowed card network. By default, JCB is disabled.
* [#2435](https://github.com/stripe/stripe-android/pull/2435) Bump 3DS2 SDK to `2.7.7`
    * On 3DS2 challenge screen, handle system back button tap as cancel button tap
* [#2436](https://github.com/stripe/stripe-android/pull/2436) Add `advancedFraudSignalsEnabled` property
    * See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection) for more details

## 14.3.0 - 2020-04-20
* [#2334](https://github.com/stripe/stripe-android/pull/2334) Add support for BACS Debit in `PaymentMethodCreateParams`
* [#2335](https://github.com/stripe/stripe-android/pull/2335) Add support for BACS Debit Payment Method
* [#2336](https://github.com/stripe/stripe-android/pull/2336) Improve `ShippingInfoWidget`
    * Make postal code and phone number fields single line
    * Make phone number field use `inputType="phone"`
* [#2342](https://github.com/stripe/stripe-android/pull/2342) Convert `CardBrand` prefixes to regex
    * Add `^81` as a pattern for `CardBrand.UnionPay`
* [#2343](https://github.com/stripe/stripe-android/pull/2343) Update `CardBrand.JCB` regex
* [#2362](https://github.com/stripe/stripe-android/pull/2362) Add support for parsing `shippingInformation` in `GooglePayResult`
* [#2365](https://github.com/stripe/stripe-android/pull/2365) Convert png assets to webp to reduce asset size
* [#2373](https://github.com/stripe/stripe-android/pull/2373) Set default `billingAddressFields` to `BillingAddressFields.PostalCode`
* [#2381](https://github.com/stripe/stripe-android/pull/2381) Add support for SOFORT PaymentMethod
* [#2384](https://github.com/stripe/stripe-android/pull/2384) Add support for P24 PaymentMethod
* [#2389](https://github.com/stripe/stripe-android/pull/2389) Add support for Bancontact PaymentMethod
* [#2390](https://github.com/stripe/stripe-android/pull/2390) Bump Kotlin version to `1.3.72`
* [#2392](https://github.com/stripe/stripe-android/pull/2392) Add `shipping` property to `PaymentIntent`
* [#2394](https://github.com/stripe/stripe-android/pull/2394) Add support for Giropay PaymentMethod
* [#2395](https://github.com/stripe/stripe-android/pull/2395) Add support for EPS PaymentMethod
* [#2396](https://github.com/stripe/stripe-android/pull/2396) Expose `klarna` property on `Source`
* [#2401](https://github.com/stripe/stripe-android/pull/2401) Bump 3DS2 SDK to `2.7.4`
    * Add new translations for 3DS2 SDK strings
    * Upgrade BouncyCastle to `1.65`

## 14.2.1 - 2020-03-26
* [#2299](https://github.com/stripe/stripe-android/pull/2299) Make `SourceParams.OwnerParams` constructor public and properties mutable
* [#2304](https://github.com/stripe/stripe-android/pull/2304) Force Canadian postal codes to be uppercase
* [#2315](https://github.com/stripe/stripe-android/pull/2315) Add `Fragment` support to `AddPaymentMethodActivityStarter`
* [#2325](https://github.com/stripe/stripe-android/pull/2325) Update `BecsDebitWidget`

## 14.2.0 - 2020-03-18
* [#2278](https://github.com/stripe/stripe-android/pull/2278) Add ability to require US ZIP code in `CardInputWidget` and `CardMultilineWidget`
* [#2279](https://github.com/stripe/stripe-android/pull/2279) Default `CardMultilineWidget` to global postal code configuration
* [#2282](https://github.com/stripe/stripe-android/pull/2282) Update pinned API version to `2020-03-02`
* [#2285](https://github.com/stripe/stripe-android/pull/2285) Create `BecsDebitMandateAcceptanceFactory` for generating BECS Debit mandate acceptance copy
* [#2290](https://github.com/stripe/stripe-android/pull/2290) Bump 3DS2 SDK to `2.7.2`
    * Fix `onActivityResult()` not called after 3DS2 challenge flow when "Don't keep activities" is enabled
    * Use view binding
    * Upgrade BouncyCastle to `1.64`
* [#2293](https://github.com/stripe/stripe-android/pull/2293) Add min length validation to BECS account number
* [#2295](https://github.com/stripe/stripe-android/pull/2295) Create `BecsDebitMandateAcceptanceTextView`
* [#2297](https://github.com/stripe/stripe-android/pull/2297) Add `BecsDebitWidget.ValidParamsCallback`

## 14.1.1 - 2020-03-09
* [#2257](https://github.com/stripe/stripe-android/pull/2257) Disable Kotlin Synthetics and migrate to [view binding](https://developer.android.com/topic/libraries/view-binding)
* [#2260](https://github.com/stripe/stripe-android/pull/2260) Update Kotlin Gradle Plugin to `1.3.70`
* [#2271](https://github.com/stripe/stripe-android/pull/2271) Update Proguard rules to remove unneeded BouncyCastle class
* [#2272](https://github.com/stripe/stripe-android/pull/2272) Update `kotlinx.coroutines` to `1.3.4`
* [#2274](https://github.com/stripe/stripe-android/pull/2274) Make `ConfirmPaymentIntentParams#savePaymentMethod` nullable

## 14.1.0 - 2020-03-02
* [#2207](https://github.com/stripe/stripe-android/pull/2207) Add `CardInputWidget#setPostalCodeTextWatcher`
* [#2211](https://github.com/stripe/stripe-android/pull/2211) Add support for 16-digit Diners Club card numbers
* [#2215](https://github.com/stripe/stripe-android/pull/2215) Set `CardInputWidget`'s postal code field's IME action to done
* [#2220](https://github.com/stripe/stripe-android/pull/2220) Highlight "Google Pay" option in payment methods screen if selected
* [#2221](https://github.com/stripe/stripe-android/pull/2221) Update 14-digit Diners Club formatting
* [#2224](https://github.com/stripe/stripe-android/pull/2224) Change `CardInputWidget` icon to represent validity of input
* [#2234](https://github.com/stripe/stripe-android/pull/2234) Add support for `setup_future_usage` in PaymentIntent confirmation
* [#2235](https://github.com/stripe/stripe-android/pull/2235) Update Android Gradle Plugin to 3.6.1
* [#2236](https://github.com/stripe/stripe-android/pull/2236) Change `CardMultilineWidget` icon to represent validity of input
* [#2238](https://github.com/stripe/stripe-android/pull/2238) Add support for `shipping` in PaymentIntent confirmation

## 14.0.0 - 2020-02-18
This release includes several breaking changes.
See the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more details.

* [#2136](https://github.com/stripe/stripe-android/pull/2136) Update `com.google.android.material:material` to `1.1.0`
* [#2141](https://github.com/stripe/stripe-android/pull/2141) Fix crash when deleting a payment method in `PaymentMethodsActivity`
* [#2155](https://github.com/stripe/stripe-android/pull/2155) Fix parceling of `PaymentRelayStarter.Args`
* [#2156](https://github.com/stripe/stripe-android/pull/2156) Fix FPX bank order
* [#2163](https://github.com/stripe/stripe-android/pull/2163) Remove return type from `PaymentSession.init()`
* [#2165](https://github.com/stripe/stripe-android/pull/2165) Simplify `PaymentSession` state and lifecycle management
    * When instantiating a `PaymentSession()` with an `Activity`, it must now be a `ComponentActivity`
      (e.g. `AppCompatActivity` or `FragmentActivity`)
    * `PaymentSession#init()` no longer takes a `savedInstanceState` argument
    * Remove `PaymentSession#savePaymentSessionInstanceState()`
    * Remove `PaymentSession#onDestroy()`
* [#2173](https://github.com/stripe/stripe-android/pull/2173) Fix Mastercard display name
* [#2174](https://github.com/stripe/stripe-android/pull/2174) Add optional params to `CustomerSession.getPaymentMethods()`
    * See [List a Customer's PaymentMethods](https://stripe.com/docs/api/payment_methods/list) for more details
* [#2179](https://github.com/stripe/stripe-android/pull/2179) Fetch previously used `PaymentMethod` in `PaymentSession`
    * If `PaymentSessionConfig.shouldPrefetchCustomer == true`, when a
      new `PaymentSession` is started, fetch the customer's previously
      selected payment method, if it exists, and return via
      `PaymentSessionListener#onPaymentSessionDataChanged()`
* [#2180](https://github.com/stripe/stripe-android/pull/2180) Remove `PaymentSession.paymentSessionData`
* [#2185](https://github.com/stripe/stripe-android/pull/2185) Convert `Card.FundingType` to `CardFunding` enum
* [#2189](https://github.com/stripe/stripe-android/pull/2189) Cleanup `StripeException` subclasses
* [#2194](https://github.com/stripe/stripe-android/pull/2194) Upgrade 3DS2 SDK to `2.5.4`
    * Update `com.google.android.material:material` to `1.1.0`
    * Fix accessibility issues on 3DS2 challenge screen
    * Update 3DS2 styles for consistency
        * Create `BaseStripe3DS2TextInputLayout` that extends `Widget.MaterialComponents.TextInputLayout.OutlinedBox`
        * Create `Stripe3DS2TextInputLayout` that extends `BaseStripe3DS2TextInputLayout`
        * Apply `Stripe3DS2TextInputLayout` to `TextInputLayout`
        * Create `BaseStripe3DS2EditText` with parent `Widget.MaterialComponents.TextInputEditText.OutlinedBox`
        * Rename `Stripe3DS2EditTextTheme` to `Stripe3DS2EditText` and change its parent to `BaseStripe3DS2EditText`
        * Apply `Stripe3DS2EditText` to `TextInputEditText`
* [#2195](https://github.com/stripe/stripe-android/pull/2195) Upgrade kotlin coroutines to 1.3.3
* [#2196](https://github.com/stripe/stripe-android/pull/2196) Create `SourceTypeModel` sealed class
    * Move `SourceCardData` subclass to `SourceTypeModel.Card`
    * Move `SourceSepaDebitData` subclass to `SourceTypeModel.SepaDebit`
    * Change type of `Source#sourceTypeModel` to `SourceTypeModel?`

## 13.3.0 - 2020-05-15
* Update 3DS2 SDK to v2.3.7
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
  * See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection) for more details
* Include sources

## 13.2.0 - 2020-02-03
* [#2112](https://github.com/stripe/stripe-android/pull/2112) Enable adding Mandate to confirm params
* [#2113](https://github.com/stripe/stripe-android/pull/2113) Enable requiring postal code in `CardInputWidget` and `CardMultilineWidget`
* [#2114](https://github.com/stripe/stripe-android/pull/2114) Fix bug in highlighting `StripeEditText` fields with errors

## 13.1.3 - 2020-01-27
* [#2105](https://github.com/stripe/stripe-android/pull/2105) Fix crash when confirming a Payment Intent or Setup Intent and an error is encountered

## 13.1.2 - 2020-01-23
* [#2093](https://github.com/stripe/stripe-android/pull/2093) Add `CardValidCallback` and add support in card forms
* [#2094](https://github.com/stripe/stripe-android/pull/2094) Make `StripeError` serializable

## 13.1.1 - 2020-01-22
* [#2074](https://github.com/stripe/stripe-android/pull/2074) Populate `isSelected` for selected `PaymentMethodsAdapter` item
* [#2076](https://github.com/stripe/stripe-android/pull/2076) Announce invalid fields when validating `CardInputWidget`
* [#2077](https://github.com/stripe/stripe-android/pull/2077) Add delete payment method accessibility action in `PaymentMethodsAdapter`
* [#2078](https://github.com/stripe/stripe-android/pull/2078) Make `StripeEditText` errors accessible 
* [#2082](https://github.com/stripe/stripe-android/pull/2082) Use ErrorMessageTranslator for AddPaymentMethodActivity errors
* [#2083](https://github.com/stripe/stripe-android/pull/2083) Add accessibility traversal rules on `AddPaymentMethodActivity`
* [#2084](https://github.com/stripe/stripe-android/pull/2084) Update `BankAccount` constructor to support all bank account token parameters
* [#2086](https://github.com/stripe/stripe-android/pull/2086) Add `id` and `status` fields to `BankAccount`
* [#2087](https://github.com/stripe/stripe-android/pull/2087) Use `BankAccountTokenParams` for bank account token creation
    * Create `Stripe#createBankAccountToken()` and `Stripe#createBankAccountTokenSynchronous()` that take a `BankAccountTokenParams` object
    * Deprecate `BankAccount` for token creation

## 13.1.0 - 2020-01-16
* [#2055](https://github.com/stripe/stripe-android/pull/2055) Fix styling of `CardInputWidget` and `CardMultilineWidget`
    * `com.google.android.material:material:1.1.0-rc01` breaks `TextInputLayout` styling;
      fix by explicitly setting a style that extends `Widget.Design.TextInputLayout`
* [#2056](https://github.com/stripe/stripe-android/pull/2056) Update `CardInputWidget`'s `EditText` size
    * Fix "Postal Code" field being partially cut off on some screens
* [#2066](https://github.com/stripe/stripe-android/pull/2066) Add support for uploading a file to Stripe
    * See `Stripe#createFile()` and `Stripe#createFileSynchronous()`
* [#2071](https://github.com/stripe/stripe-android/pull/2071) Fix accessibility issues on Payment Methods selection screen
    * Mark `View`s representing existing payment methods and add a new payment method action as focusable and clickable


## 13.0.0 - 2020-01-13
This release includes several breaking changes.
See the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more details.

* [#1950](https://github.com/stripe/stripe-android/pull/1950) Add idempotency key for `Stripe` API POST methods
     ```kotlin
    class MyActivity : AppCompatActivity() {

        private fun createPaymentMethod(
            params: PaymentMethodCreateParams,
            idempotencyKey: String?
        ) {
            stripe.createPaymentMethod(
                params = params,
                idempotencyKey = idempotencyKey,
                callback = object : ApiResultCallback<PaymentMethod> {
                    override fun onSuccess(result: PaymentMethod) {
                    }

                    override fun onError(e: Exception) {
                    }
                }
            )
        }
    }
    ```
* [#1993](https://github.com/stripe/stripe-android/pull/1993) Remove deprecated methods from `PaymentSession`
    * See the [Migration Guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more details
* [#1994](https://github.com/stripe/stripe-android/pull/1994) Enable postal code field in `CardInputWidget` by default
* [#1995](https://github.com/stripe/stripe-android/pull/1995) Enable Google Pay option in Basic Integration and Stripe Activities
    * PaymentSession
      ```kotlin
      PaymentSessionConfig.Builder()
          // other settings
          .setShouldShowGooglePay(true)
          .build()
      ```

    * PaymentMethodsActivity
      ```kotlin
      PaymentMethodsActivityStarter.Args.Builder()
          // other settings
          .setShouldShowGooglePay(true)
          .build()
      ```
* [#1996](https://github.com/stripe/stripe-android/pull/1996) Update postal code logic for `CardMultilineWidget`
    * Default to showing postal code (i.e. `shouldShowPostalCode = true`)
    * Postal code is now optional when displayed
    * Remove validation on postal code field
    * Change postal code field hint text to "Postal Code"
    * Remove `CardInputListener.onPostalCodeComplete()`
* [#1998](https://github.com/stripe/stripe-android/pull/1998) Use `CardBrand` enum to represent card brands
    * Change the type of `Card#brand` and `SourceCardData#brand` properties from `String?` to `CardBrand`
    * Remove `Card.CardBrand`
* [#1999](https://github.com/stripe/stripe-android/pull/1999) Remove deprecated `BroadcastReceiver` logic from `PaymentFlowActivity`
    * See the [Migration Guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more details
* [#2000](https://github.com/stripe/stripe-android/pull/2000) Pass `PaymentSessionConfig` instance to `PaymentSession` constructor
* [#2002](https://github.com/stripe/stripe-android/pull/2002) Fix regression in `CardInputWidget` styling
    To customize the individual `EditText` fields of `CardInputWidget`, define a `Stripe.CardInputWidget.EditText` style
    that extends `Stripe.Base.CardInputWidget.EditText`. For example,
    ```xml
    <style name="Stripe.CardInputWidget.EditText" parent="Stripe.Base.CardInputWidget.EditText">
        <item name="android:textSize">22sp</item>
        <item name="android:textColor">@android:color/holo_blue_light</item>
        <item name="android:textColorHint">@android:color/holo_orange_light</item>
    </style>
    ```
* [#2003](https://github.com/stripe/stripe-android/pull/2003) Add support for authenticating a `Source` via in-app WebView
    ```kotlin
    class MyActivity : AppCompatActivity() {
        private fun authenticateSource(source: Source) {
            stripe.authenticateSource(this, source)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (data != null && stripe.isAuthenticateSourceResult(requestCode, data)) {
                stripe.onAuthenticateSourceResult(
                    data,
                    object : ApiResultCallback<Source> {
                        override fun onSuccess(result: Source) {
                        }

                        override fun onError(e: Exception) {
                        }
                    }
                )
            }
        }
    }
    ```
* [#2006](https://github.com/stripe/stripe-android/pull/2006) Create `TokenizationMethod` enum
    * Change the type of `Card#tokenizationMethod` and `SourceCardData#tokenizationMethod` from `String?` to `TokenizationMethod?`
* [#2013](https://github.com/stripe/stripe-android/pull/2013) Populate shipping address country from pre-populated shipping info
* [#2015](https://github.com/stripe/stripe-android/pull/2015) Update `PaymentSessionConfig`'s default `BillingAddressFields` to `PostalCode`
* [#2020](https://github.com/stripe/stripe-android/pull/2020) Change `PaymentMethod.type` from `String?` to `PaymentMethod.Type?`
* [#2028](https://github.com/stripe/stripe-android/pull/2028) Update `SourceParams` fields
    * Update `setOwner()` to take `OwnerParams` instead of `Map`
    * Remove `setRedirect()`, use `setReturnUrl()` instead
    * Update some setters to take null values, simplifying usage
    * Update comments
* [#2029](https://github.com/stripe/stripe-android/pull/2029) Update `CardInputWidget` to use `TextInputLayout`
    * Make `StripeEditText` extend `TextInputEditText`
* [#2038](https://github.com/stripe/stripe-android/pull/2038) Update `CardInputWidget` to focus on first error field when validating
* [#2039](https://github.com/stripe/stripe-android/pull/2039) Add support for creating a person token
    * Add `Stripe#createPersonToken()` and `Stripe#createPersonTokenSynchronous()`
* [#2040](https://github.com/stripe/stripe-android/pull/2040) Add support for CVC recollection in PaymentIntents
    * Update `ConfirmPaymentIntentParams.createWithPaymentMethodId()` with optional `PaymentMethodOptionsParams?` argument
* [#2042](https://github.com/stripe/stripe-android/pull/2042) Create `AccountParams.BusinessTypeParams`
    * `BusinessTypeParams.Company` and `BusinessTypeParams.Individual` model the parameters for creating a
      [company](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company) or
      [individual](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual)
      [account token](https://stripe.com/docs/api/tokens/create_account).
      Use these instead of creating raw maps representing the data.
    * `AccountParams.createAccountParams()` is now deprecated. Use the appropriate `AccountParams.create()` method.

## 12.9.0 - 2020-05-15
* Update 3DS2 SDK to v2.3.7
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
  * See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection) for more details
* Include sources

## 12.8.2 - 2019-12-20
* [#1974](https://github.com/stripe/stripe-android/pull/1974) Add `PaymentSessionConfig#shouldPrefetchCustomer`
    * Mark `PaymentSessionConfig#init()` with `shouldPrefetchCustomer` argument as deprecated
* [#1980](https://github.com/stripe/stripe-android/pull/1980) Don't show a `Dialog` in `StripeActivity` if `isFinishing()`
* [#1989](https://github.com/stripe/stripe-android/pull/1989) Create `CardBrand` enum
* [#1990](https://github.com/stripe/stripe-android/pull/1990) Relax validation of UK postal codes

## 12.8.1 - 2019-12-18
* [#1968](https://github.com/stripe/stripe-android/pull/1968) Upgrade 3DS2 SDK to `2.2.7`
    * Downgrade to `com.google.android.material:material:1.0.0`

## 12.8.0 - 2019-12-17
* [#1947](https://github.com/stripe/stripe-android/pull/1947) Allow setting of window flags on Stripe Activities
    * Basic Integration
      ```
      PaymentSessionConfig.Builder()
          .setWindowFlags(WindowManager.LayoutParams.FLAG_SECURE)
          .build()
      ```

    * Custom Integration
      ```
      AddPaymentMethodActivityStarter.Args.Builder()
          .setWindowFlags(WindowManager.LayoutParams.FLAG_SECURE)
          .build()
      ```
* [#1956](https://github.com/stripe/stripe-android/pull/1956) Add support for configuring billing address fields on `AddPaymentMethodActivity`
    * Basic Integration
      ```
      PaymentSessionConfig.Builder()
          .setBillingAddressFields(BillingAddressFields.Full)
          .build()
      ```

    * Custom Integration
      ```
      AddPaymentMethodActivityStarter.Args.Builder()
          .setBillingAddressFields(BillingAddressFields.Full)
          .build()
      ```
* [#1957](https://github.com/stripe/stripe-android/pull/1957) Enable `PaymentSessionConfig.ShippingInformationValidator` and `PaymentSessionConfig.ShippingMethodsFactory`
* [#1958](https://github.com/stripe/stripe-android/pull/1958) Add validation for PaymentIntent and SetupIntent client secrets
* [#1959](https://github.com/stripe/stripe-android/pull/1959) Upgrade 3DS2 SDK to `2.2.6`

## 12.7.0 - 2019-12-16
* [#1915](https://github.com/stripe/stripe-android/pull/1915) Update API version to [2019-12-03](https://stripe.com/docs/upgrades#2019-12-03)
* [#1928](https://github.com/stripe/stripe-android/pull/1928) Make Payment Method `Wallet` a sealed class
* [#1930](https://github.com/stripe/stripe-android/pull/1930) Update text size for `CardInputWidget` fields
* [#1939](https://github.com/stripe/stripe-android/pull/1939) Update Android Gradle Plugin to `3.5.3`
* [#1946](https://github.com/stripe/stripe-android/pull/1946) Upgrade 3DS2 SDK to `2.2.5`
    * Upgrade to `com.google.android.material:material:1.2.0-alpha2`
* [#1949](https://github.com/stripe/stripe-android/pull/1949) Catch `NullPointerException` when calling `StripeEditText.setHint()`.
  This is a workaround for a [known issue on some Samsung devices](https://issuetracker.google.com/issues/37127697).
* [#1951](https://github.com/stripe/stripe-android/pull/1951) Expose ability to enable postal code on `CardInputWidget`
    * Enable via layout
      ```
        <com.stripe.android.view.CardInputWidget
            android:id="@+id/card_input_widget"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:shouldShowPostalCode="true" />
      ```
    * Enable via code
      * Java: `cardInputWidget.setPostalCodeEnabled(true)`
      * Kotlin: `cardInputWidget.postalCodeEnabled = true`

## 12.6.1 - 2019-12-02
* [#1897](https://github.com/stripe/stripe-android/pull/1897) Upgrade 3DS2 SDK to `2.2.4`
    * Fix crash when using Instant App

## 12.6.0 - 2019-11-27
* [#1859](https://github.com/stripe/stripe-android/pull/1859) Create `GooglePayJsonFactory`, a factory for generating Google Pay JSON request objects
* [#1860](https://github.com/stripe/stripe-android/pull/1860) Namespace drawables with `stripe_` prefix
* [#1861](https://github.com/stripe/stripe-android/pull/1861) Create `GooglePayResult` to parse and model Google Pay Payment Data response
* [#1863](https://github.com/stripe/stripe-android/pull/1863) Complete migration of SDK code to Kotlin 🎉
* [#1864](https://github.com/stripe/stripe-android/pull/1864) Make Klarna Source creation methods public and create example
    * See `SourceParams.createKlarna()`
* [#1865](https://github.com/stripe/stripe-android/pull/1865) Make all model classes implement `Parcelable`
* [#1871](https://github.com/stripe/stripe-android/pull/1871) Simplify configuration of example app
    * Example app can be configured via `$HOME/.gradle/gradle.properties` instead of `Settings.kt` 
      ```
      STRIPE_EXAMPLE_BACKEND_URL=https://hidden-beach-12345.herokuapp.com/
      STRIPE_EXAMPLE_PUBLISHABLE_KEY=pk_test_12345
      STRIPE_ACCOUNT_ID=
      ```
* [#1883](https://github.com/stripe/stripe-android/pull/1883) Enable `PaymentSessionConfig.ShippingInformationValidator` and `PaymentSessionConfig.ShippingMethodsFactory`
    * See the [Migration Guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for an example of how to use the new interfaces
* [#1884](https://github.com/stripe/stripe-android/pull/1884) Mark `PaymentFlowExtras` as deprecated
* [#1885](https://github.com/stripe/stripe-android/pull/1885) Create `Stripe#retrieveSource()` for asynchronous `Source` retrieval
* [#1890](https://github.com/stripe/stripe-android/pull/1890) Upgrade 3DS2 SDK to 2.2.3
    * Fix crash when using Instant App

## 12.5.0 - 2019-11-21
* [#1836](https://github.com/stripe/stripe-android/pull/1836) Add support for [statement_descriptor](https://stripe.com/docs/api/sources/object#source_object-statement_descriptor) field to `Source` model via `Source#statementDescriptor`
* [#1837](https://github.com/stripe/stripe-android/pull/1837) Add support for [source_order](https://stripe.com/docs/api/sources/create#create_source-source_order) param via `SourceOrderParams`
* [#1839](https://github.com/stripe/stripe-android/pull/1839) Add support for [source_order](https://stripe.com/docs/api/sources/object#source_object-source_order) field to `Source` model via `Source#sourceOrder`
* [#1842](https://github.com/stripe/stripe-android/pull/1842) Add `PaymentSessionConfig.Builder.setAllowedShippingCountryCodes()`. Used to specify an allowed set of countries when collecting the customer's shipping address via `PaymentSession`.
    ```kotlin
    // Example
    PaymentSessionConfig.Builder()
        // only allowed US and Canada shipping addresses
        .setAllowedShippingCountryCodes(setOf("US", "CA"))
        .build()
    ```
* [#1845](https://github.com/stripe/stripe-android/pull/1845) Fix country code validation in `PaymentFlowActivity`'s shipping information screen
    * Require that the customer submits a country that exists in the autocomplete dropdown
    * Show error UI when the submitted country fails validation
* [#1857](https://github.com/stripe/stripe-android/pull/1857) Fix crash related to Kotlin Coroutines
    * Downgrade `kotlinx-coroutines` from `1.3.2` to `1.3.0`
    * Add Proguard rules

## 12.4.0 - 2019-11-13
* [#1792](https://github.com/stripe/stripe-android/pull/1792) Remove default selection of a Payment Method from `PaymentMethodsActivity`
* [#1797](https://github.com/stripe/stripe-android/pull/1797) Document `StripeDefaultTheme` style
* [#1799](https://github.com/stripe/stripe-android/pull/1799) Document `Stripe3DS2Theme` and related styles
* [#1809](https://github.com/stripe/stripe-android/pull/1809) Update to Gradle 6.0
* [#1810](https://github.com/stripe/stripe-android/pull/1810) Update API version to [2019-11-05](https://stripe.com/docs/upgrades#2019-11-05)
* [#1812](https://github.com/stripe/stripe-android/pull/1812) Upgrade 3DS2 SDK to 2.2.2
* [#1813](https://github.com/stripe/stripe-android/pull/1813) Don't select a new PaymentMethod after deleting one in `PaymentMethodsActivity`
* [#1820](https://github.com/stripe/stripe-android/pull/1820) Update `PaymentMethodsActivity` result and `PaymentSession.handlePaymentData()` logic
    * `PaymentMethodsActivity` returns result code of `Activity.RESULT_OK` when the user selected a Payment Method
    * `PaymentMethodsActivity` returns result code of `Activity.RESULT_CANCELED` when the user taps back via the toolbar or device back button
    * `PaymentSession#handlePaymentData()` now calls `PaymentSessionListener#onPaymentSessionDataChanged()` for any result from `PaymentMethodsActivity`

## 12.3.0 - 2019-11-05
* [#1775](https://github.com/stripe/stripe-android/pull/1775) Add support for idempotency key on Stripe Token API requests
* [#1777](https://github.com/stripe/stripe-android/pull/1777) Make `Card` implement `Parcelable`
* [#1781](https://github.com/stripe/stripe-android/pull/1781) Mark `Stripe#createToken()` as `@Deprecated`; replace with `Stripe#createCardToken()`
* [#1782](https://github.com/stripe/stripe-android/pull/1782) Mark `Stripe#authenticatePayment()` and `Stripe#authenticateSetup()` as `@Deprecated`; replace with `Stripe#handleNextActionForPayment()` and `Stripe#handleNextActionForSetupIntent()`, respectively
* [#1784](https://github.com/stripe/stripe-android/pull/1784) Update API version to [2019-10-17](https://stripe.com/docs/upgrades#2019-10-17)
* [#1787](https://github.com/stripe/stripe-android/pull/1787) Fix `CardNumberEditText` performance
* [#1788](https://github.com/stripe/stripe-android/pull/1788) Fix `ExpiryDateEditText` performance

## 12.2.0 - 2019-10-31
* [#1745](https://github.com/stripe/stripe-android/pull/1745) Make `StripeEditText` public
* [#1746](https://github.com/stripe/stripe-android/pull/1746) Make `FpxBank` enum public
* [#1748](https://github.com/stripe/stripe-android/pull/1748) Update FPX bank list with offline status
* [#1755](https://github.com/stripe/stripe-android/pull/1755) Annotate `Stripe` methods with `@UiThread` or `@WorkerThread`
* [#1758](https://github.com/stripe/stripe-android/pull/1758) Refactor `CustomerSession.setCustomerShippingInformation()`
* [#1764](https://github.com/stripe/stripe-android/pull/1764) Add support for Javascript confirm dialogs in 3DS1 payment authentication WebView
* [#1765](https://github.com/stripe/stripe-android/pull/1765) Fix rotation issues with shipping info and shipping method selection screens

## 12.1.0 - 2019-10-22
* [#1738](https://github.com/stripe/stripe-android/pull/1738) Enable specifying Payment Method type to use in UI components

## 12.0.1 - 2019-10-21
* [#1721](https://github.com/stripe/stripe-android/pull/1721) Properly cleanup and destroy `PaymentAuthWebView`
* [#1722](https://github.com/stripe/stripe-android/pull/1722) Fix crash in 3DS2 challenge screen when airplane mode is enabled
* [#1731](https://github.com/stripe/stripe-android/pull/1731) Create `ConfirmSetupIntentParams.createWithoutPaymentMethod()`

## 12.0.0 - 2019-10-16
* [#1699](https://github.com/stripe/stripe-android/pull/1699) Remove deprecated methods
    * Replace `Stripe#createTokenSynchronous(Card)` with `Stripe#createCardTokenSynchronous(Card)`
    * Replace `Card#getCVC()` with `Card#getCvc()`
    * Remove `AddPaymentMethodActivity#EXTRA_NEW_PAYMENT_METHOD`, use `AddPaymentMethodActivityStarter.Result.fromIntent()` instead
    * Create overloaded `ShippingMethod` constructor with optional `detail` argument
* [#1701](https://github.com/stripe/stripe-android/pull/1701) Payment Intent API requests (i.e. requests to `/v1/payment_intents`) now return localized error messages
* [#1706](https://github.com/stripe/stripe-android/pull/1706) Add `Card#toPaymentMethodsParams()` to create a `PaymentMethodCreateParams` instance that includes both card and billing details

## 11.3.0 - 2020-05-15
* Update 3DS2 SDK to v2.3.7
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
  * See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection) for more details
* Include sources

## 11.2.2 - 2019-10-11
* [#1686](https://github.com/stripe/stripe-android/pull/1686) Fix native crash on some devices in 3DS1 payment authentication WebView
* [#1690](https://github.com/stripe/stripe-android/pull/1690) Bump API version to `2019-10-08`
* [#1693](https://github.com/stripe/stripe-android/pull/1693) Add support for SEPA Debit in PaymentMethod

## 11.2.1 - 2019-10-11
* [#1677](https://github.com/stripe/stripe-android/pull/1677) Add logging to PaymentAuthWebViewActivity

## 11.2.0 - 2019-10-07
* [#1616](https://github.com/stripe/stripe-android/pull/1616) Make `AddPaymentMethodActivityStarter.Result.fromIntent()` public
* [#1619](https://github.com/stripe/stripe-android/pull/1619) Add `CardMultilineWidget#getPaymentMethodBillingDetailsBuilder()`
* [#1643](https://github.com/stripe/stripe-android/pull/1643) Create `Stripe.createCardTokenSynchronous()`
* [#1647](https://github.com/stripe/stripe-android/pull/1647) Add `StripeDefault3DS2Theme` for 3DS2 customization via themes
* [#1652](https://github.com/stripe/stripe-android/pull/1652) In `PaymentMethodsActivity`, select a new Payment Method if the previously selected one was deleted
* [#1658](https://github.com/stripe/stripe-android/pull/1658) Add `stripe_` prefix to Stripe resources
* [#1664](https://github.com/stripe/stripe-android/pull/1664) Upgrade AGP to 3.5.1
* [#1666](https://github.com/stripe/stripe-android/pull/1666) Add logging support
    ```kotlin
    // Example
    val enableLogging: Boolean = true
    val stripe: Stripe = Stripe(this, "pk_test_demo", enableLogging = enableLogging)
    stripe.confirmPayment(this, confirmPaymentIntentParams)
    
    // View logs using
    // $ adb logcat -s StripeSdk
    ```
* [#1667](https://github.com/stripe/stripe-android/pull/1667) Add support for SEPA Debit Payment Methods
    ```kotlin
    // Example
    PaymentMethodCreateParams.create(
        PaymentMethodCreateParams.SepaDebit.Builder()
            .setIban("__iban__")
            .build()
    )
    ```
* [#1668](https://github.com/stripe/stripe-android/pull/1668) Update Google Pay integration example in example app
* [#1669](https://github.com/stripe/stripe-android/pull/1669) Update 3DS2 SDK to 2.1.3
    * Prevent challenge screen's cancel button from being clicked more than once

## 11.1.4 - 2019-09-24
* [#1603](https://github.com/stripe/stripe-android/pull/1603) Update ProGuard rules for BouncyCastle
* [#1608](https://github.com/stripe/stripe-android/pull/1608) Update ProGuard rules for Material Components

## 11.1.3 - 2019-09-18
* [#1582](https://github.com/stripe/stripe-android/pull/1582) Update 3DS2 SDK to 2.0.5
    * Add translations for `ko`, `nn`, `ru`, and `tr`
* [#1583](https://github.com/stripe/stripe-android/pull/1583) Create `AddPaymentMethodActivityStarter.Result`
    * Mark `AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD` as `@Deprecated`. Use `AddPaymentMethodActivityStarter.Result` instead.
    ```kotlin
    // Example

    // before
    val paymentMethod: PaymentMethod? = data.getParcelableExtra(EXTRA_NEW_PAYMENT_METHOD)

    // after
    val result: AddPaymentMethodActivityStarter.Result =
        AddPaymentMethodActivityStarter.Result.fromIntent(data)
    val paymentMethod: PaymentMethod? = result?.paymentMethod
    ```    
* [#1587](https://github.com/stripe/stripe-android/pull/1587) Fix logic for entering 3DS2 challenge flow

## 11.1.2 - 2019-09-18
* [#1581](https://github.com/stripe/stripe-android/pull/1581) Fix WebView issues in API 21 and 22

## 11.1.1 - 2019-09-17
* [#1578](https://github.com/stripe/stripe-android/pull/1578) Disable dokka in `:stripe` to fix release process

## 11.1.0 - 2019-09-17
* [#1545](https://github.com/stripe/stripe-android/pull/1545) Add Connect Account Id support to `GooglePayConfig`
* [#1560](https://github.com/stripe/stripe-android/pull/1560) Add swipe-to-delete gesture on added Payment Methods in `PaymentMethodsActivity`
* [#1560](https://github.com/stripe/stripe-android/pull/1560) Fix `HandlerThread` leak in `PaymentController.ChallengeFlowStarterImpl`
* [#1561](https://github.com/stripe/stripe-android/pull/1561) Move `CardMultilineWidget` focus to first error field on error
* [#1572](https://github.com/stripe/stripe-android/pull/1572) Update 3DS2 SDK to 2.0.4
* [#1574](https://github.com/stripe/stripe-android/pull/1574) Fix `HandlerThread` leak in `StripeFireAndForgetRequestExecutor`
* [#1577](https://github.com/stripe/stripe-android/pull/1577) Fix `ShippingMethodView` height

## 11.0.5 - 2019-09-13
* [#1538](https://github.com/stripe/stripe-android/pull/1538) Update `PaymentAuthWebView` to fix issues

## 11.0.4 - 2019-09-13
* [#1533](https://github.com/stripe/stripe-android/pull/1533) Update 3DS2 SDK to 2.0.3
* [#1534](https://github.com/stripe/stripe-android/pull/1534) Add ability to select checked item in `PaymentMethodsActivity`
* [#1537](https://github.com/stripe/stripe-android/pull/1537) Fix out-of-band web payment authentication

## 11.0.3 - 2019-09-12
* [#1530](https://github.com/stripe/stripe-android/pull/1530) Finish `PaymentAuthWebViewActivity` after returning from bank app

## 11.0.2 - 2019-09-12
* [#1527](https://github.com/stripe/stripe-android/pull/1527) Support `"intent://"` URIs in payment auth WebView

## 11.0.1 - 2019-09-11
* [#1518](https://github.com/stripe/stripe-android/pull/1518) Fix crash when payment authentication is started from Fragment and user taps back twice
* [#1523](https://github.com/stripe/stripe-android/pull/1523) Correctly handle deep-links in the in-app payment authentication WebView
* [#1524](https://github.com/stripe/stripe-android/pull/1524) Update 3DS2 SDK to 2.0.2
    * Fix issue with 3DS2 encryption and older BouncyCastle versions

## 11.0.0 - 2019-09-10
* [#1474](https://github.com/stripe/stripe-android/pull/1474) Fix darkmode issue with "Add an Address" form's Country selection dropdown
* [#1475](https://github.com/stripe/stripe-android/pull/1475) Hide keyboard after submitting "Add an Address" form in standard integration
* [#1478](https://github.com/stripe/stripe-android/pull/1478) Migrate to AndroidX
* [#1479](https://github.com/stripe/stripe-android/pull/1479) Persist `PaymentConfiguration` to `SharedPreferences`
* [#1480](https://github.com/stripe/stripe-android/pull/1480) Make `Source` immutable
* [#1481](https://github.com/stripe/stripe-android/pull/1481) Remove `@Deprecated` methods from `StripeIntent` models
    * Remove `PaymentIntent#getSource()`; use `PaymentIntent#getPaymentMethodId()`
    * Remove `SetupIntent#getCustomerId()`
    * Remove `SourceCallback`; use `ApiResultCallback<Source>`
    * Remove `TokenCallback`; use `ApiResultCallback<Token>`
* [#1485](https://github.com/stripe/stripe-android/pull/1485) Update 3DS2 SDK to 2.0.1
* [#1494](https://github.com/stripe/stripe-android/pull/1494) Update `PaymentMethodsActivity` UX
* [#1495](https://github.com/stripe/stripe-android/pull/1495) Remove `@Deprecated` fields and methods from `PaymentMethodsActivity`
* [#1497](https://github.com/stripe/stripe-android/pull/1497) Remove `Stripe` methods that accept a publishable key
* [#1506](https://github.com/stripe/stripe-android/pull/1506) Remove `Stripe#createToken()` with `Executor` argument
* [#1514](https://github.com/stripe/stripe-android/pull/1514) Bump API version to `2019-09-09`

## 10.5.0 - 2020-05-15
* Update 3DS2 SDK to v1.3.1
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
  * See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection) for more details
* Include sources

## 10.4.6 - 2019-10-14
* Apply hot-fixes from 11.x
    * Update BouncyCastle Proguard rules.
      Keep only the BouncyCastle provider classes.
    * Hide progress bar in `onPageFinished()` instead of
      `onPageCommitVisible()` to avoid potential crash
      on some devices.

## 10.4.5 - 2019-09-16
* Apply hot-fixes from 11.x
    * Enable DOM storage in `PaymentAuthWebView` to fix crash

## 10.4.4 - 2019-09-13
* Apply hot-fixes from 11.x

## 10.4.3 - 2019-09-04
* [#1471](https://github.com/stripe/stripe-android/pull/1471) Fix issue with `CardUtils` visibility

## 10.4.2 - 2019-08-30
* [#1461](https://github.com/stripe/stripe-android/pull/1461) Fix crash in `PaymentAuthWebView`
* [#1462](https://github.com/stripe/stripe-android/pull/1462) Animate selections in `PaymentMethodsActivity`

## 10.4.1 - 2019-08-30
* [#1457](https://github.com/stripe/stripe-android/pull/1457) Fix crash in "Add an Address" screen when value for Country is empty

## 10.4.0 - 2019-08-29
* [#1421](https://github.com/stripe/stripe-android/pull/1421) Create `PaymentMethodsActivityStarter.Result` to retrieve result of `PaymentMethodsActivity`
* [#1427](https://github.com/stripe/stripe-android/pull/1427) Mark `Stripe` methods that accept a publishable key as deprecated
    ```
    // Example

    // before
    val stripe = Stripe(context)
    stripe.createPaymentMethodSynchronous(params, "pk_test_demo123")

    // after
    val stripe = Stripe(context, "pk_test_demo123")
    stripe.createPaymentMethodSynchronous(params)
    ```
* [#1428](https://github.com/stripe/stripe-android/pull/1428) Guard against opaque URIs in `PaymentAuthWebViewClient`
* [#1433](https://github.com/stripe/stripe-android/pull/1433) Add `setCardHint()` to `CardInputWidget` and `CardMultilineWidget`
* [#1434](https://github.com/stripe/stripe-android/pull/1434) Add setters on Card widgets for card number, expiration, and CVC
* [#1438](https://github.com/stripe/stripe-android/pull/1438) Bump API version to `2019-08-14`
* [#1446](https://github.com/stripe/stripe-android/pull/1446) Update `PaymentIntent` and `SetupIntent` models
    * Add missing `PaymentIntent#getPaymentMethodId()`
    * Mark `PaymentIntent#getSource()` as `@Deprecated` - use `PaymentIntent#getPaymentMethodId()`
    * Mark `SetupIntent#getCustomerId()` as `@Deprecated` - this attribute is not available with a publishable key
* [#1448](https://github.com/stripe/stripe-android/pull/1448) Update Gradle to 5.6.1
* [#1449](https://github.com/stripe/stripe-android/pull/1449) Add support for `cancellation_reason` attribute to PaymentIntent
* [#1450](https://github.com/stripe/stripe-android/pull/1450) Add support for `cancellation_reason` attribute to SetupIntent
* [#1451](https://github.com/stripe/stripe-android/pull/1451) Update Stripe 3DS2 library to `v1.2.2`
    * Dismiss keyboard after submitting 3DS2 form
    * Exclude `org.ow2.asm:asm` dependency

## 10.3.1 - 2018-08-22
* [#1394](https://github.com/stripe/stripe-android/pull/1394) Add `shouldPrefetchCustomer` arg to `PaymentSession.init()`
* [#1395](https://github.com/stripe/stripe-android/pull/1395) Fix inconsistent usage of relative attributes on card icon in `CardMultilineWidget`
* [#1412](https://github.com/stripe/stripe-android/pull/1412) Make `AddPaymentMethodActivityStarter()` available for starting `AddPaymentMethodActivity`
    ```
    // Example usage

    AddPaymentMethodActivityStarter(activity).startForResult(
        AddPaymentMethodActivityStarter.Args.Builder()
            .setShouldAttachToCustomer(true)
            .setShouldRequirePostalCode(true)
            .build()
    )
    ```
* [#1417](https://github.com/stripe/stripe-android/pull/1417) Update Stripe 3DS2 library to `v1.2.1`
    * Add support for updating status bar color and progress color using
      `Stripe3ds2UiCustomization.Builder.createWithAppTheme(Activity)`
      or UI customization builders

      ```
      // Example usage

      Stripe3ds2UiCustomization.Builder.createWithAppTheme(this)

      PaymentAuthConfig.Stripe3ds2UiCustomization.Builder()
          .setAccentColor("#9cdbff")
          .build()

      PaymentAuthConfig.Stripe3ds2ToolbarCustomization.Builder()
          .setStatusBarColor("#392996")
          .build()
      ```

## 10.3.0 - 2018-08-16
* [#1327](https://github.com/stripe/stripe-android/pull/1327) Deprecate `SourceCallback` and `TokenCallback`
    * Use `ApiResultCallback<Source>` and `ApiResultCallback<Token>` respectively
* [#1332](https://github.com/stripe/stripe-android/pull/1332) Create `StripeParamsModel` interface for Stripe API param classes
* [#1334](https://github.com/stripe/stripe-android/pull/1344) Remove `StripeModel#toMap()`
* [#1340](https://github.com/stripe/stripe-android/pull/1340) Upgrade Android Gradle Plugin to `3.5.0-rc03`
* [#1342](https://github.com/stripe/stripe-android/pull/1342) Update Builds Tools to `29.0.2`
* [#1347](https://github.com/stripe/stripe-android/pull/1347) Mark `Stripe.setStripeAccount()` as deprecated
    * Use `new Stripe(context, "publishable_key", "stripe_account_id")` instead
* [#1376](https://github.com/stripe/stripe-android/pull/1376) Add `shouldPrefetchEphemeralKey` arg to `CustomerSession.initCustomerSession()`
* [#1378](https://github.com/stripe/stripe-android/pull/1378) Add `last_payment_error` field to PaymentIntent model
* [#1379](https://github.com/stripe/stripe-android/pull/1379) Add `last_setup_error` field to SetupIntent model
* [#1388](https://github.com/stripe/stripe-android/pull/1388) Update Stripe 3DS2 library to `v1.1.7`
    * Fix issue in `PaymentAuthConfig.Stripe3ds2UiCustomization.Builder()` and `PaymentAuthConfig.Stripe3ds2UiCustomization.Builder.createWithAppTheme()` that was resulting in incorrect color customization on 3DS2 challenge screen 
    * Improve accessibility of select options on 3DS2 challenge screen by setting minimum height to 48dp

## 10.2.1 - 2019-08-06
* [#1314](https://github.com/stripe/stripe-android/pull/1314) Expose pinned API version via [Stripe.API_VERSION](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/-companion/index.html#com.stripe.android/Stripe.Companion/API_VERSION/#/PointingToDeclaration/)
* [#1315](https://github.com/stripe/stripe-android/pull/1315) Create [SourceParams#createCardParamsFromGooglePay()](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-source-params/-companion/create-card-params-from-google-pay.html)
* [#1316](https://github.com/stripe/stripe-android/pull/1316) Fix issue where `InvalidRequestException` is thrown when confirming a Setup Intent using [ConfirmSetupIntentParams#create()](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-confirm-setup-intent-params/-companion/create.html)

## 10.2.0 - 2019-08-05
* [#1275](https://github.com/stripe/stripe-android/pull/1275) Add support for launching `PaymentSession` from a `Fragment`
    * [PaymentSession(Fragment)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-payment-session/index.html#com.stripe.android/PaymentSession/<init>/#androidx.fragment.app.Fragment#com.stripe.android.PaymentSessionConfig/PointingToDeclaration/)
* [#1288](https://github.com/stripe/stripe-android/pull/1288) Upgrade Android Gradle Plugin to `3.5.0-rc02`
* [#1289](https://github.com/stripe/stripe-android/pull/1289) Add support for launching payment confirmation/authentication flow from a `Fragment`
    * [Stripe#confirmPayment(Fragment, ConfirmPaymentIntentParams)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-payment.html)
    * [Stripe#authenticatePayment(Fragment, String)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-payment.html)
    * [Stripe#confirmSetupIntent(Fragment, ConfirmSetupIntentParams)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-setup-intent.html)
    * [Stripe#authenticateSetup(Fragment, String)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-setup.html)
* [#1290](https://github.com/stripe/stripe-android/pull/1290) Convert [samplestore app](https://github.com/stripe/stripe-android/tree/master/samplestore) to Kotlin
* [#1297](https://github.com/stripe/stripe-android/pull/1297) Convert [example app](https://github.com/stripe/stripe-android/tree/master/example) to Kotlin
* [#1300](https://github.com/stripe/stripe-android/pull/1300) Rename `StripeIntentResult#getStatus()` to [StripeIntentResult#getOutcome()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe-intent-result/index.html#com.stripe.android/StripeIntentResult/outcome/#/PointingToDeclaration/)
* [#1302](https://github.com/stripe/stripe-android/pull/1302) Add [GooglePayConfig#getTokenizationSpecification()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-google-pay-config/index.html#com.stripe.android/GooglePayConfig/tokenizationSpecification/#/PointingToDeclaration/) to configure Google Pay to use Stripe as the gateway
* [#1304](https://github.com/stripe/stripe-android/pull/1304) Add [PaymentMethodCreateParams#createFromGooglePay()](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-payment-method-create-params/-companion/create-from-google-pay.html) to create `PaymentMethodCreateParams` from a Google Pay [PaymentData](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentData) object

## 10.1.1 - 2019-07-31
* [#1275](https://github.com/stripe/stripe-android/pull/1275) Fix `StripeIntentResult.Status` logic
* [#1276](https://github.com/stripe/stripe-android/pull/1276) Update Stripe 3DS2 library to `v1.1.5`
    * Fix crash in 3DS2 challenge flow when [conscrypt](https://github.com/google/conscrypt) is installed
* [#1277](https://github.com/stripe/stripe-android/pull/1277) Fix StrictMode failure in `StripeFireAndForgetRequestExecutor`

## 10.1.0 - 2019-07-30
* [#1244](https://github.com/stripe/stripe-android/pull/1244) Add support for Stripe Connect in 3DS2
* [#1256](https://github.com/stripe/stripe-android/pull/1256) Add `StripeIntentResult.Status` flag to 3DS1 authentication results
* [#1257](https://github.com/stripe/stripe-android/pull/1257) Correctly pass `Status.FAILED` when 3DS2 auth fails
* [#1259](https://github.com/stripe/stripe-android/pull/1259) Add `StripeIntentResult.Status.TIMEDOUT` value
* [#1263](https://github.com/stripe/stripe-android/pull/1263) Update `PaymentSession#presentPaymentMethodSelection()` logic
    * Add overloaded `presentPaymentMethodSelection()` that takes a Payment Method ID to initially select
    * Add `PaymentSessionPrefs` to persist customer's Payment Method selection across `PaymentSession` instances
* [#1264](https://github.com/stripe/stripe-android/pull/1264) Update `Stripe3ds2UiCustomization`
    * Add `Stripe3ds2UiCustomization.Builder.createWithAppTheme(Activity)` to create a `Stripe3ds2UiCustomization.Builder` based on the app theme

## 10.0.3 - 2019-07-24
* Update Stripe 3DS2 library to `v1.1.2`
    * Disable R8 following [user-reported issue](https://github.com/stripe/stripe-android/issues/1236)
    * Update Proguard rules

## 10.0.2 - 2019-07-23
* [#1238](https://github.com/stripe/stripe-android/pull/1238) Update Proguard rules to fix integration issues with Stripe 3DS2 library

## 10.0.1 - 2019-07-22
* [#1226](https://github.com/stripe/stripe-android/pull/1226) Prevent non-critical network requests from blocking API requests by moving fire-and-forget requests to separate thread
* [#1228](https://github.com/stripe/stripe-android/pull/1228) Update to Android Gradle Plugin to 3.5.0-rc01
* Update Stripe 3DS2 library to `v1.1.0`
    * Re-enable R8 following bug fix in Android Studio 3.5
    * Move `org.ow2.asm:asm` dependency to `testImplementation`
    * Fix known issue with 3DS2 challenge flow and API 19 devices

## 10.0.0 - 2019-07-19
* Add support for 3DS2 authentication through the Payment Intents API and Setup Intents API. See [Supporting 3D Secure Authentication on Android](https://stripe.com/docs/mobile/android/authentication).
    * Payment Intents - see our guide for [Using Payment Intents on Android](https://stripe.com/docs/payments/payment-intents/android)
        * [Stripe#confirmPayment()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-payment.html) for automatic confirmation
        * [Stripe#authenticatePayment()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-payment.html) for manual confirmation
    * Setup Intents - see our guide for [Saving card details without a payment](https://stripe.com/docs/payments/cards/saving-cards#saving-card-without-payment)
        * [Stripe#confirmSetupIntent()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-setup-intent.html) for automatic confirmation
        * [Stripe#authenticateSetup()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-setup.html) for manual confirmation
    * [PaymentAuthConfig](https://stripe.dev/stripe-android/stripe/com.stripe.android/-payment-auth-config/index.html) for optional authentication customization
* Add support for the Setup Intents API. See [Saving card details without a payment](https://stripe.com/docs/payments/cards/saving-cards#saving-card-without-payment).
    * Use [ConfirmSetupIntentParams](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-confirm-setup-intent-params/index.html) to confirm a SetupIntent
* [#1172](https://github.com/stripe/stripe-android/pull/1172) Refactor `PaymentIntentParams`
* [#1173](https://github.com/stripe/stripe-android/pull/1173) Inline all `StringDef` values

## 9.4.0 - 2020-05-15
* Update 3DS2 SDK to v1.3.1
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
  * See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection) for more details
* Include sources

## 9.3.8 - 2019-07-16
* [#1193](https://github.com/stripe/stripe-android/pull/1193) Fix `RuntimeException` related to 3DS2

## 9.3.7 - 2019-07-15
* [#1154](https://github.com/stripe/stripe-android/pull/1154) Fix `NullPointerException` in `PaymentMethodsActivity`
* [#1174](https://github.com/stripe/stripe-android/pull/1174) Add `getCardBuilder()` method to Card widgets
* [#1184](https://github.com/stripe/stripe-android/pull/1184) Fix `NullPointerException` related to 3DS2

## 9.3.6 - 2019-07-08
* [#1148](https://github.com/stripe/stripe-android/pull/1148) Fix 3DS2 dependency Proguard issues

## 9.3.5 - 2019-06-20
* [#1138](https://github.com/stripe/stripe-android/pull/1138) Fix `AppInfo` param name

## 9.3.4 - 2019-06-19
* [#1133](https://github.com/stripe/stripe-android/pull/1133) Make `AppInfo` public

## 9.3.3 - 2019-06-19
* [#1108](https://github.com/stripe/stripe-android/pull/1108) Create `Card#toBuilder()`
* [#1114](https://github.com/stripe/stripe-android/pull/1114) Update `CustomerSession` methods to take `@NonNull` listener argument
* [#1125](https://github.com/stripe/stripe-android/pull/1125) Create `StripeIntent` interface and move `PaymentIntent.Status` and `PaymentIntent.NextActionType` to `StripeIntent`

## 9.3.2 - 2019-06-12
* [#1104](https://github.com/stripe/stripe-android/pull/1104) Handle null response body

## 9.3.1 - 2019-06-12
* [#1099](https://github.com/stripe/stripe-android/pull/1099) Fix Gradle module issue

## 9.3.0 - 2019-06-12
* [#1019](https://github.com/stripe/stripe-android/pull/1019) Introduce `CustomerSession#detachPaymentMethod()`
* [#1067](https://github.com/stripe/stripe-android/pull/1067) Remove `PaymentResultListener`. Replace `PaymentSession#completePayment()` with `PaymentSession#onCompleted()`.
* [#1075](https://github.com/stripe/stripe-android/pull/1075) Make model classes final
* [#1080](https://github.com/stripe/stripe-android/pull/1080) Update Build Tools to 29.0.0
* [#1082](https://github.com/stripe/stripe-android/pull/1082) Remove `StripeJsonModel#toJson()`
* [#1084](https://github.com/stripe/stripe-android/pull/1084) Rename `StripeJsonModel` to `StripeModel`
* [#1093](https://github.com/stripe/stripe-android/pull/1093) Add `Stripe#setAppInfo()`. See [Identifying your plug-in or library](https://stripe.com/docs/building-plugins#setappinfo) for more details.

## 9.2.0 - 2019-06-04
* [#1019](https://github.com/stripe/stripe-android/pull/1019) Upgrade pinned API version to `2019-05-16`
* [#1036](https://github.com/stripe/stripe-android/pull/1036) Validate API key before every request
* [#1046](https://github.com/stripe/stripe-android/pull/1046) Make `Card` model fields immutable

## 9.1.1 - 2019-05-28
* [#1006](https://github.com/stripe/stripe-android/pull/1006) Remove null values in `PaymentMethod.BillingDetails#toMap()`

## 9.1.0 - 2019-05-28
* [#952](https://github.com/stripe/stripe-android/pull/952) Update standard integration UI to use PaymentMethods instead of Sources
* [#962](https://github.com/stripe/stripe-android/pull/962) Add initial dark theme support to widgets
* [#963](https://github.com/stripe/stripe-android/pull/963) Add Autofill hints to forms
* [#964](https://github.com/stripe/stripe-android/pull/964) Upgrade Android Gradle Plugin to 3.4.1
* [#972](https://github.com/stripe/stripe-android/pull/964) Add PaymentIntent#getNextActionType()
* [#989](https://github.com/stripe/stripe-android/pull/989) Fix StripeEditText's error color logic
* [#1001](https://github.com/stripe/stripe-android/pull/1001) Overload `PaymentSession#presentPaymentMethodSelection` to allow requiring postal field

## 9.0.1 - 2019-05-17
* [#945](https://github.com/stripe/stripe-android/pull/945) Add `business_type` param to Account tokenization when available

## 9.0.0 - 2019-05-06
Note: this release has breaking changes. See [MIGRATING.md](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md)
* [#873](https://github.com/stripe/stripe-android/pull/873) Update pinned API version to `2019-03-14`
* [#875](https://github.com/stripe/stripe-android/pull/875) Inject `Context` in `CustomerSession` and `StripeApiHandler`
* [#907](https://github.com/stripe/stripe-android/pull/907) Update `PaymentIntent` model for API `2019-02-11`
* [#894](https://github.com/stripe/stripe-android/pull/894) Upgrade Android Gradle Plugin to 3.4.0
* [#872](https://github.com/stripe/stripe-android/pull/872) Create `CustomerSession.ActivityCustomerRetrievalListener` to handle `Activity` weak refs
* [#935](https://github.com/stripe/stripe-android/pull/935) Increase `minSdkVersion` from 14 to 19

## 8.7.0 - 2019-04-12
* [#863](https://github.com/stripe/stripe-android/pull/863) Fix garbage-collection issues with `Stripe` callbacks
* [#856](https://github.com/stripe/stripe-android/pull/856) Fix race-conditions with `CustomerSession` listeners
* [#857](https://github.com/stripe/stripe-android/pull/857) Correctly parse JSON when creating or deleting customer card source
* [#858](https://github.com/stripe/stripe-android/pull/858) Fix crash on some devices (e.g. Meizu) related to `TextInputEditText`
* [#862](https://github.com/stripe/stripe-android/pull/862) Improve `PaymentMethodCreateParams.Card` creation
* [#870](https://github.com/stripe/stripe-android/pull/870) Update `PaymentIntent#Status` enum
* [#865](https://github.com/stripe/stripe-android/pull/865) Fix some memory leak issues in example app

## 8.6.0 - 2019-04-05
* [#849](https://github.com/stripe/stripe-android/pull/849) Downgrade from AndroidX to Android Support Library
* [#843](https://github.com/stripe/stripe-android/pull/843) Add setter for custom CVC field label on `CardMultilineWidget`
* [#850](https://github.com/stripe/stripe-android/pull/850) Fix a11y traversal order in `CardInputWidget`
* [#839](https://github.com/stripe/stripe-android/pull/839) Refactor `StripeApiHandler` to use instance methods

## 8.5.0 - 2019-03-05
* [#805](https://github.com/stripe/stripe-android/pull/805) Clean up `PaymentIntent`
* [#806](https://github.com/stripe/stripe-android/pull/806) Pass `StripeError` in onError
* [#807](https://github.com/stripe/stripe-android/pull/807) Create `ErrorMessageTranslator` and default implementation
* [#809](https://github.com/stripe/stripe-android/pull/809) Make `StripeSourceTypeModel` public
* [#817](https://github.com/stripe/stripe-android/pull/817) Fix TalkBack crash in `CardInputWidget`
* [#822](https://github.com/stripe/stripe-android/pull/822) Fix account token failure on latest API version
* [#827](https://github.com/stripe/stripe-android/pull/827) Upgrade Android Gradle Plugin to 3.3.2
* [#828](https://github.com/stripe/stripe-android/pull/828) Support save to customer param on `PaymentIntent` confirm
* [#829](https://github.com/stripe/stripe-android/pull/829) Add `"not_required"` as possible value of `Source` `redirect[status]`
* [#830](https://github.com/stripe/stripe-android/pull/830) Add `"recommended"` as possible value of `Source` `card[three_d_secure]`
* [#832](https://github.com/stripe/stripe-android/pull/832) Pin all Stripe requests to API version `2017-06-05`

## 8.4.0 - 2019-02-06
* [#793](https://github.com/stripe/stripe-android/pull/793) Add StripeError field to StripeException
* [#787](https://github.com/stripe/stripe-android/pull/787) Add support for creating a CVC update Token
* [#791](https://github.com/stripe/stripe-android/pull/791) Prevent AddSourceActivity's SourceCallback from being garbage collected
* [#790](https://github.com/stripe/stripe-android/pull/790) Fix IME action logic on CVC field in CardMultilineWidget
* [#786](https://github.com/stripe/stripe-android/pull/786) Add metadata field to Card

## 8.3.0 - 2019-01-25
* [#780](https://github.com/stripe/stripe-android/pull/780) Fix bug related to ephemeral keys introduced in 8.2.0
* [#778](https://github.com/stripe/stripe-android/pull/778) Migrate to Android X
* [#771](https://github.com/stripe/stripe-android/pull/771) Add errorCode and errorDeclineCode fields to InvalidRequestException
* [#766](https://github.com/stripe/stripe-android/pull/766) Upgrade to Android Gradle Plugin 3.3.0
* [#770](https://github.com/stripe/stripe-android/pull/770) Remove Saudi Arabia from the no postal code countries list

## 8.2.0 - 2019-01-10
* [#675](https://github.com/stripe/stripe-android/pull/675) Add support for Android 28
    * Update Android SDK Build Tools to 28.0.3
    * Update Android Gradle plugin to 3.2.1
    * Update compileSdkVersion and targetSdkVersion to 28
    * Update Android Support Library to 28.0.0
* [#671](https://github.com/stripe/stripe-android/pull/671) Add ability to set card number and validate on `CardMultilineWidget`
* [#537](https://github.com/stripe/stripe-android/pull/537) Add support for iDeal Source with connected account
* [#669](https://github.com/stripe/stripe-android/pull/669) Add support for Portuguese
* [#624](https://github.com/stripe/stripe-android/pull/624) Add ability to cancel callbacks without destroying `CustomerSession` instance
* [#673](https://github.com/stripe/stripe-android/pull/673) Improve accessibility on card input fields
* [#656](https://github.com/stripe/stripe-android/pull/656) Add `AccessibilityNodeInfo` to widgets 
* [#678](https://github.com/stripe/stripe-android/pull/678) Fix crash in `ShippingMethodView` on API 16

## 8.1.0 - 2018-11-13
* Add support for latest version of PaymentIntents
* Fix NPEs in `CountryAutocompleteTextView`
* Fix NPEs in `PaymentContext` experiences
* Helper method for converting tokens to sources
* Fix bug with Canadian and UK postal code validation

## 8.0.0 - 2018-7-13
* [#609](https://github.com/stripe/stripe-android/pull/609) **BREAKING** Renamed PaymentIntentParams methods for readibility

## 7.2.0 - 2018-07-12
* Add beta support for PaymentIntents for usage with card sources []
* Add sample integration with PaymentIntents
* Fix crash in MaskedCardAdapter
* [#589](https://github.com/stripe/stripe-android/pull/589) **BREAKING** Add `preferredLanguage` parameter to `SourceParams.createBancontactParams`

## 7.1.0 - 2018-06-11
* [#583](https://github.com/stripe/stripe-android/pull/583) Add EPS and Multibanco support to `SourceParams`
* [#586](https://github.com/stripe/stripe-android/pull/586) Add `RequiredBillingAddressFields.NAME` option to enumeration 
* [#583](https://github.com/stripe/stripe-android/pull/583) **BREAKING** Fix `@Nullable` and `@NonNull` annotations for `createP24Params` function 

## 7.0.1 - 2018-05-25
* Make iDEAL params match API - `name` is optional and optional ideal `bank` and `statement_descriptor` can be set independently

## 7.0.0 - 2018-04-25
* [#559](https://github.com/stripe/stripe-android/pull/559) Remove Bitcoin source support. See MIGRATING.md.
* [#549](https://github.com/stripe/stripe-android/pull/549) Add create Masterpass source support 
* [#548](https://github.com/stripe/stripe-android/pull/548) Add support for 3 digit American Express CVC
* [#547](https://github.com/stripe/stripe-android/pull/547) Fix crash when JCB icon is shown

## 6.1.2 - 2018-03-16
* Handle soft enter key in AddSourceActivity
* Add translations for ” ending in ” in each supported language.
* Add API bindings for removing a source from a customer.
* Update Android support library to 27.1.0.
* Fix invalid response from stripe api error
* Fix proguard minification error
* Add ability to create a source with extra parameters to support SEPA debit sources.
* Catch possible ClassCastException when formatting currency string

## 6.1.1 - 2018-01-30
* Fix Dutch Translation for MM/YY
* Set the CardNumberEditText TextWatcher to the correct EditText

## 6.1.0 - 2017-12-19
* Add binding to support custom Stripe Connect onboarding in Europe
* Expose interface to add text input listeners on the Card Widgets
* Updating Android support library version to 27.0.2
* Updating gradle 3.0.1
* Fix typo in docs

## 6.0.0 - 2017-11-10
* Updating gradle and wrapper to 4.3, using android build tools 3.0.0
* Fixed double-notification bug on error in AddSourceActivity
* Fixed compile error issue with Google support libraries 27.0.0

## 5.1.1 - 2017-10-24
* Adding P24 support
* Upgrades Gradle wrapper version to 4.2.1
* Surfaces error handling in API errors from Stripe
* Make resource namespaces private to prevent collisions
* Fixing a bug for PaymentFlowActivity not returning success when collecting shipping info without method

## 5.1.0 - 2017-09-22
* Adding the PaymentSession for end-to-end session handling
* Adding Alipay source handling
* Adding controls to enter customer shipping address and custom shipping methods

## 5.0.0 - 2017-08-25
* Adding the CustomerSession and EphemeralKeyProvider classes
* Adding CardMultilineWidget class for adding Card data in material design
* Adding AddSourceActivity and PaymentMethodsActivity for selecting customer payment
* Stability and efficiency improvements
* **BREAKING** Moving networking and utils classes to be package-private instead of public
* **BREAKING** Upgrading Gradle wrapper version
* **BREAKING** Upgrading Android support library versions
* **BREAKING** Upgrading build tools version

## 4.1.6 - 2017-07-31
* Fixing Android Pay string translation crash for decimal comma languages

## 4.1.5 - 2017-07-24
* Fixing a display bug for dates in certain locales

## 4.1.4 - 2017-07-17
* Adding ability to specify countries for shipping in the Android Pay MaskedWalletRequest
* Adding ability to specify card networks in the Android Pay MaskedWalletRequest

## 4.1.3 - 2017-07-13
* Adding Stripe-Account optional header for integration with Connect merchant accounts
* Adding AndroidPayConfiguration.setCountryCode optional method to specify transaction country

## 4.1.2 - 2017-06-27
* Fixing a missing method call in android pay
* Removing the android support V4 libraries

## 4.1.1 - 2017-06-15
* Fixing a preference default in android pay

## 4.1.0 - 2017-06-14
* Added a token field to SourceParams. You can use this to create a source from an existing token.
* https://stripe.com/docs/api#create_source-token

## 4.0.3 - 2017-06-05
* Added support for PII tokens
* Added ability to clear the card input widget
* Upgraded fraud detection tools

## 4.0.2 - 2017-05-15
* Added StripePaymentSource interface, extended by both Source and Token
* Upgraded for compatibility with stripe:stripe-android-pay
* Released stripe:stripe-android-pay library, dependent on stripe:stripe-android

## 4.0.1 - 2017-04-17
* Added setters for the card number, expiration date, and cvc fields in the CardInputWidget
* Added a new example project with back end integration
* Added the ability to set a listener on the CardInputWidget for various events
* Made the card brand icon show in the widget when the CVC entry is complete

## 4.0.0 - 2017-04-10
* Fixed issue #179, where certain pasted in credit cards would be incorrectly read as invalid
* **BREAKING** Removed the try/catch required around Stripe instance creation.

## 3.1.1 - 2017-04-04
* Fixed design tab display bug on card input widget
* Upgraded to Gradle version 3.4.1
* SEPA Debit address is now optional

## 3.1.0 - 2017-03-28
* Added support for creating and retrieving Source objects
* Added support for redirect flow and polling to update Source status
* Restyled the example project, added secondary activity to demonstrate 3DS support

## 3.0.1 - 2017-02-27
* Removed `@Deprecated` tag from most setters on the Card object

## 3.0.0 - 2017-02-25
* Added a card input widget to allow easy UI for collecting card data.
* **BREAKING** Increased the minimum SDK version to 11 (needed for animation classes).
* **BREAKING** Added required Context argument to Stripe constructor for resolving resources

## 2.1.0 - 2017-01-19
* Added bindings to allow creation of bank account tokens.

## 2.0.3 - 2017-01-09
* Updated OS version logging to be relevant for Android.

## 2.0.2 - 2016-12-22
* Made the StripeApiHandler.VERSION constant public.

## 2.0.1 - 2016-12-22
* Made the Token, Card, and Error parsing functions public.

## 2.0.0 - 2016-12-20
* Removed the dependency on stripe-java and gson
* Added a synchronous version of requestToken (still deprecated)
* Removed the (previously deprecated) requestToken methods

## 1.1.1 - 2016-12-09
* Refactored the bundled example project
* Reverted change in StripeTextUtils to exclude reference to framework classes

## 1.1.0 - 2016-12-01
* Exposed funding property on Card object
* Updated getType() to getBrand() to match Stripe API (getType() is still supported)
* Added synchronous method to create tokens which allows integration with RxJava
* Updated example application to include RxJava integration example
* Flattened example project structure for clarity

## 1.0.6 - 2016-11-15
* Updated to stripe-java 3.2.0
* Fixed American Express number validation problem
* Updated build target sdk version, build tools
* Moved tests out of example project and into stripe project

## 1.0.5 - 2016-08-26
* Modified Java bindings dependency
* Updated Card model to reflect recent changes
* Updated proguard and dependency versions

## 1.0.4 - 2016-01-29
* Remove incorrect Diner's Club card prefix
* Add Fabric properties file
