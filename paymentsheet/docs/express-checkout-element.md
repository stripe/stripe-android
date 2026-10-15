# Elements Controller and Express Checkout Element (internal)

The APIs are currently restricted to the SDK library group.

`ElementsController` owns the intent configuration, elements/session metadata, payment selection,
and confirmation lifecycle shared by its elements. It supports deferred intent creation for
PaymentIntents and SetupIntents. It currently exposes the display-only `expressCheckoutElement`;
reconfigure the controller when the intent configuration changes.

The controller's `session` flow publishes the loaded intent metadata, and `isUpdating` tracks
configuration and confirmation across all elements. The session
contains the placeholder intent returned by elements/session. A single saved state holder,
loader, and operation coordinator back the controller. Future Payment Element and Shipping
Address Element adapters can use that same state and lifecycle; those integrations are not
implemented yet. Checkout and Elements Controller share the Express Checkout interactor,
analytics reporter, wallet confirmation mapping, and Elements loader configuration. Each adapter
supplies its integration state and actions to those shared implementations.

Initialize `PaymentConfiguration` with your publishable key first. Like `CheckoutController`,
create an independent controller from application context and a `SavedStateHandle`. It does not
require an Activity, ViewModel, or external coroutine scope:

```kotlin
val elementsController = ElementsController.Builder(
    application = application,
    savedStateHandle = savedStateHandle,
).resultCallback { result ->
    // Handle Completed, Canceled, or Failed.
}.createIntentCallback { paymentMethod, shouldSavePaymentMethod ->
    // Call your server to create a PaymentIntent or SetupIntent. Return its client secret.
    // Send paymentMethod.id, and set setup_future_usage when shouldSavePaymentMethod is true.
    // Return CreateIntentResult.Failure when the request fails.
    createIntentOnYourServer(paymentMethod.id, shouldSavePaymentMethod)
}.integrationName("checkout").build()
```

Use a distinct `integrationName` for each controller sharing a saved state handle; the default
is `"stripe_elements"`. An owner that does not need state restoration can supply a fresh
`SavedStateHandle()`. The controller owns its coroutine scope, callbacks, and saved state until
the owner calls `elementsController.destroy()`. Retain one instance for the lifetime of the
integration; recreation of an activity does not require rebuilding the controller.

Configure from a coroutine before displaying an element:

```kotlin
elementsController.configure(
    intentConfiguration = PaymentSheet.IntentConfiguration(
        mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = 1099, currency = "usd"),
    ),
    configuration = ElementsController.Configuration("Example Store")
        .expressCheckoutElement(ExpressCheckoutElement.Configuration())
        .googlePay(
            PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "US",
                currencyCode = "usd",
            )
        ),
).getOrThrow() // Handle configuration failures in your app.
```

`createIntentOnYourServer` represents your own server call and returns `CreateIntentResult`.
The callback runs after the customer supplies their wallet payment details. The SDK confirms the
returned intent and handles required authentication. For deferred setup, use
`PaymentSheet.IntentConfiguration.Mode.Setup(currency = "usd")` and return a SetupIntent client
secret instead.

`createIntentCallback` is required. Always supply a Google Pay currency code when using setup mode.

## Activity registration and ownership

In every activity instance, call `elementsController.register(this)` from `onCreate` after
`super.onCreate`, before the activity starts. This binds payment launchers to the current activity;
they unregister when it is destroyed. Render `elementsController.expressCheckoutElement.Content()`
from Compose. Activity registration does not own or destroy the controller.

A ViewModel is one possible owner. The playground builds its controller in its ViewModel, performs
configuration there, registers launchers in the activity, and calls `elementsController.destroy()`
from `onCleared`. Other owners can build and release the controller in their own lifecycle:

```kotlin
class CheckoutViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    val elementsController = ElementsController.Builder(application, savedStateHandle)
        .resultCallback { result -> /* Handle the result. */ }
        .createIntentCallback { paymentMethod, shouldSavePaymentMethod ->
            createIntentOnYourServer(paymentMethod.id, shouldSavePaymentMethod)
        }
        .build()

    override fun onCleared() {
        elementsController.destroy()
        super.onCleared()
    }
}
```

## Shipping address changes

To collect shipping information, configure
`ElementsController.Configuration.shippingAddress(required = true, allowedCountryCodes = setOf("US"))`.
Link is hidden when a shipping address is required.

Register `Builder.shippingAddressChangeCallback`. The callback and all event/result types live under
`ElementsController.ExpressCheckoutElement` and work with wallet-neutral data. Like the
[web Express Checkout shipping events](https://docs.stripe.com/js/elements_object/express_checkout_element_shippingratechange_event),
a shipping address change must resolve or reject. Kotlin uses a suspend callback that returns the result:

```kotlin
ElementsController.Builder(application, savedStateHandle)
    .resultCallback(resultCallback)
    .createIntentCallback(createIntentCallback)
    .shippingAddressChangeCallback { event ->
        // The wallet may redact the address; country, state, city, and postalCode are available.
        val order = updateOrderOnYourServer(event.address)
        if (order.canShip) {
            ElementsController.ExpressCheckoutElement.ShippingChangeResult.Resolve(
                amount = order.total,
                lineItems = order.items.map {
                    ElementsController.ExpressCheckoutElement.LineItem(it.name, it.amount)
                },
            )
        } else {
            ElementsController.ExpressCheckoutElement.ShippingChangeResult.Reject(
                message = "We cannot ship to this address.",
            )
        }
    }
```

Amounts use the intent currency's smallest unit. A null amount or lineItems preserves its previous
value; an empty lineItems list clears the displayed items. Currency comes from the intent (or the
Google Pay configuration for setup) and cannot change in a shipping response. The callback can be
called for the wallet's initial address as well as subsequent address changes. Google Pay translates
these generic events and results internally. Shipping rate selection is not exposed yet.

Update your order on your server before resolving, then create the intent from the latest order
when `createIntentCallback` runs. Resolving changes the wallet display; the SDK does not calculate
shipping or tax or update intents on your behalf.

## Wallet configuration

Pass `ElementsController.Configuration.expressCheckoutElement(ExpressCheckoutElement.Configuration())` to
customize visibility, appearance, and payment method ordering. Explicit `paymentMethodOrder`
values override the session's wallet ordering; unavailable wallets remain hidden. Enable Google
Pay through `ElementsController.Configuration.googlePay`; Link is enabled automatically when available.

## Playground

In the PaymentSheet example app, choose **Elements Controller** under **Integration Type**,
configure the settings, and tap **Reload**. The playground configures an `ElementsController` and
displays its Express Checkout Element below Reload.

- **Initialization → Deferred CSC** is the only supported flow. The merchant callback requests
  intent creation from the existing playground backend.
- Use **Checkout Mode → Pay**, **P & S**, or **Setup** to exercise payment, payment with future use,
  or setup. Currency, merchant country, Google Pay, Link, and default billing settings are shared
  with the existing playground.
- **Elements: Express Checkout wallet order → Session** uses `elements/session` ordering. The other options
  explicitly put Google Pay or Link first.
- **Elements: shipping → Accept** requests a shipping address and accepts callback updates.
  **Reject** returns a shipping-address error to Google Pay when an address is provided. **Off**
  disables shipping collection. The last address country and callback outcome appear below the buttons.

The shipping callback exercises merchant-controlled acceptance, rejection, and line-item updates.
It preserves the configured amount and currency. Shipping collection hides Link, so use **Off**
when testing both wallets together.
