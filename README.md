# Stripe Android SDK

[![Build Status](https://api.travis-ci.org/stripe/stripe-android.svg?branch=master)](https://travis-ci.org/stripe/stripe-android)

The Stripe Android SDK makes it quick and easy to build an excellent payment experience in your Android app. We provide powerful and customizable UI elements that can be used out-of-the-box to collect your users' payment details. We also expose the low-level APIs that power those UIs so that you can build fully custom experiences. See our [Android Integration Guide](https://stripe.com/docs/mobile/android) to get started!

> If you are building an Android application that charges a credit card, you should use the Stripe Android SDK to make sure you don't pass credit card information to your server (and, so, are PCI compliant).

## Installation

### Android Studio (or Gradle)

No need to clone the repository or download any files -- just add this line to your app's `build.gradle` inside the `dependencies` section:

```
implementation 'com.stripe:stripe-android:8.1.0'
```

Note: We recommend that you don't use `compile 'com.stripe:stripe-android:+`, as future versions of the SDK may not maintain full backwards compatibility. When such a change occurs, a major version number change will accompany it.

Please note that if enabling minification in your `build.gradle` file, you must also add this line to the `proguard-rules.pro`:

```
-keep class com.stripe.android.** { *; }
```

### Eclipse

Note - as Google has stopped supporting Eclipse for Android Development, we will no longer be actively testing the project's compatibility within Eclipse. You may still clone and include the library as you would any other Android library project.

## Usage

### Using `CardInputWidget`

You can add a single-line widget to your apps that easily handles the UI states for collecting card data.

First, add the `CardInputWidget` to your layout.

```xml
<com.stripe.android.view.CardInputWidget
    android:id="@+id/card_input_widget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    />
```

Note: The minimum width for this widget is 320dp. The widget also requires an ID to ensure proper layout on rotation, so if you don't do this, we assign one for you when the object is instantiated.

Once this widget is in your layout, you can read the `Card` object simply by querying the widget. You'll be given a `null` object if the card data is invalid according to our client-side checks.

```java
Card cardToSave = mCardInputWidget.getCard();

if (cardToSave == null) {
    mErrorDialogHandler.showError("Invalid Card Data");
    return;
}
```

### Using `CardMultilineWidget`

You can add a Material-style multiline widget to your apps that handles card data collection as well. This can be added in a layout similar to the `CardInputWidget`.

```xml
<com.stripe.android.view.CardMultilineWidget
    android:id="@+id/card_multiline_widget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:shouldShowPostalCode="true"
    />
```

Note: A `CardMultiline` widget can only be added in the view of an `Activity` whose `Theme` descends from an `AppCompat` theme.

In order to use the `app:shouldShowPostalCode` tag, you'll need to enable the app XML namespace somewhere in the layout.

Note: We currently only support US ZIP in the postal code field.

```xml
xmlns:app="http://schemas.android.com/apk/res-auto"
```

To get a `Card` object from the `CardMultilineWidget`, you query the widget for its card, just like the `CardInputWidget`.

```java
Card cardToSave = mCardMultilineWidget.getCard();

if (cardToSave == null) {
    mErrorDialogHandler.showError("Invalid Card Data");
    return;
}
```

If the returned `Card` is `null`, error states will show on the fields that need to be fixed.

Once you have a non-null `Card` object from either widget, you can call [createToken](#using-createtoken).

### Using `setDefaultPublishableKey`

A publishable key is required to identify your website when communicating with Stripe. Remember to replace the test key with your live key in production.

You can get all your keys from [your account page](https://manage.stripe.com/#account/apikeys).
This [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.

```java
new Stripe(context, "YOUR_PUBLISHABLE_KEY");
```

or

```java
new Stripe(context).setDefaultPublishableKey("YOUR_PUBLISHABLE_KEY");
```

### Using `createToken`

The `stripe.createToken` converts sensitive card data into a single-use token which you can safely pass to your server to charge the user. This [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.

```java
stripe.createToken(
    new Card("4242424242424242", 12, 2013, "123"),
    tokenCallback
);
```

The first argument to `createToken` is a Card object. A Card contains the following fields:

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
It should send the token to your server for processing `onSuccess`, and notify the user `onError`.

Here's a sample implementation of the token callback:

```java
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
```

The `stripe.createToken` is an asynchronous call – it returns immediately and invokes the callback on the UI thread when it receives a response from Stripe's servers.

### Using `createTokenSynchronous`

The `stripe.createTokenSynchronous` method allows you to handle threading on your own, using any IO framework you choose. In particular, you can now create a token using RxJava or an IntentService.

Note: Do not call this method on the main thread or your app will crash!

#### RxJava Example

```java
Observable<Token> tokenObservable =
    Observable.fromCallable(
            new Callable<Token>() {
                @Override
                public Token call() throws Exception {
                    // When executed, this method will conduct i/o on whatever thread it is run on
                    return stripe.createTokenSynchronous(cardToCharge);
                }
            });

tokenObservable
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnSubscribe(
            new Action0() {
                @Override
                public void call() {
                    // Show a progress dialog if you prefer
                    showProgressDialog();
                }
            })
    .doOnUnsubscribe(
            new Action0() {
                @Override
                public void call() {
                    // Close the progress dialog if you opened one
                    closeProgressDialog();
                }
            })
    .subscribe(
            new Action1<Token>() {
                @Override
                public void call(Token token) {
                    // Send token to your own web service
                    MyServer.chargeToken(token);
                }
            },
            new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    // Tell the user about the error
                    handleError(throwable.getLocalizedMessage());
                }
            });
