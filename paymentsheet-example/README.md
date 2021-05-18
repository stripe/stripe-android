# Stripe PaymentSheet Example App

## Setup

### Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. After [remixing the Glitch example backend](#remix-the-glitch-example-backend) and [configuring the app](#configure-the-app), build and run the `paymentsheet-example` project.

### Remix the Glitch example backend
1. [Remix the example project on Glitch](https://glitch.com/edit/#!/remix/stripe-mobile-payment-sheet).
2. Set your [Stripe testmode `secret_key` and `publishable_key`](https://dashboard.stripe.com/test/apikeys) in the 🗝️.env file on the left.
    - You can see the server logs for debugging by clicking "Tools" > "Logs" on the bottom left.

### Configure the app
1. If it doesn't exist, create a `gradle.properties` in a location defined in the
   [Gradle Build Environment docs](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
   For example, the default location on macOS is `~/.gradle/gradle.properties`.
2. Append the following entry to `gradle.properties`:

```
# Set to example backend in Glitch
STRIPE_PAYMENTSHEET_EXAMPLE_BACKEND_URL=https://your-example-backend.glitch.me/
```
