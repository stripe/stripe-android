[<img width="250" height="119" src="https://raw.githubusercontent.com/stripe/stripe-android/master/assets/stripe_logo_slate_small.png"/>](https://stripe.com/docs/mobile/android)

# Stripe Android SDK

[![Build Status](https://api.travis-ci.org/stripe/stripe-android.svg?branch=master)](https://travis-ci.org/stripe/stripe-android)
[![GitHub release](https://img.shields.io/github/release/stripe/stripe-android.svg?maxAge=60)](https://github.com/stripe/stripe-android/releases)

The Stripe Android SDK makes it quick and easy to build an excellent payment experience in your Android app. We provide powerful and customizable UI elements that can be used out-of-the-box to collect your users' payment details. We also expose the low-level APIs that power those UIs so that you can build fully custom experiences. See our [Android Integration Guide](https://stripe.com/docs/mobile/android) to get started!

> If you are building an Android application that charges a credit card, you should use the Stripe Android SDK to make sure you don't pass credit card information to your server (and, so, are PCI compliant).


## Installation

### Requirements

* Android 4.4 (API level 19) and above
* [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 3.4.1+
* [Gradle](https://gradle.org/releases/) 5.1.1+

### Configuration

Add `stripe-android` to your `build.gradle` dependencies.

```
dependencies {
    implementation 'com.stripe:stripe-android:10.3.0'
}
```

### Releases
* The [changelog](CHANGELOG.md) provides a summary of changes in each release.
* The [migration guide](MIGRATING.md) provides instructions on upgrading from older versions.

### Proguard

If enabling minification in your `build.gradle` file, you must also add this line to the `proguard-rules.pro`:

```
-keep class com.stripe.android.** { *; }
```

## Usage

### Using CardInputWidget

You can add a single-line widget to your apps that easily handles the UI states for collecting card data.

First, add the `CardInputWidget` to your layout.

```xml
<com.stripe.android.view.CardInputWidget
    android:id="@+id/card_input_widget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    />
```

Note: The minimum width for this widget is 320dp. The widget also requires an ID to ensure proper layout on rotation, so if you don't do this, an ID is assigned when the object is instantiated.

If the customer's input is valid, [CardInputWidget#getCard()](https://stripe.dev/stripe-android/com/stripe/android/view/CardInputWidget.html#getCard--) will return a [Card](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html) instance; otherwise, it will return `null`. 

```java
final Card cardToSave = mCardInputWidget.getCard();

if (cardToSave == null) {
    mErrorDialogHandler.showError("Invalid Card Data");
    return;
}
```

### Using CardMultilineWidget

You can add a Material-style multiline widget to your apps that handles card data collection as well. This can be added in a layout similar to the `CardInputWidget`.

```xml
<com.stripe.android.view.CardMultilineWidget
    android:id="@+id/card_multiline_widget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:shouldShowPostalCode="true"
    />
```

Note: A `CardMultilineWidget` can only be added in the view of an `Activity` whose `Theme` descends from an `AppCompat` theme.

In order to use the `app:shouldShowPostalCode` tag, you'll need to enable the app XML namespace somewhere in the layout.

Note: We currently only support US ZIP in the postal code field.

```xml
xmlns:app="http://schemas.android.com/apk/res-auto"
```

If the customer's input is valid, [CardMultilineWidget#getCard()](https://stripe.dev/stripe-android/com/stripe/android/view/CardMultilineWidget.html#getCard--) will return a [Card](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html) instance; otherwise, it will return `null`. 

```java
final Card cardToSave = mCardMultilineWidget.getCard();

if (cardToSave == null) {
    mErrorDialogHandler.showError("Invalid Card Data");
    return;
}
```

If the returned `Card` is `null`, error states will show on the fields that need to be fixed.

Once you have a non-null `Card` object from either widget, you can call [Stripe#createToken()](#using-createtoken).

### Setting your Publishable Key

A [publishable key](https://stripe.com/docs/keys) is required to identify your app when communicating with Stripe. Remember to replace the test key with your live key in production.

You can view your API keys in the [Stripe Dashboard](https://dashboard.stripe.com/apikeys).
The [Android Integration doc](https://stripe.com/docs/mobile/android) explains this flow in more detail.

```java
new Stripe(context, "YOUR_PUBLISHABLE_KEY");
```

### Creating Card Tokens

[Stripe#createToken()](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#createToken-com.stripe.android.model.Card-com.stripe.android.TokenCallback-) makes an asynchronous call to Stripe's [Tokens API](https://stripe.com/docs/api/tokens/create_card) that converts sensitive card data into a single-use token which you can safely pass to your server to charge the user. The [Collecting Card Details on Android](https://stripe.com/docs/payments/cards/collecting/android) explains this flow in more detail.

```java
stripe.createToken(
    new Card("4242424242424242", 12, 2013, "123"),
    tokenCallback
);
```

The first argument to `createToken()` is a `Card` object. A `Card` contains the following fields:

+ `number`: Card number as a string without any separators, e.g. `4242424242424242`.
+ `expMonth`: Integer representing the card's expiration month, e.g. `12`.
+ `expYear`: Integer representing the card's expiration year, e.g. `2013`.

The following field is optional but recommended to help prevent fraud:

+ `cvc`: Card security code as a string, e.g. `123`.

The following fields are entirely optional — they cannot result in a token creation failing:

+ `name`: Cardholder name.
+ `addressLine1`: Billing address line 1.
+ `addressLine2`: Billing address line 2.
+ `addressCity`: Billing address city.
+ `addressState`: Billing address state.
+ `addressZip`: Billing zip as a string, e.g. `94301`.
+ `addressCountry`: Billing address country.

The second argument `tokenCallback` is a callback you provide to handle responses from Stripe.
It should send the token to your server for processing `onSuccess()`, and notify the user `onError()`.

Here's a sample implementation of the token callback:

```java
stripe.createToken(
    card,
    new ApiResultCallback<Token>() {
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
```


### Creating Card Tokens synchronously

[Stripe#createTokenSynchronous()](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html#createTokenSynchronous-com.stripe.android.model.Card-) allows you to handle threading on your own, using any IO framework you choose.

Note: Do not call this method on the main thread or your app will crash.

#### RxJava Example

```kotlin
val tokenObservable = Observable.fromCallable {
    mStripe.createTokenSynchronous(cardToSave)!!
}

tokenObservable
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        { token -> ... },
        { throwable -> ... }
    )
```


### Client-side Card Validation

The [Card](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html) object allows you to validate user input before you send the information to the Stripe API.

- [Card#validateNumber()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateNumber--) - Checks that the number is formatted correctly and passes the [Luhn check](http://en.wikipedia.org/wiki/Luhn_algorithm).
- [Card#validateExpiryDate()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateExpiryDate--) - Checks whether or not the expiration date represents an actual month in the future.
- [Card#validateCVC()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateCVC--) - Checks whether or not the supplied number could be a valid verification code.
- [Card#validateCard()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateCard--) - Convenience method to validate card number, expiry date and CVC.

## Example apps

There are 2 example apps included in the repository:
- [Example project](https://github.com/stripe/stripe-android/tree/master/example) is a simple example of different ways to connect our components, including how to make tokens and sources, how to connect the synchronous and asynchronous methods, and how to use the CardInputWidget.
- [Samplestore project](https://github.com/stripe/stripe-android/tree/master/samplestore) is a full walk-through of building a shop activity, including connecting to a back end.

To build and run the example apps, clone the repository and open the project. Running "example" will run the Example application, and running "samplestore" will run the shop activity.

### Getting started with the Android example apps

Note: Both example apps require an [Android SDK](https://developer.android.com/studio/index.html) and [Gradle](https://gradle.org/) to build and run.

### Building the example project

1. Clone the git repository.
2. Be sure you've installed the Android SDK with API Level 19 and _android-support-v4_. This is only a requirement for development.
3. Import the project in Android Studio
    * Choose _Import Project..._ from the "Welcome to Android Studio" screen.
    * Select `build.gradle` at the top of the `stripe-android` repository.
4. Set your publishable key in [Settings.PUBLISHABLE_KEY](example/src/main/java/com/stripe/example/Settings.kt).
5. Build and run the project on your device or in the Android emulator.

Two different ways of creating tokens are shown, with all the Stripe-specific logic needed for each separated into the three controllers,
[AsyncTaskTokenController](example/src/main/java/com/stripe/example/controller/AsyncTaskTokenController.java) and [RxTokenController](example/src/main/java/com/stripe/example/controller/RxTokenController.java).

### Configuring the samplestore app

Before you can run the SampleStore application or use the CustomerSessionActivity in the example application, you need to provide it with your Stripe publishable key and a sample backend.

1. If you haven't already, sign up for a [Stripe account](https://dashboard.stripe.com/register) (it takes seconds). Then go to the Stripe Dashboard's [API keys](https://dashboard.stripe.com/test/apikeys).
2. Replace the `PUBLISHABLE_KEY` constant in [Settings.kt](samplestore/src/main/java/com/stripe/samplestore/Settings.kt) (where it says "put your publishable key here") with your Test Publishable Key.
3. Navigate to the [example mobile backend repo](https://github.com/stripe/example-ios-backend) and click "Deploy to Heroku" (you may have to sign up for a Heroku account as part of this process). Provide your Stripe test secret key for the `STRIPE_TEST_SECRET_KEY` field under 'Env'. Click "Deploy for Free".
4. Replace the `BASE_URL` variable (where it says "Put your backend URL here") in [Settings.kt](samplestore/src/main/java/com/stripe/samplestore/Settings.kt) with the app URL Heroku provides you with (e.g. `"https://my-example-app.herokuapp.com"`)

After this is done, you can make test payments through the app and see them in your Stripe dashboard. Head to [https://stripe.com/docs/testing#cards](https://stripe.com/docs/testing#cards) for a list of test card numbers.
