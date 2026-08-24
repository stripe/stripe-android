# API credential propagation migration

## Scope

Compared with `master`, `tyler/payment-config-phase-6` changes how the SDK obtains and propagates the
publishable key and optional Stripe account ID. The merchant-facing `PaymentConfiguration.init()` API
remains supported; the main change is internal ownership of those credentials.

The branch replaces separate ambient dependencies—`PaymentConfiguration`,
`@Named(PUBLISHABLE_KEY)`, and `@Named(STRIPE_ACCOUNT_ID)`—with one immutable value:

```kotlin
ApiConfiguration.State(
    publishableKey = publishableKey,
    stripeAccountId = stripeAccountId,
)
```

That state is resolved at an integration boundary and then passed explicitly, stored in loaded state,
carried in Android arguments, or exposed through `() -> ApiConfiguration.State` for lazy resolution.

## Previous credential model

On `master`, credentials usually originate in the global `PaymentConfiguration` singleton. Dagger
then exposes two independent named values or providers. Consumers inject one of the following:

- `Provider<PaymentConfiguration>`
- Separate publishable-key and Stripe-account-ID providers
- An injected `ApiRequest.Options` constructor backed by those providers
- A raw publishable key for analytics, fraud detection, Google Pay, or card scanning

This allows the two values to be resolved at different times, makes request ownership implicit, and
can cause eager component construction to access `PaymentConfiguration` before initialization. It
also makes it difficult for an integration such as Link to use credentials that differ from the
process-wide singleton.

## New credential model

The branch treats the publishable key and account ID as a coherent snapshot:

```text
integration or entry-point credentials
                 |
                 v
        ApiConfiguration.State
                 |
        +--------+---------+
        |                  |
        v                  v
explicit method input   lazy () -> State
        |                  |
        +--------+---------+
                 v
 ApiRequest.Options / analytics / feature integrations
```

`ApiRequest.Options` is constructed from one state, so its API key and connected account always
belong to the same snapshot. The injectable `ApiRequest.Options` constructor and the global named
credential constants are removed.

`ApiConfigurationFromPaymentConfigurationModule` is the compatibility bridge for entry points that
still use `PaymentConfiguration`. It converts a `Provider<PaymentConfiguration>` into a function that
creates `ApiConfiguration.State` only when invoked. A second binding converts the state function into
a lazy `ApiRequest.Options` function.

Function-type providers are intentional. Dagger can construct and pass the function without reading
the singleton. This prevents component creation from resolving credentials before the merchant has
initialized them.

## PaymentSheet load flow

The central flow is `DefaultPaymentElementLoader`:

1. Inspect `CommonConfiguration.apiConfiguration`.
2. If credentials were supplied by the integration, use them.
3. Otherwise, `ApiConfigurationResolver` snapshots the current `PaymentConfiguration`.
4. Use that state for the complete load operation.
5. Store it in `PaymentMethodMetadata` for all post-load consumers.

The single resolved state controls:

- Live/test-mode validation
- Elements-session requests
- Customer payment-method prefetching
- Google Pay readiness
- Payment-method-messaging promotion requests
- Tap to Add support and initialization
- Analytics initialization and load events
- Link configuration
- Payment-method metadata and subsequent confirmation operations

This ensures one PaymentSheet load cannot observe one key during session loading and a different key
or account ID during confirmation.

### Pre-load consumers

Metadata does not exist while a PaymentSheet component is being constructed. Credential-dependent
work that happens before loading completes therefore receives `ApiConfiguration.State` explicitly.
Examples include Elements Session loading, payment-method messaging, Google Pay readiness, customer
prefetch, Tap to Add setup, and initial analytics.

### Post-load consumers

`ApiConfigurationModule` supplies `() -> ApiConfiguration.State` from
`PaymentMethodMetadata.apiConfiguration`. PaymentSheet, PaymentOptions, FlowController, Embedded,
Link, and Tap to Add graphs use this provider after metadata is installed.

The provider must stay lazy: invoking it before metadata exists still fails. Components that can run
before loading instead bind credentials at their entry point or accept them as method inputs.

## Android and standalone entry points

Flows that do not participate in the standard PaymentSheet metadata lifecycle snapshot credentials at
their own boundary. These include CustomerSheet, CustomerAdapter, Checkout, Address Element,
autocomplete, Polling, PaymentLauncher, Google Pay launchers, passive challenge, attestation, 3DS2,
standalone card widgets, and browser authentication.

