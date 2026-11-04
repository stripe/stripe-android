# Checkout live instrumentation tests

`CheckoutPaymentElementLiveTest` exercises the SDK with an explicit
`CheckoutElementsController.Configuration`, the existing test `MainActivity`,
and fresh card-only USD Checkout Sessions from the playground backend. It asserts
the SDK completion and decline results. It does not depend on playground settings
or the example application's result UI.

These tests use real networking and are excluded from ordinary `paymentsheet`
instrumentation runs. Select them explicitly on the managed device:

```sh
./gradlew -PcheckoutLiveBackendTests=true \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.stripe.android.paymentsheet.RequiresLiveBackend \
  :paymentsheet:pixel2api33DebugAndroidTest
```

The default backend is `https://stp-mobile-playground-backend-v7.stripedemos.com/`.
Override its base URL with
`-Pandroid.testInstrumentationRunnerArguments.checkoutBackendUrl=https://your-backend/`.
The backend must support `publishable_key` and `create_checkout_session` for the
US test merchant and the `2026-08-26.preview` API version.

Bitrise runs this selection separately in `run-checkout-live-e2e`. The tests do
not install mocked networking or a fake Google Pay repository. Wallets are
disabled through the SDK configuration so both cases exercise the card form.
