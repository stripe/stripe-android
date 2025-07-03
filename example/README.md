# Stripe Examples App

**Contents**
1. [Setup](#setup)
2. [Examples](#examples)
   - [Google Pay](#google-pay)

## Setup

### Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. After [remixing the Glitch project](#remix-the-example-project-on-glitch) and [configuring the app](#configure-the-app), build and run the project.

<img width="215" height="108" src="https://raw.githubusercontent.com/stripe/stripe-android/master/example/images/run.png" />

### Remix the example project on Glitch
We provide an example backend hosted on Glitch, allowing you to easily test an integration end-to-end.
1. [Open the Glitch project](https://glitch.com/edit/#!/stripe-example-mobile-backend).
2. Click on "Remix", on the top right.
3. In your newly created project, open the `.env` file in the left sidebar.
4. Set your [Stripe testmode secret key](https://dashboard.stripe.com/test/apikeys) as the `STRIPE_TEST_SECRET_KEY` field.
5. Your backend implementation should now be running. You can see the logs by clicking on "Logs" in the bottom bar.

### Configure the app
1. If it doesn't exist, create a `gradle.properties` in a location defined in the
   [Gradle Build Environment docs](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
   For example, the default location on macOS is `~/.gradle/gradle.properties`.
2. Append the following entries to `gradle.properties`.

```
# Set to example backend project in Glitch
STRIPE_EXAMPLE_BACKEND_URL=https://stripe-example-mobile-backend.glitch.me/

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
5. Create a [PaymentMethodCreateParams](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-payment-method-create-params/index.html)
   object from the [Google Pay PaymentData](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentData) object using
   [PaymentMethodCreateParams.createFromGooglePay()](https://stripe.dev/stripe-android/stripe/com.stripe.android.model/-payment-method-create-params/-companion/create-from-google-pay.html).

   ```
   val paymentData = PaymentData.getFromIntent(data) ?: return

   val paymentMethodCreateParams = PaymentMethodCreateParams.createFromGooglePay(
       JSONObject(paymentData.toJson())
   )
   ```

6. Create a [Stripe Payment Method object](https://stripe.com/docs/payments/payment-methods)
   with the `PaymentMethodCreateParams` object using
   [Stripe#createPaymentMethod()](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/create-payment-method.html).

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
