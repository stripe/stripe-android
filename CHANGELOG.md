# CHANGELOG

### 8.1.0 2018-11-13
* Add support for latest version of PaymentIntents
* Fix NPEs in `CountryAutocompleteTextView`
* Fix NPEs in `PaymentContext` experiences
* Helper method for converting tokens to sources
* Fix bug with Canadian and UK postal code validation

### 8.0.0 2018-7-13
* **BREAKING** Renamed PaymentIntentParams methods for readibility [#609](https://github.com/stripe/stripe-android/pull/609)

### 7.2.0 2018-07-12
* Add beta support for PaymentIntents for usage with card sources []
* Add sample integration with PaymentIntents
* Fix crash in MaskedCardAdapter
* **BREAKING** Add `preferredLanguage` parameter to `SourceParams.createBancontactParams` [#589](https://github.com/stripe/stripe-android/pull/589)

### 7.1.0 2018-06-11
* Add EPS and Multibanco support to `SourceParams` [#583](https://github.com/stripe/stripe-android/pull/583)
* Add `RequiredBillingAddressFields.NAME` option to enumeration [#586](https://github.com/stripe/stripe-android/pull/586)
* **BREAKING** Fix `@Nullable` and `@NonNull` annotations for `createP24Params` function [#583](https://github.com/stripe/stripe-android/pull/583/commits/d95d5969bbf4c01b61624ccb9626790c124f0647)

### 7.0.1 2018-05-25
* Make iDEAL params match API - `name` is optional and optional ideal `bank` and `statement_descriptor` can be set independently

### 7.0.0 2018-04-25
* Remove Bitcoin source support. See MIGRATING.md. [#559](https://github.com/stripe/stripe-android/pull/559)
* Add create Masterpass source support [#549](https://github.com/stripe/stripe-android/pull/549)
* Add support for 3 digit American Express CVC [#548](https://github.com/stripe/stripe-android/pull/548)
* Fix crash when JCB icon is shown [#547](https://github.com/stripe/stripe-android/pull/547)

### 6.1.2 2018-03-16
* Handle soft enter key in AddSourceActivity
* Add translations for ” ending in ” in each supported language.
* Add API bindings for removing a source from a customer.
* Update Android support library to 27.1.0.
* Fix invalid response from stripe api error
* Fix proguard minification error
* Add ability to create a source with extra parameters to support SEPA debit sources.
* Catch possible ClassCastException when formatting currency string

### 6.1.1 2018-01-30
* Fix Dutch Translation for MM/YY
* Set the CardNumberEditText TextWatcher to the correct EditText

### 6.1.0 2017-12-19
* Add binding to support custom Stripe Connect onboarding in Europe
* Expose interface to add text input listeners on the Card Widgets
* Updating Android support library version to 27.0.2
* Updating gradle 3.0.1
* Fix typo in docs

### 6.0.0 2017-11-10
* Updating gradle and wrapper to 4.3, using android build tools 3.0.0
* Fixed double-notification bug on error in AddSourceActivity
* Fixed compile error issue with Google support libraries 27.0.0

### 5.1.1 2017-10-24
* Adding P24 support
* Upgrades Gradle wrapper version to 4.2.1
* Surfaces error handling in API errors from Stripe
* Make resource namespaces private to prevent collisions
* Fixing a bug for PaymentFlowActivity not returning success when collecting shipping info without method

### 5.1.0 2017-09-22
* Adding the PaymentSession for end-to-end session handling
* Adding Alipay source handling
* Adding controls to enter customer shipping address and custom shipping methods

### 5.0.0 2017-08-25
* Adding the CustomerSession and EphemeralKeyProvider classes
* Adding CardMultilineWidget class for adding Card data in material design
* Adding AddSourceActivity and PaymentMethodsActivity for selecting customer payment
* Stability and efficiency improvements
* **BREAKING** Moving networking and utils classes to be package-private instead of public
* **BREAKING** Upgrading Gradle wrapper version
* **BREAKING** Upgrading Android support library versions
* **BREAKING** Upgrading build tools version

### 4.1.6 2017-07-31
* Fixing Android Pay string translation crash for decimal comma languages

### 4.1.5 2017-07-24
* Fixing a display bug for dates in certain locales

### 4.1.4 2017-07-17
* Adding ability to specify countries for shipping in the Android Pay MaskedWalletRequest
* Adding ability to specify card networks in the Android Pay MaskedWalletRequest

### 4.1.3 2017-07-13
* Adding Stripe-Account optional header for integration with Connect merchant accounts
* Adding AndroidPayConfiguration.setCountryCode optional method to specify transaction country

### 4.1.2 2017-06-27
* Fixing a missing method call in android pay
* Removing the android support V4 libraries

### 4.1.1 2017-06-15
* Fixing a preference default in android pay

### 4.1.0 2017-06-14
* Added a token field to SourceParams. You can use this to create a source from an existing token.
* https://stripe.com/docs/api#create_source-token

### 4.0.3 2017-06-05
* Added support for PII tokens
* Added ability to clear the card input widget
* Upgraded fraud detection tools

### 4.0.2 2017-05-15
* Added StripePaymentSource interface, extended by both Source and Token
* Upgraded for compatibility with stripe:stripe-android-pay
* Released stripe:stripe-android-pay library, dependent on stripe:stripe-android

### 4.0.1 2017-04-17
* Added setters for the card number, expiration date, and cvc fields in the CardInputWidget
* Added a new example project with back end integration
* Added the ability to set a listener on the CardInputWidget for various events
* Made the card brand icon show in the widget when the CVC entry is complete

### 4.0.0 2017-04-10
* Fixed issue #179, where certain pasted in credit cards would be incorrectly read as invalid
* **BREAKING** Removed the try/catch required around Stripe instance creation.

### 3.1.1 2017-04-04
* Fixed design tab display bug on card input widget
* Upgraded to Gradle version 3.4.1
* SEPA Debit address is now optional

### 3.1.0 2017-03-28
* Added support for creating and retrieving Source objects
* Added support for redirect flow and polling to update Source status
* Restyled the example project, added secondary activity to demonstrate 3DS support

### 3.0.1 2017-02-27
* Removed `@Deprecated` tag from most setters on the Card object

### 3.0.0 2017-02-25
* Added a card input widget to allow easy UI for collecting card data.
* **BREAKING** Increased the minimum SDK version to 11 (needed for animation classes).
* **BREAKING** Added required Context argument to Stripe constructor for resolving resources

### 2.1.0 2017-01-19
* Added bindings to allow creation of bank account tokens.

### 2.0.3 2017-01-09
* Updated OS version logging to be relevant for Android.

### 2.0.2 2016-12-22
* Made the StripeApiHandler.VERSION constant public.

### 2.0.1 2016-12-22
* Made the Token, Card, and Error parsing functions public.

### 2.0.0 2016-12-20
* Removed the dependency on stripe-java and gson
* Added a synchronous version of requestToken (still deprecated)
* Removed the (previously deprecated) requestToken methods

### 1.1.1 2016-12-09
* Refactored the bundled example project
* Reverted change in StripeTextUtils to exclude reference to framework classes

### 1.1.0 2016-12-01
* Exposed funding property on Card object
* Updated getType() to getBrand() to match Stripe API (getType() is still supported)
* Added synchronous method to create tokens which allows integration with RxJava
* Updated example application to include RxJava integration example
* Flattened example project structure for clarity

### 1.0.6 2016-11-15
* Updated to stripe-java 3.2.0
* Fixed American Express number validation problem
* Updated build target sdk version, build tools
* Moved tests out of example project and into stripe project

### 1.0.5 2016-08-26
* Modified Java bindings dependency
* Updated Card model to reflect recent changes
* Updated proguard and dependency versions

### 1.0.4 2016-01-29
* Remove incorrect Diner's Club card prefix
* Add Fabric properties file
