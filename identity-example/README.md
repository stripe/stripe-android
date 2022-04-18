# Stripe Identity Example App

This app demonstrates how to integrate with Stripe Identity with native Android SDK or using Redirect web flow.

It requests a `VerificationSession` from a [sample backend](https://glitch.com/edit/#!/reflective-fossil-rib?path=README.md%3A1%3A0), you can either use the same backend or remix the sample glitch backend and provide your own secret key in `.env` file.

Please follow this [guide](https://stripe.com/docs/identity/verify-identity-documents?platform=web&type=redirect#create-a-verificationsession) for more details on creating a `VerificationSession` with a sample backend.

## Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.
3. Build and run the `identity-example` project.

## Usage - Click on the corresponding radio button to switch between native and web flow
* `Use native SDK` - use `id` and `ephemeral_key_secret` to start native Android flow with `IdentityVerificationSheet`.
* `Use web redirect` - opens the `url` in a [custom tab](https://developer.chrome.com/docs/android/custom-tabs/), complete verification flow in browser.
