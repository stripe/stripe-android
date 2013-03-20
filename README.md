##**stripe-android**

Stripe-android makes it easy to collect credit card information without having sensitive details touch your server.

These Stripe Android bindings can be used to generate tokens in your Android application. If you are building an Android application that charges a credit card, you should use stripe-android to make sure you don't pass credit card information to your server (and, so, are PCI compliant).

##**Installation**

Add stripe-android.jar to your module's libs directory.

##**setPublishableKey**

A publishable key is required to identify your website when communicating with Stripe. Remember to replace the test key with your live key in production.
You can get all your keys from your [account page](https://manage.stripe.com/#account/apikeys).
This [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.

    new Stripe("YOUR_PUBLISHABLE_KEY");

or

    new Stripe().setPublishableKey("YOUR_PUBLISHABLE_KEY");

##**createToken**

createToken converts sensitive card data to a single-use token which you can safely pass to your server to charge the user. The [tutorial](https://stripe.com/docs/tutorials/forms) explains this flow in more detail.

    new Stripe('YOUR_PUBLISHABLE_KEY').createToken(new Card("4242-4242-4242-4242", "12", "2014", "123"),
        successHandler,
        errorHandler
    );

The first argument to createToken is a Card object. You can create a Card with a simple Map, a JSONObject, JSON string, or by specifying fields directly.

Constructing a Card from a Map<String, String> or JSON should contain the following required fields:

+ number: card number as a string without any separators, e.g. '4242424242424242'.
+ exp_month: two digit number as a string representing the card's expiration month, e.g. 12.
+ exp_year: four digit number as a string representing the card's expiration year, e.g. 2013.

The following fields are optional but recommended to help prevent fraud:

+ cvc: card security code as a string, e.g. '123'.

The following fields are entirely optional — they cannot result in a token creation failing:

+ name: cardholder name.
+ address_line1: billing address line 1.
+ address_line2: billing address line 2.
+ address_city: billing address city.
+ address_state: billing address state.
+ address_zip: billing zip as a string, e.g. '94301'.
+ address_country: billing address country.

The second argument stripeSuccessHandler is a callback you provide to handle a successful response from Stripe.
It should send the token to your server for processing.

The third argument stripeErrorHandler is a callback you provide to handle an unsuccessful response from Stripe.
It should notify the user of the error.

Here's a sample implementation of the success and error handlers:

    stripe.createToken(card,
        new StripeSuccessHandler() {
            public void onSuccess(Token token) {
                // Send token to your own web service
                MyServer.chargeToken(token);
            }
        },
        new StripeErrorHandler() {
            public void onError(StripeError error) {
                Log.d("your.application.package.class", error.developerMessage);
                Toast.makeText(getContext(), error.getLocalizedString(getContext()), Toast.LENGTH_LONG).show();
            }
        }
    );

createToken is an asynchronous call – it returns immediately and invokes the success or error handler on the UI thread when it receives a response from Stripe's servers.

#**Client-side validation helpers**

The Card object allows you to validate user input before you send the information to Stripe.

##**validateNumber**

Checks that the number is formatted correctly and passes the [Luhn check](http://en.wikipedia.org/wiki/Luhn_algorithm).

##**validateExpiryDate**

Checks whether or not the expiration date represents an actual month in the future.

TODO: Currently this always returns true. To be implemented.

##**validateCVC**

Checks whether or not the supplied number could be a valid verification code.

##**Handling client side validation errors**

The validate methods will return a Validation object that holds an isValid and List<StripeError> errors field.

    Validation validation = card.validateCard();
    if (validation.isValid) {
        validation.getLocalizedErrors(getContext());
    }

#**Retrieving information about a token**

If you're implementing a complex workflow, you may want to know if you've already charged a token (since they can only be charged once). You can call requestToken with a token id and callbacks to find out whether or not the token has already been used. This will return an object with the same structure as the object returned from createToken.

    stripe.requestToken(tokenID,
        new StripeSuccessHandler() {
            public void onSuccess(Token token) {
                if (token.used) {
                    Log.d("Token has already been charged.");
                }
            }
        },
    , errorHandler);

#**Enabling log output**

If you are debugging and would like stripe-android to produce log output, you can set a log level as follows:

    StripeLog.setLogLevel(android.util.Log.DEBUG);

_Note:  We have omitted personal and/or credit card information from all log levels, to protect end-user security and confidentiality._


#**Building the example project**

If you'd like to build the example Android project:

1. Clone the git repository.
2. Be sure you've installed the Android SDK with API Level 17 and _android-support-v4_
3. Import the _example_ and _stripe_ folders into [Eclipse](http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.user/tasks/tasks-importproject.htm).
4. [Replace the value of PUBLISHABLE_KEY in PaymentActivity with your stripe test key](https://github.com/stripe/stripe-android/blob/master/example/src/main/java/com/stripe/example/activity/PaymentActivity.java#L30).
5. Build and run the project on your device or in the Android emulator.