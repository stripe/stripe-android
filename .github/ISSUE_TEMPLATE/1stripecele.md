![flags](https://github.com/stripe/stripe-android/assets/99934048/ccf0ffd9-e7db-4284-bbd3-47a108133b9e)
"Skip to content

Home
Dashboard
HomePaymentsConnect
Enable other businesses to accept payments directly
Facilitate direct payments between other businesses and their own customers.


Web

iOS

Android

Mobile Payment Element

Card element only
This guide covers letting your users accept payments, moving a portion of your users’ earnings into your balance, and paying out the remainder to your users’ bank accounts. To illustrate these concepts, we’ll use an example platform that lets businesses build their own online stores.

Securely collect card information on the client with CardInputWidget, a drop-in UI component provided by the SDK that collects the card number, expiration date, CVC, and postal code.

CardInputWidget performs on-the-fly validation and formatting.

Prerequisites

Register your platform


Add business details to activate your account


Complete your platform profile


Customize brand settings

Adding a business name, icon, and brand color is required for Connect Onboarding.
Set up Stripe
Server-side
Client-side
Server-side
This integration requires endpoints on your server that talk to the Stripe API. Use the official libraries for access to the Stripe API from your server:


Ruby

Python

PHP

Java

Node

Go

.NET
Command Line


# Available as a gem
sudo gem install stripe
Gemfile


# If you use bundler, you can add this line to your Gemfile
gem 'stripe'
Client-side
The Stripe Android SDK is open source and fully documented.

To install the SDK, add stripe-android to the dependencies block of your app/build.gradle file:

build.gradle


apply plugin: 'com.android.application'

android { ... }

dependencies {
  // ...

  // Stripe Android SDK
  implementation 'com.stripe:stripe-android:20.29.1'
}
Note
For details on the latest SDK release and past versions, see the Releases page on GitHub. To receive notifications when a new release is published, watch releases for the repository.

Configure the SDK with your Stripe publishable key so that it can make requests to the Stripe API, such as in your Application subclass:


Kotlin

Java


import com.stripe.android.PaymentConfiguration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51NjkyXDtk01xQ3OYiTUrnUuF6lfq8eAP9p17YJBiTxlttYtqnzMf7fQvANHPAJKUSmZxmvVhLQiTtP7JJFUz2dvI00VbC8yudN""pk_test_51NjkyXDtk01xQ3OYiT...JJFUz2dvI00VbC8yudN"
        )
    }
}
Note
Use your test mode keys while you test and develop, and your live mode keys when you publish your app.

Stripe samples also use OkHttp and GSON to make HTTP requests to a server.

Create a connected account
CREATING A CONNECTED ACCOUNT WITHOUT CODE
Use this guide to learn how to use code to create a connected account. If you’re not ready to integrate yet, you can start by creating a connected account through the dashboard.

When a user (seller or service provider) signs up on your platform, create a user Account (referred to as a connected account) so you can accept payments and move funds to their bank account. Connected accounts represent your user in Stripe’s API and help facilitate the collection of onboarding requirements so Stripe can verify the user’s identity. In our store builder example, the connected account represents the business setting up their Internet store.


Step 2.1: Create a Standard account and prefill information
Server-side
Use the /v1/accounts API to create a Standard account and set type to standard in the account creation request.


curl

Stripe CLI

Ruby

Python

PHP

Java

Node

Go

.NET
Command Line



curl https://api.stripe.com/v1/accounts \
  -u "sk_test_51NjkyXDtk01xQ3OYUCeJcKS6VBldRJOP93CiKqzFI2BCWNGdZPVc9ayJZh85ZRp5c1k65ExBR9jpEYL69Y8WoLug00swQBqVBPsk_test_51NjkyXDtk01xQ3OYUCe...L69Y8WoLug00swQBqVBP:" \
  -d type=standard
If you’ve already collected information for your connected accounts, you can prefill that information on the account object for the user. Connect Onboarding won’t ask for the prefilled information during account onboarding. The account holder confirms the prefilled information before accepting the Connect service agreement. You can prefill any information on the account, including personal and business information, external account information, and more.

Step 2.2: Create an account link
Server-side
You can create an account link by calling the Account Links API with the following parameters:

account
refresh_url
return_url
type = account_onboarding

curl

Stripe CLI

Ruby

Python

PHP

Java

Node

Go

.NET
Command Line



curl https://api.stripe.com/v1/account_links \
  -u "sk_test_51NjkyXDtk01xQ3OYUCeJcKS6VBldRJOP93CiKqzFI2BCWNGdZPVc9ayJZh85ZRp5c1k65ExBR9jpEYL69Y8WoLug00swQBqVBPsk_test_51NjkyXDtk01xQ3OYUCe...L69Y8WoLug00swQBqVBP:" \
  -d account={{CONNECTED_ACCOUNT_ID}} \
  --data-urlencode refresh_url="https://example.com/reauth" \
  --data-urlencode return_url="https://example.com/return" \
  -d type=account_onboarding
