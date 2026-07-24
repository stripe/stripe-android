# Threading ApiConfiguration Through the Paymentsheet Module

## Overview

This document describes the migration from global `PaymentConfiguration` injection to per-component `ApiConfiguration` threading throughout the `paymentsheet` module. The work is split across multiple branches, each handling a specific category of consumers.

## Motivation

`ApiConfiguration` is a new public API that allows users to provide a publishable key and stripe account ID per-component (e.g., per `EmbeddedPaymentElement.configure()` call) rather than relying on the global `PaymentConfiguration.init()` singleton.

**Key constraint:** We can no longer guarantee that API credentials are available at SDK initialization time. With `ApiConfiguration`, credentials are provided at configure-time, which happens after the Dagger component graph is already constructed. This means Dagger injection of `PaymentConfiguration` / `@Named(PUBLISHABLE_KEY)` / `@Named(STRIPE_ACCOUNT_ID)` into classes created at initialization is not viable.

**Solution:** Resolve `ApiConfiguration.State` once at the top of the load flow and pipe it through data classes (`PaymentMethodMetadata`, method parameters) to all consumers.

## Architecture

```
User calls configure(intentConfig, configuration)
  -> configuration.asCommonConfiguration() [has apiConfiguration]
  -> PaymentElementLoader.load(initMode, configuration, metadata)
    -> ApiConfigurationResolver resolves credentials
    -> ElementsSessionRepository.get(..., requestOptions)  [pre-load]
    -> PaymentMethodMetadata is created with apiConfiguration  [carries to post-load]
      -> All downstream consumers read from PaymentMethodMetadata.apiConfiguration
```

---

## Branch 1: `tyler/pk-metadata-setup`
**Base:** `master`
**Purpose:** Infrastructure & pre-load path

### New files
- **`ApiConfigurationResolver.kt`** — Single resolution point that returns `ApiConfiguration.State` from either the user-provided `CommonConfiguration.apiConfiguration` or the fallback `PaymentConfiguration.getInstance(context)`.

### Key changes
- **`PaymentMethodMetadata`** — Added `apiConfiguration: ApiConfiguration.State` field and `apiRequestOptions: ApiRequest.Options` convenience property. Updated both `createForPaymentElement()` and `createForCustomerSheet()` factories.
- **`DefaultPaymentElementLoader`** — Removed `Provider<PaymentConfiguration>` injection. Now resolves `ApiConfiguration.State` at the top of `load()` via `ApiConfigurationResolver` and passes it through to session loading and metadata creation.
- **`ElementsSessionRepository`** — Removed `Provider<PaymentConfiguration>` from constructor. `get()` now accepts `requestOptions: ApiRequest.Options` as a parameter.
- **`ElementsSessionLoader`** — Updated to pass `requestOptions` through.
- **CustomerSheet paths** (`CustomerSheetLoader`, `CustomerAdapterDataSource`, `CustomerSessionElementsSessionManager`) — Inject `Application` and resolve from `PaymentConfiguration` for the legacy CustomerSheet path.

---

## Branch 2: `tyler/pk-confirmation-definitions`
**Base:** `tyler/pk-metadata-setup`
**Purpose:** Post-load — simple confirmation definitions

### Key changes
- **`PassiveChallengeConfirmationDefinition`** — Removed `@Named(PUBLISHABLE_KEY)` injection. Now reads `paymentMethodMetadata.apiConfiguration.publishableKey` in both `bootstrap()` and `action()`.
- **`AttestationConfirmationDefinition`** — Same pattern: removed injection, reads from `confirmationArgs.paymentMethodMetadata.apiConfiguration.publishableKey` in `action()`.

---

## Branch 3: `tyler/pk-repositories`
**Base:** `tyler/pk-metadata-setup`
**Purpose:** Post-load — CustomerApiRepository

### Key changes
- **`CustomerRepository` interface** — All methods (`retrieveCustomer`, `getPaymentMethods`, `detachPaymentMethod`, `attachPaymentMethod`, `updatePaymentMethod`) now accept `stripeAccountId: String?` as a parameter.
- **`CustomerApiRepository`** — Removed `Provider<PaymentConfiguration>` from constructor. Uses the `stripeAccountId` parameter passed by callers.
- **Callers** (`SavedPaymentMethodMutator`, `SavedPaymentMethodRepository`, `CreateLinkState`, `RetrieveCustomerEmail`, `LogLinkHoldbackExperiment`) — Pass `stripeAccountId` from `PaymentMethodMetadata.apiConfiguration.stripeAccountId`.

---

## Branch 4: `tyler/pk-link-and-intent`
**Base:** `tyler/pk-metadata-setup`
**Purpose:** Post-load — LinkApiRepository and IntentConfirmationDefinition

