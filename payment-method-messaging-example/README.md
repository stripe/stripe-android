# Payment Method Messaging Example App

## Setup 

### Install
1. Clone the `stripe-android` repository.
2. Open the project in Android Studio.

### Configure the app
1. If it doesn't exist, create a `gradle.properties` in a location defined in the
   [Gradle Build Environment docs](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
   For example, the default location on macOS is `~/.gradle/gradle.properties`.
2. Append the following entries to `gradle.properties`.

```
# Set to a test publishable key from https://dashboard.stripe.com/test/apikeys
STRIPE_EXAMPLE_PUBLISHABLE_KEY=pk_test_mykey

# Optionally, set to a Connect Account id to test Connect
STRIPE_ACCOUNT_ID=
```

### Run
1. Select the `payment-method-messaing-example` configuration.
2. Run the app.
