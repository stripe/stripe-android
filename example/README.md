# Stripe Examples App

## Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. After configuring, build and run the project.

## Configure

### Deploy example backend to Heroku
1. [Create a Heroku account](https://signup.heroku.com/) if you don't have one.
2. Navigate to the [example mobile backend repo](https://github.com/stripe/example-ios-backend)
   and click "Deploy to Heroku".
3. Set an _App Name_ of your choice (e.g. Stripe Example Mobile Backend).
4. Under _Config Vars_, set your Stripe testmode secret key for the `STRIPE_TEST_SECRET_KEY` field.
5. Click "Deploy for Free".

### Configure app
1. Set [Settings.PUBLISHABLE_KEY](example/src/main/java/com/stripe/example/Settings.kt)
   to your [test publishable key](https://dashboard.stripe.com/test/apikeys). 

   For example,
   ```
   const val PUBLISHABLE_KEY = "pk_test_12345"
   ```

2. Set [Settings.BASE_URL](samplestore/src/main/java/com/stripe/samplestore/Settings.kt)
   to the URL of the example backend deployed to Heroku.

   For example,
   ```
   const val BASE_URL = "https://my-example-app.herokuapp.com"
   ```

## Examples

### Google Pay

#### Source
[PayWithGoogleActivity.kt](https://github.com/stripe/stripe-android/blob/master/example/src/main/java/com/stripe/example/activity/PayWithGoogleActivity.kt)

#### Overview
1. Check that Google Pay is available and ready in `isReadyToPay()`.
2. Create a Google Pay `PaymentDataRequest` in `createGooglePayRequest()`.
    - Optionally, require Billing Address (`isBillingAddressRequired`),
      Phone Number (`isPhoneNumberRequired`),
      and Email (`isEmailRequired`)
3. Display Google Pay sheet in `payWithGoogle()`.
4. After user selects a payment method, `Activity#onActivityResult()` is called.
   Handle result in `handleGooglePayResult()`.
5. Create a `PaymentMethodCreateParams` object from the Google Pay
   `PaymentData` object using
   `PaymentMethodCreateParams.createFromGooglePay()`.

   ```
   val paymentData = PaymentData.getFromIntent(data) ?: return

   val paymentMethodParams = PaymentMethodCreateParams.createFromGooglePay(
       JSONObject(paymentData.toJson())
   )
   ```

6. Create a Stripe Payment Method object with the `PaymentMethodCreateParams`
   object using `Stripe#createPaymentMethod()`.

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
