# Stripe Examples App

**Contents**
1. [Setup](#setup)
2. [Examples](#examples)
   - [Google Pay](#google-pay)

## Setup

### Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. After [deploying the example backend to Heroku](#deploy-the-example-backend-to-heroku) and [configuring the app](#configure-the-app), build and run the project.

<img width="215" height="108" src="https://raw.githubusercontent.com/stripe/stripe-android/master/example/images/run.png" />

### Deploy the example backend to Heroku
1. [Create a Heroku account](https://signup.heroku.com/) if you don't have one.
2. Navigate to the [example mobile backend repo](https://github.com/stripe/example-mobile-backend/tree/v18.1.0)
   and click "Deploy to Heroku".
3. Set an _App Name_ of your choice (e.g. Stripe Example Mobile Backend).
4. Under _Config Vars_, set your [Stripe testmode secret key](https://dashboard.stripe.com/test/apikeys)
   for the `STRIPE_TEST_SECRET_KEY` field.
5. Click "Deploy for Free".

<img width="700" height="793" src="https://raw.githubusercontent.com/stripe/stripe-android/master/example/images/heroku.png" />

### Configure the app
1. If it doesn't exist, create a `gradle.properties` in a location defined in the
   [Gradle Build Environment docs](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
   For example, the default location on macOS is `~/.gradle/gradle.properties`.
2. Append the following entries to `gradle.properties`.

```
# Set to example backend deployed to Heroku
STRIPE_EXAMPLE_BACKEND_URL=https://my-backend.herokuapp.com/

# Set to a test publishable key from https://dashboard.stripe.com/test/apikeys
STRIPE_EXAMPLE_PUBLISHABLE_KEY=pk_test_mykey

# Optionally, set to a Connect Account id to test Connect
STRIPE_ACCOUNT_ID=
```

## Examples

### Google Pay

#### Source
[PayWithGoogleActivity.kt](https://github.com/stripe/stripe-android/blob/master/example/src/main/java/com/stripe/example/activity/PayWithGoogleActivity.kt)

#### Overview
1. Check that Google Pay is available and ready in `isReadyToPay()`.
2. Create a [Google Pay PaymentDataRequest](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentDataRequest)
   in `createGooglePayRequest()`.
    - Optionally, require Billing Address with `isBillingAddressRequired`,
      Phone Number with `isPhoneNumberRequired`,
      and Email with `isEmailRequired`.
3. Display Google Pay sheet in `payWithGoogle()`.
4. After user selects a payment method, `Activity#onActivityResult()` is called.
   Handle result in `handleGooglePayResult()`.
5. Create a [PaymentMethodCreateParams](https://stripe.dev/stripe-android/com/stripe/android/model/PaymentMethodCreateParams.html)
   object from the [Google Pay PaymentData](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentData) object using
   [PaymentMethodCreateParams.createFromGooglePay()](https://stripe.dev/stripe-android/com/stripe/android/model/PaymentMethodCreateParams.html#createFromGooglePay-org.json.JSONObject-).

   ```
   val paymentData = PaymentData.getFromIntent(data) ?: return

   val paymentMethodCreateParams = PaymentMethodCreateParams.createFromGooglePay(
       JSONObject(paymentData.toJson())
   )
   ```

6. Create a [Stripe Payment Method object](https://stripe.com/docs/payments/payment-methods)
   with the `PaymentMethodCreateParams` object using
   [Stripe#createPaymentMethod()](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#createPaymentMethod-com.stripe.android.model.PaymentMethodCreateParams-com.stripe.android.ApiResultCallback-).

   ```
   stripe.createPaymentMethod(paymentMethodCreateParams,
       object : ApiResultCallback<PaymentMethod> {
           override fun onSuccess(paymentMethod: PaymentMethod) {
               // do something with paymentMethod
           }

           override fun onError(e: Exception) {
               // handle error
           }
        })
    }
    ```
