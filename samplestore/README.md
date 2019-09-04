# Stripe Emoji Apparel App

**Contents**
1. [Setup](#setup)
2. [Demo](#demo)
3. [Licenses](#licenses)

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

## Demo
<img width="320" height="658" src="https://raw.githubusercontent.com/stripe/stripe-android/master/samplestore/assets/demo.gif" />

## Licenses
- App icon from [Twemoji](https://github.com/twitter/twemoji)