Step 2.3: Redirect your user to the account link URL
Client-side
The response to your Account Links request includes a value for the key url. Redirect to this link to send your user into the flow. URLs from the Account Links API are temporary and can be used only once because they grant access to the account holder’s personal information. Authenticate the user in your application before redirecting them to this URL. If you want to prefill information, you must do so before generating the account link. After you create the account link for a Standard account, you will not be able to read or write information for the account.

Warning
Don’t email, text, or otherwise send account link URLs directly to your user. Instead, redirect the authenticated user to the account link URL from within your platform’s application.

activity_connect_with_stripe.xml


<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".activity.ConnectWithStripeActivity">

    <Button
        android:id="@+id/connect_with_stripe"
        android:text="Connect with Stripe"
        android:layout_height="wrap_content"
        android:layout_width="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        style="?attr/materialButtonOutlinedStyle"
        />

</androidx.constraintlayout.widget.ConstraintLayout>

Kotlin

Java
ConnectWithStripeActivity.kt


class ConnectWithStripeActivity : AppCompatActivity() {

    private val viewBinding: ActivityConnectWithStripeViewBinding by lazy {
        ActivityConnectWithStripeViewBinding.inflate(layoutInflater)
    }
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.connectWithStripe.setOnClickListener {
            val weakActivity = WeakReference<Activity>(this)
            val request = Request.Builder()
                .url(BACKEND_URL + "onboard-user")
                .post("".toRequestBody())
                .build()
            httpClient.newCall(request)
                .enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Request failed
                    }
                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            // Request failed
                        } else {
                            val responseData = response.body?.string()
                            val responseJson =
                                responseData?.let { JSONObject(it) } ?: JSONObject()
                            val url = responseJson.getString("url")

                            weakActivity.get()?.let {
                                val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
                                val customTabsIntent = builder.build()
                                customTabsIntent.launchUrl(it, Uri.parse(url))
                            }
                        }
                    }
                })
        }
    }

    internal companion object {
        internal const val BACKEND_URL = "https://example-backend-url.com/"
    }
}
Step 2.4: Handle the user returning to your platform
Client-side
Connect Onboarding requires you to pass both a return_url and refresh_url to handle all cases where the user will be redirected to your platform. It’s important that you implement these correctly to provide the best experience for your user. You can set up a deep link to enable Android to redirect to your app automatically.

return_url
Stripe issues a redirect to this URL when the user completes the Connect Onboarding flow. This does not mean that all information has been collected or that there are no outstanding requirements on the account. This only means the flow was entered and exited properly.

No state is passed through this URL. After a user is redirected to your return_url, check the state of the details_submitted parameter on their account by doing either of the following:

Listening to account.updated webhooks
Calling the Accounts API and inspecting the returned object
refresh_url
Your user are redirected to the refresh_url in these cases:

The link is expired (a few minutes went by since the link was created)
The link was already visited (the user refreshed the page or clicked back or forward in the browser)
The link was shared in a third-party application such as a messaging client that attempts to access the URL to preview it. Many clients automatically visit links which cause them to become expired
Your platform is no longer able to access the account
The account has been rejected
Your refresh_url should trigger a method on your server to call Account Links again with the same parameters, and redirect the user to the Connect Onboarding flow to create a seamless experience.

Step 2.5: Handle users that haven’t completed onboarding
A user that is redirected to your return_url might not have completed the onboarding process. Use the /v1/accounts endpoint to retrieve the user’s account and check for charges_enabled. If the account isn’t fully onboarded, provide UI prompts to allow the user to continue onboarding later. The user can complete their account activation through a new account link (generated by your integration). You can check the state of the details_submitted parameter on their account to see if they’ve completed the onboarding process.

Accept a payment
Step 3.1: Create your checkout page
Client-side
Securely collect card information on the client with CardInputWidget, a drop-in UI component provided by the SDK that collects the card number, expiration date, CVC, and postal code.

CardInputWidget performs on-the-fly validation and formatting.

Create an instance of the card component and a Pay button by adding the following to your checkout page’s layout:

activity_checkout.xml


<?xml version="1.0" encoding="utf-8"?>

<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="20dp"
    tools:context=".CheckoutActivityKotlin"
    tools:showIn="@layout/activity_checkout">

    <!--  ...  -->

    <com.stripe.android.view.CardInputWidget
        android:id="@+id/cardInputWidget"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <Button
        android:text="@string/pay"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:id="@+id/payButton"
        android:layout_marginTop="20dp"
        android:backgroundTint="@android:color/holo_green_light"/>

    <!--  ...  -->

</LinearLayout>
Run your app, and make sure your checkout page shows the card component and pay button.

