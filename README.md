## stripe-android

[![Build Status](https://api.travis-ci.org/stripe/stripe-android.svg?branch=master)](https://travis-ci.org/stripe/stripe-android)

Stripe-android makes it easy to collect credit card information without having sensitive details touch your server.

These Stripe Android bindings can be used to generate tokens in your Android application. If you are building an Android application that charges a credit card, you should use stripe-android to make sure you don't pass credit card information to your server (and, so, are PCI compliant).

## Installation

### Android Studio (or Gradle)

No need to clone the repository or download any files -- just add this line to your app's `build.gradle` inside the `dependencies` section:

    compile 'com.stripe:stripe-android:2.0.2'

Note: We recommend that you don't use `compile 'com.stripe:stripe-android:+`, as future versions of the SDK may not maintain full backwards compatibility. When such a change occurs, a major version number change will accompany it.

### Eclipse

Note - as Google has stopped supporting Eclipse for Android Development, we will no longer be actively testing the project's compatibility within Eclipse. You may still clone and include the library as you would any other Android library project.

### ProGuard

If you're planning on optimizing your app with ProGuard, make sure that you exclude the Stripe bindings. You can do this by adding the following to your app's `proguard.cfg` file:

    -keep class com.stripe.** { *; }

You also need to add some configuration options for Gson, which is used by the Stripe bindings to serialize and deserialize JSON data. You can find the recommended ProGuard configuration for Gson [here](https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg).

## Usage

### setDefaultPublishableKey

A publishable key is required to identify your website when communicating with Stripe. Remember to replace the test key with your live key in production.

You can get all your keys from [your account page](https://manage.stripe.com/#account/apikeys).
This [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.
```java
    new Stripe("YOUR_PUBLISHABLE_KEY");
```
or

```java
    new Stripe().setDefaultPublishableKey("YOUR_PUBLISHABLE_KEY");
```
### createToken

createToken converts sensitive card data to a single-use token which you can safely pass to your server to charge the user. The [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.

```java
stripe.createToken(
    new Card("4242424242424242", 12, 2013, "123"),
    tokenCallback
);
```

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
createToken is an asynchronous call – it returns immediately and invokes the callback on the UI thread when it receives a response from Stripe's servers.

### createTokenSynchronous

The createTokenSynchronous method allows you to handle threading on your own, using any IO framework you choose. In particular, you can now create a token using RxJava or an IntentService. **Note: do not call this method on the main thread or your app will crash!**

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

### Retrieving information about a token

The bindings for retrieving information about a token has been removed from the Android SDK because only older Stripe accounts (from early 2014) can perform this operation with a public key. If you still need this functionality, make sure to use the last version of the Android bindings that contained this functionality by setting your version in the `build.gradle` file as follows.
```groovy
    // Using older bindings to have access to requestToken
    compile 'com.stripe:stripe-android:1.1.1'
```

## Building the example project

1. Clone the git repository.
2. Be sure you've installed the Android SDK with API Level 17 and _android-support-v4_. This is only a requirement for development. Our bindings require the API Level 7 as a minimum at runtime which would work on almost any modern version of Android.
3. Import the project.
    * For Android Studio, choose _Import Project..._ from the "Welcome to Android Studio" screen. Select the `build.gradle` file at the top of the `stripe-android` repository.
    * For Eclipse, [import](http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.user/tasks/tasks-importproject.htm) the _example_ and _stripe_ folders into, by using `Import -> General -> Existing Projects into Workspace`, and browsing to the `stripe-android` folder.
4. Build and run the project on your device or in the Android emulator.

The example application ships with a sample publishable key, but if you want to test with your own Stripe account, you can [replace the value of PUBLISHABLE_KEY in DependencyHandler with your test key](example/src/main/java/com/stripe/example/module/DependencyHandler.java#L30).

Three different ways of creating tokens are shown, with all the Stripe-specific logic needed for each separated into the three controllers,
[AsyncTaskTokenController](example/src/main/java/com/stripe/example/controller/AsyncTaskTokenController.java), [RxTokenController](example/src/main/java/com/stripe/example/controller/RxTokenController.java), and [IntentServiceTokenController](example/src/main/java/com/stripe/example/controller/IntentServiceTokenController.java).
