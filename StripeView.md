#**StripeView**
    
`StripeView` is a custom view for accepting card numbers, dates and CVCs. It formats the input and validates it on the fly.

![StripeView](StripeView-jellybean.png)

##**Step 1: Collecting card information**

To use `StripeView` to collect payment information, add the [Stripe android library](https://github.com/stripe/stripe-android) to your Eclipse project:

1. Clone the git repository.
2. Be sure you've installed the Android SDK with API Level 17 and _android-support-v4_
3. Import the _stripe_ folder into [Eclipse](http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.user/tasks/tasks-importproject.htm).
4. In your project settings, add the _stripe_ project under the "Libraries" section of the "Android" category.

Next add `StripeView` to your layout xml file:

    <com.stripe.android.widget.StripeView
              android:id="@+id/stripe"
              android:layout_width="match_parent"
              android:layout_height="wrap_content" />

Define a member variable in your Activity or Fragment:

    private StripeView mStripeView;
            
Grab the `StripeView` in `onCreate`:

    mStripeView = (StripeView) findViewById(R.id.stripe);

Define a `OnValidationChangeListener` so you are notified when all the card data is added and valid.

    private StripeView.OnValidationChangeListener mValidationListener
        = new StripeView.OnValidationChangeListener() {
            @Override
            public void onChange(boolean valid) {
                mSaveButton.setEnabled(valid);
            }
    };

In the callback, for example, we could enable a 'save button' that allows users to submit their valid cards.

Register the listener `onResume`:

    mStripeView.registerListener(mValidationListener);

And unregister it `onPause`:

    mStripeView.registerListener(mValidationListener);
    
###**Themes**

`StripeView` uses the `EditText` style from your theme. For instance, the [example application](https://github.com/stripe/stripe-android/tree/master/example) uses `Theme.Black.NoTitleBar` as default theme, which looks like a white box: 

![StripeView](StripeView-gingerbread.png)

On devices Honeycomb and above it uses the `Theme.Holo.NoActionBar` theme, which gives a transparent background with a underline bar:

![StripeView](StripeView-jellybean.png)              

`StripeView` supports different layouts by equally dividing the available widths among the card number, expiry date and CVC.
    
##**Step 2: Creating a single use token**

With our mobile library, we shoulder the burden of PCI compliance by helping you avoid the need to send card data directly to your server. Instead, our libraries send the card data directly to our servers, where we can convert them to tokens. You can charge these tokens later in your server-side code.

Once the `StripeView` is valid, you can call the `createToken` method, instructing the library to send off the credit card data to Stripe and return a token.

    public void saveCreditCard(View view) {
        mStripeView.createToken(PUBLISHABLE_KEY, new TokenCallback() {
            public void onSuccess(Token token) {
                // Send off token to your server
                // handleToken(token);
            }
            public void onError(Exception error) {
                // Handle error
                // handleError(error.getLocalizedMessage());
            }
        });
    }
    
In the example above, we're calling `createToken` when a save button is tapped. The important thing to ensure is the `createToken` isn't called before the card data is valid (otherwise it will trigger the `onError` callback).

Handling error messages and network pending notifications are up to you. In the full example we use `ProgressDialog` to show a spinner whenever the network is pending, and handle network errors by showing a `AlertDialog`.

##**Step 3: Sending the token to your server**

The `TokenCallback` you gave to `createToken` will be called whenever Stripe returns with a token (or error). You'll need to send the token off to your server so you can, for example, [charge the card](https://stripe.com/docs/tutorials/charges). Make sure any communication with your server is [SSL secured](https://stripe.com/help/ssl) to prevent eavesdropping.

Now we have a Stripe token representing a card on our server we can go ahead and charge it, save it for charging later, or sign the user up for a subscription. For more information, take a look at our tutorial on [creating charges](https://stripe.com/docs/tutorials/charges).

Take a look at the full [example application](https://github.com/stripe/stripe-android/tree/master/example) to see everything put together.