Step 3.2: Create a PaymentIntent
Server-side
Client-side
Stripe uses a PaymentIntent object to represent your intent to collect payment from a customer, tracking your charge attempts and payment state changes throughout the process.

Server-side
On your server, make an endpoint that creates a PaymentIntent with an amount and currency. Always decide how much to charge on the server side, a trusted environment, as opposed to the client. This prevents malicious customers from being able to choose their own prices.


curl

Ruby

Python

PHP

Java

Node

Go

.NET
Command Line


curl https://api.stripe.com/v1/payment_intents \
  -u sk_test_51NjkyXDtk01xQ3OYUCeJcKS6VBldRJOP93CiKqzFI2BCWNGdZPVc9ayJZh85ZRp5c1k65ExBR9jpEYL69Y8WoLug00swQBqVBPsk_test_51NjkyXDtk01xQ3OYUCe...L69Y8WoLug00swQBqVBP: \
  -d amount=1000 \
  -d currency="usd" \
  -d "automatic_payment_methods[enabled]"=true \
  -d application_fee_amount="" \
  -H "Stripe-Account: {{CONNECTED_STRIPE_ACCOUNT_ID}}"
In our store builder example, we want to build an experience where customers pay businesses directly. To set this experience up:

Indicate a purchase from the business is a direct charge with the Stripe-Account header.
Specify how much of the purchase from the business will go to the platform with application_fee_amount.
When a sale occurs, Stripe transfers the application_fee_amount from the connected account to the platform and deducts the Stripe fee from the connected account’s share. An illustration of this funds flow is below:


Instead of passing the entire PaymentIntent object to your app, just return its client secret. The PaymentIntent’s client secret is a unique key that lets you confirm the payment and update card details on the client, without allowing manipulation of sensitive information, like payment amount.

Client-side
Set the connected account id as an argument to the client application in the client-side libraries.


Kotlin

Java


import com.stripe.android.PaymentConfiguration

class MyActivity: Activity() {
    private lateinit var stripe: Stripe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stripe = Stripe(
            this,
            PaymentConfiguration.getInstance(this).publishableKey,
            "{{CONNECTED_STRIPE_ACCOUNT_ID}}"
        )
    }
}
On the client, request a PaymentIntent from your server and store its client secret.


Kotlin

Java
CheckoutActivity.kt
View full sample


class CheckoutActivity : AppCompatActivity() {

  private lateinit var paymentIntentClientSecret: String

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      // ...
      startCheckout()
  }

  private fun startCheckout() {
      // Request a PaymentIntent from your server and store its client secret in paymentIntentClientSecret
      // Click View full sample to see a complete implementation
  }
}
Step 3.3: Submit the payment to Stripe
Client-side
When the customer taps the Pay button, confirm the PaymentIntent to complete the payment.

First, assemble a ConfirmPaymentIntentParams object with:

The card component’s payment method details
The PaymentIntent client secret from your server
Rather than sending the entire PaymentIntent object to the client, use its client secret. This is different from your API keys that authenticate Stripe API requests. The client secret is a string that lets your app access important fields from the PaymentIntent (for example, status) while hiding sensitive ones (for example, customer).

The client secret should still be handled carefully because it can complete the charge. Do not log it, embed it in URLs, or expose it to anyone but the customer.

Next, complete the payment by calling the PaymentLauncher confirm method.


Kotlin

Java
CheckoutActivity.kt


class CheckoutActivity : AppCompatActivity() {
    // ...
    private lateinit var paymentIntentClientSecret: String
    private lateinit var paymentLauncher: PaymentLauncher

    private fun startCheckout() {
        // ...

        // Hook up the pay button to the card widget and stripe instance
        val payButton: Button = findViewById(R.id.payButton)
        payButton.setOnClickListener {
            val params = cardInputWidget.paymentMethodCreateParams
            if (params != null) {
                val confirmParams = ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(params, paymentIntentClientSecret)
                val paymentConfiguration = PaymentConfiguration.getInstance(applicationContext)
                paymentLauncher = PaymentLauncher.Companion.create(
                    this,
                    paymentConfiguration.publishableKey,
                    paymentConfiguration.stripeAccountId,
                    ::onPaymentResult
                )
                paymentLauncher.confirm(confirmParams)
            }
        }
    }

    private fun onPaymentResult(paymentResult: PaymentResult) {
        val message = when (paymentResult) {
            is PaymentResult.Completed -> {
                "Completed!"
            }
            is PaymentResult.Canceled -> {
                "Canceled!"
            }
            is PaymentResult.Failed -> {
                // This string comes from the PaymentIntent's error message.
                // See here: https://stripe.com/docs/api/payment_intents/object#payment_intent_object-last_payment_error-message
                "Failed: " + paymentResult.throwable.message
            }
        }
        displayAlert(
            "PaymentResult: "
            message,
            restartDemo = true
        )
    }
}
If authentication is required by regulation such as Strong Customer Authentication, the SDK presents additional activities and walks the customer through that process. See Supporting 3D Secure Authentication on Android to learn more.

