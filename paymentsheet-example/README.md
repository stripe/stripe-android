# Stripe PaymentSheet Example App

This app demonstrates how to integrate with Stripe, using our prebuilt ui (single-step and multi-step).

For step-by-step instructions, follow our [Integration Guide](https://stripe.com/docs/payments/accept-a-payment?platform=android).

## Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. Build and run the `paymentsheet-example` project.

If you want to use your own Stripe account, follow the steps below to [remix the Glitch example backend](#remix-the-glitch-example-backend) and [configure the app](#configure-the-app).

## Remix the Glitch example backend
1. [Remix the example project on Glitch](https://glitch.com/edit/#!/remix/stripe-mobile-payment-sheet).
2. Set your [Stripe testmode `secret_key` and `publishable_key`](https://dashboard.stripe.com/test/apikeys) in the 🗝️.env file on the left.
    - You can see the server logs for debugging by clicking "Tools" > "Logs" on the bottom left.

## Configure the app
1. If it doesn't exist, create a `gradle.properties` in a location defined in the
   [Gradle Build Environment docs](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
   For example, the default location on macOS is `~/.gradle/gradle.properties`.
2. Append the following entry to `gradle.properties`:

```
# Set to example backend in Glitch
STRIPE_PAYMENTSHEET_EXAMPLE_BACKEND_URL=https://your-example-backend.glitch.me/
```
