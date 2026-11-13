# Source-verified Android Express Checkout documentation

Parent fetched these files with Sourcegraph on 2026-11-12. Revision: `7f9d2f1a0f1349f4d6f0d617009b7f8882337eae`. This is a curated extract of common/Android content, not a complete page export. Full source links identify the exact evidence and line ranges. Link branding expressions are rendered as Link in prose below.

## Overview

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@7f9d2f1a0f1349f4d6f0d617009b7f8882337eae/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/express-checkout-element.md

- L22–33: one-tap Google Pay on Android and Link on both platforms; one-time payment preview; same Session/controller can be reused with Payment Element; express-only checkout needs no Payment Element or separate Pay button. Completing wallet flow confirms Session and reports result.
- L45–50: displayed methods must be enabled, supported, and available. Configuration can hide but cannot make ineligible wallet available. Google Pay/Link display by default when available.
- L67–69: Android presentation is Jetpack Compose.
- L78–80: default button layout is a single column; `buttonLayout` can override to grid.
- L167–198: Android `GooglePayConfiguration.buttonType` uses `ButtonType`; default `Pay`. Values: `Plain`, `Book`, `Buy`, `Checkout`, `Donate`, `Order`, `Pay`, `Subscribe`. Text localized for device.
- L200–202: Link has one type, “Pay with Link”.
- L207: light/dark/automatic button themes; wallet controls logo/brand colors.
- L240: cannot force Google Pay if device unsupported.
- L275–279: `availableExpressCheckoutPaymentMethods` is ordered string list `google_pay`, `link`; empty means no content. Express-only merchant should supply fallback/message; hide labels/separators until available if desired.

Exact Android theme snippet L224–233:
```kotlin
val expressCheckout = ExpressCheckoutElement.Configuration()
    .appearance(
        ExpressCheckoutElement.Configuration.Appearance()
            .buttonTheme(
                ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Dark
            )
    )
configuration.expressCheckoutElement(expressCheckout)
```

Exact Android hide-Link snippet L259–268:
```kotlin
val expressCheckout = ExpressCheckoutElement.Configuration()
    .linkConfiguration(
        ExpressCheckoutElement.Configuration.LinkConfiguration()
            .display(
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
            )
    )
configuration.expressCheckoutElement(expressCheckout)
```

## Accept a payment

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@7f9d2f1a0f1349f4d6f0d617009b7f8882337eae/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/express-checkout-element/accept-a-payment.md

- Common L24–28: completing wallet confirms Session; no separate Pay button. Element uses Session to determine availability and displays line items/total in Google Pay/Apple Pay. If combined with Payment Element, configure same controller and put express first.
- Android branch L264–550 includes shared `partials/setup.md` with platform=android.
- L270–274: configuration required; default Google Pay/Link when available. Controller in ViewModel with SavedStateHandle; define result handler before builder; fetch/configure in viewModelScope.
- L276–355 code imports `com.stripe.android.checkout.CheckoutController`, uses `CheckoutController.Result`, builds `CheckoutController.Builder(...).resultCallback(::handleCheckoutResult).build()`. `_loadError` and `_checkoutResult` StateFlows. `configure()` returns early if configureJob active OR controller.session.value != null. Coroutine calls fetchClientSecret, creates `CheckoutController.Configuration().expressCheckoutElement(ExpressCheckoutElement.Configuration())`, and calls `controller.configure(...).fold(onSuccess={}, onFailure={...})`. CancellationException is rethrown; others set load-error strings. `onCleared` calls destroy. The controller-name drift is already covered by the foundation report; verify `.fold` against OperationResult as another manifestation if relevant.
- L359–365: add express to existing configuration preserving paymentElement, other elements/defaults. Same controller/presenter/Session. Keep normal Payment Element/Pay calling presenter.confirm(). Existing result callback receives results from both Elements.
- L373–411: one presenter during Activity creation; retain element returned from presenter. `viewModel.configure()` in onCreate before setContent; collect session/loadError; show error, loading, or `expressCheckoutElement.Content()` respectively.
- L413–415: Express itself handles confirmation. No content if no eligible methods; merchant fallback/message required.
- L421–439: collect checkoutResult in Activity; LaunchedEffect handles Completed -> showOrderConfirmation, Canceled -> restoreCheckout, Failed -> showError. Clear result after handling to avoid repeating action on recreation. Fulfillment via signed Session webhooks, not client callback alone.
- L445–459: collect session and read availableExpressCheckoutPaymentMethods; google_pay/link in display order. No buttons until initial load. AnimatedVisibility on nonempty list wraps Content.
- L493–525: optional source tracking with `calledConfirm` flag set only around merchant's presenter.confirm(); reset on EVERY callback including cancel/failure. Shared callback does not identify initiating element. Disable merchant Pay while updating or required data incomplete.
- L532–546: Stripe test API keys, supported device. Google Pay instructions: “Add a real card to Google Wallet so that Google Pay is available on your test device. Use Google's Android test card suite to test payment scenarios with Stripe test cards.” Link test partial `partials/link/testing.md`. Test success/cancel/error, signed server checkout.session.completed and fulfillment only after success, no eligible-wallet fallback, alternate payment paths, device rotation/state/result handling.

Exact Android styling snippet L467–487:
```kotlin
val expressCheckout = ExpressCheckoutElement.Configuration()
    .googlePayConfiguration(
        ExpressCheckoutElement.Configuration.GooglePayConfiguration()
            .buttonType(
                ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Buy
            )
    )
    .appearance(
        ExpressCheckoutElement.Configuration.Appearance()
            .buttonTheme(
                ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Automatic
            )
            .buttonLayout(
                ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                    .maxColumns(2)
                    .maxRows(1)
            )
    )
configuration.expressCheckoutElement(expressCheckout)
```

No extra Home fetch is necessary for these contracts: authoritative Markdoc was read directly. Ask parent for additional exact source lines/partials when needed; direct root MCP tools are not present in functions.ALL_TOOLS.

## Link test partials

Parent also read `pay-server/docs/content/partials/link/testing.md`, `otp-codes-table.md`, and `sandbox-callout.md` at revision `dc416033337fcf663dd0bc4a33782b4c83a46ca8` via Sourcegraph.

- Testing lines14–18: Link supports credit/debit cards and qualified US bank purchases. Sandbox account can use any valid email.
- OTP table: `000001` invalid-code error; `000002` expired-code error; `000003` max-attempts error; any other6 digits succeeds.
- Sandbox callout: do not store real user data in Link sandbox accounts; treat as publicly available because associated with publishable key.

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@dc416033337fcf663dd0bc4a33782b4c83a46ca8/-/blob/pay-server/docs/content/partials/link/otp-codes-table.md
