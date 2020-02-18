# Migration Guide

## Migrating from versions < 14.0.0
- Changes to Stripe Error localization
    - All [Stripe Error messages](https://stripe.com/docs/api/errors#errors-message) are now localized
      based on the device locale.
      
      For example, when retrieving a SetupIntent with a nonexistent `id`
      when the device locale is set to `Locale.JAPAN`, the error message will now be localized.
      ```
      // before - English
      "No such setupintent: seti_invalid123"

      // after - Japanese
      "そのような setupintent はありません : seti_invalid123"
      ```
- Changes to [Material Components](https://material.io/develop/android/docs/getting-started/)
    - `com.google.android.material:material` has been updated to `v1.1.0`
- Changes to `PaymentSession`
    - When instantiating a `PaymentSession()` with an `Activity`, it must now be a `ComponentActivity`
      (e.g. `AppCompatActivity` or `FragmentActivity`)
    - `PaymentSession#init()` no longer takes a `savedInstanceState` argument
      ```kotlin
      // before
      paymentSession.init(listener, savedInstancedState)

      // after
      paymentSession.init(listener)
      ```
    - `PaymentSession#savePaymentSessionInstanceState()` has been removed; state management is handled
      by `PaymentSession`
    - `PaymentSession#onDestroy()` has been removed; the `listener` instance is now released
      automatically when the host `Activity` or `Fragment` is destroyed
    - Remove `PaymentSessionData.paymentSessionData`; this data will be returned via
      `PaymentSessionListener#onPaymentSessionDataChanged()
- Changes to `StripeSourceTypeModel`
    - Rename to `SourceTypeModel` and make a sealed class
    - Move `SourceCardData` subclass to `SourceTypeModel.Card`
    - Move `SourceSepaDebitData` subclass to `SourceTypeModel.SepaDebit`
    - Change type of `Source#sourceTypeModel` to `SourceTypeModel?`
- Changes to `Card.FundingType`
    - Rename to `CardFunding` and convert to an `enum`
- Changes to `StripeException` subclasses
    - `InvalidRequestException` is now `final`
    - Remove `InvalidRequestException`'s `param`, `errorCode`, and `errorDeclineCode` properties;
      these can be accessed via the `stripeError` property
      ```kotlin
      // before
      invalidRequestException.param
      invalidRequestException.errorCode
      invalidRequestException.errorDeclineCode

      // after
      invalidRequestException.stripeError?.param
      invalidRequestException.stripeError?.code
      invalidRequestException.stripeError?.declineCode
      ```
    - `RateLimitException` now extends `StripeException`
- Update 3DS2 styles
    - Create `BaseStripe3DS2TextInputLayout` that extends `Widget.MaterialComponents.TextInputLayout.OutlinedBox`
    - Create `Stripe3DS2TextInputLayout` that extends `BaseStripe3DS2TextInputLayout`
    - Apply `Stripe3DS2TextInputLayout` to `TextInputLayout`
    - Create `BaseStripe3DS2EditText` with parent `Widget.MaterialComponents.TextInputEditText.OutlinedBox`
    - Rename `Stripe3DS2EditTextTheme` to `Stripe3DS2EditText` and change its parent to `BaseStripe3DS2EditText`
    - Apply `Stripe3DS2EditText` to `TextInputEditText`

## Migrating from versions < 13.0.0
- Changes to `Card` and `SourceCardData`
    - Change the type of `brand` property from `String?` to `CardBrand`
    - Remove `Card.CardBrand`
    - Change the type of `tokenizationMethod` property from `String?` to `TokenizationMethod?`
      ```kotlin
      // before
      card.tokenizationMethod == "google_pay"

      // after
      card.tokenizationMethod == TokenizationMethod.GooglePay
      ```
- Changes to `PaymentMethod`
    - Change the type of `type` property from `String?` to `PaymentMethod.Type?`
      ```kotlin
      // before
      paymentMethod.type == "card"

      // after
      paymentMethod.type == PaymentMethod.Type.Card
      ```
- Changes to `SourceParams`
    - `setOwner()` now takes `OwnerParams` instead of `Map`
      ```kotlin
      // before
      sourceParams
          .setOwner(mapOf(
              "name" to "Jenny Rosen"
          ))

      // after
      sourceParams
          .setOwner(OwnerParams(
              name = "Jenny Rosen"
          ))
      ```
    - `setRedirect()` has been removed; use `setReturnUrl()` instead
      ```kotlin
      // before
      sourceParams
          .setRedirect(mapOf("return_url" to "myapp://return"))

      // after
      sourceParams
          .setReturnUrl("myapp://return")
      ```
- Changes to `AccountParams`
    - `AccountParams.createAccountParams()` is now deprecated; use the appropriate `AccountParams.create()` method.
      ```kotlin
      // before
      AccountParams.createAccountParams(
          true,
          AccountParams.BusinessType.Individual,
          mapOf(
              "address" to mapOf(
                  "line1" to "123 Market St",
                  "line2" to "#345",
                  "city" to "San Francisco",
                  "state" to "CA",
                  "postal_code" to "94107",
                  "country" to "US"
              ),
              "ssn_last_4" to "1234",
              "first_name" to "Jenny",
              "last_name" to "Rosen"
          )
      )

      // after
      AccountParams.create(
          true,
          AccountParams.BusinessTypeParams.Individual(
              address = Address(
                  line1 = "123 Market St",
                  line2 = "#345",
                  city = "San Francisco",
                  state = "CA",
                  postal_code = "94107",
                  country = "US"
              ),
              ssnLast4 = "1234",
              firstName = "Jenny",
              lastName = "Rosen"
          )
      )
      ```
- Changes to `StripeEditText`
    - `StripeEditText` now extends `TextInputEditText` instead of `AppCompatEditText`
- Changes to `CardInputWidget`
    - The postal code field is now displayed by default
    - The postal code input is not validated
    - `StripeEditText` subclasses (e.g. `CardNumberEditText`) used in this widget are now wrapped in `TextInputLayout`
- Changes to `CardMultilineWidget`
    - The postal code field is now displayed by default
    - The postal code field is now optional when displayed
    - The postal code input is not validated
    - `CardInputListener.onPostalCodeComplete()` has been removed
- Changes to `Stripe`
    - Bindings for API POST methods now take an optional `idempotencyKey`.
      Read about [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests) for more details.
      ```kotlin
      // before
      stripe.createPaymentMethod(
            paymentMethodCreateParams,
            callback
      )

      // after - without idempotency key
      stripe.createPaymentMethod(
            paymentMethodCreateParams,
            callback = callback
      )

      // after - with idempotency key
      stripe.createPaymentMethod(
            paymentMethodCreateParams,
            idempotencyKey = idempotencyKey,
            callback = callback
      )
      ```
- Changes to `PaymentMethodsActivityStarter.Result`
    - The type of `paymentMethod` has been changed from `PaymentMethod` to `PaymentMethod?` (i.e. it is nullable)
- Changes to `PaymentSession`
    - `PaymentSession` now takes the `PaymentSessionConfig` instance through its constructor, instead of `init()`
      ```kotlin
      // before
      val paymentSession = PaymentSession(activity)
      paymentSession.init(listener, paymentSessionConfig)

      // after
      val paymentSession = PaymentSession(activity, paymentSessionConfig)
      paymentSession.init(listener)
      ```
    - By default, users will be asked for their postal code (i.e. `BillingAddressFields.PostalCode`) when adding a new
      card payment method.
    - `PaymentSession#init()` no longer takes a `shouldPrefetchCustomer` argument.
      If you need to specify `shouldPrefetchCustomer`, use `PaymentSessionConfig.Builder.setShouldPrefetchCustomer()`.
    - `PaymentSession#presentPaymentMethodSelection(shouldRequirePostalCode, userSelectedPaymentMethodId)` is deleted.
      If you need to specify billing details, use `PaymentSessionConfig.Builder#setBillingAddressFields()`.
    - `setShouldRequirePostalCode()` has been removed from `AddPaymentMethodActivityStarter.Args` and
      `PaymentMethodsActivityStarter.Args`. Use `setBillingAddressFields()` instead.
    - Shipping information validation and shipping methods creation logic when using `PaymentSession` has been improved.
      Previously, this was accomplished by creating a `BroadcastReceiver` and registering it for a
      particular `Intent`. This has been greatly simplified by providing `ShippingInformationValidator`
      and `ShippingMethodsFactory` interfaces that can be defined when creating the `PaymentSessionConfig`.
      See below for an example.
    
      - **Before**, using `BroadcastReceiver`
        ```kotlin
        class PaymentSessionActivity : AppCompatActivity() {
            private lateinit var broadcastReceiver: BroadcastReceiver

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                val localBroadcastManager = LocalBroadcastManager.getInstance(this)
                broadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val shippingInformation = intent
                            .getParcelableExtra<ShippingInformation>(EXTRA_SHIPPING_INFO_DATA)
                        val shippingInfoProcessedIntent = Intent(EVENT_SHIPPING_INFO_PROCESSED)
                        if (!isValidShippingInfo(shippingInformation)) {
                            shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false)
                        } else {
                            shippingInfoProcessedIntent
                                .putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true)
                                .putParcelableArrayListExtra(
                                    EXTRA_VALID_SHIPPING_METHODS, SHIPPING_METHODS
                                )
                                .putExtra(
                                    EXTRA_DEFAULT_SHIPPING_METHOD, SHIPPING_METHODS.last()
                                )
                        }
                        localBroadcastManager.sendBroadcast(shippingInfoProcessedIntent)
                    }

                    private fun isValidShippingInfo(shippingInfo: ShippingInformation?): Boolean {
                        return shippingInfo?.address?.country == Locale.US.country
                    }
                }
                localBroadcastManager.registerReceiver(broadcastReceiver,
                    IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED))
            }

            override fun onDestroy() {
                LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(broadcastReceiver)
                super.onDestroy()
            }

            private companion object {
                private val SHIPPING_METHODS = arrayListOf(
                    ShippingMethod("UPS Ground", "ups-ground",
                        0, "USD", "Arrives in 3-5 days"),
                    ShippingMethod("FedEx", "fedex",
                        599, "USD", "Arrives tomorrow")
                )
            }
        }
        ```

      - **After**, using `ShippingInformationValidator` and `ShippingMethodsFactory`
        ```kotlin
        class PaymentSessionActivity : AppCompatActivity() {
            private fun createPaymentSession(): PaymentSession {
                val customerSession = createCustomerSession()
                val paymentSession = PaymentSession(this)
                paymentSession.init(
                    listener = createPaymentSessionListener(),
                    paymentSessionConfig = createPaymentSessionConfig(),
                    savedInstanceState = savedInstanceState
                )
                return paymentSession
            }

            private fun createPaymentSessionConfig(): PaymentSessionConfig {
                PaymentSessionConfig.Builder()
                    .setShippingInformationValidator(ShippingInformationValidator())
                    .setShippingMethodsFactory(ShippingMethodsFactory())
                    .build()
            }

            private class ShippingInformationValidator :
                    PaymentSessionConfig.ShippingInformationValidator {
                override fun isValid(
                    shippingInformation: ShippingInformation
                ): Boolean {
                    return shippingInformation.address?.country == Locale.US.country
                }

                override fun getErrorMessage(
                    shippingInformation: ShippingInformation
                ): String {
                    return "The country must be US."
                }
            }

            private class ShippingMethodsFactory :
                    PaymentSessionConfig.ShippingMethodsFactory {
                override fun create(
                    shippingInformation: ShippingInformation
                ): List<ShippingMethod> {
                    return listOf(
                        ShippingMethod("UPS Ground", "ups-ground",
                            0, "USD", "Arrives in 3-5 days"),
                        ShippingMethod("FedEx", "fedex",
                            599, "USD", "Arrives tomorrow")
                    )
                }
            }
        }
        ```
    - Related to the above, `PaymentFlowExtras` is deleted

## Migrating from versions < 12.0.0
- Replace `Stripe#createTokenSynchronous(Card)` with `Stripe#createCardTokenSynchronous(Card)`
    ```java
    // Java

    // before
    stripe.createTokenSynchronous(card);

    // after
    stripe.createCardTokenSynchronous(card);
    ```

    ```kotlin
    // Kotlin

    // before
    stripe.createTokenSynchronous(card)

    // after
    stripe.createCardTokenSynchronous(card)
    ```

- Replace `Card#getCVC()` with `Card#getCvc()`
    ```java
    // Java

    // before
    card.getCVC();

    // after
    card.getCvc();
    ```

    ```kotlin
    // Kotlin

    // before
    card.getCVC()

    // after
    card.cvc
    ```

- Remove `AddPaymentMethodActivity#EXTRA_NEW_PAYMENT_METHOD`, use `AddPaymentMethodActivityStarter.Result.fromIntent()` instead
    ```java
    // Java

    // before
    private PaymentMethod getPaymentMethodFromIntent(@NonNull Intent intent) {
        return intent.getParcelableExtra<PaymentMethod>(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD);
    }

    // after
    private PaymentMethod getPaymentMethodFromIntent(@NonNull Intent intent) {
        final AddPaymentMethodActivityStarter.Result result =
            AddPaymentMethodActivityStarter.Result.fromIntent(intent)
        return result != null ? result.paymentMethod : null;
    }
    ```

    ```kotlin
    // Kotlin

    // before
    private fun getPaymentMethodFromIntent(intent: Intent): PaymentMethod? {
        return intent.getParcelableExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD)
    }

    // after
    private fun getPaymentMethodFromIntent(intent: Intent): PaymentMethod? {
        val result: AddPaymentMethodActivityStarter.Result? =
            AddPaymentMethodActivityStarter.Result.fromIntent(intent)
        return result?.paymentMethod
    }
    ```

- Create overloaded `ShippingMethod` constructor with optional `detail` argument
    ```java
    // Java

    // before
    ShippingMethod("FedEx", "fedex", null, 599, "USD");
    ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD");

    // after
    ShippingMethod("FedEx", "fedex", 599, "USD");
    ShippingMethod("FedEx", "fedex", 599, "USD", "Arrives tomorrow");
    ```

    ```kotlin
    // Kotlin

    // before
    ShippingMethod("FedEx", "fedex", null, 599, "USD")
    ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD")

    // after
    ShippingMethod("FedEx", "fedex", 599, "USD")
    ShippingMethod("FedEx", "fedex", 599, "USD", "Arrives tomorrow")
    ```
    
- Payment Intent requests (i.e. requests to `/v1/payment_intents`) now return localized error messages
    ```java
    // Java

    // before
    @Test
    public void testErrorMessage() {
        Locale.setDefault(Locale.GERMANY);

        final Stripe stripe = createStripe();
        final InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.retrievePaymentIntentSynchronous("invalid");
                    }
                });
        
        // Locale is Germany, error message is in English
        assertEquals(
                "No such payment_intent: invalid",
                exception.getStripeError().getMessage()
        );
    }

    // after
    @Test
    public void testErrorMessage() {
        Locale.setDefault(Locale.GERMANY);

        final Stripe stripe = createStripe();
        final InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.retrievePaymentIntentSynchronous("invalid");
                    }
                });

        // Locale is Germany, error message is in Germany
        assertEquals(
                "Keine solche payment_intent: invalid",
                exception.getStripeError().getMessage()
        );
    }
    ```

## Migrating from versions < 11.0.0
- [AndroidX](https://developer.android.com/jetpack/androidx) is required. Please read the [Migrating to AndroidX](https://developer.android.com/jetpack/androidx/migrate) guide for more information. See [#1478](https://github.com/stripe/stripe-android/pull/1478).
- The signatures of `PaymentConfiguration.init()` and `PaymentConfiguration.getInstance()` have been changed. They now both take a `Context` instance as the first argument. See [#1479](https://github.com/stripe/stripe-android/pull/1479).
    ```kotlin
    // before
    PaymentConfiguration.init("publishable_key")
    PaymentConfiguration.getInstance()

    // after
    PaymentConfiguration.init(context, "publishable_key")
    PaymentConfiguration.getInstance(context)
    ```
- Remove `Stripe` methods that accept a publishable key. Pass publishable key in the `Stripe` constructor.
    ```
    // Example

    // before
    val stripe = Stripe(context)
    stripe.createPaymentMethodSynchronous(params, "pk_test_demo123")

    // after
    val stripe = Stripe(context, "pk_test_demo123")
    stripe.createPaymentMethodSynchronous(params)
    ```
- `Source` is immutable. All setter methods have been removed. See [#1480](https://github.com/stripe/stripe-android/pull/1480).
- `TokenCallback` and `SourceCallback` have been removed. Use `ApiResultCallback<Token>` and `ApiResultCallback<Source>` instead. See [#1481](https://github.com/stripe/stripe-android/pull/1481).
- `StripeIntent#getStatus()` has been removed. Use `StripeIntent#getOutcome()` instead.
- `PaymentIntent#getSource()` has been removed. Use `PaymentIntent#getPaymentMethodId()` instead.
- `SetupIntent#getCustomerId()` has been removed. This method was unintentionally added and always returned `null`.
- The `samplestore` app has moved to [stripe-samples/sample-store-android](https://github.com/stripe-samples/sample-store-android).
- Remove `PaymentMethodsActivity.newIntent()`. Use `PaymentMethodsActivityStarter#startForResult()` to start `PaymentMethodsActivity`.
- Remove `PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT`. Use `PaymentMethodsActivityStarter.Result#fromIntent(intent)` to obtain the result of `PaymentMethodsActivity`.
- Remove `Stripe#createToken()` with `Executor` argument. Use [Stripe#createToken(Card, ApiResultCallback)](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#createToken-com.stripe.android.model.Card-com.stripe.android.ApiResultCallback-) instead.

## Migration from versions < 10.1.0
- You must call `PaymentConfiguration.init()` before calling `CustomerSession.initCustomerSession()`.
    ```java
    PaymentConfiguration.init(PUBLISHABLE_KEY);
    CustomerSession.initCustomerSession(context, ephemeralKeyProvider);
    ```

## Migration from versions < 10.0.0
- The signature of `Stripe#retrievePaymentIntentSynchronous()` has [changed](https://github.com/stripe/stripe-android/commit/5e56663c739ec694a6a393e96b651bd3d3c7a3e7#diff-26062503dd732750d15be3173b992de6R618). It now takes a [client_secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret) `String` instead of a `PaymentIntentParams` instance.
  See [#1172](https://github.com/stripe/stripe-android/pull/1172).
    ```java
    // before
    stripe.retrievePaymentIntentSynchronous(
          PaymentIntentParams.createRetrievePaymentIntentParams(clientSecret));

    // after
    stripe.retrievePaymentIntentSynchronous(clientSecret);
    ```

- `PaymentIntentParams` is now [`ConfirmPaymentIntentParams`](https://github.com/stripe/stripe-android/blob/master/stripe/src/main/java/com/stripe/android/model/ConfirmPaymentIntentParams.java) and its method names have been simplified.
  See [#1172](https://github.com/stripe/stripe-android/pull/1172).
    ```java
    // before
    PaymentIntentParams.createConfirmPaymentIntentWithPaymentMethodId(
          paymentMethodId, clientSecret, returnUrl);

    // after
    ConfirmPaymentIntentParams.createWithPaymentMethodId(paymentMethodId, clientSecret, returnUrl);
    ```

- All `@StringDef` constants have been inlined in their respective `@interface`. Below is an example from `Card.FundingType`.
  See [#1173](https://github.com/stripe/stripe-android/pull/1173).
    ```java
    // before
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            FUNDING_CREDIT,
            FUNDING_DEBIT,
            FUNDING_PREPAID,
            FUNDING_UNKNOWN
    })
    public @interface FundingType { }
    public static final String FUNDING_CREDIT = "credit";
    public static final String FUNDING_DEBIT = "debit";
    public static final String FUNDING_PREPAID = "prepaid";
    public static final String FUNDING_UNKNOWN = "unknown";

    // after
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            FundingType.CREDIT,
            FundingType.DEBIT,
            FundingType.PREPAID,
            FundingType.UNKNOWN
    })
    public @interface FundingType {
        String CREDIT = "credit";
        String DEBIT = "debit";
        String PREPAID = "prepaid";
        String UNKNOWN = "unknown";
    }
    ```

## Migration from versions < 9.3.3
- The enum `PaymentIntent.Status` is now `StripeIntent.Status`
- The enum `PaymentIntent.NextActionType` is now `StripeIntent.NextActionType`

These changes should not impact Java integrations. For Kotlin integrations, please change your reference as shown in the example below.

```kotlin
// before
PaymentIntent.Status.Succeeded

// after
StripeIntent.Status.Succeeded
```

## Migration from versions < 9.3.0
- `CustomerSession`'s Listener interfaces's `onError()` method now have a `@NonNull` `errorMessage` argument
    ```java
    // before
    new CustomerSession.CustomerRetrievalListener() {
        @Override
        public void onError(int errorCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {

        }
    }

    // after
    new CustomerSession.CustomerRetrievalListener() {
        @Override
        public void onError(int errorCode, @NonNull String errorMessage,
                            @Nullable StripeError stripeError) {

        }
    }
    ```

- `PaymentResultListener` has been removed
- `PaymentSession#completePayment()` has been replaced with `PaymentSession#onCompleted()`
    ```java
    // before
    private void chargePaymentMethod() {
        mPaymentSession.completePayment(new PaymentCompletionProvider() {
            @Override
            public void completePayment(@NonNull PaymentSessionData data,
                                        @NonNull PaymentResultListener listener) {
                // Make async request to your backend to charge the payment method.
                // Upon success, call:
                listener.onPaymentResult(PaymentResultListener.SUCCESS);
            }
        });
    }

    // after
    private void chargePaymentMethod() {
        // Make async request to your backend to charge the payment method.
        // Upon success, call:
        mPaymentSession.onCompleted();
    }
    ```

## Migration from versions < 9.2.0
- `Card` model is now immutable
  - `Card#getType()` is now `Card#getBrand()`

## Migration from versions < 9.1.0
- [Standard UI components](https://stripe.com/docs/mobile/android/standard) now use `PaymentMethod` instead of `Source`
    - Setting a customer's default payment method is not available for PaymentMethod objects
    - `CustomerSession#getPaymentMethods()` and `CustomerSession#attachPaymentMethod()` have been added
    - `PaymentSessionData#getSelectedPaymentMethodId()` is now `PaymentSessionData#getPaymentMethod()` and returns a `PaymentMethod`. See usage in the samplestore app's [PaymentActivity.java](https://github.com/stripe/stripe-android/blob/194fef4c1c1981b8423125f26abf4b23f12631f8/samplestore/src/main/java/com/stripe/samplestore/PaymentActivity.java).
- Remove the following unused methods from `PaymentConfiguration`
  - `getRequiredBillingAddressFields()`
  - `setRequiredBillingAddressFields()`
  - `getShouldUseSourcesForCards()`
  - `setShouldUseSourcesForCards()`

## Migration from versions < 9.0.0
- `minSdkVersion` is now 19

- `AccountParams.createAccountParams()` requires a `AccountParams#BusinessType` parameter
    ```java
    // before
    AccountParams.createAccountParams(
      true,
      createBusinessData()
    );
    
    // after, AccountParams.BusinessType is required
    
    // for Individual entities
    AccountParams.createAccountParams(
      true,
      BusinessType.Individual,
      createBusinessData()
    );
    
    // for Company entities
    AccountParams.createAccountParams(
      true,
      BusinessType.Company,
      createBusinessData()
    );
    ```

- `CustomerSession.initCustomerSession()` now has a `Context` parameter.
   Related, `CustomerSession` public instance methods no longer have a `Context` parameter.
   ```java
   // before
   CustomerSession.initCustomerSession(ephemeralKeyProvider);
   CustomerSession.getInstance().setCustomerShippingInformation(this, listener);

   // after
   CustomerSession.initCustomerSession(this, ephemeralKeyProvider);
   CustomerSession.getInstance().setCustomerShippingInformation(listener);
   ```

- [`PaymentIntent`](https://github.com/stripe/stripe-android/blob/master/stripe/src/main/java/com/stripe/android/model/PaymentIntent.java) has been updated to reflect [API version 2019-02-11](https://stripe.com/docs/upgrades#2019-02-11)
    - `PaymentIntent.Status.RequiresSource` is now `PaymentIntent.Status.RequiresPaymentMethod`
    - `PaymentIntent.Status.RequiresSourceAction` is now `PaymentIntent.Status.RequiresAction`
    - `PaymentIntent.NextActionType.AuthorizeWithUrl` has been removed
    - `PaymentIntent#getNextSourceAction()` is now `PaymentIntent#getNextAction()`
    - `PaymentIntent#getAuthorizationUrl()` is now `PaymentIntent#getRedirectUrl()`
    - `PaymentIntent#requiresAction()` has been added as a convenience
    - `PaymentIntent#getStatus()` now returns a `PaymentIntent.Status` enum value instead of a `String`

- [`Address`](https://github.com/stripe/stripe-android/blob/master/stripe/src/main/java/com/stripe/android/model/Address.java) is now immutable and its setters have been removed.
  Use `Address.Builder` to create a new `Address` object.

## Migrating from versions < 7.0.0
- Remove Bitcoin source support because Stripe [no longer processes Bitcoin payments](https://stripe.com/blog/ending-bitcoin-support)
    - Sources can no longer have a "BITCOIN" source type. These sources will now be interpreted as "UNKNOWN".
    - You can no longer `createBitcoinParams`. Please use a different payment method.

## Migrating from versions < 5.0.0
- `StripeApiHandler` methods can no longer be called directly.
- `PaymentConfiguration` now stores your public key and is depended upon for `CustomerSession`.
- Many *Utils* classes have been migrated to package-private access.

## Migrating from versions < 4.0.0

- Instantiation of a Stripe object can no longer throw an `AuthenticationException`.
    - Any time you were instantiating a Stripe object in a try/catch block will be simplified.
    ```java
    Stripe stripe;
    try {
       stripe = new Stripe(mContext, MY_PUBLISHABLE_KEY);
    } catch (AuthenticationException authEx) {
       // This never happens because you check your key.
    }
    ```
    now becomes
    ```java
    Stripe stripe = new Stripe(mContext, MY_PUBLISHABLE_KEY);
    ```
- `Stripe#setDefaultPublishableKey(String key)` has similarly been changed, and no longer needs to be wrapped.
- Both methods can still throw an `IllegalArgumentException` if an invalid key is used, but as a
runtime exception, that does not need to be wrapped.
- `AuthenticationException` will now only be thrown if you attempt to create a `Token` or `Source`
with an invalid key.
