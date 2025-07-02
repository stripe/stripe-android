# Stripe Payment Card Scan Example App

## Setup

### Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. After [deploying the example backend to Code Sandbox or using our existing backend](#set-up-your-own-backend-with-codesandbox) and [configuring the app](#configure-the-app), build and run the project.

<img width="215" height="108" src="https://raw.githubusercontent.com/stripe/stripe-android/master/stripecardscan-example/images/run_project.png" />

### Set up permissions to use the flow
The stripecardscan module is currently in private beta, and merchants must be manually included in the beta for this demo to work.

If you're from outside Stripe, contact Stripe support to request to be added to the card image verification beta.

If you're internal to Stripe, contact the Bouncer team for help.

### Set up your own backend with CodeSandbox
1. [Create a Glitch account](https://glitch.com/signup/) if you don't have one.
2. Create your own copy of the [example mobile backend application](https://codesandbox.io/p/devbox/stripe-card-scan-civ-example-app-d7sjq9/), login/sign up for CodeSandbox
   and press "Fork". You can also use the backend that we've already set up [Stripe Demo](https://https://stripe-card-scan-civ-example-app.stripedemos.com/)
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
# Set to example backend deployed to Stripe Demos
STRIPE_CARDSCAN_EXAMPLE_BACKEND_URL=https://https://stripe-card-scan-civ-example-app.stripedemos.com/

# Set to a test publishable key from https://dashboard.stripe.com/test/apikeys
STRIPE_CARDSCAN_EXAMPLE_PUBLISHABLE_KEY=pk_test_mykey
```

## Example
1. Input the first six digits and last four digits of the card that will be required to be scanned
2. Click the `Generate CIV Intent` button to create a new CIV intent with Stripe
3. Click the `CardImageVerificationSheet` button to launch the card scanner with this intent
4. Once the scan has completed, the result will be displayed below the form.
