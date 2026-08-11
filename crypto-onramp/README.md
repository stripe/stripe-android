# Stripe Crypto Onramp Android SDK

The crypto-onramp helps you build a headless crypto onramp flow in your Android app to allow your customers to securely purchase and exchange cryptocurrencies. It provides a coordinator that manages Link authentication, know your customer (KYC) and identity verification, payment method collection, and checkout handling while leaving your app in control of most of the surrounding UI and navigation.

> [!IMPORTANT]
> This SDK is currently in *private preview*. Learn more and request access via the [Stripe docs](https://docs.stripe.com/crypto/onramp/embedded-components).

## Table of contents
<!-- NOTE: Use case-sensitive anchor links for docc compatibility -->
<!--ts-->
* [Features](#Features)
* [Getting started](#Getting-started)
   * [Integration](#Integration)
   * [Samsung Pay](#Samsung-Pay)
   * [Example](#Example)

<!--te-->

## Features

**Headless coordinator**: 
- Use `OnrampCoordinator` to orchestrate an onramp flow with minimal Stripe-provided UI

**Link authentication**:
- Check if an email has a Link account with `hasLinkAccount(email:)`
- Register new users with `registerLinkUser(info:)`
- Authorize a Link auth intent with `authorize(linkAuthIntentId:)`
- Support seamless sign-in for returning users with `authenticateUserWithToken(linkAuthTokenClientSecret:)`

**KYC and identity verification**:
- Submit KYC information with `attachKycInfo(info:)` and confirm it with `verifyKycInfo(updatedAddress:)`
- Present identification document verification using `verifyIdentity()`

**Wallets and payment methods**:
- Register and delete crypto wallets with `registerWalletAddress(walletAddress:network:)` and `deleteWalletAddress(walletId:)`
- Collect payment methods via Link (card, bank account), Google Pay, or Samsung Pay with `collectPaymentMethod(...)`
- Create crypto payment tokens with `createCryptoPaymentToken()`

**Checkout handling**: 
- Complete purchases for an onramp session with `performCheckout(onrampSessionId:checkoutHandler:)`

**Theming**:
- The minimal Stripe-provided UI supports light customization via `LinkAppearance`
- Customize component colors, button properties, and light / dark interface styles

## Getting started

### Integration

Get started with Embedded components onramp [📚 Android integration guide](https://docs.stripe.com/crypto/onramp/embedded-components) and [example project](../crypto-onramp-example).

### Samsung Pay

Samsung Pay support was added using the the Samsung Pay SDK version `2.22.00`. The SDK is not included in
the Stripe Android SDK or its dependency metadata. Past or future versions may have compatibility issues. 
Obtain the JAR from Samsung, add it to the client application's `libs` directory, and declare it in the application module:

```kotlin
dependencies {
    implementation(files("libs/samsungpay_2.22.00.jar"))
}
```

Opt in while configuring the coordinator. The merchant display name is used as the Samsung Pay
merchant name unless one is supplied explicitly. The Samsung merchant ID is only sent
when provided in `SamsungPayConfig`:

```kotlin
val configuration = OnrampConfiguration()
    .merchantDisplayName("Example merchant")
    .publishableKey("pk_test_...")
    .samsungPayConfig(
        OnrampConfiguration.SamsungPayConfig(
            serviceId = "your-samsung-service-id",
        )
    )
```

Use the readiness callback to decide whether to show Samsung Pay. The availability result includes
a rich error with recovery guidance when Samsung Pay is unavailable.

```kotlin
val callbacks = OnrampCallbacks()
    // Configure the other required onramp callbacks.
    .samsungPayIsReadyCallback { isReady, availabilityResult ->
        samsungPayButton.isVisible = isReady
    }
```

Present Samsung Pay with an ISO 4217 currency code, a positive amount in that currency's minor
unit, and a required order identifier:

```kotlin
presenter.collectPaymentMethod(
    PaymentMethodSelection.SamsungPay(
        currencyCode = "USD",
        amount = 1_099,
        orderNumber = "order-123",
    )
)
```

The resulting Samsung payment credential is exchanged for a Stripe token and then a card
PaymentMethod using the onramp platform publishable key. An absent SDK or payment failure is
delivered through the existing `OnrampCollectPaymentMethodResult.Failed` callback.

### Example

[CryptoOnramp Example](../crypto-onramp-example) – This example demonstrates an end-to-end headless onramp flow (Link authentication, KYC and identity verification, wallet selection, payment method collection, and checkout) using a demo backend.
