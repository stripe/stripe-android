# Stripe Payment Card Scan Example App

## Setup

### Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. After [deploying the example backend to Glitch](#set-up-your-own-backend-with-glitch) and [configuring the app](#configure-the-app), build and run the project.

<img width="215" height="108" src="https://raw.githubusercontent.com/stripe/stripe-android/master/stripecardscan-example/images/run_project.png" />

### Set up your own backend with Glitch
1. [Create a Glitch account](https://glitch.com/signup/) if you don't have one.
2. Create your own copy of the [example mobile backend application](https://stripe-card-scan-civ-example-app.glitch.me/)
   by clicking "[Remix on Glitch](https://glitch.com/edit?utm_source=button&utm_medium=button&utm_campaign=glitchButton&utm_content=stripe-card-scan-civ-example-app/#!/remix/stripe-card-scan-civ-example-app)".
3. Set an _App Name_ of your choice (e.g. Stripe Example Mobile Backend).
4. Under the `.env` file, set your [Stripe testmode secret key](https://dashboard.stripe.com/test/apikeys)
   for the `secret_key` field.

<img width="700" height="351" src="https://raw.githubusercontent.com/stripe/stripe-android/master/stripecardscan-example/images/glitch_remix_project.png" />

### Configure the app
1. If it doesn't exist, create a `gradle.properties` in a location defined in the
   [Gradle Build Environment docs](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
   For example, the default location on macOS is `~/.gradle/gradle.properties`.
2. Append the following entries to `gradle.properties`.

```
# Set to example backend deployed to Glitch
STRIPE_CARDSCAN_EXAMPLE_BACKEND_URL=https://stripe-card-scan-civ-example-app.glitch.me/

# Set to a test publishable key from https://dashboard.stripe.com/test/apikeys
STRIPE_CARDSCAN_EXAMPLE_PUBLISHABLE_KEY=pk_test_mykey
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
