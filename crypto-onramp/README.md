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
- Register crypto wallet addresses with `registerWalletAddress(walletAddress:network:)`
- Collect payment methods via Link (card, bank account) or Apple Pay with `collectPaymentMethod(type:)`
- Create crypto payment tokens with `createCryptoPaymentToken()`

**Checkout handling**: 
- Complete purchases for an onramp session with `performCheckout(onrampSessionId:checkoutHandler:)`

**Theming**:
- The minimal Stripe-provided UI supports light customization via `LinkAppearance`
- Customize component colors, button properties, and light / dark interface styles

## Getting started

### Integration

Get started with Embedded components onramp [📚 Android integration guide](https://docs.stripe.com/crypto/onramp/embedded-components) and [example project](../crypto-onramp-example).

### Example

[CryptoOnramp Example](../crypto-onramp-example) – This example demonstrates an end-to-end headless onramp flow (Link authentication, KYC and identity verification, wallet selection, payment method collection, and checkout) using a demo backend.