When the payment completes, onSuccess is called and the value of the returned PaymentIntent’s status is Succeeded. Any other value indicates the payment was not successful. Inspect lastPaymentError to determine the cause.

You can also check the status of a PaymentIntent in the Dashboard or by inspecting the status property on the object.

Step 3.4: Test the integration
Client-side
​​Several test cards are available for you to use in test mode to make sure this integration is ready. Use them with any CVC and an expiration date in the future.

NUMBER	DESCRIPTION
4242424242424242	Succeeds and immediately processes the payment.
4000002500003155	Requires authentication. Stripe triggers a modal asking for the customer to authenticate.
4000000000009995	Always fails with a decline code of insufficient_funds.
For the full list of test cards see our guide on testing.

Step 3.5: Fulfillment
Server-side
After payment completes, you must handle any necessary fulfillment. For example, a store builder must alert the business to send the purchased item to the customer.

Configure a webhook endpoint in your dashboard (for events from your Connect applications).


Then create an HTTP endpoint on your server to monitor for completed payments to then enable your users (connected accounts) to fulfill purchases.


Ruby

Python

PHP

Java

Node

Go

.NET
server.rb


# Using Sinatra.
require 'sinatra'
require 'stripe'

set :port, 4242

# Set your secret key. Remember to switch to your live secret key in production.
# See your keys here: https://dashboard.stripe.com/apikeys
Stripe.api_key = 'sk_test_51NjkyXDtk01xQ3OYUCeJcKS6VBldRJOP93CiKqzFI2BCWNGdZPVc9ayJZh85ZRp5c1k65ExBR9jpEYL69Y8WoLug00swQBqVBP''sk_test_51NjkyXDtk01xQ3OYUC...69Y8WoLug00swQBqVBP'

# If you are testing your webhook locally with the Stripe CLI you
# can find the endpoint's secret by running `stripe listen`
# Otherwise, find your endpoint's secret in your webhook settings in
# the Developer Dashboard
endpoint_secret = 'whsec_...'

post '/webhook' do
  payload = request.body.read
  sig_header = request.env['HTTP_STRIPE_SIGNATURE']

  event = nil

  # Verify webhook signature and extract the event.
  # See https://stripe.com/docs/webhooks#verify-events for more information.
  begin
    event = Stripe::Webhook.construct_event(
      payload, sig_header, endpoint_secret
    )
  rescue JSON::ParserError => e
    # Invalid payload.
    status 400
    return
  rescue Stripe::SignatureVerificationError => e
    # Invalid Signature.
    status 400
    return
  end

  if event['type'] == 'payment_intent.succeeded'
    payment_intent = event['data']['object']
    connected_account_id = event['account']
    handle_successful_payment_intent(connected_account_id, payment_intent)
  end

  status 200
end

def handle_successful_payment_intent(connected_account_id, payment_intent)
  # Fulfill the purchase
  puts 'Connected account ID: ' + connected_account_id
  puts payment_intent.to_s
end
Learn more in our fulfillment guide for payments.

Testing webhooks locally
Testing webhooks locally is easy with the Stripe CLI.

First, install the Stripe CLI on your machine if you haven’t already.

Then, to log in run stripe login in the command line, and follow the instructions.

Finally, to allow your local host to receive a simulated event on your connected account run stripe listen --forward-connect-to localhost:{PORT}/webhook in one terminal window, and run stripe trigger --stripe-account={{CONNECTED_STRIPE_ACCOUNT_ID}} payment_intent.succeeded (or trigger any other supported event) in another.

Testing
Test your account creation flow by creating accounts and using OAuth. Test your payment method settings for your connected accounts by logging into one of your test accounts and navigating to the payment method settings. Test your checkout flow with your test keys and a test account. You can use the available test cards to test your payments flow and simulate various payment outcomes.

Payouts
By default, any charge that you create for a connected account accumulates in the connected account’s Stripe balance and is paid out on a daily rolling basis. Standard accounts manage their own payout schedules in their Dashboard.

See also
Manage connected accounts in the Dashboard
Issue refunds
Customize statement descriptors
Work with multiple currencies
Was this page helpful?

Yes

No
Need help? Contact Support.
Watch our developer tutorials.
Check out our product changelog.
Questions? Contact Sales.


Sign up for developer updates:


Sign up
You can unsubscribe at any time. Read our privacy policy.
"
 https://stripe.com/docs/connect/enable-payment-acceptance-guide?platform=android&ui=custom#:~:text=Skip%20to%20content,our%20privacy%20policy.
