# Shipping documentation contracts

Parent read common/Android Markdoc via Sourcegraph on2026-11-12, revision `4ed3418fb5a9b0af2cf2dcdca3aefb89b5b400cf`. Curated excerpts, not a full page export.

Page: https://docs.corp.stripe.com/payments/mobile/checkout-sessions/build-a-checkout-page?platform=android

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@4ed3418fb5a9b0af2cf2dcdca3aefb89b5b400cf/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/build-a-checkout-page.md?L558-645

Exact common text at558–569:
> Present the Shipping Address Element when the customer adds or edits a delivery address.
>
> After the customer saves an address, the Element updates the Checkout Session and dismisses.
> Read the updated shipping address, tax, and totals from the `CheckoutController.session`.
>
> If you collect a shipping address, set `shipping_address_collection.allowed_countries` to the countries that are available to ship to on the Checkout Session.

Android607–613:
> Before calling `controller.configure`, add the Shipping Address Element configuration as described in Configure the controller:
```kotlin
configuration.shippingAddressElement(ShippingAddressElement.Configuration())
```
> Create the presenter when you create the Activity. Wait for the controller configuration to succeed before allowing the customer to add or edit a shipping address.

Android sample616–641 creates one presenter in Activity.onCreate, then collects `controller.session` and `controller.isUpdating` in Compose. Button calls `presenter.shippingAddressElement().present()`; enabled condition `session != null && !isUpdating`; text uses `session?.shippingAddress?.address?.line1 ?: "Add shipping address"`.

Root/build-page docs also state that shipping can be omitted if not needed or collected in the merchant's own UI. All Elements on the same controller share Session state. Preview excludes shipping-option collection and checkout phone-number collection. Do not report these exclusions as bugs. The obsolete CheckoutController name is already in the foundation docs API-drift report.

Validation focus: supported configuration; working save; allowed-country constraints; edit/cancel/persistence; public session+actual backend shipping details; tax/totals refresh where relevant. Cancellation/defaults and merchant own-UI behavior should be investigated against SDK supported public API/source; these excerpts do not promise unspecified semantics.