When a flow crosses an Activity or process-recreation boundary, the relevant publishable key and
account ID are carried in Parcelable arguments. This prevents a recreated destination from silently
switching to the current global configuration.

## Link

Link receives the strongest explicit request-ownership changes:

- `LinkConfiguration` contains `ApiConfiguration.State` and derives `ApiRequest.Options` from it.
- `LinkController.Configuration.State` carries its credentials into `CommonConfiguration`.
- Native and web Activity contracts preserve the session credentials.
- Link account and authentication classes use options derived from their `LinkConfiguration`.
- `LinkRepository` operations accept `ApiRequest.Options` explicitly rather than having the
  repository read ambient credentials.

Link sometimes uses a consumer or merchant-of-record key for a specific operation. When that custom
key is present, `LinkApiRepository` creates options with the custom key and no connected account.
Otherwise it uses the caller's full publishable-key/account pair.

Crypto Onramp initializes `PaymentConfiguration` from its own configuration before configuring Link,
preserving compatibility for remaining global consumers.

## Repository ownership

`ElementsSessionRepository`, `CustomerRepository`, `SavedPaymentMethodRepository`,
`CheckoutSessionRepository`, and the Link repositories move toward operation-level credential
inputs. The resulting rules are:

- Credentials belonging to a load or session are passed with that operation.
- Objects used only after metadata loads use the lazy metadata-backed provider.
- Standalone SDK entry points snapshot `PaymentConfiguration` at their boundary.
- Android destinations carry credentials in their arguments when they can be recreated.

The Stripe account ID is now explicitly forwarded to customer and saved-payment-method requests that
previously obtained it indirectly.

## Payments Core

Payments Core migrates PaymentLauncher, result processors, polling, authentication, next-action
handlers, 3DS2, Google Pay, card range lookup, and Stripe repository creation to paired state or
explicit request options. Fraud detection and analytics still need only the publishable key, but now
receive a lazy key function derived from the owning state.

Browser-authentication and error-reporting fallbacks prefer credentials carried by arguments. A few
standalone APIs continue to read `PaymentConfiguration` because they have no integration-level
configuration object; these remain compatibility entry points rather than general dependency sources.

## Other modules

- Financial Connections converts its sheet configuration directly to `ApiConfiguration.State` and
  removes its module-local publishable-key binding.
- Financial Connections Lite derives request options from the paired configuration.
- Crypto Onramp and Payment Method Messaging use the lazy compatibility module.
- Card Scan requires the publishable key in `CardScanConfiguration`; PaymentSheet supplies it from
  loaded metadata through `payments-ui-core`.
- NFC, analytics, and fraud-detection consumers derive their key-only providers from the credential
  state owned by their integration.

## Behavioral guarantees and remaining compatibility

The migration provides four primary guarantees:

1. A publishable key and Stripe account ID are resolved together.
2. A PaymentSheet load uses one credential snapshot.
3. Pre-load work does not depend on metadata-backed credentials.
4. Request ownership is explicit where more than one credential context can exist.

Standard merchant integrations still primarily fall back to `PaymentConfiguration`; this branch does
not broadly expose a new public PaymentSheet credential API. Metadata-backed providers remain invalid
before load completion, and a small number of standalone/error fallback paths still access the global
configuration. Key-only consumers remain key-only when the Stripe account ID is irrelevant.

## Stacked rollout

The migration should be introduced with the following independently reviewable stack. Each PR should
remain below 500 changed lines, including tests.

1. Credential and request-options foundation.
2. Payments Core launch and result-processing paths.
3. Payments Core authentication and next actions.
4. Google Pay and standalone Payments Core consumers.
5. PaymentSheet credential resolution and load pipeline.
6. PaymentSheet repositories and pre-load operations.
7. PaymentSheet confirmation, polling, address, and bank-account flows.
8. CustomerSheet and CustomerAdapter.
9. Link configuration and Activity/session propagation.
10. Link repository explicit request options.
11. Tap to Add, NFC, and Card Scan.
12. Financial Connections, Crypto Onramp, Payment Method Messaging, final named-binding cleanup, and
    this migration documentation.

Named credential bindings should remain available as temporary adapters until the final cleanup PR.
Each PR should keep production changes with focused tests and should be based on the preceding stack
branch.