```

#### IntentService Example

You can invoke the following from your code (where `cardToSave` is some Card object that you have created.)

```java
Intent tokenServiceIntent = TokenIntentService.createTokenIntent(
        mActivity,
        cardToSave.getNumber(),
        cardToSave.getExpMonth(),
        cardToSave.getExpYear(),
        cardToSave.getCVC(),
        mPublishableKey);
mActivity.startService(tokenServiceIntent);
```

Your IntentService can then perform the following in its `onHandleIntent` method.

```java
@Override
protected void onHandleIntent(Intent intent) {
    String errorMessage = null;
    Token token = null;
    if (intent != null) {
        String cardNumber = intent.getStringExtra(EXTRA_CARD_NUMBER);
        Integer month = (Integer) intent.getExtras().get(EXTRA_MONTH);
        Integer year = (Integer) intent.getExtras().get(EXTRA_YEAR);
        String cvc = intent.getStringExtra(EXTRA_CVC);
        String publishableKey = intent.getStringExtra(EXTRA_PUBLISHABLE_KEY);
        Card card = new Card(cardNumber, month, year, cvc);
        Stripe stripe = new Stripe();
        try {
            token = stripe.createTokenSynchronous(card, publishableKey);
        } catch (StripeException stripeEx) {
            errorMessage = stripeEx.getLocalizedMessage();
        }
    }
    Intent localIntent = new Intent(TOKEN_ACTION);
    if (token != null) {
        // extract whatever information you want from your Token object
        localIntent.putExtra(STRIPE_CARD_LAST_FOUR, token.getCard().getLast4());
        localIntent.putExtra(STRIPE_CARD_TOKEN_ID, token.getId());
    }
    if (errorMessage != null) {
        localIntent.putExtra(STRIPE_ERROR_MESSAGE, errorMessage);
    }
    // Broadcasts the Intent to receivers in this app.
    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
}
```

Registering a local BroadcastReceiver in your activity then allows you to handle the results.

```java
private class TokenBroadcastReceiver extends BroadcastReceiver {

    private TokenBroadcastReceiver() { }

    @Override
    public void onReceive(Context context, Intent intent) {
        mProgressDialogController.finishProgress();
        if (intent == null) {
            return;
        }
        if (intent.hasExtra(TokenIntentService.STRIPE_ERROR_MESSAGE)) {
            // handle your error!
            return;
        }
        if (intent.hasExtra(TokenIntentService.STRIPE_CARD_TOKEN_ID) &&
                intent.hasExtra(TokenIntentService.STRIPE_CARD_LAST_FOUR)) {
                    // handle your resulting token here
                }
        }
    }
}
```

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

## Example apps

There are 2 example apps included in the repository:
- Example project is a simple example of different ways to connect our components, including how to make tokens and sources, how to connect the synchronous and asynchronous methods, and how to use the CardInputWidget.
- SampleStore project is a full walk-through of building a shop activity, including connecting to a back end.

To build and run the example apps, clone the repository and open the project. Running "example" will run the Example application, and running "samplestore" will run the shop activity.

### Getting started with the Android example apps

Note: Both example apps require an [Android SDK](https://developer.android.com/studio/index.html) and [Gradle](https://gradle.org/) to build and run.

### Building the example project

1. Clone the git repository.
2. Be sure you've installed the Android SDK with API Level 17 and _android-support-v4_. This is only a requirement for development. Our bindings require the API Level 7 as a minimum at runtime which would work on almost any modern version of Android.
3. Import the project.
    * For Android Studio, choose _Import Project..._ from the "Welcome to Android Studio" screen. Select the `build.gradle` file at the top of the `stripe-android` repository.
    * For Eclipse, [import](http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.user/tasks/tasks-importproject.htm) the _example_ and _stripe_ folders into, by using `Import -> General -> Existing Projects into Workspace`, and browsing to the `stripe-android` folder.
4. Build and run the project on your device or in the Android emulator.

The example application needs a public key from your Stripe account to interact with the Stripe API. To add this, [replace the value of PUBLISHABLE_KEY in LauncherActivity with your test key](example/src/main/java/com/stripe/example/activity/LauncherActivity.java#L25).

Three different ways of creating tokens are shown, with all the Stripe-specific logic needed for each separated into the three controllers,
[AsyncTaskTokenController](example/src/main/java/com/stripe/example/controller/AsyncTaskTokenController.java), [RxTokenController](example/src/main/java/com/stripe/example/controller/RxTokenController.java), and [IntentServiceTokenController](example/src/main/java/com/stripe/example/controller/IntentServiceTokenController.java).

### Building and Running the samplestore project and CustomerSessions

Before you can run the SampleStore application or use the CustomerSessionActivity in the example application, you need to provide it with your Stripe publishable key and a sample backend.

1. If you haven't already, sign up for a [Stripe account](https://dashboard.stripe.com/register) (it takes seconds). Then go to https://dashboard.stripe.com/account/apikeys.
2. Replace the `PUBLISHABLE_KEY` constant in StoreActivity.java (where it says "put your publishable key here") with your Test Publishable Key.
3. Head to https://github.com/stripe/example-ios-backend and click "Deploy to Heroku" (you may have to sign up for a Heroku account as part of this process). Provide your Stripe test secret key for the STRIPE_TEST_SECRET_KEY field under 'Env'. Click "Deploy for Free".
4. Replace the `BASE_URL` variable (where it says "Put your backend URL here") in the RetrofitFactory.java file with the app URL Heroku provides you with (e.g. "https://my-example-app.herokuapp.com")

After this is done, you can make test payments through the app and see them in your Stripe dashboard. Head to https://stripe.com/docs/testing#cards for a list of test card numbers.

## Migrating from older versions

See `MIGRATING.md`