### Key changes
- **`LinkApiRepository`** — Replaced `@Named(PUBLISHABLE_KEY)` and `@Named(STRIPE_ACCOUNT_ID)` with `apiConfiguration: ApiConfiguration.State` in the constructor. Updated `buildRequestOptions()` to use the new field.
- **`IntentConfirmationDefinition`** — Changed `paymentLauncherFactory` signature to accept `ApiConfiguration.State` as a parameter. In `launch()`, credentials are obtained from `confirmationArgs.paymentMethodMetadata.apiConfiguration` and passed to the factory.
- **`IntentConfirmationModule`** — Removed `Provider<PaymentConfiguration>`. The factory lambda no longer captures config at DI time.
- **`NativeLinkComponent`** — Removed `publishableKeyProvider` and `stripeAccountIdProvider` from the Factory interface. `NativeLinkModule` now derives these from the bound `PaymentMethodMetadata`.
- **`NativeLinkArgs`** — Removed `publishableKey` and `stripeAccountId` fields.
- **`PaymentConfigurationModule`** — Added `provideApiConfigurationState()` method for components that need `ApiConfiguration.State` from legacy `PaymentConfiguration`.

---

## Branch 5: `tyler/pk-modules-components`
**Base:** `tyler/pk-metadata-setup`
**Purpose:** CheckoutSessionRepository

### Key changes
- **`CheckoutSessionRepository`** — Removed `@Named(PUBLISHABLE_KEY)` and `@Named(STRIPE_ACCOUNT_ID)` from constructor. All public methods (`init`, `confirm`, `detachPaymentMethod`, `updatePaymentMethod`, `applyPromotionCode`, `updateLineItemQuantity`, `selectShippingRate`, `updateTaxRegion`, `updateTaxId`, `updateCurrency`) now accept `requestOptions: ApiRequest.Options`.
- **`CheckoutController`** — Injects `ApiRequest.Options` and passes to all repository calls.
- **`DefaultSavedPaymentMethodRepository`** — Injects `ApiRequest.Options` and passes to repository calls.
- **`CheckoutSessionConfirmationInterceptor`** — Passes existing `requestOptions` to `confirm()`.
- **`PaymentConfigurationModule`** — Added `provideApiRequestOptions()` so `ApiRequest.Options` is available via DI.

---

## Branch 6: `tyler/pk-viewmodels`
**Base:** `tyler/pk-metadata-setup`
**Purpose:** USBankAccountFormViewModel and PaymentMethodMessagingPromotionsHelper

### Key changes
- **`USBankAccountFormViewModel`** — Removed `Provider<PaymentConfiguration>` from constructor. Now receives `publishableKey` and `stripeAccountId` via its `Args` data class.
- **`USBankAccountFormArguments`** — Added `publishableKey: String` and `stripeAccountId: String?` fields. Both `create()` and `createForEmbedded()` factories populate these from `PaymentMethodMetadata.apiConfiguration`.
- **`PaymentMethodMessagePromotionsHelper`** — `fetchPromotionsAsync()` now takes `requestOptions: ApiRequest.Options` as a parameter.
- **`DefaultPaymentMethodMessagePromotionsHelper`** — Removed `Provider<PaymentConfiguration>` from constructor. Uses the `requestOptions` parameter directly.
- **`DefaultPaymentElementLoader`** — Passes `requestOptions` to `fetchPaymentMethodMessaging()`.

---

## Branch 7: `tyler/pk-taptoadd`
**Base:** `tyler/pk-metadata-setup`
**Purpose:** TapToAdd classes

### Key changes
- **`TapToAddConnectionManager`** — Replaced `Provider<PaymentConfiguration>` with `publishableKeyProvider: () -> String` in constructor and companion `create()` factory.
- **`TapToAddIsSimulatedProvider`** — Replaced `Provider<PaymentConfiguration>` with `isLiveModeProvider: () -> Boolean`.
- **`TapToAddConnectionModule`** — Updated to derive `publishableKeyProvider` and `isLiveModeProvider` from `Provider<PaymentConfiguration>` at the module level.

---

## Execution Order

These branches are all based on `tyler/pk-metadata-setup` and can be merged independently (they don't depend on each other). The recommended merge order:

1. **`tyler/pk-metadata-setup`** — Must go first (infrastructure)
2. **Branches 2-7** — Can be merged in any order after #1

## Verification

Each branch passes:
- `./gradlew :paymentsheet:compileDebugKotlin` (production code)
- `./gradlew :paymentsheet:compileDebugUnitTestKotlin` (test code)

## What remains

- **`NfcScanningEventReporterModule`** — Still provides `@Named(PUBLISHABLE_KEY)` from `PaymentConfiguration.getInstance(context)`. Can be updated once the component it belongs to binds `PaymentMethodMetadata`.
- **`AttestationComponent`** — Still takes `@Named(PUBLISHABLE_KEY)` as `@BindsInstance`. Caller should pass from `PaymentMethodMetadata` at build site.
- **`AddressElementViewModelModule`** — Provides from `AddressElementActivityContract.Args.publishableKey` (public API boundary). Internal callers should pass the resolved key.
- **`LinkHoldbackExposureModule`** — Currently derives from `Provider<PaymentConfiguration>`. Can be updated to use `PaymentMethodMetadata` when available in the component graph.
- **Final cleanup** — Remove `PaymentConfigurationModule` inclusion from paymentsheet-specific components where no longer needed.
