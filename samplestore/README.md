# Stripe Emoji Apparel App

**Contents**
1. [Overview](#overview)
2. [Setup](#setup)
3. [Demo](#demo)
4. [Licenses](#licenses)


## Overview

The Emoji Apparel app is an app that demonstrates integrating with the Stripe Android SDK.
It uses the [standard integration](https://stripe.com/docs/mobile/android/standard) approach,
which means it uses [PaymentSession](https://github.com/stripe/stripe-android/blob/master/stripe/src/main/java/com/stripe/android/PaymentSession.java)
to manage the checkout flow, including selecting a Payment Method and specifying a shipping address and shipping method.

### APIs
The integration is powered by Stripe's [Payment Intents API](https://stripe.com/docs/payments/payment-intents/android),
[Setup Intents API](https://stripe.com/docs/payments/cards/saving-cards-without-payment), and
[Payment Methods API](https://stripe.com/docs/mobile/android/payment-methods).

### App components
The app is comprised of two Activity classes:
1. [StoreActivity](https://github.com/stripe/stripe-android/blob/master/samplestore/src/main/java/com/stripe/samplestore/StoreActivity.kt), which represents the customer adding items to their cart

   <img width="246" height="506" src="https://raw.githubusercontent.com/stripe/stripe-android/master/samplestore/assets/screenshots/screenshot01.png" />

2. [PaymentActivity](https://github.com/stripe/stripe-android/blob/master/samplestore/src/main/java/com/stripe/samplestore/PaymentActivity.kt), which represents the checkout experience

   <img width="246" height="506" src="https://raw.githubusercontent.com/stripe/stripe-android/master/samplestore/assets/screenshots/screenshot05.png" />


## Setup

### Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. After [deploying the example backend to Heroku](#deploy-the-example-backend-to-heroku) and [configuring the app](#configure-the-app), build and run the project.

<img width="215" height="108" src="https://raw.githubusercontent.com/stripe/stripe-android/master/samplestore/assets/run.png" />

### Deploy the example backend to Heroku
1. [Create a Heroku account](https://signup.heroku.com/) if you don't have one.
2. Navigate to the [example mobile backend repo](https://github.com/stripe/example-ios-backend)
   and click "Deploy to Heroku".
3. Set an _App Name_ of your choice (e.g. Stripe Example Mobile Backend).
4. Under _Config Vars_, set your [Stripe testmode secret key](https://dashboard.stripe.com/test/apikeys)
   for the `STRIPE_TEST_SECRET_KEY` field.
5. Click "Deploy for Free".

<img width="700" height="793" src="https://raw.githubusercontent.com/stripe/stripe-android/master/samplestore/assets/heroku.png" />

### Configure the samplestore app

#### Required
1. Set [Settings.PUBLISHABLE_KEY](example/src/main/java/com/stripe/example/Settings.kt)
   to your [test publishable key](https://dashboard.stripe.com/test/apikeys). 

   For example,
   ```
   const val PUBLISHABLE_KEY = "pk_test_12345"
   ```

2. Set [Settings.BASE_URL](samplestore/src/main/java/com/stripe/samplestore/Settings.kt)
   to the URL of the [example backend deployed to Heroku](#deploy-example-backend-to-heroku).

   For example,
   ```
   const val BASE_URL = "https://my-example-app.herokuapp.com"
   ```

#### Optional
1. Set [Settings.CURRENCY](samplestore/src/main/java/com/stripe/samplestore/Settings.kt)
   to the currency that the app should use. The default is `usd`.

   For example,
   ```
   const val CURRENCY = "usd"
   ```
   
2. Set [Settings.ALLOWED_PAYMENT_METHOD_TYPES](samplestore/src/main/java/com/stripe/samplestore/Settings.kt)
   to the [payment method types](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-payment_method_types)
   that the customer can use for payment. The default is `card`.

   For example,
   ```
   val ALLOWED_PAYMENT_METHOD_TYPES = listOf(
       PaymentMethod.Type.Card
   )
   ```

## Demo

The following is a demonstration of a Customer
1. Adding items to their cart
2. Navigating to the checkout screen
3. Choosing their Payment Method, which happens to require 3D Secure 2 (3DS2)
4. Specifying their shipping address and shipping method
5. Confirming their intent to pay
6. Authenticating their payment with 3DS2
7. Completing their purchase

<img width="320" height="658" src="https://raw.githubusercontent.com/stripe/stripe-android/master/samplestore/assets/demo.gif" />


## Licenses
- App icon from [Twemoji](https://github.com/twitter/twemoji)
