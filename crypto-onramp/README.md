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
   * [Platform Pay before Link authentication](#Platform-Pay-before-Link-authentication)
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
- Present required partner terms of service with `presentTermsOfServiceIfNeeded()` during onboarding after Link authentication
- Submit KYC information with `attachKycInfo(info:)` and confirm it with `verifyKycInfo(updatedAddress:)`
- Present identification document verification using `verifyIdentity()`

**Wallets and payment methods**:
- Register and delete crypto wallets with `registerWalletAddress(walletAddress:network:)` and `deleteWalletAddress(walletId:)`
- Collect payment methods via Link (card, bank account), Google Pay, or Samsung Pay with `collectPaymentMethod(...)`
- Create crypto payment tokens with `createCryptoPaymentToken()`

**Checkout handling**: 
- Present required partner terms and conditions with `presentTermsAndConditionsIfNeeded()` before checkout
- Complete purchases for an onramp session with `performCheckout(onrampSessionId:checkoutHandler:)`

**Theming**:
- The minimal Stripe-provided UI supports light customization via `LinkAppearance`
- Customize component colors, button properties, and light / dark interface styles

## Getting started

### Integration

Get started with Embedded components onramp [📚 Android integration guide](https://docs.stripe.com/crypto/onramp/embedded-components) and [example project](../crypto-onramp-example).

### Platform Pay before Link authentication

Google Pay and Samsung Pay can be collected before Link authentication. Configure the coordinator
first, then call `presenter.collectPaymentMethod(...)`. Token creation still requires a crypto
customer obtained through registration or authentication (or supplied in configuration).

Provide a merchant-selected country with `OnrampConfiguration.countryHint(countryCode)` when known.
The SDK sends this optional hint to both platform settings and payment-token creation. The API
validates it and gives an established KYC region precedence. The SDK does not infer the hint from
the device locale. Omitting it omits `country_hint` from both requests.

To request contact information from Google Pay, set these options on your existing Google Pay config:

```kotlin
GooglePayPaymentMethodLauncher.Config(
    environment = GooglePayEnvironment.Test,
    merchantCountryCode = "US", // Your merchant's country, not the customer's country hint.
    merchantName = "Example merchant",
    isEmailRequired = true,
    billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
        isRequired = true,
        format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
        isPhoneNumberRequired = true,
    ),
)
```

Request the full billing address along with the phone number: the billing country is needed to
normalize national phone numbers. In `OnrampCollectPaymentMethodResult.Completed.kycInfo`:

- `email` contains a nonblank wallet email, trimmed for prefill.
- `phone` contains a validated E.164 number or `null`. Pass this to `LinkUserInfo.phone` when present.
- `rawPhone` preserves the original nonblank wallet phone string for your UI or manual correction.
- Name and billing address continue to be returned when available. Contact-only responses now
  produce `KycInfo`; a response without any usable fields still returns `null`.

Contact fields are optional and are not included in `attachKycInfo` submissions. Samsung Pay uses
this same mapping if its resulting PaymentMethod contains billing details; its credential exchange
does not request contact fields, so callers must handle absent information.

After wallet collection, use the returned email with `hasLinkAccount`. For a new user, collect any
missing required details and call `registerLinkUser(LinkUserInfo(...))`, followed by the required KYC
steps and `createCryptoPaymentToken()`. Registration itself presents no Stripe screen or OTP; your
app is responsible for surfacing Link terms. For a returning user, use `presenter.authorize(...)`
for Link consent/OTP, or `authenticateUserWithToken(...)` with a server-issued token secret. Handle
an existing-account registration failure by taking the returning-user path.

Authentication or KYC can resolve a different platform account from the one used to collect the
wallet payment method. Token creation also requires the original collection key to be available;
a successful wallet collection is still returned if that key is missing. An account mismatch or
missing collection key causes token creation to return `PlatformPayAccountChangedException`
with code `platform_pay_account_changed`. Call `collectPaymentMethod` again for the wallet and retry
token creation. Retrying token creation alone will not repair an account mismatch. API errors for
unsupported country hints retain their backend error code.

For manual validation, exercise new and returning users, wallet cancellation, contact fields omitted,
a non-US country hint, an unsupported hint, and a hint that disagrees with the user's persisted KYC
region. Also exercise the existing authentication-first flow for card, bank account, and both wallets.

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

### Platform Pay E2E tests

`OnrampPlatformPayFlowTest` in `crypto-onramp-example` drives the real Google Pay sheet before
Link authentication. It covers a returning user through Link OTP and a fresh user through
registration, OTP, KYC, and identity verification, then creates a token from the originally collected
wallet. It also checks the wallet contact fields and that token creation fails before authentication.
A third test exercises the headless new-user path: Google Pay → `registerLinkUser` →
`attachKycInfo` → `createCryptoPaymentToken`, without authorization, OTP, a second wallet collection,
or any activity launch after Google Pay returns. It uses the wallet phone, name, and address directly.
For repeatability, it derives a unique email alias from the wallet email and asserts that the alias has
no existing Link account before registration. This tests contact-driven registration, but does not
register the exact, unchanged Google account email on every run. KYC here means submitting the wallet
name/address; this test does not complete identity verification or a purchase.

Use an online Google Play device signed into a Google account, with Google Pay test cards and a
complete US billing contact (including a valid phone number). The device must have network access
to the example backend. The tests fail if Google Pay is unavailable; they do not substitute a fake
wallet result or silently skip the flow.

```shell
./gradlew :crypto-onramp-example:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.stripe.android.crypto.onramp.example.OnrampPlatformPayFlowTest
```
