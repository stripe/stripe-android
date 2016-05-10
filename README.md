## stripe-android

[![Build Status](https://api.travis-ci.org/stripe/stripe-android.svg?branch=master)](https://travis-ci.org/stripe/stripe-android)

Stripe-android makes it easy to collect credit card information without having sensitive details touch your server.

These Stripe Android bindings can be used to generate tokens in your Android application. If you are building an Android application that charges a credit card, you should use stripe-android to make sure you don't pass credit card information to your server (and, so, are PCI compliant).

## Installation

### Android Studio (or Gradle)

No need to clone the repository or download any files -- just add this line to your app's `build.gradle` inside the `dependencies` section:

    compile 'com.stripe:stripe-android:+'

### Eclipse

1. Clone the repository.
2. Be sure you've installed the Android SDK with API Level 17 and _android-support-v4_. This is only a requirement for development. Our bindings require the API Level 7 as a minimum at runtime which would work on almost any modern version of Android.
3. Import the _stripe_ folder into [Eclipse](http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.user/tasks/tasks-importproject.htm) (use "Existing Projects into Workspace", [not "Existing Android Code"](https://github.com/stripe/stripe-android/issues/7)).
4. In your project settings, add the _stripe_ project under the "Libraries" section of the "Android" category.

### ProGuard

If you're planning on optimizing your app with ProGuard, make sure that you exclude the Stripe bindings. You can do this by adding the following to your app's `proguard.cfg` file:

    -keep class com.stripe.** { *; }

You also need to add some configuration options for Gson, which is used by the Stripe bindings to serialize and deserialize JSON data. You can find the recommended ProGuard configuration for Gson [here](https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg).

## Usage

### setDefaultPublishableKey

A publishable key is required to identify your website when communicating with Stripe. Remember to replace the test key with your live key in production.

You can get all your keys from [your account page](https://manage.stripe.com/#account/apikeys).
This [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.

    new Stripe("YOUR_PUBLISHABLE_KEY");

or

    new Stripe().setDefaultPublishableKey("YOUR_PUBLISHABLE_KEY");

### createToken

createToken converts sensitive card data to a single-use token which you can safely pass to your server to charge the user. The [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.

    stripe.createToken(
        new Card("4242424242424242", 12, 2013, "123"),
        tokenCallback
    );

The first argument to createToken is a Card object. A Card contains the following fields:

+ number: card number as a string without any separators, e.g. '4242424242424242'.
+ expMonth: integer representing the card's expiration month, e.g. 12.
+ expYear: integer representing the card's expiration year, e.g. 2013.

The following field is optional but recommended to help prevent fraud:

+ cvc: card security code as a string, e.g. '123'.

The following fields are entirely optional — they cannot result in a token creation failing:

+ name: cardholder name.
+ addressLine1: billing address line 1.
+ addressLine2: billing address line 2.
+ addressCity: billing address city.
+ addressState: billing address state.
+ addressZip: billing zip as a string, e.g. '94301'.
+ addressCountry: billing address country.

The second argument tokenCallback is a callback you provide to handle responses from Stripe.
It should send the token to your server for processing onSuccess, and notify the user onError.

Here's a sample implementation of the token callback:

    stripe.createToken(
        card,
        new TokenCallback() {
            public void onSuccess(Token token) {
                // Send token to your own web service
                MyServer.chargeToken(token);
            }
            public void onError(Exception error) {
                Toast.makeText(getContext(),
                    error.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
            }
        }
    );

createToken is an asynchronous call – it returns immediately and invokes the callback on the UI thread when it receives a response from Stripe's servers.

### Client-side validation helpers

The Card object allows you to validate user input before you send the information to Stripe.

#### validateNumber

Checks that the number is formatted correctly and passes the [Luhn check](http://en.wikipedia.org/wiki/Luhn_algorithm).

#### validateExpiryDate

Checks whether or not the expiration date represents an actual month in the future.

#### validateCVC

Checks whether or not the supplied number could be a valid verification code.

#### validateCard

Convenience method to validate card number, expiry date and CVC.

### Retrieving information about a token

If you're implementing a complex workflow, you may want to know if you've already charged a token (since they can only be charged once). You can call requestToken with a token id and callbacks to find out whether or not the token has already been used. This will return an object with the same structure as the object returned from createToken.

    stripe.requestToken(
        tokenID,
        new TokenCallback() {
            public void onSuccess(Token token) {
                if (token.getUsed()) {
                    Log.d("Token has already been charged.");
                }
            }
            public void onError(Exception error) {
                // handle error
            }
        });

## Building the example project

1. Clone the git repository.
2. Be sure you've installed the Android SDK with API Level 17 and _android-support-v4_. This is only a requirement for development. Our bindings require the API Level 7 as a minimum at runtime which would work on almost any modern version of Android.
3. Import the project.
    * For Android Studio, choose _Import Project..._ from the "Welcome to Android Studio" screen. Select the `build.gradle` file at the top of the `stripe-android` repository.
    * For Eclipse, [import](http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.user/tasks/tasks-importproject.htm) the _example_ and _stripe_ folders into, by using `Import -> General -> Existing Projects into Workspace`, and browsing to the `stripe-android` folder.
4. Build and run the project on your device or in the Android emulator.

The example application ships with a sample publishable key, but if you want to test with your own Stripe account, you can [replace the value of PUBLISHABLE_KEY in PaymentActivity with your test key](https://github.com/stripe/stripe-android/blob/master/example/src/main/java/com/stripe/example/activity/PaymentActivity.java#L25).
