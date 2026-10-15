# Standalone Express Checkout Element (internal)

The APIs are currently restricted to the SDK library group.

`StandaloneExpressCheckoutElement` wraps an `ExpressCheckoutElement` for use without a Checkout
Session. The wrapper loads wallet availability and ordering from `elements/session`, and confirms
PaymentIntents or SetupIntents using the shared authentication flow. It owns configuration and
callbacks, and exposes the display-only component through `expressCheckoutElement`. Reconfigure
the wrapper directly when the intent or wallet configuration changes.

Initialize `PaymentConfiguration` with your publishable key first. In Compose, create the element
unconditionally during initialization:

```kotlin
@Composable
fun ExpressCheckout() {
    val standaloneElement = rememberStandaloneExpressCheckoutElement(
        StandaloneExpressCheckoutElement.Builder(
            resultCallback = { result -> /* Handle Completed, Canceled, or Failed. */ },
        ).createIntentCallback { paymentMethod, shouldSavePaymentMethod ->
            // Call your server to create a PaymentIntent or SetupIntent. Return its client secret.
            // Send paymentMethod.id, and set setup_future_usage when shouldSavePaymentMethod is true.
            // Return CreateIntentResult.Failure when the request fails.
            createIntentOnYourServer(paymentMethod.id, shouldSavePaymentMethod)
        }
    )
    LaunchedEffect(standaloneElement) {
        standaloneElement.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1099,
                    currency = "usd",
                ),
            ),
            configuration = StandaloneExpressCheckoutElement.Configuration("Example Store")
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                        currencyCode = "usd",
                    )
                ),
        ).getOrThrow() // Handle configuration failures in your app.
    }
    standaloneElement.expressCheckoutElement.Content()
}
```

`createIntentOnYourServer` represents your own server call and returns `CreateIntentResult`.
The callback runs after the customer supplies their wallet payment details. The SDK confirms the
returned intent and handles any required authentication. For deferred setup, use
`PaymentSheet.IntentConfiguration.Mode.Setup(currency = "usd")` and return a SetupIntent client
secret instead.

For an intent created ahead of time, call `standaloneElement.configureWithPaymentIntent(clientSecret, configuration)`
or `standaloneElement.configureWithSetupIntent(clientSecret, configuration)`. These flows do not require a
`createIntentCallback`. Always supply a Google Pay currency code when using a SetupIntent.

For activity-based initialization, use `StandaloneExpressCheckoutElement.Builder(resultCallback).build(activity, key = "checkout")`
before the activity starts. Pass a distinct, stable `key` when creating multiple elements. The
same key restores the element's state across activity recreation.

## Google Pay updates

To collect shipping information, configure
`StandaloneExpressCheckoutElement.Configuration.shippingAddress(required = true, allowedCountryCodes = setOf("US"))`.
Link is hidden when a shipping address is required.

Register `Builder.googlePayPaymentDataUpdateCallback` to handle shipping address changes and
Google Pay initialization. The callback receives `GooglePayPaymentDataUpdate` and returns a
`GooglePayPaymentDataUpdateResponse`. Return `newTransactionInfo` to change the total or `error`
to explain why an address cannot be used. Return an empty response when no change is necessary.

Update your order on your server before returning the response. For an existing intent, update
that intent on your server as needed. For a deferred intent, create it from the latest order when
`createIntentCallback` runs. The SDK does not calculate shipping or tax and does not create or
update intents on your behalf.

## Wallet configuration

Pass `StandaloneExpressCheckoutElement.Configuration.elementConfiguration(ExpressCheckoutElement.Configuration())` to
customize visibility, appearance, and payment method ordering. Explicit `paymentMethodOrder`
values override the session's wallet ordering; unavailable wallets remain hidden. Enable Google
Pay through `StandaloneExpressCheckoutElement.Configuration.googlePay`; Link is enabled automatically when available.

## Playground

In the PaymentSheet example app, choose **Express Checkout Element** under **Integration Type**,
configure the settings, and tap **Reload**. Wallet buttons appear below Reload after configuration.

- Use **Initialization → Normal** to configure an existing intent, or **Deferred CSC** to create
  the intent in the merchant callback. The existing playground backend supplies the intent and keys.
- Use **Checkout Mode → Pay**, **P & S**, or **Setup** to exercise payment, payment with future use,
  or setup. Currency, merchant country, Google Pay, Link, and default billing settings are shared
  with the existing playground.
- **Express Checkout wallet order → Session** uses `elements/session` ordering. The other options
  explicitly put Google Pay or Link first.
- **Express Checkout shipping → Accept** requests a shipping address and accepts callback updates.
  **Reject** returns a shipping-address error to Google Pay when an address is provided. **Off**
  disables shipping collection. The last callback trigger and outcome appear below the buttons.

The shipping callback exercises merchant-controlled responses without changing the transaction
amount. Shipping collection hides Link, so use **Off** when testing both wallets together.
