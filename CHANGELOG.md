# CHANGELOG

## 10.2.0 - 2019-08-05
* [#1275](https://github.com/stripe/stripe-android/pull/1275) Add support for launching `PaymentSession` from a `Fragment`
    * `PaymentSession(Fragment)`
* [#1288](https://github.com/stripe/stripe-android/pull/1288) Upgrade Android Gradle Plugin to 3.5.0-rc02
* [#1289](https://github.com/stripe/stripe-android/pull/1289) Add `Fragment` support to payment confirmation/authentication flow
    * `Stripe#confirmPayment(Fragment, ConfirmPaymentIntentParams)`
    * `Stripe#authenticatePayment(Fragment, String)`
    * `Stripe#confirmSetupIntent(Fragment, ConfirmSetupIntentParams)`
    * `Stripe#authenticateSetup(Fragment, String)`
* [#1290](https://github.com/stripe/stripe-android/pull/1290) Convert [samplestore app](https://github.com/stripe/stripe-android/tree/master/samplestore) to Kotlin
* [#1297](https://github.com/stripe/stripe-android/pull/1297) Convert [example app](https://github.com/stripe/stripe-android/tree/master/example) to Kotlin
* [#1300](https://github.com/stripe/stripe-android/pull/1300) Rename `StripeIntentResult#getStatus()` to `StripeIntentResult#getOutcome()`
* [#1302](https://github.com/stripe/stripe-android/pull/1302) Create `GooglePayConfig`
* [#1304](https://github.com/stripe/stripe-android/pull/1304) Create `PaymentMethodCreateParams#createFromGooglePay()`

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
        * [Stripe#confirmPayment()](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#confirmPayment-android.app.Activity-com.stripe.android.model.ConfirmPaymentIntentParams-) for automatic confirmation
        * [Stripe#authenticatePayment()](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#authenticatePayment-android.app.Activity-java.lang.String-) for manual confirmation
    * Setup Intents - see our guide for [Saving card details without a payment](https://stripe.com/docs/payments/cards/saving-cards#saving-card-without-payment)
        * [Stripe#confirmSetupIntent()](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#confirmSetupIntent-android.app.Activity-com.stripe.android.model.ConfirmSetupIntentParams-) for automatic confirmation
        * [Stripe#authenticateSetup()](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#authenticateSetup-android.app.Activity-java.lang.String-) for manual confirmation
    * [PaymentAuthConfig](https://stripe.dev/stripe-android/com/stripe/android/PaymentAuthConfig.html) for optional authentication customization
* Add support for the Setup Intents API. See [Saving card details without a payment](https://stripe.com/docs/payments/cards/saving-cards#saving-card-without-payment).
    * Use [ConfirmSetupIntentParams](https://stripe.dev/stripe-android/com/stripe/android/model/ConfirmSetupIntentParams.html) to confirm a SetupIntent
* [#1172](https://github.com/stripe/stripe-android/pull/1172) Refactor `PaymentIntentParams`
* [#1173](https://github.com/stripe/stripe-android/pull/1173) Inline all `StringDef` values

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
