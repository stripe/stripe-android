# CHANGELOG

## 13.2.0 - unreleased
* [#2112](https://github.com/stripe/stripe-android/pull/2112) Enable adding Mandate to confirm params

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
* [#1314](https://github.com/stripe/stripe-android/pull/1314) Expose pinned API version via [Stripe.API_VERSION](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#API_VERSION)
* [#1315](https://github.com/stripe/stripe-android/pull/1315) Create [SourceParams#createCardParamsFromGooglePay()](https://stripe.dev/stripe-android/com/stripe/android/model/SourceParams.html#createCardParamsFromGooglePay-org.json.JSONObject-)
* [#1316](https://github.com/stripe/stripe-android/pull/1316) Fix issue where `InvalidRequestException` is thrown when confirming a Setup Intent using [ConfirmSetupIntentParams#create(PaymentMethodCreateParams, String)](https://stripe.dev/stripe-android/com/stripe/android/model/ConfirmSetupIntentParams.html#create-com.stripe.android.model.PaymentMethodCreateParams-java.lang.String-)

## 10.2.0 - 2019-08-05
* [#1275](https://github.com/stripe/stripe-android/pull/1275) Add support for launching `PaymentSession` from a `Fragment`
    * [PaymentSession(Fragment)](https://stripe.dev/stripe-android/com/stripe/android/PaymentSession.html#PaymentSession-android.support.v4.app.Fragment-)
* [#1288](https://github.com/stripe/stripe-android/pull/1288) Upgrade Android Gradle Plugin to `3.5.0-rc02`
* [#1289](https://github.com/stripe/stripe-android/pull/1289) Add support for launching payment confirmation/authentication flow from a `Fragment`
    * [Stripe#confirmPayment(Fragment, ConfirmPaymentIntentParams)](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#confirmPayment-android.support.v4.app.Fragment-com.stripe.android.model.ConfirmPaymentIntentParams-)
    * [Stripe#authenticatePayment(Fragment, String)](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#authenticatePayment-android.support.v4.app.Fragment-java.lang.String-)
    * [Stripe#confirmSetupIntent(Fragment, ConfirmSetupIntentParams)](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#confirmSetupIntent-android.support.v4.app.Fragment-com.stripe.android.model.ConfirmSetupIntentParams-)
    * [Stripe#authenticateSetup(Fragment, String)](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#authenticateSetup-android.support.v4.app.Fragment-java.lang.String-)
* [#1290](https://github.com/stripe/stripe-android/pull/1290) Convert [samplestore app](https://github.com/stripe/stripe-android/tree/master/samplestore) to Kotlin
* [#1297](https://github.com/stripe/stripe-android/pull/1297) Convert [example app](https://github.com/stripe/stripe-android/tree/master/example) to Kotlin
* [#1300](https://github.com/stripe/stripe-android/pull/1300) Rename [StripeIntentResult#getStatus()](https://stripe.dev/stripe-android/com/stripe/android/StripeIntentResult.html#getStatus--) to [StripeIntentResult#getOutcome()](https://stripe.dev/stripe-android/com/stripe/android/StripeIntentResult.html#getOutcome--)
* [#1302](https://github.com/stripe/stripe-android/pull/1302) Add [GooglePayConfig#getTokenizationSpecification()](https://stripe.dev/stripe-android/com/stripe/android/GooglePayConfig.html#getTokenizationSpecification--) to configure Google Pay to use Stripe as the gateway
* [#1304](https://github.com/stripe/stripe-android/pull/1304) Add [PaymentMethodCreateParams#createFromGooglePay()](https://stripe.dev/stripe-android/com/stripe/android/model/PaymentMethodCreateParams.html#createFromGooglePay-org.json.JSONObject-) to create `PaymentMethodCreateParams` from a Google Pay [PaymentData](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentData) object

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
