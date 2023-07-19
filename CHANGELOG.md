# CHANGELOG

## XX.XX.XX - 2023-XX-XX

### Payments
*[CHANGED] The return type for several methods in `Stripe` has changed from `T?` (nullable) to `T` to better reflect possible behavior. These methods continue to be throwing and should be wrapped in a `try/catch` block.
*[FIXED][6977](https://github.com/stripe/stripe-android/pull/6977) Fixed an issue where `Stripe.retrievePossibleBrands()` returned incorrect results. 

## 20.27.2 - 2023-07-18

### PaymentSheet
* [FIXED] Fixed various bugs in Link private beta.

## 20.27.1 - 2023-07-17

### PaymentSheet
* [FIXED][6992](https://github.com/stripe/stripe-android/pull/6992) Fixed an issue where incorrect padding was set on the Google Pay button.

## 20.27.0 - 2023-07-10

### PaymentSheet
* [ADDED][6857](https://github.com/stripe/stripe-android/pull/6857) You can now collect payment details before creating a PaymentIntent or SetupIntent. See [our docs](https://stripe.com/docs/payments/accept-a-payment-deferred?platform=android) for more info. This integration also allows you to [confirm the Intent on the server](https://stripe.com/docs/payments/finalize-payments-on-the-server?platform=android).

## 20.26.0 - 2023-07-05

### PaymentSheet
* [ADDED][6583](https://github.com/stripe/stripe-android/pull/6583) Added top-level methods `rememberPaymentSheet()` and `rememberPaymentSheetFlowController()` for easier integration in Compose.
* [DEPRECATED][6583](https://github.com/stripe/stripe-android/pull/6583) `PaymentSheetContract` has been deprecated and will be removed in a future release. Use the `PaymentSheet` constructor or new `rememberPaymentSheet()` method instead.

### Payments
* [ADDED][6912](https://github.com/stripe/stripe-android/pull/6912) `GooglePayPaymentMethodLauncher` can now be presented with an amount of type `Long`. The method to present with an `Int` has been deprecated.
* [DEPRECATED][6912](https://github.com/stripe/stripe-android/pull/6912) `GooglePayLauncherContract` and `GooglePayPaymentMethodLauncherContract` have been deprecated and will be removed in a future release. Use `GooglePayLauncher` and `GooglePayPaymentMethodLauncher` directly instead.

## 20.25.8 - 2023-06-26

### Financial Connections
* [CHANGED][6919](https://github.com/stripe/stripe-android/pull/6919) Updated polling options for account retrieval and OAuth results to match other platforms.

## Payments
[ADDED][6925](https://github.com/stripe/stripe-android/pull/6925) Added top-level remember methods for `PaymentLauncher`, `GooglePayLauncher`, and `GooglePayPaymentMethodLauncher`.
[DEPRECATED][6925](https://github.com/stripe/stripe-android/pull/6925) Deprecated static `rememberLauncher()` methods for `PaymentLauncher`, `GooglePayLauncher`, and `GooglePayPaymentMethodLauncher`.

## 20.25.7 - 2023-06-20

### Financial Connections
* [FIXED][6900](https://github.com/stripe/stripe-android/pull/6900) Stop using getParcelableExtra from API 33 (see https://issuetracker.google.com/issues/240585930)

## 20.25.6 - 2023-06-12

### Financial Connections
* [FIXED][6836](https://github.com/stripe/stripe-android/pull/6836) Prevents double navigation when tapping too quickly.
* [FIXED][6853](https://github.com/stripe/stripe-android/pull/6853) Handle process kills after returning from browsers in Auth sessions.
* [FIXED][6837](https://github.com/stripe/stripe-android/pull/6837) Don't create duplicated Auth sessions after user closes web browser.
* [CHANGED][6850](https://github.com/stripe/stripe-android/pull/6850) Removes Toast shown after gracefully failing if no browser installed.

## 20.25.5 - 2023-06-05

### PaymentSheet
* [CHANGED] The experimental API for [finalizing payments on the server](https://stripe.com/docs/payments/finalize-payments-on-the-server?platform=android) has changed:
  * Instead of providing only the `PaymentMethod` ID, `CreateIntentCallback` now provides the entire `PaymentMethod` object.
  * `CreateIntentCallbackForServerSideConfirmation` has been removed. If you’re using server-side confirmation, use `CreateIntentCallback` and its new `shouldSavePaymentMethod` parameter.
  * `CreateIntentCallback`, `CreateIntentResult`, and `ExperimentalPaymentSheetDecouplingApi` have been moved to the `paymentsheet` module. Update your imports from `com.stripe.android.*` to `com.stripe.android.paymentsheet.*`.

### Financial Connections
* [FIXED][6794](https://github.com/stripe/stripe-android/pull/6794) Gracefully fails when no web browser available.
* [FIXED][6813](https://github.com/stripe/stripe-android/pull/6813) Added Mavericks related proguard rules to the consumer-rules file.

## 20.25.4 - 2023-05-30

### All SDKs
* [FIXED][6771](https://github.com/stripe/stripe-android/pull/6771) Fixed the length of phone number field.

### Financial Connections
* [CHANGED][6789](https://github.com/stripe/stripe-android/pull/6789) Updated Mavericks to 3.0.3.

## 20.25.3 - 2023-05-23

### PaymentSheet
* [CHANGED][6687](https://github.com/stripe/stripe-android/pull/6687) Show the US Bank Account payment method if the specified verification method is either automatic or instant. Otherwise, hide the payment method.
* [FIXED][6736](https://github.com/stripe/stripe-android/pull/6736) Fixed an issue where Google Places caused errors with R8.

## 20.25.2 - 2023-05-15

### PaymentSheet
* [FIXED][6680](https://github.com/stripe/stripe-android/pull/6680) Made payments with Cash App Pay more reliable.

### Payments
* [FIXED][6680](https://github.com/stripe/stripe-android/pull/6680) Made payments with Cash App Pay more reliable.

## 20.25.1 - 2023-05-10
* [CHANGED][6697](https://github.com/stripe/stripe-android/pull/6697) Revert BOM change and use compose 1.4.3. 
* [FIXED][6698](https://github.com/stripe/stripe-android/pull/6698) ImageDecoder: Exception in invokeOnCancellation handler.

## 20.25.0 - 2023-05-08

### All SDKs
* [CHANGED][6635](https://github.com/stripe/stripe-android/pull/6635) Use non transitive R classes.
* [CHANGED][6676](https://github.com/stripe/stripe-android/pull/6676) Updated Compose BOM to 2023.05.00.

### Identity
* [ADDED][6642](https://github.com/stripe/stripe-android/pull/6642) Support Test mode M1.
 
## 20.24.2 - 2023-05-03

### Payments
* [FIXED][6664](https://github.com/stripe/stripe-android/pull/6664) Fixed an issue where 3DS2 would crash when using payments SDKs with the card scan SDK.

## 20.24.1 - 2023-05-01

### Payments
* [FIXED][6612](https://github.com/stripe/stripe-android/pull/6612) Fixed an issue where the Android Gradle Plugin 8.0 and later would cause issues with R8 in full mode related to missing classes.

### All SDKs
* [CHANGED][6603](https://github.com/stripe/stripe-android/pull/6603) Added a `stripe` prefix to our resources to avoid name conflicts.
* [FIXED][6602](https://github.com/stripe/stripe-android/pull/6602) Fixed an issue which caused a compiler error (duplicate class) when including payments *and* identity SDKs.
* [FIXED][6611](https://github.com/stripe/stripe-android/pull/6611) Fixed an issue where countries might be filtered out on old Android versions (notably Kosovo).

## 20.24.0 - 2023-04-24

### PaymentSheet
* [CHANGED][6471](https://github.com/stripe/stripe-android/pull/6471) Updated Google Pay button to match new brand guidelines. You can now change the radius of the Google Pay and Link buttons with the [Appearance API](https://stripe.com/docs/elements/appearance-api?platform=android#shapes-android). Additionally, this change updates the `com.google.android.gms:play-services-wallet` version from `19.1.0` to `19.2.0-beta01`.

## 20.23.1 - 2023-04-17

### PaymentSheet
* [FIXED][6551](https://github.com/stripe/stripe-android/pull/6551) Fixed a build issue where `BillingDetailsCollectionConfiguration` couldn't be found in the classpath. If you worked around this issue by importing `payments-ui-core` directly, undo this change and update the import of `BillingDetailsCollectionConfiguration` to `com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration`.

## 20.23.0 - 2023-04-17

### PaymentSheet
* [ADDED] Added `billingDetailsCollectionConfiguration` to configure how you want to collect billing details. See the docs [here](https://stripe.com/docs/payments/accept-a-payment?platform=android&ui=payment-sheet#billing-details-collection).

### Identity
* [ADDED][6536](https://github.com/stripe/stripe-android/pull/6536) Added test mode for the SDK.

## 20.22.0 - 2023-04-10

### All SDKs
* [CHANGED][6492](https://github.com/stripe/stripe-android/pull/6492) Updated Compose to 1.4.1.

### PaymentSheet
* [FIXED][6434](https://github.com/stripe/stripe-android/pull/6434) Fixed an issue where the `Save this card for future payments` checkbox wasn't displayed in some cases even though it should have been.

### Financial Connections
* [CHANGED][6436](https://github.com/stripe/stripe-android/pull/6436) Updated Mavericks to 3.0.2.

## 20.21.1 - 2023-03-27

### PaymentSheet
* [FIXED][6411](https://github.com/stripe/stripe-android/pull/6411) Fixed an issue in the expiry date field that could cause an exception.

### Financial Connections
* [CHANGED][6403](https://github.com/stripe/stripe-android/pull/6403) Use light status bar and navigation bar.
* [CHANGED][6404](https://github.com/stripe/stripe-android/pull/6404) Update UI for no search results in institution picker.

### StripeCardScan
* [ADDED][6409](https://github.com/stripe/stripe-android/pull/6409) Support running Cardscan with TFLite in Google Play.

## 20.21.0 - 2023-03-20

### Payments
* [ADDED][6335](https://github.com/stripe/stripe-android/pull/6335) Added `Stripe.possibleCardBrands` which retrieves a list of possible card brands given a card number.
* [FIXED][6376](https://github.com/stripe/stripe-android/pull/6376) Fixed BLIK payment bindings.

### PaymentSheet
* [FIXED][6366](https://github.com/stripe/stripe-android/pull/6366) Fixed an issue where the result couldn't be parsed in `PaymentSheetContract`.
* [FIXED][6386](https://github.com/stripe/stripe-android/pull/6386) Fixed an issue where `FlowController.getPaymentOption()` and `PaymentOptionCallback` might return an outdated payment option in some cases.

### Financial Connections
* [FIXED][6375](https://github.com/stripe/stripe-android/pull/6375) Fixed Accessible data callout texts.

### Identity
* [ADDED][6380](https://github.com/stripe/stripe-android/pull/6380) Integrate with `ml-core` and allow user to swap TFLite runtime.

## 20.20.0 - 2023-03-13

### Payments
* [ADDED][6306](https://github.com/stripe/stripe-android/pull/6306) Added support for Cash App Pay. See the docs [here](https://stripe.com/docs/payments/cash-app-pay).

### PaymentSheet
* [ADDED][6306](https://github.com/stripe/stripe-android/pull/6306) Added support for Cash App Pay.
* [FIXED][6326](https://github.com/stripe/stripe-android/pull/6326) Fixed an issue where the primary button would lose its padding on configuration changes.
* [ADDED][5672](https://github.com/stripe/stripe-android/pull/5672) Added support for credit card autofill.

### Identity
* [FIXED][6341](https://github.com/stripe/stripe-android/pull/6341) Fixed an issue when remote image URI contains query parameters.

## 20.19.5 - 2023-03-06

### Payments
* [ADDED][6279](https://github.com/stripe/stripe-android/pull/6279) Update to Stripe 3DS2 6.1.7, removed keep-all proguard rules in favor of the minimal required ones.

### PaymentSheet
* [ADDED][6283](https://github.com/stripe/stripe-android/pull/6283) Added support for Zip payments.

### Identity
* [ADDED] ID/Address verification

## 20.19.4 - 2023-02-27

### StripeCardScan
* [FIXED][6253](https://github.com/stripe/stripe-android/pull/6253) Use the full screen card scanner to alleviate fragment crashes

### All SDKs
* [ADDED][6227](https://github.com/stripe/stripe-android/pull/6227) Removed keep-all proguard rules in favor of the minimal required ones. 

## 20.19.3 - 2023-02-13

### Financial Connections
* Stability and efficiency improvements.

## 20.19.2 - 2023-02-06

### PaymentSheet
* [ADDED][6174](https://github.com/stripe/stripe-android/pull/6174) Make PaymentLauncher.create methods Java-friendly.
* [FIXED][6172](https://github.com/stripe/stripe-android/pull/6172) Centers PaymentSheet on tablets.

## 20.19.1 - 2023-01-30

### PaymentSheet
* [FIXED][6136](https://github.com/stripe/stripe-android/pull/6136) Fixed an issue where the primary button wouldn't show the amount for payment intents.
* [FIXED][6142](https://github.com/stripe/stripe-android/pull/6142) Fixed an issue where pressing the back button during processing would cause a `PaymentSheetResult.Canceled`.

## 20.19.0 - 2023-01-23

### PaymentSheet
* [CHANGED][5927](https://github.com/stripe/stripe-android/pull/5927) Customers can now re-enter the autocomplete flow of the Address Element by tapping an icon in the line 1 text field.
* [FIXED][6090](https://github.com/stripe/stripe-android/pull/6090) Fixed an issue where adding a payment method to a Link account didn’t work when using `PaymentSheet.FlowController`.

## 20.18.0 - 2023-01-17

### Payments
* [ADDED][6012](https://github.com/stripe/stripe-android/pull/6012) Support for the predictive back gesture.

### PaymentSheet
* [DEPRECATED][5928](https://github.com/stripe/stripe-android/pull/5928) Deprecated `PaymentOption` public constructor, and `drawableResourceId` property.
* [ADDED][5928](https://github.com/stripe/stripe-android/pull/5928) Added `PaymentOption.icon()`, which returns a `Drawable`, and replaces `PaymentOption.drawableResourceId`.
* [ADDED][6012](https://github.com/stripe/stripe-android/pull/6012) Support for the predictive back gesture.

### Financial Connections
* [ADDED][6012](https://github.com/stripe/stripe-android/pull/6012) Support for the predictive back gesture.

### CardScan
* [ADDED][6012](https://github.com/stripe/stripe-android/pull/6012) Support for the predictive back gesture.

### Identity
* [CHANGED][5981](https://github.com/stripe/stripe-android/pull/5981) Fully migrate to Jetpack Compose.

## 20.17.0 - 2022-12-12

### Payments
* [CHANGED][5938](https://github.com/stripe/stripe-android/pull/5938) Methods on `Stripe` for retrieving and confirming intents now accept an optional `expand` argument to expand fields in the response.

### PaymentSheet
* [FIXED][5910](https://github.com/stripe/stripe-android/pull/5910) PaymentSheet now fails gracefully when launched with invalid arguments.

## 20.16.2 - 2022-12-05

### PaymentSheet
* [FIXED][5888](https://github.com/stripe/stripe-android/pull/5888) The primary button no longer stays disabled when returning from the `Add payment method` to the `Saved payment methods` screen.
* [CHANGED][5883](https://github.com/stripe/stripe-android/pull/5883) Fixed a few crashes when activities were launched on rooted devices.

### Financial Connections
* [CHANGED][5891](https://github.com/stripe/stripe-android/pull/5891) Fixed a few crashes when activities were launched on rooted devices.

## 20.16.1 - 2022-11-21

### PaymentSheet
* [CHANGED][5848](https://github.com/stripe/stripe-android/pull/5848) We now disable the back button while processing intents in `PaymentSheet` to prevent them from incorrectly being displayed as canceled.

### CardScan
* [SECURITY][5798](https://github.com/stripe/stripe-android/pull/5798) URL-encode IDs used in URLs to prevent injection attacks.

## 20.16.0 - 2022-11-14

### Payments

* [CHANGED][5789](https://github.com/stripe/stripe-android/pull/5789) We now disable the back button while confirming intents with `PaymentLauncher` to prevent them from incorrectly being displayed as failed.

### PaymentSheet

* [ADDED][5676](https://github.com/stripe/stripe-android/pull/5676) Added `AddressLauncher`, an [activity](https://stripe.com/docs/elements/address-element?platform=android) that collects local and international addresses for your customers.
* [ADDED][5769](https://github.com/stripe/stripe-android/pull/5769) Added `PaymentSheet.Configuration.allowsPaymentMethodsRequiringShippingAddress`. Previously, to allow payment methods that require a shipping address (e.g. Afterpay and Affirm) in `PaymentSheet`, you attached a shipping address to the PaymentIntent before initializing `PaymentSheet`. Now, you can instead set this property to `true` and set `PaymentSheet.Configuration.shippingDetails` or `PaymentSheet.FlowController.shippingDetails` whenever your customer’s shipping address becomes available. The shipping address will be attached to the PaymentIntent when the customer completes the checkout.

### Identity

* [FIXED][5816](https://github.com/stripe/stripe-android/pull/5816) Fixed an issue where the SDK would crash when recovering from process death.

## 20.15.4 - 2022-11-07

### CardScan

* [FIXED][5768](https://github.com/stripe/stripe-android/pull/5768) Fixed SDK version reporting in cardscan scan stats

### Identity

* [FIXED][5762](https://github.com/stripe/stripe-android/pull/5762) Use a custom implementation of FileProvider to avoid collision with client app.

## 20.15.3 - 2022-10-31

### PaymentSheet

* [ADDED][5729](https://github.com/stripe/stripe-android/pull/5729) Added support for a custom primary button label via `PaymentSheet.Configuration.primaryButtonLabel`.

### CardScan

* [FIXED][5749](https://github.com/stripe/stripe-android/pull/5749) Prevent multiple invocations to `/verify_frames`

## 20.15.2 - 2022-10-25

This release fixes a few bugs in `PaymentSession`, `PaymentSheet` and `CardScan`.

### Payments

* [FIXED][5722](https://github.com/stripe/stripe-android/pull/5722) Fix saving and restoring Google Pay selection in `PaymentSession`.

### PaymentSheet

* [FIXED][5738](https://github.com/stripe/stripe-android/pull/5738) Fix crash on Payment Sheet when integrating with Compose.

### CardScan

* [FIXED][5730](https://github.com/stripe/stripe-android/pull/5730) Fix crash during initialization.

## 20.15.1 - 2022-10-17

This release fixes some bugs in `ShippingInfoWidget`, `PaymentSheet`, and when the app is backgrounded during confirmation on Android 10 and 11.

### Payments

* [FIXED][5701](https://github.com/stripe/stripe-android/pull/5701) Treat blank fields as invalid in `ShippingInfoWidget`.
* [FIXED][5667](https://github.com/stripe/stripe-android/pull/5667) Completed payments are no longer incorrectly reported as having failed if the app is backgrounded during confirmation on Android 10 and 11.

### PaymentSheet

* [FIXED][5715](https://github.com/stripe/stripe-android/pull/5715) Postal codes for countries other than US and Canada are no longer limited to a single character.

## 20.15.0 - 2022-10-11

This release adds Link as a payment method to the SDK and fixes a minor issue with CardScan.

### PaymentSheet

* [ADDED][5692](https://github.com/stripe/stripe-android/pull/5692) Enable Link as a payment method.

### CardScan

* [FIXED][5679](https://github.com/stripe/stripe-android/pull/5679) Fix oversized verification_frames payloads leading to failed scans.

## 20.14.1 - 2022-10-03

This release expands the `payment_method` field on ACH requests and fixes a formatting error in `CardInputWidget`, `CardMultilineWidget`, and `CardFormView`.

### Payments

* [FIXED][5547](https://github.com/stripe/stripe-android/pull/5547) Expiry dates in `CardInputWidget`, `CardMultilineWidget`, and `CardFormView` are no longer formatted incorrectly on certain devices.

### PaymentSheet

* [FIXED][5624](https://github.com/stripe/stripe-android/pull/5624) `CollectBankAccountResult` included intents will now contain the expanded `payment_method` field.

## 20.14.0 - 2022-09-26
This release fixes a payment-method related error in `PaymentSheet` and manages missing permissions
on Financial Connections.

### PaymentSheet

* [FIXED][5592](https://github.com/stripe/stripe-android/pull/5592)[5613](https://github.com/stripe/stripe-android/pull/5613) Fix deletion of the last used payment method.

### Financial Connections

* [CHANGED][5583](https://github.com/stripe/stripe-android/pull/5583) Adds support for `account_numbers` permission.

## 20.13.0 - 2022-09-19
This release makes the `PaymentMethod.Card.networks` field public, fixes the Alipay integration and the card scan form encoding.

### Payments

* [CHANGED] [5552](https://github.com/stripe/stripe-android/pull/5552) Make `PaymentMethod.Card.networks` field public.
* [FIXED][5554](https://github.com/stripe/stripe-android/pull/5554) Fix Alipay integration when using the Alipay SDK.

### CardScan

* [FIXED] [5574](https://github.com/stripe/stripe-android/pull/5574) Fix encoding for form parameters for scan stats. 

## 20.12.0 - 2022-09-13
This release upgrades `compileSdkVersion` to 33, updates Google Pay button to match the new brand 
guidelines and fixes some bugs in `FlowController`.

* [CHANGED] [5495](https://github.com/stripe/stripe-android/pull/5495) Upgrade `compileSdkVersion`
  to 33.

### PaymentSheet

* [ADDED][5502](https://github.com/stripe/stripe-android/pull/5502) Added phone number minimum
  length validation.
* [ADDED][5518](https://github.com/stripe/stripe-android/pull/5518) Added state/province dropdown
  for US and Canada.
* [CHANGED][5487](https://github.com/stripe/stripe-android/pull/5487) Updated Google Pay button to
  match new brand guidelines.
* [FIXED][5480](https://github.com/stripe/stripe-android/pull/5480) `FlowController` now correctly
  preserves the previously selected payment method for guests.
* [FIXED][5545](https://github.com/stripe/stripe-android/pull/5545) Fix an issue where custom flow 
  PaymentSheet UI would have the bottom of the form cut off.

## 20.11.0 - 2022-08-29
This release adds postal code validation for PaymentSheet and fixed a fileprovider naming bug for Identity.

### PaymentSheet

* [ADDED][5456](https://github.com/stripe/stripe-android/pull/5456) Added postal code validation.

### Identity 

* [FIXED][5474](https://github.com/stripe/stripe-android/pull/5474) Update fileprovider name.

## 20.10.0 - 2022-08-22
This release contains several bug fixes for PaymentSheet and binary size optimization for Identity.

### PaymentSheet

* [FIXED][5422](https://github.com/stripe/stripe-android/pull/5422) Card expiration dates with a single-digit month are now preserved correctly when closing and re-opening the `PaymentSheet` via the `FlowController`.

### Identity
* [FIXED][5404](https://github.com/stripe/stripe-android/pull/5404) Remove Flex OP dependency from 
  Identity SDK and reduce its binary size.

## 20.9.0 - 2022-08-16
This release contains several bug fixes for Payments, PaymentSheet and Financial Connections. 
Adds `IdentityVerificationSheet#rememberIdentityVerificationSheet` for Identity. 

### PaymentSheet

* [ADDED][5340](https://github.com/stripe/stripe-android/pull/5340) Add a `resetCustomer` method to 
  `PaymentSheet`, that clears any persisted authentication state.
* [FIXED][5388](https://github.com/stripe/stripe-android/pull/5388) Fixed issue with Appearance API
  not working with `FlowController`.
* [FIXED][5399](https://github.com/stripe/stripe-android/pull/5399) Bank Account Payments that pass 
  stripeAccountId for connected accounts will now succeed.

### Payments

* [FIXED][5399](https://github.com/stripe/stripe-android/pull/5399) `CollectBankAccountLauncher` now 
  accepts `stripeAccountId` for Connect merchants.

### Financial Connections

* [FIXED][5408](https://github.com/stripe/stripe-android/pull/5408) `FinancialConnectionsSheet#Configuration`
  now accepts `stripeAccountId` for Connect merchants.

### Identity
* [ADDED][5370](https://github.com/stripe/stripe-android/pull/5370) Add factory method for Compose.

## 20.8.0 - 2022-08-01

This release contains several bug fixes for Payments, PaymentSheet, deprecates `createForCompose`, 
and adds new `rememberLauncher` features for Payments

### PaymentSheet

* [FIXED][5321](https://github.com/stripe/stripe-android/pull/5321) Fixed issue with forever loading
  and mochi library.
* [FIXED][5253](https://github.com/stripe/stripe-android/pull/5253) Setting setup_future_usage on
  the LPM level Payment Method Options remove the checkbox.

### Payments

* [FIXED][5308](https://github.com/stripe/stripe-android/pull/5308) OXXO so that processing is
  considered a successful terminal state, similar to Konbini and Boleto.
* [FIXED][5138](https://github.com/stripe/stripe-android/pull/5138) Fixed an issue where
  PaymentSheet will show a failure even when 3DS2 Payment/SetupIntent is successful.
* [ADDED][5274](https://github.com/stripe/stripe-android/pull/5274) Create `rememberLauncher` method
  enabling usage of `GooglePayLauncher`, `GooglePayPaymentMethodLauncher` and `PaymentLauncher` in
  Compose.
* [DEPRECATED][5274](https://github.com/stripe/stripe-android/pull/5274)
  Deprecate `PaymentLauncher.createForCompose` in favor of  `PaymentLauncher.rememberLauncher`.

## 20.7.0 - 2022-07-06

This release adds additional support for Afterpay/Clearpay in PaymentSheet.

### PaymentSheet

* [ADDED][5221](https://github.com/stripe/stripe-android/pull/5221) Afterpay/Clearpay support for
  FR, ES countries and EUR currencies
* [FIXED][5215](https://github.com/stripe/stripe-android/pull/5215) Fix issue with us_bank_account
  appearing in payment sheet when Financial Connections SDK is not available

### Payments

* [FIXED][5226](https://github.com/stripe/stripe-android/pull/5226)
  Persist `GooglePayLauncherViewModel` state across process death
* [ADDED][5238](https://github.com/stripe/stripe-android/pull/5238) Expose the current card brand in
  CardFormView, CardInputWidget, and CardMultiLineWidget

### Identity

* [ADDED][5149](https://github.com/stripe/stripe-android/pull/5149) First release to fully support
  selfie capture

## 20.6.2 - 2022-06-23

This release contains several bug fixes for Payments, reduces the size of StripeCardScan, and adds
new `rememberFinancialConnections` features for Financial Connections.

* [CHANGED] [5162](https://github.com/stripe/stripe-android/pull/5162) Upgrade `compileSdkVersion`
  to 32, Kotlin version to 1.6.21, Android Gradle plugin to 7.2.1.

### Financial Connections

* [ADDED][5117](https://github.com/stripe/stripe-android/pull/5117) Adds
  rememberFinancialConnectionsSheet and rememberFinancialConnectionsSheetForToken.

### Payments

* [FIXED][5195](https://github.com/stripe/stripe-android/pull/5195) Fix focus when navigating across
  compose fields
* [FIXED][5196](https://github.com/stripe/stripe-android/pull/5196) Fix
  PaymentOptionsAddPaymentMethodFragmentTest
* [FIXED][5183](https://github.com/stripe/stripe-android/pull/5183) Fix Link payment option card in
  dark mode
* [FIXED][5148](https://github.com/stripe/stripe-android/pull/5148) Restore selected payment method
  when user returns to Link
* [FIXED][5142](https://github.com/stripe/stripe-android/pull/5142) Fix issue with animations
  running on main thread

### CardScan

* [CHANGED][5144](https://github.com/stripe/stripe-android/pull/5144) Add a minimal TFLite module to
  stripecardscan.

## 20.5.0 - 2022-06-01

This release contains several bug fixes for Payments and PaymentSheet, deprecates the
PaymentSheet's `primaryButtonColor` api in favor of the
new [appearance api](https://stripe.com/docs/elements/appearance-api?platform=android), and adds
card brand icons to the card details form.

### PaymentSheet

* [DEPRECATED][5061](https://github.com/stripe/stripe-android/pull/5061) Add Deprecated annotation
  to old primaryButtonColor api.
* [FIXED][5068](https://github.com/stripe/stripe-android/pull/5068) Fix missing theming for add lpm
  button and notes text.
* [ADDED][5069](https://github.com/stripe/stripe-android/pull/5069) Add card brand icons to card
  details form.

### Payments

* [FIXED][5079](https://github.com/stripe/stripe-android/pull/5079) Add 3ds2 url to list of
  completion URLs so callbacks work correctly.
* [FIXED][5094](https://github.com/stripe/stripe-android/pull/5094) Use correct cvc icon in card
  form view.

### CardScan

* [FIXED] [5075](https://github.com/stripe/stripe-android/pull/5075) Prevent a crash when the
  fragment is detached.

## 20.4.0 - 2022-05-23

This release
adds [appearance customization APIs](https://github.com/stripe/stripe-android/blob/master/paymentsheet/src/main/java/com/stripe/android/paymentsheet/PaymentSheet.kt#L186)
to payment sheet and enables Affirm and AU BECS direct debit as payment methods within Payment
Sheet.

### Payments

* [CHANGED][5038](https://github.com/stripe/stripe-android/pull/5038) Remove force portrait mode in
  Google Pay.
* [ADDED][5011](https://github.com/stripe/stripe-android/pull/5011) Add `allowCreditCards`
  to `GooglePayLauncher`

### PaymentSheet

* [FIXED][5039](https://github.com/stripe/stripe-android/pull/5039) Fixed the format of the country
  dropdown in PaymentSheet for all languages.
* [ADDED][5042](https://github.com/stripe/stripe-android/pull/5042) Added Affirm and AU BECS Direct
  Debit.
* [ADDED][5020](https://github.com/stripe/stripe-android/pull/5020) Merge Appearance APIs to master.
* [FIXED][5022](https://github.com/stripe/stripe-android/pull/5022) Add missing translation for card
  information.
* [FIXED][5048](https://github.com/stripe/stripe-android/pull/5048) Fixed a crash when removing the
  last payment method in the custom flow editor.

## 20.3.0 - 2022-05-16

This release adds `us_bank_account` PaymentMethod to PaymentSheet.

### PaymentSheet

* [FIXED][5011](https://github.com/stripe/stripe-android/pull/5011) fix flying payment sheet by
  downgrading material from 1.6 to 1.5
* [ADDED][4964](https://github.com/stripe/stripe-android/pull/4964) `us_bank_account` PaymentMethod
  is now available in PaymentSheet

### camera-core

* [FIXED][5004](https://github.com/stripe/stripe-android/pull/5004) Fix front camera callback to
  return an upside down image

## 20.2.2 - 2022-05-09

This release contains bug fixes in PaymentSheet.

### PaymentSheet

* [FIXED][4966](https://github.com/stripe/stripe-android/pull/4966) Replaced alpha
  androidx.lifecycle dependencies with stable versions
* [FIXED][4961](https://github.com/stripe/stripe-android/pull/4961) Fix issue entering text with
  small forms.

## 20.2.1 - 2022-05-03

This release contains bug fixes in PaymentSheet and Payments.

### Payments

* [CHANGED] [4910](https://github.com/stripe/stripe-android/pull/4910) Some changes affecting
  CollectBankAccountLauncher (ACH)
    * `CollectBankAccountResponse#LinkAccountSession` to `FinancialConnectionsSession`
    * `LinkedAccount` to `FinancialConnectionsAccount`.

### PaymentSheet

* [FIXED] [4918](https://github.com/stripe/stripe-android/pull/4918) Fix a problem introduced in
  20.0.0 where save for future use was defaulted to true.
* [FIXED] [4921](https://github.com/stripe/stripe-android/pull/4921) Fixed a crash that could happen
  when switching between LPMs.

## 20.2.0 - 2022-04-25

This release adds card scanning to PaymentSheet.

### PaymentSheet

* [ADDED] [4804](https://github.com/stripe/stripe-android/pull/4804) Card-scanning in PaymentSheet
* [FIXED] [4861](https://github.com/stripe/stripe-android/pull/4861) Remove font resource to save
  space and default to system default
* [FIXED] [4909](https://github.com/stripe/stripe-android/pull/4909) In the multi-step flow when
  re-opening to a new card the form will pre-populate. Also the default billing address will
  pre-populate in the form.

### Financial Connections

* [CHANGED] [4887](https://github.com/stripe/stripe-android/pull/4887) Renamed Connections to
  Financial Connections.

## 20.1.0 - 2022-04-18

This release includes several Payments and PaymentSheet bug fixes.

### Payments (`com.stripe:stripe-android`)

* [ADDED] [4874](https://github.com/stripe/stripe-android/pull/4874) `us_bank_account` PaymentMethod
  is now available for ACH Direct Debit payments, including APIs to collect customer bank
  information (requires Connections SDK) and verify microdeposits.
* [FIXED] [4875](https://github.com/stripe/stripe-android/pull/4875) fix postal code callback not
  firing when enabled

### PaymentSheet

* [FIXED] [4861](https://github.com/stripe/stripe-android/pull/4861) Remove font resource to save
  space and default to system default
* [CHANGED] [4855](https://github.com/stripe/stripe-android/pull/4855) Remove force portrait mode in
  PaymentLauncher.

## 20.0.1 - 2022-04-11

This release includes several PaymentSheet bug fixes.

### PaymentSheet

* [FIXED] [4840](https://github.com/stripe/stripe-android/pull/4840) Multi-step now shows the last 4
  of the card number instead of 'card'.
* [FIXED] [4847](https://github.com/stripe/stripe-android/pull/4847) Fix the width of the
  PaymentSheet payment method selector.
* [FIXED] [4851](https://github.com/stripe/stripe-android/pull/4851) Add support for http logoUri.

## 20.0.0 - 2022-04-04

This release patches on a crash on PaymentLauncher, updates the package name of some public classes,
changes the public API for CardImageVerificationSheet and releases Identity SDK.

### Payments (`com.stripe:stripe-android`)

* [ADDED] [4804](https://github.com/stripe/stripe-android/pull/4804) Added card-scanning feature to
  PaymentSheet
* [FIXED] [4776](https://github.com/stripe/stripe-android/pull/4776) fix issue with PaymentLauncher
  configuration change
* [CHANGED] [4358](https://github.com/stripe/stripe-android/pull/4358) Updated the card element on
  PaymentSheet to use Compose.

### Identity (`com.stripe:identity`)

* [ADDED] [4820](https://github.com/stripe/stripe-android/pull/4820) Release Stripe's Identity SDK.

### Financial Connections (`com.stripe:financial-connections`)

* [ADDED] [4818](https://github.com/stripe/stripe-mandroid/pull/4818) Connections SDK can be
  optionally included to support ACH Direct Debit payments.

### CardScan (`com.stripe:stripecardscan`)

* [CHANGED] [4778](https://github.com/stripe/stripe-android/pull/4778) CardImageVerificationSheet:
  removed the callback from present to create, wrapping it inside a
  CardImageVerificationResultCallback object.

### Core (`com.stripe:stripe-core`)

* [CHANGED] [4800](https://github.com/stripe/stripe-android/pull/4800) Relocated network exceptions
  to :stripe-core.
* [CHANGED] [4803](https://github.com/stripe/stripe-android/pull/4803) Remove network related
  internal files.
* [CHANGED] [4803](https://github.com/stripe/stripe-android/pull/4803) The following classes'
  packages are changed
    * `com.stripe.android.AppInfo` -> `com.stripe.android.core.AppInfo`
    * `com.stripe.android.model.StripeFile` -> `com.stripe.android.core.model.StripeFile`
    * `com.stripe.android.model.StripeFileParams`
      -> `com.stripe.android.core.model.StripeFileParams`
    * `com.stripe.android.model.StripeFilePurpose`
      -> `com.stripe.android.core.model.StripeFilePurpose`

## 19.3.1 - 2022-03-22

This release patches an issue with 3ds2 confirmation

### Payments

* [FIXED] [4747](https://github.com/stripe/stripe-android/pull/4747) update 3ds2 to v6.1.5, see PR
  for specific issues addressed.

## 19.3.0 - 2022-03-16

This release enables a new configuration object to be defined for StripeCardScan and updates our
3ds2 SDK.

### PaymentSheet

* [FIXED] [4646](https://github.com/stripe/stripe-android/pull/4646) Update 3ds2 to latest version
  6.1.4, see PR for specific issues addressed.
* [FIXED] [4669](https://github.com/stripe/stripe-android/pull/4669) Restrict the list of SEPA debit
  supported countries.

### CardScan

* [ADDED] [4689](https://github.com/stripe/stripe-android/pull/4689)
  The `CardImageVerificationSheet` initializer can now take an additional `Configuration` object.

## 19.2.2 - 2022-03-01

* [FIXED] [4606](https://github.com/stripe/stripe-android/pull/4606) Keep status bar color in
  PaymentLauncher

### Card scanning

* [ADDED] [4592](https://github.com/stripe/stripe-android/pull/4592) Add support for launching card
  scan from fragments.

## 19.2.0 - 2022-02-14

This release includes several bug fixes and upgrades Kotlin to 1.6.

* [CHANGED] [4546](https://github.com/stripe/stripe-android/pull/4546) Update to kotlin 1.6
* [FIXED] [4560](https://github.com/stripe/stripe-android/pull/4560) Fix `cardValidCallback` being
  added multiple times in `CardInputWidget`.
* [FIXED] [4574](https://github.com/stripe/stripe-android/pull/4574) Take `postalCode` into account
  in `CardMultilineWidget` validation.
* [FIXED] [4579](https://github.com/stripe/stripe-android/pull/4579) Fix crash when no bank is
  selected in `AddPaymentMethodActivity`.

### PaymentSheet

* [FIXED] [4466](https://github.com/stripe/stripe-android/pull/4466) Fix issues when activities are
  lost on low resource phones.
* [FIXED] [4557](https://github.com/stripe/stripe-android/pull/4557) Add missing app info to some
  Stripe API requests

### Card scanning

* [FIXED] [4548](https://github.com/stripe/stripe-android/pull/4548) Potential work leak when
  canceling a card scan in StripeCardScan
* [ADDED] [4562](https://github.com/stripe/stripe-android/pull/4562) Add an example page for
  cardscan
* [FIXED] [4575](https://github.com/stripe/stripe-android/pull/4575) Fix card add display bug

## 19.1.1 - 2022-01-31

### PaymentSheet

* [CHANGED] [4515](https://github.com/stripe/stripe-android/pull/4515) Disable card saving by
  default in PaymentSheet
* [FIXED] [4504](https://github.com/stripe/stripe-android/pull/4504) Fix CardValidCallback not
  firing on postal code changes
* [CHANGED] [4512](https://github.com/stripe/stripe-android/pull/4512) Relay error message on
  PaymentResult.Failed

## 19.1.0 - 2022-01-05

This release enables new payment methods in the Mobile Payment Element: Eps, Giropay, P24, Klarna,
PayPal, AfterpayClearpay. For a full list of the supported payment methods, refer
to [our documentation](https://stripe.com/docs/payments/payment-methods/integration-options#payment-method-product-support)
.

* [4492](https://github.com/stripe/stripe-android/pull/4492) Enable Afterpay in Payment Sheet
* [4489](https://github.com/stripe/stripe-android/pull/4489) Enable Eps, Giropay, p24, klarna and
  paypal in payment sheet
* [4481](https://github.com/stripe/stripe-android/pull/4481) Add minimal user key auth support to
  PaymentSheet

Dependencies updated:

* [4484](https://github.com/stripe/stripe-android/pull/4484) Bump kotlinCoroutinesVersion from 1.5.2
  to 1.6.0
* [4485](https://github.com/stripe/stripe-android/pull/4485) Bump kotlinx-serialization-json from
  1.3.1 to 1.3.2
* [4478](https://github.com/stripe/stripe-android/pull/4478) Bump navigation-compose from
  2.4.0-beta02 to 2.4.0-rc01
* [4475](https://github.com/stripe/stripe-android/pull/4475) Update gradle version from 7.1.1 to
  7.3.2
* [4464](https://github.com/stripe/stripe-android/pull/4464) Bump accompanist-flowlayout from 0.20.2
  to 0.20.3
* [4472](https://github.com/stripe/stripe-android/pull/4472) Bump
  org.jetbrains.kotlin.plugin.serialization from 1.6.0 to 1.6.10

## 19.0.0 - 2021-12-13

This release includes several bug fixes and has the first release
of [Stripe CardScan SDK](https://github.com/stripe/stripe-android/tree/master/stripecardscan)

* [4426](https://github.com/stripe/stripe-android/pull/4426) don't override returnUrl for instant
  app
* [4424](https://github.com/stripe/stripe-android/pull/4424) callback for postal code complete
* [4438](https://github.com/stripe/stripe-android/pull/4438) allow non-terminal state for
  PaymentSheet
* [4432](https://github.com/stripe/stripe-android/pull/4432) Span PMs across the PaymentSheet when
  there are only two of them
* [4414](https://github.com/stripe/stripe-android/pull/4414) Add support for new languages: fil, hr,
  in, ms-rMY, th, vi.
* [4408](https://github.com/stripe/stripe-android/pull/4408) revert static height on cmw text boxes
* [4396](https://github.com/stripe/stripe-android/pull/4396) Fix snackbar NPE
* [4385](https://github.com/stripe/stripe-android/pull/4385) Remove filter on postal codes when
  switching away from US
* [4354](https://github.com/stripe/stripe-android/pull/4354) Convert toast to snackbar in examples
* [4383](https://github.com/stripe/stripe-android/pull/4383) Convert entered country code to display
  name if needed
* [4384](https://github.com/stripe/stripe-android/pull/4384) CardFormView will auto convert the
  country name to country code when typed

Dependencies updated:

* [4463](https://github.com/stripe/stripe-android/pull/4463) Bump play-services-wallet from 18.1.3
  to 19.0.0
* [4456](https://github.com/stripe/stripe-android/pull/4456) Bump gradle from 7.0.3 to 7.0.4
* [4447](https://github.com/stripe/stripe-android/pull/4447) Bump daggerVersion from 2.40.4 to
  2.40.5
* [4441](https://github.com/stripe/stripe-android/pull/4441) Bump json from 20210307 to 20211205
* [4434](https://github.com/stripe/stripe-android/pull/4434) Bump daggerVersion from 2.40.3 to
  2.40.4
* [4433](https://github.com/stripe/stripe-android/pull/4433) Bump ktlint from 0.43.1 to 0.43.2
* [4428](https://github.com/stripe/stripe-android/pull/4428) Bump robolectric from 4.7.2 to 4.7.3
* [4425](https://github.com/stripe/stripe-android/pull/4425) Bump ktlint from 0.43.0 to 0.43.1
* [4422](https://github.com/stripe/stripe-android/pull/4422) Bump daggerVersion from 2.40.2 to
  2.40.3
* [4407](https://github.com/stripe/stripe-android/pull/4407) Bump dokka-gradle-plugin from 1.5.31 to
  1.6.0
* [4406](https://github.com/stripe/stripe-android/pull/4406) Bump daggerVersion from 2.40.1 to
  2.40.2
* [4395](https://github.com/stripe/stripe-android/pull/4395) Bump robolectric from 4.7.1 to 4.7.2
* [4394](https://github.com/stripe/stripe-android/pull/4394) Bump logging-interceptor from 4.9.2 to
  4.9.3
* [4393](https://github.com/stripe/stripe-android/pull/4393) Bump mockitoCoreVersion from 4.0.0 to
  4.1.0
* [4388](https://github.com/stripe/stripe-android/pull/4388) Bump robolectric from 4.7 to 4.7.1
* [4384](https://github.com/stripe/stripe-android/pull/4384) Bump activity-compose from 1.3.1 to
  1.4.0
* [4382](https://github.com/stripe/stripe-android/pull/4382) Bump
  org.jetbrains.kotlin.plugin.serialization from 1.5.31 to 1.6.0
* [4379](https://github.com/stripe/stripe-android/pull/4379) Bump daggerVersion from 2.40 to 2.40.1
* [4378](https://github.com/stripe/stripe-android/pull/4378) Bump kotlinSerializationVersion from
  1.3.0 to 1.3.1
* [4377](https://github.com/stripe/stripe-android/pull/4377) Bump robolectric from 4.6.1 to 4.7
* [4373](https://github.com/stripe/stripe-android/pull/4373) Bump tensorflow-lite from 2.6.0 to
  2.7.0
* [4365](https://github.com/stripe/stripe-android/pull/4365) Bump binary-compatibility-validator
  from 0.7.1 to 0.8.0
* [4363](https://github.com/stripe/stripe-android/pull/4363) Bump accompanist-flowlayout from 0.20.1
  to 0.20.2
* [4356](https://github.com/stripe/stripe-android/pull/4356) Bump composeVersion from 1.0.4 to 1.0.5
* [4353](https://github.com/stripe/stripe-android/pull/4353) Bump accompanist-flowlayout from 0.20.0
  to 0.20.1
* [4352](https://github.com/stripe/stripe-android/pull/4352) Bump ktlint from 0.42.1 to 0.43.0
* [4347](https://github.com/stripe/stripe-android/pull/4347) Bump gson from 2.8.8 to 2.8.9

## 18.2.0 - 2021-10-29

This release includes several bug fixes, introduces Klarna as a payment method binding, and
renables [WeChat Pay](https://github.com/stripe/stripe-android/tree/master/wechatpay) within the SDK

* [4323](https://github.com/stripe/stripe-android/pull/4323) reship wechat module
* [4325](https://github.com/stripe/stripe-android/pull/4325) Add klarna to sdk w/ example
* [4339](https://github.com/stripe/stripe-android/pull/4339) Bump tensorflow-lite from 2.4.0 to
  2.6.0
* [4340](https://github.com/stripe/stripe-android/pull/4340) Bump okio from 2.10.0 to 3.0.0
* [4334](https://github.com/stripe/stripe-android/pull/4334) Bank value is allowed to be null in the
  case of "other"
* [4330](https://github.com/stripe/stripe-android/pull/4330) Bump lifecycle-viewmodel-compose from
  2.4.0-rc01 to 2.4.0
* [4329](https://github.com/stripe/stripe-android/pull/4329) Bump daggerVersion from 2.39.1 to 2.40
* [4309](https://github.com/stripe/stripe-android/pull/4309) Card number, CVC, postal, and
  expiration date should only show digits in keypad
* [4198](https://github.com/stripe/stripe-android/pull/4198) Bump lifecycle-viewmodel-compose from
  1.0.0-alpha07 to 2.4.0-rc01
* [4296](https://github.com/stripe/stripe-android/pull/4296) When processing Result for a PI,
  refresh until reaches deterministic state
* [4290](https://github.com/stripe/stripe-android/pull/4290) Bump composeVersion from 1.0.2 to 1.0.4

## 18.1.0 - 2021-10-18

### PaymentSheet

This release adds several new features to `PaymentSheet`, our drop-in UI integration:

#### More supported payment methods

The list of supported payment methods depends on your integration. If you’re using a `PaymentIntent`
, we support:

- Card
- SEPA Debit, bancontact, iDEAL, sofort

If you’re using a `PaymentIntent` with `setup_future_usage` or a `SetupIntent`, we support:

- Card
- GooglePay

Note: To enable SEPA Debit and sofort, set `PaymentSheet.Configuration.allowsDelayedPaymentMethods`
to `true` on the client. These payment methods can't guarantee you will receive funds from your
customer at the end of the checkout because they take time to settle. Don't enable these if your
business requires immediate payment (e.g., an on-demand service).
See https://stripe.com/payments/payment-methods-guide

#### Pre-fill billing details

`PaymentSheet` collects billing details like name and email for certain payment methods. Pre-fill
these fields to save customers time by setting `PaymentSheet.Configuration.defaultBillingDetails`.

#### Save payment methods on payment

> This is currently only available for cards + Apple/Google Pay.

`PaymentSheet` supports `PaymentIntents` with `setup_future_usage` set. This property tells us to
save the payment method for future use (e.g., taking initial payment of a recurring subscription).
When set, PaymentSheet hides the 'Save this card for future use' checkbox and always saves.

#### SetupIntent support

> This is currently only available for cards + Apple/Google Pay.

Initialize `PaymentSheet` with a `SetupIntent` to set up cards for future use without charging.

#### Smart payment method ordering

When a customer is adding a new payment method, `PaymentSheet` uses information like the customer's
region to show the most relevant payment methods first.

### Other changes

* [4165](https://github.com/stripe/stripe-android/pull/4165) Postal code collection for cards is now
  limited to US, CA, UK
* [4274](https://github.com/stripe/stripe-android/pull/4274) Bump mockitoCoreVersion from 3.12.4 to
  4.0.0
* [4279](https://github.com/stripe/stripe-android/pull/4279) Fix dependency incorrectly marked as
  implementation
* [4281](https://github.com/stripe/stripe-android/pull/4281) Add analytics event for failure
  creating 3ds2 params
* [4282](https://github.com/stripe/stripe-android/pull/4282) Bump gradle from 7.0.2 to 7.0.3
* [4283](https://github.com/stripe/stripe-android/pull/4283) Bump mockito-kotlin from 3.2.0 to 4.0.0
* [4291](https://github.com/stripe/stripe-android/pull/4291) Fix so empty string parameters are sent
  to the server
* [4295](https://github.com/stripe/stripe-android/pull/4295) Fix height on CardMultilineWidget
  textboxes
* [4297](https://github.com/stripe/stripe-android/pull/4297) Bump accompanist-flowlayout from 0.19.0
  to 0.20.0

## 18.0.0 - 2021-10-07

This release includes several bug fixes, introduces a test mode indicator, makes a builder class
for [payment sheet configuration](https://github.com/stripe/stripe-android/blob/master/paymentsheet/src/main/java/com/stripe/android/paymentsheet/PaymentSheet.kt#L130)
and makes config properties immutable.

* [4202](https://github.com/stripe/stripe-android/pull/4202) CardInputWidget.CardValidCallback will
  consider the postal code in validation
* [4183](https://github.com/stripe/stripe-android/pull/4183) Fix address pre-populate on line 1
* [4192](https://github.com/stripe/stripe-android/pull/4192) Remove internal on CardUtils
* [4214](https://github.com/stripe/stripe-android/pull/4214) Create test mode indicator
* [4265](https://github.com/stripe/stripe-android/pull/4265) Make config properties immutable,
  create Builder

## 17.2.0 - 2021-09-10

This release includes several bug fixes,
introduces [PaymentLauncher](https://github.com/stripe/stripe-android/blob/master/payments-core/src/main/java/com/stripe/android/payments/paymentlauncher/PaymentLauncher.kt)
as a replacement
of [https://github.com/stripe/stripe-android/blob/master/payments-core/src/main/java/com/stripe/android/Stripe.kt](https://github.com/stripe/stripe-android/blob/master/payments-core/src/main/java/com/stripe/android/Stripe.kt)
see example
in [StripeIntentActivity](https://github.com/stripe/stripe-android/blob/master/example/src/main/java/com/stripe/example/activity/StripeIntentActivity.kt)
on [line 28](https://github.com/stripe/stripe-android/blob/master/example/src/main/java/com/stripe/example/activity/StripeIntentActivity.kt#L28)

* [4157](https://github.com/stripe/stripe-android/pull/4157) Pass GooglePayLauncher arguments to
  DefaultGooglePayRepository
* [4165](https://github.com/stripe/stripe-android/pull/4165) Require credit postal for only US, CA,
  and GB
* [4173](https://github.com/stripe/stripe-android/pull/4173) Re-expose
  CardUtils.getPossibleCardBrand
* [4183](https://github.com/stripe/stripe-android/pull/4183) Fix address pre-populate on line 1
* [4159](https://github.com/stripe/stripe-android/pull/4159) Migrate PaymentController to
  PaymentLauncher within PaymentSheet components.
* [4175](https://github.com/stripe/stripe-android/pull/4175) Migrate Custom Payment Sheet Example to
  PaymentLauncher

## 17.1.2 - 2021-08-25

This release includes several bug fixes, adds the capability to remove cards saved in `PaymentSheet`
and to set default billing address fields in the `PaymentSheet.Configuration`. It is now also
possible to use Setup Intents with Google Pay on the multi-step Payment Sheet UI.

* [4107](https://github.com/stripe/stripe-android/pull/4107) Dokka updates for 17.1.1
* [4108](https://github.com/stripe/stripe-android/pull/4108) Support deleting saved payment methods
* [4113](https://github.com/stripe/stripe-android/pull/4113) Adds support for Google Pay with Setup
  Intents on Payment Sheet multi-step UI
* [4115](https://github.com/stripe/stripe-android/pull/4115) Fix infinite loading when StripeIntent
  is already confirmed
* [4117](https://github.com/stripe/stripe-android/pull/4117) Fix crash on `GooglePayLauncher` when
  confirmation fails
* [4118](https://github.com/stripe/stripe-android/pull/4118) Bump binary-compatibility-validator
  from 0.6.0 to 0.7.0
* [4119](https://github.com/stripe/stripe-android/pull/4119) Bump gradle from 7.0.0 to 7.0.1
* [4122](https://github.com/stripe/stripe-android/pull/4122) Remove global length and CA postal
  restrictions
* [4124](https://github.com/stripe/stripe-android/pull/4124) Add default billing detail
  configuration to PaymentSheet
* [4127](https://github.com/stripe/stripe-android/pull/4127) Bump gson from 2.8.7 to 2.8.8
* [4128](https://github.com/stripe/stripe-android/pull/4128) Bump mockitoCoreVersion from 3.11.2 to
  3.12.1

## 17.1.1 - 2021-08-13

This release includes several bug fixes and temporarily
disabled [WeChat Pay module](https://github.com/stripe/stripe-android/blob/master/wechatpay/README.md)
due to a backend bug, For WeChat Pay integration now, please use `Stripe.confirmWeChatPayPayment`.

* [4057](https://github.com/stripe/stripe-android/pull/4057) Bump daggerVersion from 2.38 to 2.38.1
* [4065](https://github.com/stripe/stripe-android/pull/4065) Bump activity-compose from 1.3.0-rc02
  to 1.3.0
* [4066](https://github.com/stripe/stripe-android/pull/4066) Bump gradle from 4.2.2 to 7.0.0
* [4069](https://github.com/stripe/stripe-android/pull/4069) `GooglePayPaymentMethodLauncher`
  takes `transactionId` and returns error code
* [4086](https://github.com/stripe/stripe-android/pull/4086) Bump activity-compose from 1.3.0 to
  1.3.1
* [4088](https://github.com/stripe/stripe-android/pull/4088) Upgrade kotlinVersion to 1.5.21,
  composeVersion to 1.0.1
* [4092](https://github.com/stripe/stripe-android/pull/4092) Bump ktlint from 0.41.0 to 0.42.1
* [4098](https://github.com/stripe/stripe-android/pull/4098) Disable WeChat Pay module

## 17.1.0 - 2021-07-27

This release includes several bug fixes and
introduces [WeChat Pay module](https://github.com/stripe/stripe-android/blob/master/wechatpay/README.md)

* [3978](https://github.com/stripe/stripe-android/pull/3978) Fix bug when cancelling Payment Sheet
  payment with Google Pay
* [4014](https://github.com/stripe/stripe-android/pull/4014) Added support for WeChatPay
* [4026](https://github.com/stripe/stripe-android/pull/4026) Bump daggerVersion from 2.37 to 2.38
* [4034](https://github.com/stripe/stripe-android/pull/4034) Fix PaymentSheetActivity getting stuck
  in loading state during recreation
* [4035](https://github.com/stripe/stripe-android/pull/4035) Remove dependency from PaymentSheet on
  RxJava
* [4046](https://github.com/stripe/stripe-android/pull/4046) Fix 3DS2 redirect on Firefox
* [4049](https://github.com/stripe/stripe-android/pull/4026) Fix bug where 3DS2 completion did not
  close the PaymentSheet
* [4051](https://github.com/stripe/stripe-android/pull/4051) Fix Setup Intent confirmation when
  using GooglePayLauncher

## 17.0.0 - 2021-07-15

This release includes several breaking changes. See
the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more
details.

### What's New

This release introduces `GooglePayLauncher` and `GooglePayPaymentMethodLauncher` to simplify Google
Pay integration. See the [Google Pay integration guide](https://stripe.com/docs/google-pay)
for more details.

### Notable Changes

* [#3820](https://github.com/stripe/stripe-android/pull/3820) Upgrade Kotlin to `1.5.10`
* [#3883](https://github.com/stripe/stripe-android/pull/3883) Remove `PaymentIntent#nextAction`
* [#3899](https://github.com/stripe/stripe-android/pull/3899) Introduce `GooglePayLauncher`
  and `GooglePayPaymentMethodLauncher`
    * Drop-in classes that simplify integrating Google Pay
* [#3918](https://github.com/stripe/stripe-android/pull/3918) Upgrade Android Gradle plugin
  to `4.2.2`
* [#3942](https://github.com/stripe/stripe-android/pull/3942) Upgrade Gradle to `7.1.1`
* [#3944](https://github.com/stripe/stripe-android/pull/3944) Introduce `GooglePayLauncherContract`
  and `GooglePayPaymentMethodLauncherContract`
    * `ActivityResultContract` classes that enable integrating `GooglePayLauncher`
      and `GooglePayPaymentMethodLauncher` in Jetpack Compose
* [#3951](https://github.com/stripe/stripe-android/pull/3951) Upgrade material-components to `1.4.0`
* [#3976](https://github.com/stripe/stripe-android/pull/3976) Upgrade Kotlin Coroutines to `1.5.1`
* [#4010](https://github.com/stripe/stripe-android/pull/4010) Upgrade 3DS2 SDK to `6.1.1`
    * Migrate challenge flow to use Activity Result API
    * Add support for Cartes Bancaires and UnionPay
    * Upgrade `nimbus-jose-jwt` to `9.11.1`

## 16.10.2 - 2021-07-02

* [#3811](https://github.com/stripe/stripe-android/pull/3811) Fix `CountryCode` parceling
* [#3833](https://github.com/stripe/stripe-android/pull/3833) Fix coroutine usage in
  Stripe3DS2Authenticator
* [#3863](https://github.com/stripe/stripe-android/pull/3863)
  Make `PayWithGoogleUtils#getPriceString()` Locale-agnostic
* [#3892](https://github.com/stripe/stripe-android/pull/3892) Add PaymentSheet support for Jetpack
  Compose
* [#3905](https://github.com/stripe/stripe-android/pull/3905) Fix `StripeEditText` crash on
  instantiation

## 16.10.0 - 2021-05-28

* [#3752](https://github.com/stripe/stripe-android/pull/3752) Support connected accounts when using
  Google Pay in PaymentSheet
* [#3761](https://github.com/stripe/stripe-android/pull/3761) Publish `CardFormView`
* [#3762](https://github.com/stripe/stripe-android/pull/3762) Add SetupIntent support in
  PaymentSheet
* [#3769](https://github.com/stripe/stripe-android/pull/3769) Upgrade Google Pay SDK to `18.1.3`

## 16.9.0 - 2021-05-21

* [#3727](https://github.com/stripe/stripe-android/pull/3727) Upgrade Gradle to `7.0.2`
* [#3734](https://github.com/stripe/stripe-android/pull/3734) Upgrade `androidx.appcompat:appcompat`
  to `1.3.0`
* [#3735](https://github.com/stripe/stripe-android/pull/3735) Upgrade `fragment-ktx` to `1.3.4`
* [#3737](https://github.com/stripe/stripe-android/pull/3737) Add `Stripe.createRadarSession()` API
  binding for `/v1/radar/session`
* [#3739](https://github.com/stripe/stripe-android/pull/3739) Downgrade Kotlin from `1.5.0`
  to `1.4.32`

## 16.8.2 - 2021-05-14

* [#3709](https://github.com/stripe/stripe-android/pull/3709) Upgrade
  org.jetbrains.kotlin.plugin.serialization to `1.5.0`
* [#3710](https://github.com/stripe/stripe-android/pull/3710) Upgrade kotlinx-serialization-json
  to `1.2.0`
* [#3711](https://github.com/stripe/stripe-android/pull/3711) Upgrade Gradle to `7.0.1`
* [#3712](https://github.com/stripe/stripe-android/pull/3712) Move PaymentSheet example into its own
  app
* [#3717](https://github.com/stripe/stripe-android/pull/3717) Upgrade mockito-core to `3.10.0`
* [#3721](https://github.com/stripe/stripe-android/pull/3721) Fix crash on Android 8 and 9 when
  opening the PaymentSheet
* [#3722](https://github.com/stripe/stripe-android/pull/3722) Upgrade Android Gradle Plugin
  to `4.2.1`

## 16.8.0 - 2021-05-07

This release adds a prebuilt UI. It combines all the steps required to pay - collecting payment
details and confirming the payment - into a single sheet that displays on top of your app. See
the [guide](https://stripe.com/docs/payments/accept-a-payment?platform=android) for more details.

* [#3663](https://github.com/stripe/stripe-android/pull/3663) Add support for using Chrome to host a
  3DS1 authentication page when Custom Tabs are not available
* [#3677](https://github.com/stripe/stripe-android/pull/3677) Upgrade Android Gradle Plugin
  to `4.2.0`
* [#3680](https://github.com/stripe/stripe-android/pull/3680) Deprecate `returnUrl` in
  some `ConfirmPaymentIntentParams` create() methods
    * A custom `return_url` is not needed to return control to the app after an authentication
      attempt
* [#3681](https://github.com/stripe/stripe-android/pull/3681) Reset PaymentIntent and SetupIntent
  status after 3DS1 cancellation in Custom Tabs
    * When a customer closed a 3DS1 authentication page hosted in Custom Tabs, the Intent's `status`
      was not reset from `requires_action` to `requires_payment_method`. This is now fixed.
* [#3685](https://github.com/stripe/stripe-android/pull/3685) Upgrade Kotlin to `1.5.0`
* [#3687](https://github.com/stripe/stripe-android/pull/3687) Add support for PaymentSheet prebuilt
  UI.
* [#3696](https://github.com/stripe/stripe-android/pull/3696) Upgrade `activity-ktx` to `1.2.3`

## 16.7.1 - 2021-04-29

* [#3653](https://github.com/stripe/stripe-android/pull/3653) Support WeChat Pay for creating
  a `PaymentMethod` and confirming a `PaymentIntent`
    * WeChat Pay is still in beta. To enable support in API bindings, pass
      the `StripeApiBeta.WeChatPayV1` as an argument when instantiating a `Stripe` instance.
* [#3567](https://github.com/stripe/stripe-android/pull/3567) Use `lifecycleScope` where possible
  in `Stripe.kt`
    * When calling payment and setup confirmation methods (e.g. `confirmPayment()`), using
      a `ComponentActivity` subclass (e.g. `AppCompatActivity`) will make the call lifecycle-aware.
* [#3635](https://github.com/stripe/stripe-android/pull/3635) Deprecate `extraParams`
  in `ConfirmPaymentIntentParams`
    * Use `setupFutureUsage` instead.
      ```kotlin
      // before
      ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
          params,
          clientSecret,
          extraParams = mapOf("setup_future_usage" to "off_session")
      )
      
      // after
      ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
          params,
          clientSecret,
          setupFutureUsage = SetupFutureUsage.OffSession
      )
      ```
* [#3640](https://github.com/stripe/stripe-android/pull/3640) Add support for beta headers
  using `StripeApiBeta`
    * The following example includes the `wechat_pay_beta=v1` flag in API requests:
      ```kotlin
      Stripe(
          context,
          publishableKey,
          betas = setOf(StripeApiBeta.WechatPayV1)
      )
      ```
* [#3644](https://github.com/stripe/stripe-android/pull/3644) Use Custom Tabs for 3DS1 payment
  authentication when available
    * When a `ConfirmPaymentIntentParams` or `ConfirmSetupIntentParams` instance is created
      **without** a custom `return_url` value and used to confirm a `PaymentIntent` or
      `SetupIntent` **and** the device supports
      [Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/overview/), use Custom
      Tabs instead of a WebView to render the authentication page.
    *
    See [Stripe#confirmPayment()](https://github.com/stripe/stripe-android/blob/8bf5b738878362c6b9e4ac79edc7c515d9ba63ef/stripe/src/main/java/com/stripe/android/Stripe.kt#L145)
    for more details.
* [#3646](https://github.com/stripe/stripe-android/pull/3646) Upgrade 3DS2 SDK to `5.3.1`
    * Gracefully handle unknown directory server ids
* [#3656](https://github.com/stripe/stripe-android/pull/3656) Deprecate `return_url`
  in `ConfirmSetupIntentParams`
    * Setting a custom `return_url` prevents authenticating 3DS1 with Custom Tabs. Instead, a
      WebView fallback will be used.

## 16.6.1 - 2021-04-26

* [#3568](https://github.com/stripe/stripe-android/pull/3568) Add suspending function variants for
  payment confirmation methods
* [#3587](https://github.com/stripe/stripe-android/pull/3587) Upgrade Kotlin to `1.4.32`
* [#3606](https://github.com/stripe/stripe-android/pull/3606) Upgrade Gradle to `7.0`
* [#3626](https://github.com/stripe/stripe-android/pull/3626) Upgrade Fragment to `1.3.3`
* [#3632](https://github.com/stripe/stripe-android/pull/3632) Upgrade 3DS2 SDK to `5.3.0`
    * Upgrade `nimbus-jose-jwt` to `9.8.1`
    * Gracefully handle unknown directory servers

## 16.5.0 - 2021-04-08

* [#3557](https://github.com/stripe/stripe-android/pull/3557) Add suspending function variants of
  API methods
* [#3559](https://github.com/stripe/stripe-android/pull/3559) Fix OXXO confirmation flow
* [#3575](https://github.com/stripe/stripe-android/pull/3575) Upgrade `recyclerview` to `1.2.0`

## 16.4.3 - 2021-04-02

* [#3555](https://github.com/stripe/stripe-android/pull/3555) Fix 3DS2 challenge completion endpoint
  request

## 16.4.2 - 2021-04-01

* [#3548](https://github.com/stripe/stripe-android/pull/3548) Refine `PaymentAuthWebViewClient`
  completion URL logic
* [#3549](https://github.com/stripe/stripe-android/pull/3549) Add SDK user agent
  to `PaymentAuthWebView`'s user agent
* [#3551](https://github.com/stripe/stripe-android/pull/3551) Add extra headers
  to `PaymentAuthWebViewActivity`'s loadUrl request

## 16.4.1 - 2021-04-01

* [#3537](https://github.com/stripe/stripe-android/pull/3537) Add account id support
  in `IssuingCardPinService`
* [#3538](https://github.com/stripe/stripe-android/pull/3538) Add support for retrying rate-limited
  API requests
* [#3543](https://github.com/stripe/stripe-android/pull/3543) Add better support for
  auto-dismissing `PaymentAuthWebViewActivity` when `return_url` is not provided

## 16.4.0 - 2021-03-29

* [#3457](https://github.com/stripe/stripe-android/pull/3457) Fix issue where `StripeEditText` was
  overriding default text color changes
* [#3476](https://github.com/stripe/stripe-android/pull/3476) Fix `PaymentMethodsAdapter` "new card"
  click handling
* [#3479](https://github.com/stripe/stripe-android/pull/3479) Add support for Blik payment method
* [#3482](https://github.com/stripe/stripe-android/pull/3482) Mark incomplete fields
  in `CardMultilineWidget` as invalid
* [#3484](https://github.com/stripe/stripe-android/pull/3484) Add new
  method `CardInputWidget#setCvcLabel`
* [#3493](https://github.com/stripe/stripe-android/pull/3493) Correctly return error results
  from `PaymentAuthWebViewActivity`
* [#3504](https://github.com/stripe/stripe-android/pull/3504) Add default mandate data for all
  applicable payment method types
* [#3507](https://github.com/stripe/stripe-android/pull/3507) Invoke issuing API requests on
  background thread
* [#3508](https://github.com/stripe/stripe-android/pull/3508) Make `EphemeralKeyProvider` a fun
  interface
* [#3517](https://github.com/stripe/stripe-android/pull/3517) Add retry logic for 3DS2 challenge
  completion endpoint
* [#3519](https://github.com/stripe/stripe-android/pull/3519) Update AndroidX dependencies
    * `androidx.activity:activity-ktx` to `1.2.2`
    * `androidx.annotation:annotation` to `1.2.0`
    * `androidx.fragment:fragment-ktx` to `1.3.2`
    * `androidx.lifecycle:lifecycle-*` to `2.3.1`
* [#3520](https://github.com/stripe/stripe-android/pull/3520)
  Invoke `CardInputWidget#cardInputListener` when postal code field gets focus
* [#3524](https://github.com/stripe/stripe-android/pull/3524) Update `layoutDirection` for card
  widget fields
    * Card number, expiration date, and CVC are always LTR
    * Postal code is defined by the locale

## 16.3.1 - 2021-03-16

* [#3381](https://github.com/stripe/stripe-android/pull/3381) Add `fingerprint` property
  to `PaymentMethod.Card`
* [#3401](https://github.com/stripe/stripe-android/pull/3401) Upgrade Gradle to `6.8.3`
* [#3429](https://github.com/stripe/stripe-android/pull/3429) Fix `ExpiryDateEditText` error
  messages
* [#3436](https://github.com/stripe/stripe-android/pull/3436) Upgrade `kotlin-coroutines` to `1.4.3`
* [#3438](https://github.com/stripe/stripe-android/pull/3438) Upgrade Kotlin to `1.4.31`
* [#3443](https://github.com/stripe/stripe-android/pull/3443) Create new transparent theme for
  invisible activities
* [#3456](https://github.com/stripe/stripe-android/pull/3456) Fix tint flicker in `CardBrandView`
* [#3460](https://github.com/stripe/stripe-android/pull/3460) Upgrade `activity-ktx` to `1.2.1`

## 16.3.0 - 2021-02-11

* [#3334](https://github.com/stripe/stripe-android/pull/3334) Upgrade Kotlin to `1.4.30`
* [#3346](https://github.com/stripe/stripe-android/pull/3346) Upgrade Gradle to `6.8.2`
* [#3349](https://github.com/stripe/stripe-android/pull/3349) Upgrade `material-components`
  to `1.3.0`
* [#3359](https://github.com/stripe/stripe-android/pull/3359) Add `brand` and `last4` properties
  to `CardParams`
* [#3367](https://github.com/stripe/stripe-android/pull/3367) Upgrade `fragment-ktx` to `1.3.0`
  and `activity-ktx` to `1.2.0`
* [#3368](https://github.com/stripe/stripe-android/pull/3368) Upgrade `androidx.lifecycle`
  dependencies to `2.3.0`
* [#3372](https://github.com/stripe/stripe-android/pull/3372) Upgrade 3DS2 SDK to `5.2.0`
    * Upgrade `nimbus-jose-jwt` to `9.5`
    * Upgrade Kotlin to `1.4.30`
    * Upgrade `material-components` to `1.3.0`
    * Upgrade `activity-ktx` to `1.2.0` and `fragment-ktx` to `1.3.0`
    * Upgrade `androidx.lifecycle` to `2.3.0`
    * Migrate `ProgressBar` to `CircularProgressIndicator`

## 16.2.1 - 2021-01-29

* [#3275](https://github.com/stripe/stripe-android/pull/3257) Fix spinner positioning
  in `CardMultilineWidget`
* [#3275](https://github.com/stripe/stripe-android/pull/3275) Upgrade Android Gradle Plugin
  to `4.1.2`
* [#3291](https://github.com/stripe/stripe-android/pull/3291) Upgrade Gradle to `6.8.1`
* [#3300](https://github.com/stripe/stripe-android/pull/3300) Upgrade AndroidX fragment dependency
  to `1.3.0-rc2`
* [#3302](https://github.com/stripe/stripe-android/pull/3302) Add support for
  creating `afterpay_clearpay` payment methods
* [#3315](https://github.com/stripe/stripe-android/pull/3315) Upgrade 3DS2 SDK to `5.1.1`
    * Upgrade `nimbus-jose-jwt` to `9.4.2`

## 16.2.0 - 2021-01-11

* [#3088](https://github.com/stripe/stripe-android/pull/3088) Mark some builders
  in `PaymentMethodCreateParams` as deprecated
* [#3134](https://github.com/stripe/stripe-android/pull/3134) Upgrade Kotlin to `1.4.21`
* [#3154](https://github.com/stripe/stripe-android/pull/3154) Fix `CvcEditText` layout issues
  in `CardMultilineWidget`
* [#3176](https://github.com/stripe/stripe-android/pull/3176) Update `GooglePayConfig` constructor
* [#3205](https://github.com/stripe/stripe-android/pull/3205)
  Add `androidx.activity:activity-ktx:1.2.0-rc01` as a dependency
* [#3232](https://github.com/stripe/stripe-android/pull/3232) Align card number field icon to end
  in `CardMultilineWidget`
* [#3237](https://github.com/stripe/stripe-android/pull/3237) Upgrade 3DS2 SDK to `5.0.1`
    * Sources are now included with the 3DS2 SDK
    * Upgrade `bcprov-jdk15to18` to `1.6.8`
    * Upgrade `nimbus-jose-jwt` to `9.4`
* [#3241](https://github.com/stripe/stripe-android/pull/3241) Upgrade Gradle to `6.8`

## 16.1.1 - 2020-11-25

* [#3028](https://github.com/stripe/stripe-android/pull/3028) Upgrade Android Gradle Plugin
  to `4.1.1`
* [#3035](https://github.com/stripe/stripe-android/pull/3035) Update handling of deeplinks
  in `PaymentAuthWebViewClient`
* [#3046](https://github.com/stripe/stripe-android/pull/3046) Upgrade Gradle to `6.7.1`
* [#3056](https://github.com/stripe/stripe-android/pull/3056) Upgrade Kotlin to `1.4.20`
* [#3058](https://github.com/stripe/stripe-android/pull/3058) Migrate to Kotlin Parcelize plugin
* [#3072](https://github.com/stripe/stripe-android/pull/3072) Fix crash in card widgets
* [#3083](https://github.com/stripe/stripe-android/pull/3083) Upgrade `stripe-3ds2-android`
  to `4.1.2`
    * Fix crash

## 16.1.0 - 2020-11-06

* [#2930](https://github.com/stripe/stripe-android/pull/2930) Upgrade Android Gradle Plugin
  to `4.1.0`
* [#2936](https://github.com/stripe/stripe-android/pull/2936) Upgrade Gradle to `6.7`
* [#2955](https://github.com/stripe/stripe-android/pull/2955) Add support for UPI payment method
* [#2965](https://github.com/stripe/stripe-android/pull/2965) Add support for Netbanking payment
  method
* [#2976](https://github.com/stripe/stripe-android/pull/2976) Update `ExpiryDateEditText` input
  allowlist
* [#2977](https://github.com/stripe/stripe-android/pull/2977) Fix crash
  in `CardNumberTextInputLayout`
* [#2981](https://github.com/stripe/stripe-android/pull/2981) Fix `PaymentMethodCreateParams`
  annotations on create methods
* [#2988](https://github.com/stripe/stripe-android/pull/2988)
  Update `PaymentSession.handlePaymentData()` to take a nullable `Intent`
* [#2989](https://github.com/stripe/stripe-android/pull/2989) Handle null `client_secret` in
  result `Intent`
* [#2995](https://github.com/stripe/stripe-android/pull/2995) Upgrade constraintlayout to `2.0.4`
* [#3006](https://github.com/stripe/stripe-android/pull/3006) Upgrade coroutines to `1.4.1`
* [#3010](https://github.com/stripe/stripe-android/pull/3010) Upgrade `stripe-3ds2-android`
  to `4.1.1`
    * Upgrade `bcprov-jdk15to18` to `1.6.7`
    * Upgrade `nimbus-jose-jwt` to `9.1.2`

## 16.0.1 - 2020-10-06

* [#2894](https://github.com/stripe/stripe-android/pull/2894) Make `CardParams` constructor public
* [#2895](https://github.com/stripe/stripe-android/pull/2895) Add support for configuring a footer
  layout in payment methods screen
* [#2897](https://github.com/stripe/stripe-android/pull/2897) Only allow digits in `CvcEditText`
* [#2900](https://github.com/stripe/stripe-android/pull/2900) Only allow digits in BECS BSB and
  account number fields
* [#2913](https://github.com/stripe/stripe-android/pull/2913) Add support for Oxxo PaymentMethod

## 16.0.0 - 2020-09-23

This release includes several breaking changes. See
the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more
details.

This release adds support for 19-digit cards in `CardInputWidget` and `CardMultilineWidget`.

* [#2715](https://github.com/stripe/stripe-android/pull/2715) Add support for GrabPay PaymentMethod
* [#2721](https://github.com/stripe/stripe-android/pull/2721) Upgrade Kotlin coroutines to `1.3.9`
* [#2735](https://github.com/stripe/stripe-android/pull/2735) Upgrade Android Gradle Plugin
  to `4.0.1`
* [#2766](https://github.com/stripe/stripe-android/pull/2766) Upgrade Gradle to `6.6.1`
* [#2821](https://github.com/stripe/stripe-android/pull/2821) Support pasting a 19 digit PAN
  in `CardNumberEditText`
* [#2836](https://github.com/stripe/stripe-android/pull/2836) Handle `CustomerSession` failure
  in `PaymentMethodsActivity`
* [#2837](https://github.com/stripe/stripe-android/pull/2837) Upgrade Kotlin to `1.4.10`
* [#2841](https://github.com/stripe/stripe-android/pull/2841) Add new string translations
    * Adds support for several new languages
* [#2847](https://github.com/stripe/stripe-android/pull/2847) Update `CardInputWidget` text size
  for `ldpi` screens
* [#2854](https://github.com/stripe/stripe-android/pull/2854)
  Upgrade `com.google.android.material:material` to `1.2.1`
* [#2867](https://github.com/stripe/stripe-android/pull/2867) Upgrade 3DS2 SDK to `4.1.0`
    * Upgrade `material-components` to `1.2.1`
    * Upgrade `com.nimbusds:nimbus-jose-jwt` to `9.0.1`
    * Guard against crash when `TransactionTimer` is unavailable
* [#2873](https://github.com/stripe/stripe-android/pull/2873) Fix `CardInputWidget` field rendering
  in RTL
* [#2878](https://github.com/stripe/stripe-android/pull/2878) Remove `Stripe.createToken()`
* [#2880](https://github.com/stripe/stripe-android/pull/2880) Fix date formatting
  in `KlarnaSourceParams`
* [#2887](https://github.com/stripe/stripe-android/pull/2887) Re-render shipping methods screen when
  shipping methods change

## 15.1.0 - 2020-08-13

* [#2671](https://github.com/stripe/stripe-android/pull/2671) Add `cardParams` property
  to `CardInputWidget` and `CardMultilineWidget`
* [#2675](https://github.com/stripe/stripe-android/pull/2675) Add `CardParams` methods to `Stripe`
* [#2677](https://github.com/stripe/stripe-android/pull/2677) Deprecate `Card.create()`
* [#2679](https://github.com/stripe/stripe-android/pull/2679) Add missing `TokenizationMethod`
  values
    * `TokenizationMethod.Masterpass` and `TokenizationMethod.VisaCheckout`
* [#2692](https://github.com/stripe/stripe-android/pull/2692) Add support for Alipay PaymentMethod
    * See `Stripe#confirmAlipayPayment()`
* [#2693](https://github.com/stripe/stripe-android/pull/2693) Upgrade `androidx.appcompat:appcompat`
  to `1.2.0`
* [#2696](https://github.com/stripe/stripe-android/pull/2696) Upgrade to Gradle `6.6`
* [#2704](https://github.com/stripe/stripe-android/pull/2704) Deprecate metadata field on retrieved
  API objects
    * See `MIGRATING.md` for more details
* [#2708](https://github.com/stripe/stripe-android/pull/2708) Bump 3DS2 SDK to `4.0.5`
    * Fix crash related to SDK app id
    * Upgrade `com.nimbusds:nimbus-jose-jwt` to `8.20`

## 15.0.2 - 2020-08-03

* [#2666](https://github.com/stripe/stripe-android/pull/2666) Bump 3DS2 SDK to `4.0.4`
* [#2671](https://github.com/stripe/stripe-android/pull/2671) Add `cardParams` property
  to `CardInputWidget` and `CardMultilineWidget`
* [#2674](https://github.com/stripe/stripe-android/pull/2674) Add `SourceParams` creation method
  for `CardParams`
* [#2675](https://github.com/stripe/stripe-android/pull/2675) Add `CardParams` methods to `Stripe`
  class
* [#2679](https://github.com/stripe/stripe-android/pull/2679) Add missing `TokenizationMethod`
  values
    * Add `Masterpass` and `VisaCheckout`
* [#2681](https://github.com/stripe/stripe-android/pull/2681) Mark code using `Card` for card object
  creation as `@Deprecated`

## 15.0.1 - 2020-07-28

* [#2641](https://github.com/stripe/stripe-android/pull/2641) Add support for Bank Account as source
  on `Customer` object
* [#2643](https://github.com/stripe/stripe-android/pull/2643) Add missing fields to `Customer` model
* [#2644](https://github.com/stripe/stripe-android/pull/2644) Support new directory server network
  names
    * Enable support for Discover and other new networks
* [#2646](https://github.com/stripe/stripe-android/pull/2646) Allow `CardMultilineWidget`'
  s `TextInputLayout`s to be styled
* [#2649](https://github.com/stripe/stripe-android/pull/2649) Add `@JvmOverloads`
  to `GooglePayJsonFactory` methods
* [#2651](https://github.com/stripe/stripe-android/pull/2651) Update Kotlin coroutines to `1.3.8`
* [#2657](https://github.com/stripe/stripe-android/pull/2657) Fix HTML select option rendering
  in `WebView`

## 15.0.0 - 2020-07-09

This release includes several breaking changes. See
the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more
details.

* [#2542](https://github.com/stripe/stripe-android/pull/2542) Use `CustomerSession.stripeAccountId`
  in `AddPaymentMethodActivity`
* [#2543](https://github.com/stripe/stripe-android/pull/2543) Target JVM 1.8
* [#2544](https://github.com/stripe/stripe-android/pull/2544) Remove deprecated code related
  to `BankAccount` and `ActivityStarter`
* [#2545](https://github.com/stripe/stripe-android/pull/2545) Remove
  deprecated `AccountParams.create()` method
* [#2546](https://github.com/stripe/stripe-android/pull/2546) Remove deprecated `next_action` data
  objects from `PaymentIntent` and `SetupIntent`
* [#2547](https://github.com/stripe/stripe-android/pull/2547) Convert `BankAccount.BankAccountType`
  to `BankAccount.Type` enum
* [#2554](https://github.com/stripe/stripe-android/pull/2554) Update `PaymentAuthWebViewActivity`'s
  back button behavior
* [#2551](https://github.com/stripe/stripe-android/pull/2551) Fix `StrictMode` `DiskReadViolation`
  violations
* [#2555](https://github.com/stripe/stripe-android/pull/2555) Improve `PaymentAuthWebViewActivity`
* [#2559](https://github.com/stripe/stripe-android/pull/2559) Represent `PaymentIntent`'
  s `confirmationMethod` and `captureMethod` as enums
* [#2561](https://github.com/stripe/stripe-android/pull/2561) Make `CardInputListener.FocusField` an
  enum
* [#2562](https://github.com/stripe/stripe-android/pull/2562)
  Make `SourceTypeModel.Card.ThreeDSecureStatus` an enum
* [#2563](https://github.com/stripe/stripe-android/pull/2563) Make `PaymentMethod.Card#brand`
  a `CardBrand`
* [#2566](https://github.com/stripe/stripe-android/pull/2566) Make `Token.Type` an enum
* [#2569](https://github.com/stripe/stripe-android/pull/2569) Refactor `Source` class and related
  classes
* [#2572](https://github.com/stripe/stripe-android/pull/2572) Fix `StrictMode` `DiskReadViolation`
  violations when starting 3DS2
* [#2577](https://github.com/stripe/stripe-android/pull/2577)
  Make `CustomerSource#tokenizationMethod` a `TokenizationMethod?`
* [#2579](https://github.com/stripe/stripe-android/pull/2579) Make `PaymentMethod.Card.Networks`
  fields public
* [#2587](https://github.com/stripe/stripe-android/pull/2587) Fix BouncyCastle Proguard rule
* [#2594](https://github.com/stripe/stripe-android/pull/2594) Fix vector icon references in layout
  files
    * Reduce SDK size by ~30kb
    * Fix Google Pay icon in `PaymentMethodsActivity`
* [#2595](https://github.com/stripe/stripe-android/pull/2595) Bump SDK `minSdkVersion` to `21`
* [#2599](https://github.com/stripe/stripe-android/pull/2599) Remove `StripeSSLSocketFactory`
* [#2604](https://github.com/stripe/stripe-android/pull/2604) Add `stripeAccountId`
  to `PaymentConfiguration` and use in `AddPaymentMethodActivity`
* [#2609](https://github.com/stripe/stripe-android/pull/2609)
  Refactor `AddPaymentMethodActivity.Result`
* [#2610](https://github.com/stripe/stripe-android/pull/2610) Remove `PaymentSession`
  and `CustomerSession`'s "Activity" Listeners
* [#2611](https://github.com/stripe/stripe-android/pull/2611) Target API 30
* [#2617](https://github.com/stripe/stripe-android/pull/2617) Convert `CustomizableShippingField` to
  enum
* [#2623](https://github.com/stripe/stripe-android/pull/2623) Update Gradle to `6.5.1`
* [#2557](https://github.com/stripe/stripe-android/pull/2634) Update 3DS2 SDK to `4.0.3`

## 14.5.0 - 2020-06-04

* [#2453](https://github.com/stripe/stripe-android/pull/2453)
  Add `ConfirmPaymentIntentParams#receiptEmail`
* [#2458](https://github.com/stripe/stripe-android/pull/2458) Remove INTERAC
  from `GooglePayJsonFactory.DEFAULT_CARD_NETWORKS`
* [#2462](https://github.com/stripe/stripe-android/pull/2462) Capitalize currency code
  in `GooglePayJsonFactory`
* [#2466](https://github.com/stripe/stripe-android/pull/2466)
  Deprecate `ActivityStarter.startForResult()` with no args
* [#2467](https://github.com/stripe/stripe-android/pull/2467) Update `CardBrand.MasterCard` regex
* [#2475](https://github.com/stripe/stripe-android/pull/2475) Support `android:focusedByDefault`
  in `CardInputWidget`
    * Fixes #2463 on Android API level 26 and above
* [#2483](https://github.com/stripe/stripe-android/pull/2483) Fix formatting of `maxTimeout` value
  in `Stripe3ds2AuthParams`
* [#2494](https://github.com/stripe/stripe-android/pull/2494) Support starting 3DS2 challenge flow
  from a Fragment
* [#2496](https://github.com/stripe/stripe-android/pull/2496) Deprecate `StripeIntent.stripeSdkData`
* [#2497](https://github.com/stripe/stripe-android/pull/2497) Deprecate `StripeIntent.redirectData`
* [#2513](https://github.com/stripe/stripe-android/pull/2513) Add `canDeletePaymentMethods`
  to `PaymentSessionConfig`
    * `canDeletePaymentMethods` controls whether the user can delete a payment method by swiping on
      it in `PaymentMethodsActivity`
* [#2525](https://github.com/stripe/stripe-android/pull/2525) Upgrade Android Gradle Plugin to 4.0.0
* [#2531](https://github.com/stripe/stripe-android/pull/2531) Update 3DS2 SDK to 3.0.2
    * Fix bug in 3DS2 SDK where multi-screen challenges were not correctly returning result to
      starting Activity/Fragment
* [#2536](https://github.com/stripe/stripe-android/pull/2536) Update Gradle to 6.5

## 14.4.1 - 2020-04-30

* [#2441](https://github.com/stripe/stripe-android/pull/2441) Catch `IllegalArgumentException`
  in `ApiOperation`
* [#2442](https://github.com/stripe/stripe-android/pull/2442) Capitalize `GooglePayJsonFactory`'
  s `allowedCountryCodes`
* [#2445](https://github.com/stripe/stripe-android/pull/2445) Bump 3DS2 SDK to `2.7.8`
    * Downgrade BouncyCastle to `1.64`

## 14.4.0 - 2020-04-28

* [#2379](https://github.com/stripe/stripe-android/pull/2379) Add optional `stripeAccountId` param
  to most `Stripe` methods
    * This enables passing a `Stripe-Account` header on a per-request basis
* [#2398](https://github.com/stripe/stripe-android/pull/2398) Add optional `stripeAccountId` param
  to `Stripe#confirmPayment()`
* [#2405](https://github.com/stripe/stripe-android/pull/2405) Add optional `stripeAccountId` param
  to `Stripe#confirmSetupIntent()`
* [#2406](https://github.com/stripe/stripe-android/pull/2406) Add optional `stripeAccountId` param
  to `Stripe#authenticateSource()`
* [#2408](https://github.com/stripe/stripe-android/pull/2408) Update `PaymentMethod.Type#isReusable`
  values
* [#2412](https://github.com/stripe/stripe-android/pull/2412) Make `StripeIntentResult`
  implement `Parcelable`
* [#2413](https://github.com/stripe/stripe-android/pull/2413)
  Create `Stripe#retrievePaymentIntent()` and `Stripe#retrieveSetupIntent()` async methods
* [#2421](https://github.com/stripe/stripe-android/pull/2421) Capitalize `countryCode`
  in `GooglePayJsonFactory`
* [#2429](https://github.com/stripe/stripe-android/pull/2429) Fix SDK source paths and source
  publishing
* [#2430](https://github.com/stripe/stripe-android/pull/2430)
  Add `GooglePayJsonFactory.isJcbEnabled`
    * Enables JCB as an allowed card network. By default, JCB is disabled.
* [#2435](https://github.com/stripe/stripe-android/pull/2435) Bump 3DS2 SDK to `2.7.7`
    * On 3DS2 challenge screen, handle system back button tap as cancel button tap
* [#2436](https://github.com/stripe/stripe-android/pull/2436) Add `advancedFraudSignalsEnabled`
  property
    *
    See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection)
    for more details

## 14.3.0 - 2020-04-20

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [14.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1440---2020-04-28)
or greater.*

* [#2334](https://github.com/stripe/stripe-android/pull/2334) Add support for BACS Debit
  in `PaymentMethodCreateParams`
* [#2335](https://github.com/stripe/stripe-android/pull/2335) Add support for BACS Debit Payment
  Method
* [#2336](https://github.com/stripe/stripe-android/pull/2336) Improve `ShippingInfoWidget`
    * Make postal code and phone number fields single line
    * Make phone number field use `inputType="phone"`
* [#2342](https://github.com/stripe/stripe-android/pull/2342) Convert `CardBrand` prefixes to regex
    * Add `^81` as a pattern for `CardBrand.UnionPay`
* [#2343](https://github.com/stripe/stripe-android/pull/2343) Update `CardBrand.JCB` regex
* [#2362](https://github.com/stripe/stripe-android/pull/2362) Add support for
  parsing `shippingInformation` in `GooglePayResult`
* [#2365](https://github.com/stripe/stripe-android/pull/2365) Convert png assets to webp to reduce
  asset size
* [#2373](https://github.com/stripe/stripe-android/pull/2373) Set default `billingAddressFields`
  to `BillingAddressFields.PostalCode`
* [#2381](https://github.com/stripe/stripe-android/pull/2381) Add support for SOFORT PaymentMethod
* [#2384](https://github.com/stripe/stripe-android/pull/2384) Add support for P24 PaymentMethod
* [#2389](https://github.com/stripe/stripe-android/pull/2389) Add support for Bancontact
  PaymentMethod
* [#2390](https://github.com/stripe/stripe-android/pull/2390) Bump Kotlin version to `1.3.72`
* [#2392](https://github.com/stripe/stripe-android/pull/2392) Add `shipping` property
  to `PaymentIntent`
* [#2394](https://github.com/stripe/stripe-android/pull/2394) Add support for Giropay PaymentMethod
* [#2395](https://github.com/stripe/stripe-android/pull/2395) Add support for EPS PaymentMethod
* [#2396](https://github.com/stripe/stripe-android/pull/2396) Expose `klarna` property on `Source`
* [#2401](https://github.com/stripe/stripe-android/pull/2401) Bump 3DS2 SDK to `2.7.4`
    * Add new translations for 3DS2 SDK strings
    * Upgrade BouncyCastle to `1.65`

## 14.2.1 - 2020-03-26

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [14.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1440---2020-04-28)
or greater.*

* [#2299](https://github.com/stripe/stripe-android/pull/2299) Make `SourceParams.OwnerParams`
  constructor public and properties mutable
* [#2304](https://github.com/stripe/stripe-android/pull/2304) Force Canadian postal codes to be
  uppercase
* [#2315](https://github.com/stripe/stripe-android/pull/2315) Add `Fragment` support
  to `AddPaymentMethodActivityStarter`
* [#2325](https://github.com/stripe/stripe-android/pull/2325) Update `BecsDebitWidget`

## 14.2.0 - 2020-03-18

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [14.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1440---2020-04-28)
or greater.*

* [#2278](https://github.com/stripe/stripe-android/pull/2278) Add ability to require US ZIP code
  in `CardInputWidget` and `CardMultilineWidget`
* [#2279](https://github.com/stripe/stripe-android/pull/2279) Default `CardMultilineWidget` to
  global postal code configuration
* [#2282](https://github.com/stripe/stripe-android/pull/2282) Update pinned API version
  to `2020-03-02`
* [#2285](https://github.com/stripe/stripe-android/pull/2285)
  Create `BecsDebitMandateAcceptanceFactory` for generating BECS Debit mandate acceptance copy
* [#2290](https://github.com/stripe/stripe-android/pull/2290) Bump 3DS2 SDK to `2.7.2`
    * Fix `onActivityResult()` not called after 3DS2 challenge flow when "Don't keep activities" is
      enabled
    * Use view binding
    * Upgrade BouncyCastle to `1.64`
* [#2293](https://github.com/stripe/stripe-android/pull/2293) Add min length validation to BECS
  account number
* [#2295](https://github.com/stripe/stripe-android/pull/2295)
  Create `BecsDebitMandateAcceptanceTextView`
* [#2297](https://github.com/stripe/stripe-android/pull/2297)
  Add `BecsDebitWidget.ValidParamsCallback`

## 14.1.1 - 2020-03-09

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [14.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1440---2020-04-28)
or greater.*

* [#2257](https://github.com/stripe/stripe-android/pull/2257) Disable Kotlin Synthetics and migrate
  to [view binding](https://developer.android.com/topic/libraries/view-binding)
* [#2260](https://github.com/stripe/stripe-android/pull/2260) Update Kotlin Gradle Plugin
  to `1.3.70`
* [#2271](https://github.com/stripe/stripe-android/pull/2271) Update Proguard rules to remove
  unneeded BouncyCastle class
* [#2272](https://github.com/stripe/stripe-android/pull/2272) Update `kotlinx.coroutines` to `1.3.4`
* [#2274](https://github.com/stripe/stripe-android/pull/2274)
  Make `ConfirmPaymentIntentParams#savePaymentMethod` nullable

## 14.1.0 - 2020-03-02

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [14.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1440---2020-04-28)
or greater.*

* [#2207](https://github.com/stripe/stripe-android/pull/2207)
  Add `CardInputWidget#setPostalCodeTextWatcher`
* [#2211](https://github.com/stripe/stripe-android/pull/2211) Add support for 16-digit Diners Club
  card numbers
* [#2215](https://github.com/stripe/stripe-android/pull/2215) Set `CardInputWidget`'s postal code
  field's IME action to done
* [#2220](https://github.com/stripe/stripe-android/pull/2220) Highlight "Google Pay" option in
  payment methods screen if selected
* [#2221](https://github.com/stripe/stripe-android/pull/2221) Update 14-digit Diners Club formatting
* [#2224](https://github.com/stripe/stripe-android/pull/2224) Change `CardInputWidget` icon to
  represent validity of input
* [#2234](https://github.com/stripe/stripe-android/pull/2234) Add support for `setup_future_usage`
  in PaymentIntent confirmation
* [#2235](https://github.com/stripe/stripe-android/pull/2235) Update Android Gradle Plugin to 3.6.1
* [#2236](https://github.com/stripe/stripe-android/pull/2236) Change `CardMultilineWidget` icon to
  represent validity of input
* [#2238](https://github.com/stripe/stripe-android/pull/2238) Add support for `shipping` in
  PaymentIntent confirmation

## 14.0.0 - 2020-02-18

This release includes several breaking changes. See
the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more
details.

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [14.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1440---2020-04-28)
or greater.*

* [#2136](https://github.com/stripe/stripe-android/pull/2136)
  Update `com.google.android.material:material` to `1.1.0`
* [#2141](https://github.com/stripe/stripe-android/pull/2141) Fix crash when deleting a payment
  method in `PaymentMethodsActivity`
* [#2155](https://github.com/stripe/stripe-android/pull/2155) Fix parceling
  of `PaymentRelayStarter.Args`
* [#2156](https://github.com/stripe/stripe-android/pull/2156) Fix FPX bank order
* [#2163](https://github.com/stripe/stripe-android/pull/2163) Remove return type
  from `PaymentSession.init()`
* [#2165](https://github.com/stripe/stripe-android/pull/2165) Simplify `PaymentSession` state and
  lifecycle management
    * When instantiating a `PaymentSession()` with an `Activity`, it must now be
      a `ComponentActivity`
      (e.g. `AppCompatActivity` or `FragmentActivity`)
    * `PaymentSession#init()` no longer takes a `savedInstanceState` argument
    * Remove `PaymentSession#savePaymentSessionInstanceState()`
    * Remove `PaymentSession#onDestroy()`
* [#2173](https://github.com/stripe/stripe-android/pull/2173) Fix Mastercard display name
* [#2174](https://github.com/stripe/stripe-android/pull/2174) Add optional params
  to `CustomerSession.getPaymentMethods()`
    * See [List a Customer's PaymentMethods](https://stripe.com/docs/api/payment_methods/list) for
      more details
* [#2179](https://github.com/stripe/stripe-android/pull/2179) Fetch previously used `PaymentMethod`
  in `PaymentSession`
    * If `PaymentSessionConfig.shouldPrefetchCustomer == true`, when a new `PaymentSession` is
      started, fetch the customer's previously selected payment method, if it exists, and return via
      `PaymentSessionListener#onPaymentSessionDataChanged()`
* [#2180](https://github.com/stripe/stripe-android/pull/2180)
  Remove `PaymentSession.paymentSessionData`
* [#2185](https://github.com/stripe/stripe-android/pull/2185) Convert `Card.FundingType`
  to `CardFunding` enum
* [#2189](https://github.com/stripe/stripe-android/pull/2189) Cleanup `StripeException` subclasses
* [#2194](https://github.com/stripe/stripe-android/pull/2194) Upgrade 3DS2 SDK to `2.5.4`
    * Update `com.google.android.material:material` to `1.1.0`
    * Fix accessibility issues on 3DS2 challenge screen
    * Update 3DS2 styles for consistency
        * Create `BaseStripe3DS2TextInputLayout` that
          extends `Widget.MaterialComponents.TextInputLayout.OutlinedBox`
        * Create `Stripe3DS2TextInputLayout` that extends `BaseStripe3DS2TextInputLayout`
        * Apply `Stripe3DS2TextInputLayout` to `TextInputLayout`
        * Create `BaseStripe3DS2EditText` with
          parent `Widget.MaterialComponents.TextInputEditText.OutlinedBox`
        * Rename `Stripe3DS2EditTextTheme` to `Stripe3DS2EditText` and change its parent
          to `BaseStripe3DS2EditText`
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
    *
    See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection)
    for more details
* Include sources

## 13.2.0 - 2020-02-03

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [13.2.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1330---2020-05-15)
or greater.*

* [#2112](https://github.com/stripe/stripe-android/pull/2112) Enable adding Mandate to confirm
  params
* [#2113](https://github.com/stripe/stripe-android/pull/2113) Enable requiring postal code
  in `CardInputWidget` and `CardMultilineWidget`
* [#2114](https://github.com/stripe/stripe-android/pull/2114) Fix bug in
  highlighting `StripeEditText` fields with errors

## 13.1.3 - 2020-01-27

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [13.2.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1330---2020-05-15)
or greater.*

* [#2105](https://github.com/stripe/stripe-android/pull/2105) Fix crash when confirming a Payment
  Intent or Setup Intent and an error is encountered

## 13.1.2 - 2020-01-23

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [13.2.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1330---2020-05-15)
or greater.*

* [#2093](https://github.com/stripe/stripe-android/pull/2093) Add `CardValidCallback` and add
  support in card forms
* [#2094](https://github.com/stripe/stripe-android/pull/2094) Make `StripeError` serializable

## 13.1.1 - 2020-01-22

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [13.2.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1330---2020-05-15)
or greater.*

* [#2074](https://github.com/stripe/stripe-android/pull/2074) Populate `isSelected` for
  selected `PaymentMethodsAdapter` item
* [#2076](https://github.com/stripe/stripe-android/pull/2076) Announce invalid fields when
  validating `CardInputWidget`
* [#2077](https://github.com/stripe/stripe-android/pull/2077) Add delete payment method
  accessibility action in `PaymentMethodsAdapter`
* [#2078](https://github.com/stripe/stripe-android/pull/2078) Make `StripeEditText` errors
  accessible
* [#2082](https://github.com/stripe/stripe-android/pull/2082) Use ErrorMessageTranslator for
  AddPaymentMethodActivity errors
* [#2083](https://github.com/stripe/stripe-android/pull/2083) Add accessibility traversal rules
  on `AddPaymentMethodActivity`
* [#2084](https://github.com/stripe/stripe-android/pull/2084) Update `BankAccount` constructor to
  support all bank account token parameters
* [#2086](https://github.com/stripe/stripe-android/pull/2086) Add `id` and `status` fields
  to `BankAccount`
* [#2087](https://github.com/stripe/stripe-android/pull/2087) Use `BankAccountTokenParams` for bank
  account token creation
    * Create `Stripe#createBankAccountToken()` and `Stripe#createBankAccountTokenSynchronous()` that
      take a `BankAccountTokenParams` object
    * Deprecate `BankAccount` for token creation

## 13.1.0 - 2020-01-16

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [13.2.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1330---2020-05-15)
or greater.*

* [#2055](https://github.com/stripe/stripe-android/pull/2055) Fix styling of `CardInputWidget`
  and `CardMultilineWidget`
    * `com.google.android.material:material:1.1.0-rc01` breaks `TextInputLayout` styling; fix by
      explicitly setting a style that extends `Widget.Design.TextInputLayout`
* [#2056](https://github.com/stripe/stripe-android/pull/2056) Update `CardInputWidget`'s `EditText`
  size
    * Fix "Postal Code" field being partially cut off on some screens
* [#2066](https://github.com/stripe/stripe-android/pull/2066) Add support for uploading a file to
  Stripe
    * See `Stripe#createFile()` and `Stripe#createFileSynchronous()`
* [#2071](https://github.com/stripe/stripe-android/pull/2071) Fix accessibility issues on Payment
  Methods selection screen
    * Mark `View`s representing existing payment methods and add a new payment method action as
      focusable and clickable

## 13.0.0 - 2020-01-13

This release includes several breaking changes. See
the [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) for more
details.

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [13.2.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1330---2020-05-15)
or greater.*

* [#1950](https://github.com/stripe/stripe-android/pull/1950) Add idempotency key for `Stripe` API
  POST methods
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
* [#1993](https://github.com/stripe/stripe-android/pull/1993) Remove deprecated methods
  from `PaymentSession`
    * See the [Migration Guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md)
      for more details
* [#1994](https://github.com/stripe/stripe-android/pull/1994) Enable postal code field
  in `CardInputWidget` by default
* [#1995](https://github.com/stripe/stripe-android/pull/1995) Enable Google Pay option in Basic
  Integration and Stripe Activities
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
* [#1996](https://github.com/stripe/stripe-android/pull/1996) Update postal code logic
  for `CardMultilineWidget`
    * Default to showing postal code (i.e. `shouldShowPostalCode = true`)
    * Postal code is now optional when displayed
    * Remove validation on postal code field
    * Change postal code field hint text to "Postal Code"
    * Remove `CardInputListener.onPostalCodeComplete()`
* [#1998](https://github.com/stripe/stripe-android/pull/1998) Use `CardBrand` enum to represent card
  brands
    * Change the type of `Card#brand` and `SourceCardData#brand` properties from `String?`
      to `CardBrand`
    * Remove `Card.CardBrand`
* [#1999](https://github.com/stripe/stripe-android/pull/1999) Remove deprecated `BroadcastReceiver`
  logic from `PaymentFlowActivity`
    * See the [Migration Guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md)
      for more details
* [#2000](https://github.com/stripe/stripe-android/pull/2000) Pass `PaymentSessionConfig` instance
  to `PaymentSession` constructor
* [#2002](https://github.com/stripe/stripe-android/pull/2002) Fix regression in `CardInputWidget`
  styling To customize the individual `EditText` fields of `CardInputWidget`, define
  a `Stripe.CardInputWidget.EditText` style that extends `Stripe.Base.CardInputWidget.EditText`. For
  example,
    ```xml
    <style name="Stripe.CardInputWidget.EditText" parent="Stripe.Base.CardInputWidget.EditText">
        <item name="android:textSize">22sp</item>
        <item name="android:textColor">@android:color/holo_blue_light</item>
        <item name="android:textColorHint">@android:color/holo_orange_light</item>
    </style>
    ```
* [#2003](https://github.com/stripe/stripe-android/pull/2003) Add support for authenticating
  a `Source` via in-app WebView
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
    * Change the type of `Card#tokenizationMethod` and `SourceCardData#tokenizationMethod`
      from `String?` to `TokenizationMethod?`
* [#2013](https://github.com/stripe/stripe-android/pull/2013) Populate shipping address country from
  pre-populated shipping info
* [#2015](https://github.com/stripe/stripe-android/pull/2015) Update `PaymentSessionConfig`'s
  default `BillingAddressFields` to `PostalCode`
* [#2020](https://github.com/stripe/stripe-android/pull/2020) Change `PaymentMethod.type`
  from `String?` to `PaymentMethod.Type?`
* [#2028](https://github.com/stripe/stripe-android/pull/2028) Update `SourceParams` fields
    * Update `setOwner()` to take `OwnerParams` instead of `Map`
    * Remove `setRedirect()`, use `setReturnUrl()` instead
    * Update some setters to take null values, simplifying usage
    * Update comments
* [#2029](https://github.com/stripe/stripe-android/pull/2029) Update `CardInputWidget` to
  use `TextInputLayout`
    * Make `StripeEditText` extend `TextInputEditText`
* [#2038](https://github.com/stripe/stripe-android/pull/2038) Update `CardInputWidget` to focus on
  first error field when validating
* [#2039](https://github.com/stripe/stripe-android/pull/2039) Add support for creating a person
  token
    * Add `Stripe#createPersonToken()` and `Stripe#createPersonTokenSynchronous()`
* [#2040](https://github.com/stripe/stripe-android/pull/2040) Add support for CVC recollection in
  PaymentIntents
    * Update `ConfirmPaymentIntentParams.createWithPaymentMethodId()` with
      optional `PaymentMethodOptionsParams?` argument
* [#2042](https://github.com/stripe/stripe-android/pull/2042)
  Create `AccountParams.BusinessTypeParams`
    * `BusinessTypeParams.Company` and `BusinessTypeParams.Individual` model the parameters for
      creating a
      [company](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company)
      or
      [individual](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual)
      [account token](https://stripe.com/docs/api/tokens/create_account). Use these instead of
      creating raw maps representing the data.
    * `AccountParams.createAccountParams()` is now deprecated. Use the
      appropriate `AccountParams.create()` method.

## 12.9.0 - 2020-05-15

* Update 3DS2 SDK to v2.3.7
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
    *
    See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection)
    for more details
* Include sources

## 12.8.2 - 2019-12-20

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1974](https://github.com/stripe/stripe-android/pull/1974)
  Add `PaymentSessionConfig#shouldPrefetchCustomer`
    * Mark `PaymentSessionConfig#init()` with `shouldPrefetchCustomer` argument as deprecated
* [#1980](https://github.com/stripe/stripe-android/pull/1980) Don't show a `Dialog`
  in `StripeActivity` if `isFinishing()`
* [#1989](https://github.com/stripe/stripe-android/pull/1989) Create `CardBrand` enum
* [#1990](https://github.com/stripe/stripe-android/pull/1990) Relax validation of UK postal codes

## 12.8.1 - 2019-12-18

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1968](https://github.com/stripe/stripe-android/pull/1968) Upgrade 3DS2 SDK to `2.2.7`
    * Downgrade to `com.google.android.material:material:1.0.0`

## 12.8.0 - 2019-12-17

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1947](https://github.com/stripe/stripe-android/pull/1947) Allow setting of window flags on
  Stripe Activities
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
* [#1956](https://github.com/stripe/stripe-android/pull/1956) Add support for configuring billing
  address fields on `AddPaymentMethodActivity`
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
* [#1957](https://github.com/stripe/stripe-android/pull/1957)
  Enable `PaymentSessionConfig.ShippingInformationValidator`
  and `PaymentSessionConfig.ShippingMethodsFactory`
* [#1958](https://github.com/stripe/stripe-android/pull/1958) Add validation for PaymentIntent and
  SetupIntent client secrets
* [#1959](https://github.com/stripe/stripe-android/pull/1959) Upgrade 3DS2 SDK to `2.2.6`

## 12.7.0 - 2019-12-16

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1915](https://github.com/stripe/stripe-android/pull/1915) Update API version
  to [2019-12-03](https://stripe.com/docs/upgrades#2019-12-03)
* [#1928](https://github.com/stripe/stripe-android/pull/1928) Make Payment Method `Wallet` a sealed
  class
* [#1930](https://github.com/stripe/stripe-android/pull/1930) Update text size for `CardInputWidget`
  fields
* [#1939](https://github.com/stripe/stripe-android/pull/1939) Update Android Gradle Plugin
  to `3.5.3`
* [#1946](https://github.com/stripe/stripe-android/pull/1946) Upgrade 3DS2 SDK to `2.2.5`
    * Upgrade to `com.google.android.material:material:1.2.0-alpha2`
* [#1949](https://github.com/stripe/stripe-android/pull/1949) Catch `NullPointerException` when
  calling `StripeEditText.setHint()`. This is a workaround for
  a [known issue on some Samsung devices](https://issuetracker.google.com/issues/37127697).
* [#1951](https://github.com/stripe/stripe-android/pull/1951) Expose ability to enable postal code
  on `CardInputWidget`
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

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1897](https://github.com/stripe/stripe-android/pull/1897) Upgrade 3DS2 SDK to `2.2.4`
    * Fix crash when using Instant App

## 12.6.0 - 2019-11-27

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1859](https://github.com/stripe/stripe-android/pull/1859) Create `GooglePayJsonFactory`, a
  factory for generating Google Pay JSON request objects
* [#1860](https://github.com/stripe/stripe-android/pull/1860) Namespace drawables with `stripe_`
  prefix
* [#1861](https://github.com/stripe/stripe-android/pull/1861) Create `GooglePayResult` to parse and
  model Google Pay Payment Data response
* [#1863](https://github.com/stripe/stripe-android/pull/1863) Complete migration of SDK code to
  Kotlin 🎉
* [#1864](https://github.com/stripe/stripe-android/pull/1864) Make Klarna Source creation methods
  public and create example
    * See `SourceParams.createKlarna()`
* [#1865](https://github.com/stripe/stripe-android/pull/1865) Make all model classes
  implement `Parcelable`
* [#1871](https://github.com/stripe/stripe-android/pull/1871) Simplify configuration of example app
    * Example app can be configured via `$HOME/.gradle/gradle.properties` instead of `Settings.kt`
      ```
      STRIPE_EXAMPLE_BACKEND_URL=https://hidden-beach-12345.herokuapp.com/
      STRIPE_EXAMPLE_PUBLISHABLE_KEY=pk_test_12345
      STRIPE_ACCOUNT_ID=
      ```
* [#1883](https://github.com/stripe/stripe-android/pull/1883)
  Enable `PaymentSessionConfig.ShippingInformationValidator`
  and `PaymentSessionConfig.ShippingMethodsFactory`
    * See the [Migration Guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md)
      for an example of how to use the new interfaces
* [#1884](https://github.com/stripe/stripe-android/pull/1884) Mark `PaymentFlowExtras` as deprecated
* [#1885](https://github.com/stripe/stripe-android/pull/1885) Create `Stripe#retrieveSource()` for
  asynchronous `Source` retrieval
* [#1890](https://github.com/stripe/stripe-android/pull/1890) Upgrade 3DS2 SDK to 2.2.3
    * Fix crash when using Instant App

## 12.5.0 - 2019-11-21

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1836](https://github.com/stripe/stripe-android/pull/1836) Add support
  for [statement_descriptor](https://stripe.com/docs/api/sources/object#source_object-statement_descriptor)
  field to `Source` model via `Source#statementDescriptor`
* [#1837](https://github.com/stripe/stripe-android/pull/1837) Add support
  for [source_order](https://stripe.com/docs/api/sources/create#create_source-source_order) param
  via `SourceOrderParams`
* [#1839](https://github.com/stripe/stripe-android/pull/1839) Add support
  for [source_order](https://stripe.com/docs/api/sources/object#source_object-source_order) field
  to `Source` model via `Source#sourceOrder`
* [#1842](https://github.com/stripe/stripe-android/pull/1842)
  Add `PaymentSessionConfig.Builder.setAllowedShippingCountryCodes()`. Used to specify an allowed
  set of countries when collecting the customer's shipping address via `PaymentSession`.
    ```kotlin
    // Example
    PaymentSessionConfig.Builder()
        // only allowed US and Canada shipping addresses
        .setAllowedShippingCountryCodes(setOf("US", "CA"))
        .build()
    ```
* [#1845](https://github.com/stripe/stripe-android/pull/1845) Fix country code validation
  in `PaymentFlowActivity`'s shipping information screen
    * Require that the customer submits a country that exists in the autocomplete dropdown
    * Show error UI when the submitted country fails validation
* [#1857](https://github.com/stripe/stripe-android/pull/1857) Fix crash related to Kotlin Coroutines
    * Downgrade `kotlinx-coroutines` from `1.3.2` to `1.3.0`
    * Add Proguard rules

## 12.4.0 - 2019-11-13

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1792](https://github.com/stripe/stripe-android/pull/1792) Remove default selection of a Payment
  Method from `PaymentMethodsActivity`
* [#1797](https://github.com/stripe/stripe-android/pull/1797) Document `StripeDefaultTheme` style
* [#1799](https://github.com/stripe/stripe-android/pull/1799) Document `Stripe3DS2Theme` and related
  styles
* [#1809](https://github.com/stripe/stripe-android/pull/1809) Update to Gradle 6.0
* [#1810](https://github.com/stripe/stripe-android/pull/1810) Update API version
  to [2019-11-05](https://stripe.com/docs/upgrades#2019-11-05)
* [#1812](https://github.com/stripe/stripe-android/pull/1812) Upgrade 3DS2 SDK to 2.2.2
* [#1813](https://github.com/stripe/stripe-android/pull/1813) Don't select a new PaymentMethod after
  deleting one in `PaymentMethodsActivity`
* [#1820](https://github.com/stripe/stripe-android/pull/1820) Update `PaymentMethodsActivity` result
  and `PaymentSession.handlePaymentData()` logic
    * `PaymentMethodsActivity` returns result code of `Activity.RESULT_OK` when the user selected a
      Payment Method
    * `PaymentMethodsActivity` returns result code of `Activity.RESULT_CANCELED` when the user taps
      back via the toolbar or device back button
    * `PaymentSession#handlePaymentData()` now
      calls `PaymentSessionListener#onPaymentSessionDataChanged()` for any result
      from `PaymentMethodsActivity`

## 12.3.0 - 2019-11-05

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1775](https://github.com/stripe/stripe-android/pull/1775) Add support for idempotency key on
  Stripe Token API requests
* [#1777](https://github.com/stripe/stripe-android/pull/1777) Make `Card` implement `Parcelable`
* [#1781](https://github.com/stripe/stripe-android/pull/1781) Mark `Stripe#createToken()`
  as `@Deprecated`; replace with `Stripe#createCardToken()`
* [#1782](https://github.com/stripe/stripe-android/pull/1782) Mark `Stripe#authenticatePayment()`
  and `Stripe#authenticateSetup()` as `@Deprecated`; replace
  with `Stripe#handleNextActionForPayment()` and `Stripe#handleNextActionForSetupIntent()`,
  respectively
* [#1784](https://github.com/stripe/stripe-android/pull/1784) Update API version
  to [2019-10-17](https://stripe.com/docs/upgrades#2019-10-17)
* [#1787](https://github.com/stripe/stripe-android/pull/1787) Fix `CardNumberEditText` performance
* [#1788](https://github.com/stripe/stripe-android/pull/1788) Fix `ExpiryDateEditText` performance

## 12.2.0 - 2019-10-31

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1745](https://github.com/stripe/stripe-android/pull/1745) Make `StripeEditText` public
* [#1746](https://github.com/stripe/stripe-android/pull/1746) Make `FpxBank` enum public
* [#1748](https://github.com/stripe/stripe-android/pull/1748) Update FPX bank list with offline
  status
* [#1755](https://github.com/stripe/stripe-android/pull/1755) Annotate `Stripe` methods
  with `@UiThread` or `@WorkerThread`
* [#1758](https://github.com/stripe/stripe-android/pull/1758)
  Refactor `CustomerSession.setCustomerShippingInformation()`
* [#1764](https://github.com/stripe/stripe-android/pull/1764) Add support for Javascript confirm
  dialogs in 3DS1 payment authentication WebView
* [#1765](https://github.com/stripe/stripe-android/pull/1765) Fix rotation issues with shipping info
  and shipping method selection screens

## 12.1.0 - 2019-10-22

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1738](https://github.com/stripe/stripe-android/pull/1738) Enable specifying Payment Method type
  to use in UI components

## 12.0.1 - 2019-10-21

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1721](https://github.com/stripe/stripe-android/pull/1721) Properly cleanup and
  destroy `PaymentAuthWebView`
* [#1722](https://github.com/stripe/stripe-android/pull/1722) Fix crash in 3DS2 challenge screen
  when airplane mode is enabled
* [#1731](https://github.com/stripe/stripe-android/pull/1731)
  Create `ConfirmSetupIntentParams.createWithoutPaymentMethod()`

## 12.0.0 - 2019-10-16

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [12.9.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1290---2020-05-15)
or greater.*

* [#1699](https://github.com/stripe/stripe-android/pull/1699) Remove deprecated methods
    * Replace `Stripe#createTokenSynchronous(Card)` with `Stripe#createCardTokenSynchronous(Card)`
    * Replace `Card#getCVC()` with `Card#getCvc()`
    * Remove `AddPaymentMethodActivity#EXTRA_NEW_PAYMENT_METHOD`,
      use `AddPaymentMethodActivityStarter.Result.fromIntent()` instead
    * Create overloaded `ShippingMethod` constructor with optional `detail` argument
* [#1701](https://github.com/stripe/stripe-android/pull/1701) Payment Intent API requests (i.e.
  requests to `/v1/payment_intents`) now return localized error messages
* [#1706](https://github.com/stripe/stripe-android/pull/1706) Add `Card#toPaymentMethodsParams()` to
  create a `PaymentMethodCreateParams` instance that includes both card and billing details

## 11.3.0 - 2020-05-15

* Update 3DS2 SDK to v2.3.7
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
    *
    See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection)
    for more details
* Include sources

## 11.2.2 - 2019-10-11

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1686](https://github.com/stripe/stripe-android/pull/1686) Fix native crash on some devices in
  3DS1 payment authentication WebView
* [#1690](https://github.com/stripe/stripe-android/pull/1690) Bump API version to `2019-10-08`
* [#1693](https://github.com/stripe/stripe-android/pull/1693) Add support for SEPA Debit in
  PaymentMethod

## 11.2.1 - 2019-10-11

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1677](https://github.com/stripe/stripe-android/pull/1677) Add logging to
  PaymentAuthWebViewActivity

## 11.2.0 - 2019-10-07

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1616](https://github.com/stripe/stripe-android/pull/1616)
  Make `AddPaymentMethodActivityStarter.Result.fromIntent()` public
* [#1619](https://github.com/stripe/stripe-android/pull/1619)
  Add `CardMultilineWidget#getPaymentMethodBillingDetailsBuilder()`
* [#1643](https://github.com/stripe/stripe-android/pull/1643)
  Create `Stripe.createCardTokenSynchronous()`
* [#1647](https://github.com/stripe/stripe-android/pull/1647) Add `StripeDefault3DS2Theme` for 3DS2
  customization via themes
* [#1652](https://github.com/stripe/stripe-android/pull/1652) In `PaymentMethodsActivity`, select a
  new Payment Method if the previously selected one was deleted
* [#1658](https://github.com/stripe/stripe-android/pull/1658) Add `stripe_` prefix to Stripe
  resources
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
* [#1667](https://github.com/stripe/stripe-android/pull/1667) Add support for SEPA Debit Payment
  Methods
    ```kotlin
    // Example
    PaymentMethodCreateParams.create(
        PaymentMethodCreateParams.SepaDebit.Builder()
            .setIban("__iban__")
            .build()
    )
    ```
* [#1668](https://github.com/stripe/stripe-android/pull/1668) Update Google Pay integration example
  in example app
* [#1669](https://github.com/stripe/stripe-android/pull/1669) Update 3DS2 SDK to 2.1.3
    * Prevent challenge screen's cancel button from being clicked more than once

## 11.1.4 - 2019-09-24

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1603](https://github.com/stripe/stripe-android/pull/1603) Update ProGuard rules for BouncyCastle
* [#1608](https://github.com/stripe/stripe-android/pull/1608) Update ProGuard rules for Material
  Components

## 11.1.3 - 2019-09-18

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1582](https://github.com/stripe/stripe-android/pull/1582) Update 3DS2 SDK to 2.0.5
    * Add translations for `ko`, `nn`, `ru`, and `tr`
* [#1583](https://github.com/stripe/stripe-android/pull/1583)
  Create `AddPaymentMethodActivityStarter.Result`
    * Mark `AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD` as `@Deprecated`.
      Use `AddPaymentMethodActivityStarter.Result` instead.
    ```kotlin
    // Example

    // before
    val paymentMethod: PaymentMethod? = data.getParcelableExtra(EXTRA_NEW_PAYMENT_METHOD)

    // after
    val result: AddPaymentMethodActivityStarter.Result =
        AddPaymentMethodActivityStarter.Result.fromIntent(data)
    val paymentMethod: PaymentMethod? = result?.paymentMethod
    ```    
* [#1587](https://github.com/stripe/stripe-android/pull/1587) Fix logic for entering 3DS2 challenge
  flow

## 11.1.2 - 2019-09-18

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1581](https://github.com/stripe/stripe-android/pull/1581) Fix WebView issues in API 21 and 22

## 11.1.1 - 2019-09-17

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1578](https://github.com/stripe/stripe-android/pull/1578) Disable dokka in `:stripe` to fix
  release process

## 11.1.0 - 2019-09-17

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1545](https://github.com/stripe/stripe-android/pull/1545) Add Connect Account Id support
  to `GooglePayConfig`
* [#1560](https://github.com/stripe/stripe-android/pull/1560) Add swipe-to-delete gesture on added
  Payment Methods in `PaymentMethodsActivity`
* [#1560](https://github.com/stripe/stripe-android/pull/1560) Fix `HandlerThread` leak
  in `PaymentController.ChallengeFlowStarterImpl`
* [#1561](https://github.com/stripe/stripe-android/pull/1561) Move `CardMultilineWidget` focus to
  first error field on error
* [#1572](https://github.com/stripe/stripe-android/pull/1572) Update 3DS2 SDK to 2.0.4
* [#1574](https://github.com/stripe/stripe-android/pull/1574) Fix `HandlerThread` leak
  in `StripeFireAndForgetRequestExecutor`
* [#1577](https://github.com/stripe/stripe-android/pull/1577) Fix `ShippingMethodView` height

## 11.0.5 - 2019-09-13

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1538](https://github.com/stripe/stripe-android/pull/1538) Update `PaymentAuthWebView` to fix
  issues

## 11.0.4 - 2019-09-13

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1533](https://github.com/stripe/stripe-android/pull/1533) Update 3DS2 SDK to 2.0.3
* [#1534](https://github.com/stripe/stripe-android/pull/1534) Add ability to select checked item
  in `PaymentMethodsActivity`
* [#1537](https://github.com/stripe/stripe-android/pull/1537) Fix out-of-band web payment
  authentication

## 11.0.3 - 2019-09-12

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1530](https://github.com/stripe/stripe-android/pull/1530) Finish `PaymentAuthWebViewActivity`
  after returning from bank app

## 11.0.2 - 2019-09-12

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1527](https://github.com/stripe/stripe-android/pull/1527) Support `"intent://"` URIs in payment
  auth WebView

## 11.0.1 - 2019-09-11

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1518](https://github.com/stripe/stripe-android/pull/1518) Fix crash when payment authentication
  is started from Fragment and user taps back twice
* [#1523](https://github.com/stripe/stripe-android/pull/1523) Correctly handle deep-links in the
  in-app payment authentication WebView
* [#1524](https://github.com/stripe/stripe-android/pull/1524) Update 3DS2 SDK to 2.0.2
    * Fix issue with 3DS2 encryption and older BouncyCastle versions

## 11.0.0 - 2019-09-10

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [11.3.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1130---2020-05-15)
or greater.*

* [#1474](https://github.com/stripe/stripe-android/pull/1474) Fix darkmode issue with "Add an
  Address" form's Country selection dropdown
* [#1475](https://github.com/stripe/stripe-android/pull/1475) Hide keyboard after submitting "Add an
  Address" form in standard integration
* [#1478](https://github.com/stripe/stripe-android/pull/1478) Migrate to AndroidX
* [#1479](https://github.com/stripe/stripe-android/pull/1479) Persist `PaymentConfiguration`
  to `SharedPreferences`
* [#1480](https://github.com/stripe/stripe-android/pull/1480) Make `Source` immutable
* [#1481](https://github.com/stripe/stripe-android/pull/1481) Remove `@Deprecated` methods
  from `StripeIntent` models
    * Remove `PaymentIntent#getSource()`; use `PaymentIntent#getPaymentMethodId()`
    * Remove `SetupIntent#getCustomerId()`
    * Remove `SourceCallback`; use `ApiResultCallback<Source>`
    * Remove `TokenCallback`; use `ApiResultCallback<Token>`
* [#1485](https://github.com/stripe/stripe-android/pull/1485) Update 3DS2 SDK to 2.0.1
* [#1494](https://github.com/stripe/stripe-android/pull/1494) Update `PaymentMethodsActivity` UX
* [#1495](https://github.com/stripe/stripe-android/pull/1495) Remove `@Deprecated` fields and
  methods from `PaymentMethodsActivity`
* [#1497](https://github.com/stripe/stripe-android/pull/1497) Remove `Stripe` methods that accept a
  publishable key
* [#1506](https://github.com/stripe/stripe-android/pull/1506) Remove `Stripe#createToken()`
  with `Executor` argument
* [#1514](https://github.com/stripe/stripe-android/pull/1514) Bump API version to `2019-09-09`

## 10.5.0 - 2020-05-15

* Update 3DS2 SDK to v1.3.1
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
    *
    See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection)
    for more details
* Include sources

## 10.4.6 - 2019-10-14

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* Apply hot-fixes from 11.x
    * Update BouncyCastle Proguard rules. Keep only the BouncyCastle provider classes.
    * Hide progress bar in `onPageFinished()` instead of
      `onPageCommitVisible()` to avoid potential crash on some devices.

## 10.4.5 - 2019-09-16

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* Apply hot-fixes from 11.x
    * Enable DOM storage in `PaymentAuthWebView` to fix crash

## 10.4.4 - 2019-09-13

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* Apply hot-fixes from 11.x

## 10.4.3 - 2019-09-04

* [#1471](https://github.com/stripe/stripe-android/pull/1471) Fix issue with `CardUtils` visibility

## 10.4.2 - 2019-08-30

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1461](https://github.com/stripe/stripe-android/pull/1461) Fix crash in `PaymentAuthWebView`
* [#1462](https://github.com/stripe/stripe-android/pull/1462) Animate selections
  in `PaymentMethodsActivity`

## 10.4.1 - 2019-08-30

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1457](https://github.com/stripe/stripe-android/pull/1457) Fix crash in "Add an Address" screen
  when value for Country is empty

## 10.4.0 - 2019-08-29

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1421](https://github.com/stripe/stripe-android/pull/1421)
  Create `PaymentMethodsActivityStarter.Result` to retrieve result of `PaymentMethodsActivity`
* [#1427](https://github.com/stripe/stripe-android/pull/1427) Mark `Stripe` methods that accept a
  publishable key as deprecated
    ```
    // Example

    // before
    val stripe = Stripe(context)
    stripe.createPaymentMethodSynchronous(params, "pk_test_demo123")

    // after
    val stripe = Stripe(context, "pk_test_demo123")
    stripe.createPaymentMethodSynchronous(params)
    ```
* [#1428](https://github.com/stripe/stripe-android/pull/1428) Guard against opaque URIs
  in `PaymentAuthWebViewClient`
* [#1433](https://github.com/stripe/stripe-android/pull/1433) Add `setCardHint()`
  to `CardInputWidget` and `CardMultilineWidget`
* [#1434](https://github.com/stripe/stripe-android/pull/1434) Add setters on Card widgets for card
  number, expiration, and CVC
* [#1438](https://github.com/stripe/stripe-android/pull/1438) Bump API version to `2019-08-14`
* [#1446](https://github.com/stripe/stripe-android/pull/1446) Update `PaymentIntent`
  and `SetupIntent` models
    * Add missing `PaymentIntent#getPaymentMethodId()`
    * Mark `PaymentIntent#getSource()` as `@Deprecated` - use `PaymentIntent#getPaymentMethodId()`
    * Mark `SetupIntent#getCustomerId()` as `@Deprecated` - this attribute is not available with a
      publishable key
* [#1448](https://github.com/stripe/stripe-android/pull/1448) Update Gradle to 5.6.1
* [#1449](https://github.com/stripe/stripe-android/pull/1449) Add support for `cancellation_reason`
  attribute to PaymentIntent
* [#1450](https://github.com/stripe/stripe-android/pull/1450) Add support for `cancellation_reason`
  attribute to SetupIntent
* [#1451](https://github.com/stripe/stripe-android/pull/1451) Update Stripe 3DS2 library to `v1.2.2`
    * Dismiss keyboard after submitting 3DS2 form
    * Exclude `org.ow2.asm:asm` dependency

## 10.3.1 - 2018-08-22

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1394](https://github.com/stripe/stripe-android/pull/1394) Add `shouldPrefetchCustomer` arg
  to `PaymentSession.init()`
* [#1395](https://github.com/stripe/stripe-android/pull/1395) Fix inconsistent usage of relative
  attributes on card icon in `CardMultilineWidget`
* [#1412](https://github.com/stripe/stripe-android/pull/1412)
  Make `AddPaymentMethodActivityStarter()` available for starting `AddPaymentMethodActivity`
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

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1327](https://github.com/stripe/stripe-android/pull/1327) Deprecate `SourceCallback`
  and `TokenCallback`
    * Use `ApiResultCallback<Source>` and `ApiResultCallback<Token>` respectively
* [#1332](https://github.com/stripe/stripe-android/pull/1332) Create `StripeParamsModel` interface
  for Stripe API param classes
* [#1334](https://github.com/stripe/stripe-android/pull/1344) Remove `StripeModel#toMap()`
* [#1340](https://github.com/stripe/stripe-android/pull/1340) Upgrade Android Gradle Plugin
  to `3.5.0-rc03`
* [#1342](https://github.com/stripe/stripe-android/pull/1342) Update Builds Tools to `29.0.2`
* [#1347](https://github.com/stripe/stripe-android/pull/1347) Mark `Stripe.setStripeAccount()` as
  deprecated
    * Use `new Stripe(context, "publishable_key", "stripe_account_id")` instead
* [#1376](https://github.com/stripe/stripe-android/pull/1376) Add `shouldPrefetchEphemeralKey` arg
  to `CustomerSession.initCustomerSession()`
* [#1378](https://github.com/stripe/stripe-android/pull/1378) Add `last_payment_error` field to
  PaymentIntent model
* [#1379](https://github.com/stripe/stripe-android/pull/1379) Add `last_setup_error` field to
  SetupIntent model
* [#1388](https://github.com/stripe/stripe-android/pull/1388) Update Stripe 3DS2 library to `v1.1.7`
    * Fix issue in `PaymentAuthConfig.Stripe3ds2UiCustomization.Builder()`
      and `PaymentAuthConfig.Stripe3ds2UiCustomization.Builder.createWithAppTheme()` that was
      resulting in incorrect color customization on 3DS2 challenge screen
    * Improve accessibility of select options on 3DS2 challenge screen by setting minimum height to
      48dp

## 10.2.1 - 2019-08-06

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1314](https://github.com/stripe/stripe-android/pull/1314) Expose pinned API version
  via [Stripe.API_VERSION](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/-companion/index.html#com.stripe.android/Stripe.Companion/API_VERSION/#/PointingToDeclaration/)
* [#1315](https://github.com/stripe/stripe-android/pull/1315)
  Create [SourceParams#createCardParamsFromGooglePay()](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-source-params/-companion/create-card-params-from-google-pay.html)
* [#1316](https://github.com/stripe/stripe-android/pull/1316) Fix issue
  where `InvalidRequestException` is thrown when confirming a Setup Intent
  using [ConfirmSetupIntentParams#create()](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-confirm-setup-intent-params/-companion/create.html)

## 10.2.0 - 2019-08-05

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1275](https://github.com/stripe/stripe-android/pull/1275) Add support for
  launching `PaymentSession` from a `Fragment`
    * [PaymentSession(Fragment)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-payment-session/index.html#com.stripe.android/PaymentSession/<init>/#androidx.fragment.app.Fragment#com.stripe.android.PaymentSessionConfig/PointingToDeclaration/)
* [#1288](https://github.com/stripe/stripe-android/pull/1288) Upgrade Android Gradle Plugin
  to `3.5.0-rc02`
* [#1289](https://github.com/stripe/stripe-android/pull/1289) Add support for launching payment
  confirmation/authentication flow from a `Fragment`
    * [Stripe#confirmPayment(Fragment, ConfirmPaymentIntentParams)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-payment.html)
    * [Stripe#authenticatePayment(Fragment, String)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-payment.html)
    * [Stripe#confirmSetupIntent(Fragment, ConfirmSetupIntentParams)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-setup-intent.html)
    * [Stripe#authenticateSetup(Fragment, String)](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-setup.html)
* [#1290](https://github.com/stripe/stripe-android/pull/1290)
  Convert [samplestore app](https://github.com/stripe/stripe-android/tree/master/samplestore) to
  Kotlin
* [#1297](https://github.com/stripe/stripe-android/pull/1297)
  Convert [example app](https://github.com/stripe/stripe-android/tree/master/example) to Kotlin
* [#1300](https://github.com/stripe/stripe-android/pull/1300)
  Rename `StripeIntentResult#getStatus()`
  to [StripeIntentResult#getOutcome()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe-intent-result/index.html#com.stripe.android/StripeIntentResult/outcome/#/PointingToDeclaration/)
* [#1302](https://github.com/stripe/stripe-android/pull/1302)
  Add [GooglePayConfig#getTokenizationSpecification()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-google-pay-config/index.html#com.stripe.android/GooglePayConfig/tokenizationSpecification/#/PointingToDeclaration/)
  to configure Google Pay to use Stripe as the gateway
* [#1304](https://github.com/stripe/stripe-android/pull/1304)
  Add [PaymentMethodCreateParams#createFromGooglePay()](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-payment-method-create-params/-companion/create-from-google-pay.html)
  to create `PaymentMethodCreateParams` from a Google
  Pay [PaymentData](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentData)
  object

## 10.1.1 - 2019-07-31

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1275](https://github.com/stripe/stripe-android/pull/1275) Fix `StripeIntentResult.Status` logic
* [#1276](https://github.com/stripe/stripe-android/pull/1276) Update Stripe 3DS2 library to `v1.1.5`
    * Fix crash in 3DS2 challenge flow when [conscrypt](https://github.com/google/conscrypt) is
      installed
* [#1277](https://github.com/stripe/stripe-android/pull/1277) Fix StrictMode failure
  in `StripeFireAndForgetRequestExecutor`

## 10.1.0 - 2019-07-30

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1244](https://github.com/stripe/stripe-android/pull/1244) Add support for Stripe Connect in 3DS2
* [#1256](https://github.com/stripe/stripe-android/pull/1256) Add `StripeIntentResult.Status` flag
  to 3DS1 authentication results
* [#1257](https://github.com/stripe/stripe-android/pull/1257) Correctly pass `Status.FAILED` when
  3DS2 auth fails
* [#1259](https://github.com/stripe/stripe-android/pull/1259)
  Add `StripeIntentResult.Status.TIMEDOUT` value
* [#1263](https://github.com/stripe/stripe-android/pull/1263)
  Update `PaymentSession#presentPaymentMethodSelection()` logic
    * Add overloaded `presentPaymentMethodSelection()` that takes a Payment Method ID to initially
      select
    * Add `PaymentSessionPrefs` to persist customer's Payment Method selection
      across `PaymentSession` instances
* [#1264](https://github.com/stripe/stripe-android/pull/1264) Update `Stripe3ds2UiCustomization`
    * Add `Stripe3ds2UiCustomization.Builder.createWithAppTheme(Activity)` to create
      a `Stripe3ds2UiCustomization.Builder` based on the app theme

## 10.0.3 - 2019-07-24

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* Update Stripe 3DS2 library to `v1.1.2`
    * Disable R8
      following [user-reported issue](https://github.com/stripe/stripe-android/issues/1236)
    * Update Proguard rules

## 10.0.2 - 2019-07-23

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1238](https://github.com/stripe/stripe-android/pull/1238) Update Proguard rules to fix
  integration issues with Stripe 3DS2 library

## 10.0.1 - 2019-07-22

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* [#1226](https://github.com/stripe/stripe-android/pull/1226) Prevent non-critical network requests
  from blocking API requests by moving fire-and-forget requests to separate thread
* [#1228](https://github.com/stripe/stripe-android/pull/1228) Update to Android Gradle Plugin to
  3.5.0-rc01
* Update Stripe 3DS2 library to `v1.1.0`
    * Re-enable R8 following bug fix in Android Studio 3.5
    * Move `org.ow2.asm:asm` dependency to `testImplementation`
    * Fix known issue with 3DS2 challenge flow and API 19 devices

## 10.0.0 - 2019-07-19

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [10.5.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#1050---2020-05-15)
or greater.*

* Add support for 3DS2 authentication through the Payment Intents API and Setup Intents API.
  See [Supporting 3D Secure Authentication on Android](https://stripe.com/docs/mobile/android/authentication)
  .
    * Payment Intents - see our guide
      for [Using Payment Intents on Android](https://stripe.com/docs/payments/payment-intents/android)
        * [Stripe#confirmPayment()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-payment.html)
          for automatic confirmation
        * [Stripe#authenticatePayment()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-payment.html)
          for manual confirmation
    * Setup Intents - see our guide
      for [Saving card details without a payment](https://stripe.com/docs/payments/cards/saving-cards#saving-card-without-payment)
        * [Stripe#confirmSetupIntent()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/confirm-setup-intent.html)
          for automatic confirmation
        * [Stripe#authenticateSetup()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/authenticate-setup.html)
          for manual confirmation
    * [PaymentAuthConfig](https://stripe.dev/stripe-android/stripe/com.stripe.android/-payment-auth-config/index.html)
      for optional authentication customization
* Add support for the Setup Intents API.
  See [Saving card details without a payment](https://stripe.com/docs/payments/cards/saving-cards#saving-card-without-payment)
  .
    *
    Use [ConfirmSetupIntentParams](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-confirm-setup-intent-params/index.html)
    to confirm a SetupIntent
* [#1172](https://github.com/stripe/stripe-android/pull/1172) Refactor `PaymentIntentParams`
* [#1173](https://github.com/stripe/stripe-android/pull/1173) Inline all `StringDef` values

## 9.4.0 - 2020-05-15

* Update 3DS2 SDK to v1.3.1
* Update dependencies
* Add `advancedFraudSignalsEnabled` property
    *
    See [Advanced fraud detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection)
    for more details
* Include sources

## 9.3.8 - 2019-07-16

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1193](https://github.com/stripe/stripe-android/pull/1193) Fix `RuntimeException` related to 3DS2

## 9.3.7 - 2019-07-15

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1154](https://github.com/stripe/stripe-android/pull/1154) Fix `NullPointerException`
  in `PaymentMethodsActivity`
* [#1174](https://github.com/stripe/stripe-android/pull/1174) Add `getCardBuilder()` method to Card
  widgets
* [#1184](https://github.com/stripe/stripe-android/pull/1184) Fix `NullPointerException` related to
  3DS2

## 9.3.6 - 2019-07-08

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1148](https://github.com/stripe/stripe-android/pull/1148) Fix 3DS2 dependency Proguard issues

## 9.3.5 - 2019-06-20

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1138](https://github.com/stripe/stripe-android/pull/1138) Fix `AppInfo` param name

## 9.3.4 - 2019-06-19

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1133](https://github.com/stripe/stripe-android/pull/1133) Make `AppInfo` public

## 9.3.3 - 2019-06-19

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1108](https://github.com/stripe/stripe-android/pull/1108) Create `Card#toBuilder()`
* [#1114](https://github.com/stripe/stripe-android/pull/1114) Update `CustomerSession` methods to
  take `@NonNull` listener argument
* [#1125](https://github.com/stripe/stripe-android/pull/1125) Create `StripeIntent` interface and
  move `PaymentIntent.Status` and `PaymentIntent.NextActionType` to `StripeIntent`

## 9.3.2 - 2019-06-12

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1104](https://github.com/stripe/stripe-android/pull/1104) Handle null response body

## 9.3.1 - 2019-06-12

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1099](https://github.com/stripe/stripe-android/pull/1099) Fix Gradle module issue

## 9.3.0 - 2019-06-12

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1019](https://github.com/stripe/stripe-android/pull/1019)
  Introduce `CustomerSession#detachPaymentMethod()`
* [#1067](https://github.com/stripe/stripe-android/pull/1067) Remove `PaymentResultListener`.
  Replace `PaymentSession#completePayment()` with `PaymentSession#onCompleted()`.
* [#1075](https://github.com/stripe/stripe-android/pull/1075) Make model classes final
* [#1080](https://github.com/stripe/stripe-android/pull/1080) Update Build Tools to 29.0.0
* [#1082](https://github.com/stripe/stripe-android/pull/1082) Remove `StripeJsonModel#toJson()`
* [#1084](https://github.com/stripe/stripe-android/pull/1084) Rename `StripeJsonModel`
  to `StripeModel`
* [#1093](https://github.com/stripe/stripe-android/pull/1093) Add `Stripe#setAppInfo()`.
  See [Identifying your plug-in or library](https://stripe.com/docs/building-plugins#setappinfo) for
  more details.

## 9.2.0 - 2019-06-04

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1019](https://github.com/stripe/stripe-android/pull/1019) Upgrade pinned API version
  to `2019-05-16`
* [#1036](https://github.com/stripe/stripe-android/pull/1036) Validate API key before every request
* [#1046](https://github.com/stripe/stripe-android/pull/1046) Make `Card` model fields immutable

## 9.1.1 - 2019-05-28

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#1006](https://github.com/stripe/stripe-android/pull/1006) Remove null values
  in `PaymentMethod.BillingDetails#toMap()`

## 9.1.0 - 2019-05-28

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#952](https://github.com/stripe/stripe-android/pull/952) Update standard integration UI to use
  PaymentMethods instead of Sources
* [#962](https://github.com/stripe/stripe-android/pull/962) Add initial dark theme support to
  widgets
* [#963](https://github.com/stripe/stripe-android/pull/963) Add Autofill hints to forms
* [#964](https://github.com/stripe/stripe-android/pull/964) Upgrade Android Gradle Plugin to 3.4.1
* [#972](https://github.com/stripe/stripe-android/pull/964) Add PaymentIntent#getNextActionType()
* [#989](https://github.com/stripe/stripe-android/pull/989) Fix StripeEditText's error color logic
* [#1001](https://github.com/stripe/stripe-android/pull/1001)
  Overload `PaymentSession#presentPaymentMethodSelection` to allow requiring postal field

## 9.0.1 - 2019-05-17

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#945](https://github.com/stripe/stripe-android/pull/945) Add `business_type` param to Account
  tokenization when available

## 9.0.0 - 2019-05-06

Note: this release has breaking changes.
See [MIGRATING.md](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md)

*This version of the SDK is not compliant with Google Play's Prominent Disclosure & Consent
Requirements. The non-compliant code was unused and has been removed.*
*Please upgrade to
version [9.4.0](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md#940---2020-05-15)
or greater.*

* [#873](https://github.com/stripe/stripe-android/pull/873) Update pinned API version
  to `2019-03-14`
* [#875](https://github.com/stripe/stripe-android/pull/875) Inject `Context` in `CustomerSession`
  and `StripeApiHandler`
* [#907](https://github.com/stripe/stripe-android/pull/907) Update `PaymentIntent` model for
  API `2019-02-11`
* [#894](https://github.com/stripe/stripe-android/pull/894) Upgrade Android Gradle Plugin to 3.4.0
* [#872](https://github.com/stripe/stripe-android/pull/872)
  Create `CustomerSession.ActivityCustomerRetrievalListener` to handle `Activity` weak refs
* [#935](https://github.com/stripe/stripe-android/pull/935) Increase `minSdkVersion` from 14 to 19

## 8.7.0 - 2019-04-12

* [#863](https://github.com/stripe/stripe-android/pull/863) Fix garbage-collection issues
  with `Stripe` callbacks
* [#856](https://github.com/stripe/stripe-android/pull/856) Fix race-conditions
  with `CustomerSession` listeners
* [#857](https://github.com/stripe/stripe-android/pull/857) Correctly parse JSON when creating or
  deleting customer card source
* [#858](https://github.com/stripe/stripe-android/pull/858) Fix crash on some devices (e.g. Meizu)
  related to `TextInputEditText`
* [#862](https://github.com/stripe/stripe-android/pull/862) Improve `PaymentMethodCreateParams.Card`
  creation
* [#870](https://github.com/stripe/stripe-android/pull/870) Update `PaymentIntent#Status` enum
* [#865](https://github.com/stripe/stripe-android/pull/865) Fix some memory leak issues in example
  app

## 8.6.0 - 2019-04-05

* [#849](https://github.com/stripe/stripe-android/pull/849) Downgrade from AndroidX to Android
  Support Library
* [#843](https://github.com/stripe/stripe-android/pull/843) Add setter for custom CVC field label
  on `CardMultilineWidget`
* [#850](https://github.com/stripe/stripe-android/pull/850) Fix a11y traversal order
  in `CardInputWidget`
* [#839](https://github.com/stripe/stripe-android/pull/839) Refactor `StripeApiHandler` to use
  instance methods

## 8.5.0 - 2019-03-05

* [#805](https://github.com/stripe/stripe-android/pull/805) Clean up `PaymentIntent`
* [#806](https://github.com/stripe/stripe-android/pull/806) Pass `StripeError` in onError
* [#807](https://github.com/stripe/stripe-android/pull/807) Create `ErrorMessageTranslator` and
  default implementation
* [#809](https://github.com/stripe/stripe-android/pull/809) Make `StripeSourceTypeModel` public
* [#817](https://github.com/stripe/stripe-android/pull/817) Fix TalkBack crash in `CardInputWidget`
* [#822](https://github.com/stripe/stripe-android/pull/822) Fix account token failure on latest API
  version
* [#827](https://github.com/stripe/stripe-android/pull/827) Upgrade Android Gradle Plugin to 3.3.2
* [#828](https://github.com/stripe/stripe-android/pull/828) Support save to customer param
  on `PaymentIntent` confirm
* [#829](https://github.com/stripe/stripe-android/pull/829) Add `"not_required"` as possible value
  of `Source` `redirect[status]`
* [#830](https://github.com/stripe/stripe-android/pull/830) Add `"recommended"` as possible value
  of `Source` `card[three_d_secure]`
* [#832](https://github.com/stripe/stripe-android/pull/832) Pin all Stripe requests to API
  version `2017-06-05`

## 8.4.0 - 2019-02-06

* [#793](https://github.com/stripe/stripe-android/pull/793) Add StripeError field to StripeException
* [#787](https://github.com/stripe/stripe-android/pull/787) Add support for creating a CVC update
  Token
* [#791](https://github.com/stripe/stripe-android/pull/791) Prevent AddSourceActivity's
  SourceCallback from being garbage collected
* [#790](https://github.com/stripe/stripe-android/pull/790) Fix IME action logic on CVC field in
  CardMultilineWidget
* [#786](https://github.com/stripe/stripe-android/pull/786) Add metadata field to Card

## 8.3.0 - 2019-01-25

* [#780](https://github.com/stripe/stripe-android/pull/780) Fix bug related to ephemeral keys
  introduced in 8.2.0
* [#778](https://github.com/stripe/stripe-android/pull/778) Migrate to Android X
* [#771](https://github.com/stripe/stripe-android/pull/771) Add errorCode and errorDeclineCode
  fields to InvalidRequestException
* [#766](https://github.com/stripe/stripe-android/pull/766) Upgrade to Android Gradle Plugin 3.3.0
* [#770](https://github.com/stripe/stripe-android/pull/770) Remove Saudi Arabia from the no postal
  code countries list

## 8.2.0 - 2019-01-10

* [#675](https://github.com/stripe/stripe-android/pull/675) Add support for Android 28
    * Update Android SDK Build Tools to 28.0.3
    * Update Android Gradle plugin to 3.2.1
    * Update compileSdkVersion and targetSdkVersion to 28
    * Update Android Support Library to 28.0.0
* [#671](https://github.com/stripe/stripe-android/pull/671) Add ability to set card number and
  validate on `CardMultilineWidget`
* [#537](https://github.com/stripe/stripe-android/pull/537) Add support for iDeal Source with
  connected account
* [#669](https://github.com/stripe/stripe-android/pull/669) Add support for Portuguese
* [#624](https://github.com/stripe/stripe-android/pull/624) Add ability to cancel callbacks without
  destroying `CustomerSession` instance
* [#673](https://github.com/stripe/stripe-android/pull/673) Improve accessibility on card input
  fields
* [#656](https://github.com/stripe/stripe-android/pull/656) Add `AccessibilityNodeInfo` to widgets
* [#678](https://github.com/stripe/stripe-android/pull/678) Fix crash in `ShippingMethodView` on API
  16

## 8.1.0 - 2018-11-13

* Add support for latest version of PaymentIntents
* Fix NPEs in `CountryAutocompleteTextView`
* Fix NPEs in `PaymentContext` experiences
* Helper method for converting tokens to sources
* Fix bug with Canadian and UK postal code validation

## 8.0.0 - 2018-7-13

* [#609](https://github.com/stripe/stripe-android/pull/609) **BREAKING** Renamed PaymentIntentParams
  methods for readibility

## 7.2.0 - 2018-07-12

* Add beta support for PaymentIntents for usage with card sources []
* Add sample integration with PaymentIntents
* Fix crash in MaskedCardAdapter
* [#589](https://github.com/stripe/stripe-android/pull/589) **BREAKING** Add `preferredLanguage`
  parameter to `SourceParams.createBancontactParams`

## 7.1.0 - 2018-06-11

* [#583](https://github.com/stripe/stripe-android/pull/583) Add EPS and Multibanco support
  to `SourceParams`
* [#586](https://github.com/stripe/stripe-android/pull/586) Add `RequiredBillingAddressFields.NAME`
  option to enumeration
* [#583](https://github.com/stripe/stripe-android/pull/583) **BREAKING** Fix `@Nullable`
  and `@NonNull` annotations for `createP24Params` function

## 7.0.1 - 2018-05-25

* Make iDEAL params match API - `name` is optional and optional ideal `bank`
  and `statement_descriptor` can be set independently

## 7.0.0 - 2018-04-25

* [#559](https://github.com/stripe/stripe-android/pull/559) Remove Bitcoin source support. See
  MIGRATING.md.
* [#549](https://github.com/stripe/stripe-android/pull/549) Add create Masterpass source support
* [#548](https://github.com/stripe/stripe-android/pull/548) Add support for 3 digit American Express
  CVC
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
* Fixing a bug for PaymentFlowActivity not returning success when collecting shipping info without
  method

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
