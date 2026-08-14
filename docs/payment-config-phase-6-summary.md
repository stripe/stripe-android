# Branch Summary: `tyler/payment-config-phase-6`

**293 files changed, 3459 insertions, 2082 deletions**

## Goal

Replace the old `@Named(PUBLISHABLE_KEY) () -> String` / `@Named(STRIPE_ACCOUNT_ID) () -> String?` DI injections with a unified `() -> ApiConfiguration.State` provider pattern throughout the SDK, and thread API credentials explicitly where needed to avoid timing issues.

---

## New Core Class: `ApiConfiguration` (`stripe-core`)

```kotlin
data class State(
    val publishableKey: String,
    val stripeAccountId: String?
) : Parcelable {
    fun isLiveMode(): Boolean = !publishableKey.startsWith("pk_test")
}
```

A builder-pattern class with a `Parcelable` inner `State` that replaces scattered `@Named` string injections with a single typed credential holder.

---

## How `ApiConfiguration.State` is Provided (4 Different Paths)

### 1. `ApiConfigurationModule` (paymentsheet — post-load)
**Used by:** `PaymentSheetLauncherComponent`, `FlowControllerStateComponent`, `EmbeddedCommonModule`, `PaymentOptionsViewModelFactoryComponent`

Reads from `PaymentMethodMetadata.apiConfiguration` (only valid after session loads):
```kotlin
return { requireNotNull(paymentMethodMetadata.get()?.apiConfiguration) }
```

### 2. `ApiConfigurationFromPaymentConfigurationModule` (payments-core — standalone)
**Used by:** Standalone components outside paymentsheet (`PaymentMethodMessagingComponent`, `StripePaymentLauncherComponent`, `CollectBankAccountComponent`, etc.)

Reads from `PaymentConfiguration.getInstance()` (the global singleton):
```kotlin
return { val config = paymentConfiguration.get(); ApiConfiguration.State(config.publishableKey, config.stripeAccountId) }
```

### 3. `LinkControllerModule` (paymentsheet — standalone Link)
**Used by:** `LinkControllerComponent` (standalone link flow, crypto-onramp)

Reads from `LinkControllerInteractor.configuration` (set at configure-time):
```kotlin
return { val config = requireNotNull(interactor.get().configuration); ApiConfiguration.State(config.publishableKey, config.stripeAccountId) }
```

### 4. `LinkConfiguration.apiConfiguration` (paymentsheet — inline Link)
**Used by:** `LinkApiRepository` via explicit method parameters

Passed through the call chain from `PaymentElementLoader` -> `CreateLinkState` -> `LinkConfiguration` -> `DefaultLinkAccountManager`/`DefaultLinkAuth` -> every `LinkRepository` method.

---

## Key Architectural Changes

### `LinkRepository` — Explicit `requestOptions` Parameter
All 21 methods on the `LinkRepository` interface now accept `requestOptions: ApiRequest.Options` as their first parameter. This eliminates the timing dependency where `LinkApiRepository` (a `@Singleton`) would call `apiConfigProvider()` before `PaymentMethodMetadata` existed.

**Before:** `LinkApiRepository` captured `apiRequestOptionsProvider` at construction and called it lazily in each method.

**After:** Callers (`DefaultLinkAccountManager`, `DefaultLinkAuth`) derive `requestOptions` from `config.requestOptions` (a computed property on `LinkConfiguration`) and pass it explicitly.

### `LinkConfiguration.apiConfiguration`
Added `val apiConfiguration: ApiConfiguration.State` to `LinkConfiguration` (which is `Parcelable`). This is set during `PaymentElementLoader.load()` from the resolved API configuration and flows through to all Link components.

### `TapToAddConnectionManager` — Explicit Parameters
Removed `apiConfigProvider` from `DefaultTapToAddConnectionManager` and `DefaultTapToAddIsSimulatedProvider`. Instead:
- `ConnectionConfig` carries `publishableKey` and `isLiveMode`
- `isSupported(publishableKey, isLiveMode)` is now a function (was a property)
- `TapToAddConnectionStarter.start()` accepts `publishableKey` and `isLiveMode`

### `RetrieveCustomerEmail` — Explicit `stripeAccountId`
Removed `apiConfigProvider` from `DefaultRetrieveCustomerEmail`. The `stripeAccountId` is now passed as a method parameter from callers that have it available.

### `LoadingEventReporter` — Explicit `publishableKey`
`onLoadStarted` and `onLoadFailed` now accept `publishableKey: String` so analytics events include the correct key even before `PaymentMethodMetadata` exists.

### `ApiConfigurationResolver` (bridge for loading)
Used only in `PaymentElementLoader.load()` to resolve credentials before metadata exists — checks `CommonConfiguration.apiConfiguration` first, falls back to `PaymentConfiguration.getInstance()`.

---

## Modules Removed / Simplified

- **`@Named(PUBLISHABLE_KEY)` and `@Named(STRIPE_ACCOUNT_ID)`** — Removed from `PaymentConfigurationModule` (it now only provides `PaymentConfiguration`). The named constants in `NamedConstants.kt` are deleted.
- **`ApiRequest.Options` constructor on `ApiRequest`** — Removed the factory that built options from named providers.
- **`PaymentConfiguration.init()` in `LinkControllerInteractor`** — No longer needed; credentials come from the explicit configuration.

---

## Summary of Timing Fixes

| Component | Before (broken) | After (fixed) |
|-----------|----------------|---------------|
| Link account lookup | `ApiConfigurationModule` -> reads null `PaymentMethodMetadata` | `LinkConfiguration.requestOptions` -> set before lookup |
| TapToAdd isSupported | `apiConfigProvider()` -> reads null metadata | Explicit `publishableKey`/`isLiveMode` params from loader |
| RetrieveCustomerEmail | `apiConfigProvider()` -> reads null metadata | Explicit `stripeAccountId` param |
| EventReporter.onLoadStarted | No publishable key available | Explicit `publishableKey` param |

---

## All Changed Files by Module

### `stripe-core` (3 files)
| File | Change |
|------|--------|
| `ApiConfiguration.kt` | **NEW** — Core credential holder class with `State` Parcelable |
| `core/injection/NamedConstants.kt` | Removed `PUBLISHABLE_KEY` and `STRIPE_ACCOUNT_ID` constants |
| `core/networking/ApiRequest.kt` | Removed `Options` factory that built from named providers |

### `payments-core` (53 files)
| File | Change |
|------|--------|
| **DI / Injection** | |
| `payments/core/injection/ApiConfigurationFromPaymentConfigurationModule.kt` | **NEW** — Provides `() -> ApiConfiguration.State` from `PaymentConfiguration` singleton |
| `payments/core/injection/PaymentConfigurationModule.kt` | Simplified — only provides `PaymentConfiguration`, no longer provides named key/account strings |
| `payments/core/injection/StripeRepositoryModule.kt` | Passes `publishableKeyProvider` to `DefaultFraudDetectionDataRepository`; no longer reads from `@Named` constants |
| `payments/core/injection/PaymentLauncherModule.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `payments/core/injection/PaymentLauncherViewModelFactoryComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `payments/core/injection/NextActionHandlerComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `payments/core/injection/Stripe3ds2TransactionViewModelFactoryComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| **Networking** | |
| `networking/StripeApiRepository.kt` | Passes `publishableKeyProvider` to `DefaultFraudDetectionDataRepository`; no longer reads from `@Named` constants |
| `networking/PaymentAnalyticsRequestFactory.kt` | Constructor takes `() -> String` publishable key provider instead of eager `String` |
| `PaymentsFraudDetectionDataRepositoryFactory.kt` | Now takes `publishableKeyProvider: () -> String` param |
| `PaymentConfiguration.kt` | `init()` passes publishable key to fraud detection |
| **Payment Launchers** | |
| `payments/paymentlauncher/StripePaymentLauncher.kt` | Replaced `@Named(PUBLISHABLE_KEY) publishableKeyProvider` and `@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider` constructor params with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `payments/paymentlauncher/StripePaymentLauncherAssistedFactory.kt` | Replaced `@Named(PUBLISHABLE_KEY) publishableKeyProvider` and `@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider` constructor params with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `payments/paymentlauncher/PaymentLauncherViewModel.kt` | Replaced `@Named(PUBLISHABLE_KEY) publishableKeyProvider` and `@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider` constructor params with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `payments/paymentlauncher/PaymentLauncherFactory.kt` | Replaced `@Named(PUBLISHABLE_KEY) publishableKeyProvider` and `@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider` constructor params with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `payments/paymentlauncher/PaymentLauncherConfirmationActivity.kt` | Passes `ApiConfiguration.State` to component construction |
| **Google Pay** | |
| `googlepaylauncher/GooglePayLauncher.kt` | Changed `PaymentConfiguration.getInstance().publishableKey` to lazy `publishableKeyProvider` lambda for `PaymentAnalyticsRequestFactory` and `ErrorReporter` |
| `googlepaylauncher/GooglePayPaymentMethodLauncher.kt` | Changed `PaymentConfiguration.getInstance().publishableKey` to lazy `publishableKeyProvider` lambda for `PaymentAnalyticsRequestFactory` and `ErrorReporter` |
| `googlepaylauncher/GooglePayLauncherActivity.kt` | Passes `ApiConfiguration.State` through intent args instead of reading `PaymentConfiguration` at launch |
| `googlepaylauncher/GooglePayLauncherViewModel.kt` | Receives `ApiConfiguration.State` from args; passes to repository factory |
| `googlepaylauncher/GooglePayPaymentMethodLauncherActivity.kt` | Passes `ApiConfiguration.State` through intent args instead of reading `PaymentConfiguration` at launch |
| `googlepaylauncher/GooglePayPaymentMethodLauncherContractV2.kt` | Passes `ApiConfiguration.State` through intent args instead of reading `PaymentConfiguration` at launch |
| `googlepaylauncher/GooglePayPaymentMethodLauncherViewModel.kt` | Receives `ApiConfiguration.State` from args; passes to repository factory |
| `googlepaylauncher/GooglePayRepository.kt` | Added optional `apiConfiguration: ApiConfiguration.State?` to construct `GooglePayConfig` without reading `PaymentConfiguration` |
| `googlepaylauncher/InternalGooglePayPaymentMethodLauncher.kt` | Passes `ApiConfiguration.State` through intent args instead of reading `PaymentConfiguration` at launch |
| `googlepaylauncher/injection/GooglePayPaymentMethodLauncherModule.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `googlepaylauncher/injection/GooglePayPaymentMethodLauncherViewModelFactoryComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `googlepaylauncher/injection/GooglePayRepositoryFactory.kt` | Takes `apiConfiguration: ApiConfiguration.State` instead of reading from `PaymentConfiguration` |
| **Other** | |
| `GooglePayJsonFactory.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` with `apiConfigProvider: () -> ApiConfiguration.State` |
| `IssuingCardPinService.kt` | Replaced named key/account params with `ApiConfiguration.State` |
| `StripePaymentController.kt` | Replaced named key/account providers with `() -> ApiConfiguration.State` |
| `cards/DefaultCardAccountRangeRepositoryFactory.kt` | Takes `() -> ApiConfiguration.State` instead of named key/account providers |
| `payments/PaymentFlowResultProcessor.kt` | Replaced named key/account providers with `() -> ApiConfiguration.State` |
| `payments/StripeBrowserLauncherActivity.kt` | Reads publishable key from intent args via `ApiConfiguration.State` instead of `PaymentConfiguration.getInstance()` |
| `payments/StripeBrowserLauncherViewModel.kt` | Reads publishable key from intent args via `ApiConfiguration.State` instead of `PaymentConfiguration.getInstance()` |
| `payments/bankaccount/di/CollectBankAccountComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `payments/bankaccount/di/CollectBankAccountModule.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `payments/core/analytics/ErrorReporter.kt` | Added `publishableKeyProvider: () -> String` param to `createFallbackInstance` |
| `payments/core/authentication/DefaultPaymentNextActionHandlerRegistry.kt` | Replaced named key/account providers with `() -> ApiConfiguration.State` |
| `payments/core/authentication/VoucherNextActionHandler.kt` | Replaced named key/account providers with `() -> ApiConfiguration.State` |
| `payments/core/authentication/WebIntentNextActionHandler.kt` | Replaced named key/account providers with `() -> ApiConfiguration.State` |
| `payments/core/authentication/threeds2/Stripe3DS2NextActionHandler.kt` | Replaced named key/account providers with `() -> ApiConfiguration.State` |
| `payments/core/authentication/threeds2/Stripe3ds2TransactionViewModel.kt` | Replaced `@Named(PUBLISHABLE_KEY) publishableKeyProvider` and `@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider` constructor params with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `challenge/confirmation/IntentConfirmationChallengeNextActionHandler.kt` | Replaced named key/account providers with `() -> ApiConfiguration.State` |
| `challenge/confirmation/di/IntentConfirmationChallengeComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `challenge/passive/PassiveChallengeComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `challenge/passive/PassiveChallengeViewModel.kt` | Replaced `@Named(PUBLISHABLE_KEY) publishableKeyProvider` and `@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider` constructor params with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `challenge/passive/warmer/activity/PassiveChallengeWarmerActivityComponent.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| `challenge/passive/warmer/activity/PassiveChallengeWarmerViewModel.kt` | Replaced `@Named(PUBLISHABLE_KEY) publishableKeyProvider` and `@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider` constructor params with `apiConfigurationProvider: () -> ApiConfiguration.State` |
| `polling/DefaultIntentStatusPoller.kt` | Replaced `Provider<PaymentConfiguration>` with `() -> ApiConfiguration.State`; uses `apiConfig.publishableKey`/`stripeAccountId` directly |
| `view/CardWidgetViewModel.kt` | Replaced `Provider<PaymentConfiguration>` with `() -> ApiConfiguration.State`; uses `apiConfig.publishableKey`/`stripeAccountId` directly |
| `view/PaymentAuthWebViewActivity.kt` | Reads publishable key from intent args via `ApiConfiguration.State` instead of `PaymentConfiguration.getInstance()` |
| `view/PaymentAuthWebViewActivityViewModel.kt` | Reads publishable key from intent args via `ApiConfiguration.State` instead of `PaymentConfiguration.getInstance()` |

### `payments-ui-core` (3 files)
| File | Change |
|------|--------|
| `ui/core/cardscan/CardScanStripeLauncher.kt` | Passes `ApiConfiguration.State` to card scan launcher instead of reading `PaymentConfiguration` |
| `ui/core/cardscan/RememberCardScanLauncher.kt` | Passes `ApiConfiguration.State` to card scan launcher instead of reading `PaymentConfiguration` |
| `ui/core/elements/CardScanAction.kt` | Passes `ApiConfiguration.State` to card scan launcher instead of reading `PaymentConfiguration` |

### `paymentsheet` (108 files)
| File | Change |
|------|--------|
| **DI / Injection** | |
| `paymentsheet/injection/ApiConfigurationModule.kt` | **NEW** — Provides `() -> ApiConfiguration.State` from `PaymentMethodMetadata` |
| `paymentsheet/injection/ApiConfigurationResolver.kt` | **NEW** — Resolves API config with `PaymentConfiguration` fallback |
| `paymentsheet/injection/PaymentSheetLauncherComponent.kt` | Added `ApiConfigurationModule` and `ApiRequestOptionsModule` to component modules |
| `paymentsheet/injection/PaymentSheetLauncherModule.kt` | Provides `PaymentMethodMetadata` from ViewModel's `StateFlow`; separated from ViewModel reference to avoid memory leak |
| `paymentsheet/injection/LinkHoldbackExposureModule.kt` | Removed `apiRequestOptionsProvider`; uses `() -> ApiConfiguration.State` |
| `paymentsheet/injection/PaymentOptionsViewModelFactoryComponent.kt` | Added `ApiConfigurationModule` and `ApiRequestOptionsModule` to component modules |
| `paymentsheet/injection/PaymentSheetAutocompleteModule.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/injection/AddressElementViewModelFactoryComponent.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/injection/AddressElementViewModelModule.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/injection/AutocompleteViewModelFactoryComponent.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/injection/AutocompleteViewModelModule.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| **Link** | |
| `link/LinkConfiguration.kt` | Added `apiConfiguration: ApiConfiguration.State` and `requestOptions` property |
| `link/repositories/LinkRepository.kt` | Added `requestOptions: ApiRequest.Options` to all 21 methods |
| `link/repositories/LinkApiRepository.kt` | Uses passed `requestOptions`; removed `apiRequestOptionsProvider` from constructor |
| `link/account/DefaultLinkAccountManager.kt` | Passes `config.requestOptions` to all repository calls |
| `link/account/DefaultLinkAuth.kt` | Passes `config.requestOptions` to all repository calls |
| `link/injection/LinkControllerComponent.kt` | Includes `ApiRequestOptionsModule` |
| `link/injection/LinkControllerModule.kt` | Provides `() -> ApiConfiguration.State` from `LinkControllerInteractor.configuration` |
| `link/injection/NativeLinkComponent.kt` | Removed `publishableKeyProvider`/`stripeAccountIdProvider` BindsInstance params |
| `link/injection/NativeLinkModule.kt` | Provides `() -> ApiConfiguration.State` from `PaymentMethodMetadata` |
| `link/LinkActivityViewModel.kt` | Removed key providers from component creation |
| `link/LinkControllerInteractor.kt` | Exposed `configuration` as `internal`; removed `PaymentConfiguration.init()` |
| `link/NativeLinkActivityContract.kt` | Uses injected `apiConfigurationProvider` instead of `PaymentConfiguration.getInstance()` |
| `link/WebLinkActivityContract.kt` | Reads publishable key from `apiConfigurationProvider()` instead of `PaymentConfiguration.getInstance()` |
| `link/ui/paymentmenthod/PaymentMethodViewModel.kt` | Replaced `@Named(PUBLISHABLE_KEY)` with reading from `PaymentMethodMetadata.apiConfiguration` |
| **TapToAdd** | |
| `common/taptoadd/TapToAddIsSimulatedProvider.kt` | `get(isLiveMode: Boolean)` — removed `apiConfigProvider` |
| `common/taptoadd/TapToAddConnectionManager.kt` | Removed `apiConfigProvider`; `isSupported` is now a function; `ConnectionConfig` has `publishableKey`/`isLiveMode` |
| `common/taptoadd/TapToAddConnectionModule.kt` | Removed `apiConfigProvider` from provider |
| `common/taptoadd/TapToAddCollectionHandler.kt` | Removed `apiConfigurationProvider` from constructor; reads from `metadata.apiConfiguration` |
| `common/taptoadd/TapToAddModule.kt` | Removed `apiConfigProvider`; `isSupported` is now a function; `ConnectionConfig` has `publishableKey`/`isLiveMode` |
| `common/taptoadd/TapToAddViewModelComponent.kt` | Removed `apiConfigProvider`; `isSupported` is now a function; `ConnectionConfig` has `publishableKey`/`isLiveMode` |
| `common/taptoadd/CreateCardPresentSetupIntentCallbackRetriever.kt` | Removed `apiConfigProvider`; `isSupported` is now a function; `ConnectionConfig` has `publishableKey`/`isLiveMode` |
| **Loading / State** | |
| `paymentsheet/state/PaymentElementLoader.kt` | Resolves API config early; passes to `createLinkState`, `tapToAddConnectionStarter`, `eventReporter` |
| `paymentsheet/state/CreateLinkState.kt` | Accepts `apiConfiguration` param; threads to `LinkConfiguration` |
| `paymentsheet/state/RetrieveCustomerEmail.kt` | Takes `stripeAccountId` param instead of `apiConfigProvider` |
| `paymentsheet/state/TapToAddConnectionStarter.kt` | `isSupported(publishableKey, isLiveMode)` and `start(config, publishableKey, isLiveMode)` |
| `paymentsheet/state/TapToAddAvailabilityFactory.kt` | `isAvailable(...)` now takes `publishableKey`/`isLiveMode` |
| `paymentsheet/state/LoadSession.kt` | Uses `() -> ApiRequest.Options` instead of eager `ApiRequest.Options` |
| `paymentsheet/state/PaymentMethodMessagingPromotionsHelper.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` with `() -> ApiConfiguration.State` |
| **Analytics** | |
| `paymentsheet/analytics/EventReporter.kt` | `onLoadStarted` and `onLoadFailed` accept `publishableKey` |
| `paymentsheet/analytics/DefaultEventReporter.kt` | Passes `publishableKey` to `fireEvent` |
| `common/analytics/experiment/LogLinkHoldbackExperiment.kt` | Injects `apiConfigProvider`; passes options to lookup call |
| **Repositories** | |
| `paymentsheet/repositories/ElementsSessionRepository.kt` | Uses `() -> ApiRequest.Options` provider instead of constructing options from named key providers |
| `paymentsheet/repositories/CustomerApiRepository.kt` | Removed `Provider<PaymentConfiguration>`; `retrieveCustomer` now takes explicit `stripeAccountId: String?` param |
| `paymentsheet/repositories/CustomerRepository.kt` | `retrieveCustomer` takes `stripeAccountId` |
| `paymentsheet/repositories/SavedPaymentMethodRepository.kt` | Uses `() -> ApiRequest.Options` provider instead of constructing options from named key providers |
| `paymentsheet/repositories/CheckoutSessionRepository.kt` | Uses `() -> ApiRequest.Options` provider instead of constructing options from named key providers |
| **Payment Method Metadata** | |
| `lpmfoundations/paymentmethod/PaymentMethodMetadata.kt` | Added `apiConfiguration: ApiConfiguration.State` field |
| `lpmfoundations/paymentmethod/definitions/CardDefinition.kt` | Passes `apiConfiguration` when constructing card scan launcher |
| **Confirmation** | |
| `paymentelement/confirmation/intent/IntentConfirmationModule.kt` | Changed `requestOptions: ApiRequest.Options` (eager) to `requestOptionsProvider: () -> ApiRequest.Options` (lazy) |
| `paymentelement/confirmation/intent/DeferredIntentConfirmationInterceptor.kt` | Changed `requestOptions: ApiRequest.Options` (eager) to `requestOptionsProvider: () -> ApiRequest.Options` (lazy) |
| `paymentelement/confirmation/intent/DeferredIntentCallbackRetriever.kt` | Changed `Provider<ApiRequest.Options>` to `() -> ApiRequest.Options` |
| `paymentelement/confirmation/intent/CheckoutSessionConfirmationInterceptor.kt` | Changed `requestOptions: ApiRequest.Options` (eager) to `requestOptionsProvider: () -> ApiRequest.Options` (lazy) |
| `paymentelement/confirmation/intent/ConfirmationTokenConfirmationInterceptor.kt` | Changed `requestOptions: ApiRequest.Options` (eager) to `requestOptionsProvider: () -> ApiRequest.Options` (lazy) |
| `paymentelement/confirmation/intent/CustomerSheetConfirmationInterceptor.kt` | Changed `requestOptions: ApiRequest.Options` (eager) to `requestOptionsProvider: () -> ApiRequest.Options` (lazy) |
| `paymentelement/confirmation/intent/SharedPaymentTokenConfirmationInterceptor.kt` | Changed `requestOptions: ApiRequest.Options` (eager) to `requestOptionsProvider: () -> ApiRequest.Options` (lazy) |
| `paymentelement/confirmation/gpay/GooglePayConfirmationDefinition.kt` | Passes `ApiConfiguration.State` through intent args instead of reading `PaymentConfiguration` at launch |
| `paymentelement/confirmation/challenge/PassiveChallengeConfirmationDefinition.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` BindsInstance params with single `apiConfigurationProvider: () -> ApiConfiguration.State`; added `ApiRequestOptionsModule` |
| **Embedded** | |
| `paymentelement/embedded/EmbeddedCommonModule.kt` | Includes `ApiConfigurationModule` and `ApiRequestOptionsModule` in component graph |
| `paymentelement/embedded/content/EmbeddedPaymentElementViewModel.kt` | Includes `ApiConfigurationModule` and `ApiRequestOptionsModule` in component graph |
| `paymentelement/embedded/content/EmbeddedPaymentElementViewModelComponent.kt` | Includes `ApiConfigurationModule` and `ApiRequestOptionsModule` in component graph |
| `paymentelement/embedded/sheet/EmbeddedSheetComponent.kt` | Includes `ApiConfigurationModule` and `ApiRequestOptionsModule` in component graph |
| **CustomerSheet** | |
| `customersheet/CustomerAdapter.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/CustomerSheet.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/CustomerSheetLoader.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/CustomerSheetViewModel.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/StripeCustomerAdapter.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/util/CustomerSheetHacks.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/data/CustomerAdapterDataSource.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/data/CustomerSessionElementsSessionManager.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/data/CustomerSessionPaymentMethodDataSource.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/data/CustomerSessionSavedSelectionDataSource.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/data/injection/CustomerAdapterDataSourceComponent.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/data/injection/CustomerSessionDataSourceComponent.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/data/injection/CustomerSheetDataSourceCommonModule.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/injection/CustomerSheetDataCommonModule.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/injection/CustomerSheetViewModelComponent.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/injection/CustomerSheetViewModelModule.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| `customersheet/injection/StripeCustomerAdapterComponent.kt` | Uses `() -> ApiConfiguration.State` provider; removed direct `PaymentConfiguration` dependency |
| **Other PaymentSheet** | |
| `paymentsheet/PaymentSheetActivity.kt` | Passes `ApiConfiguration.State` to component construction |
| `paymentsheet/PaymentSheetViewModel.kt` | No longer provides named key/account bindings to sub-components |
| `paymentsheet/flowcontroller/FlowControllerStateComponent.kt` | Includes `ApiConfigurationModule` and `ApiRequestOptionsModule` in component graph |
| `paymentsheet/flowcontroller/FlowControllerViewModel.kt` | No longer provides named key/account bindings to sub-components |
| `paymentsheet/viewmodels/BaseSheetViewModel.kt` | No longer provides named key/account bindings to sub-components |
| `paymentsheet/addresselement/AddressElementViewModel.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/addresselement/AutocompleteContract.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/addresselement/AutocompleteLauncher.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/addresselement/AutocompleteViewModel.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/addresselement/DefaultStripeAutocompleteRepository.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/addresselement/PaymentElementAutocompleteAddressInteractor.kt` | Passes `() -> ApiRequest.Options` through instead of named key providers |
| `paymentsheet/paymentdatacollection/ach/USBankAccountForm.kt` | Passes `ApiConfiguration.State` via `USBankAccountFormArguments` instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/ach/USBankAccountFormArguments.kt` | Passes `ApiConfiguration.State` via `USBankAccountFormArguments` instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/ach/USBankAccountFormViewModel.kt` | Passes `ApiConfiguration.State` via `USBankAccountFormArguments` instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/ach/di/USBankAccountFormViewModelModule.kt` | Passes `ApiConfiguration.State` via `USBankAccountFormArguments` instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/polling/PollingActivity.kt` | Passes `ApiConfiguration.State` through contract args instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/polling/PollingContract.kt` | Passes `ApiConfiguration.State` through contract args instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/polling/PollingViewModel.kt` | Passes `ApiConfiguration.State` through contract args instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/polling/PollingNextActionHandler.kt` | Passes `ApiConfiguration.State` through contract args instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/polling/di/PollingComponent.kt` | Passes `ApiConfiguration.State` through contract args instead of reading `PaymentConfiguration` |
| `paymentsheet/paymentdatacollection/polling/di/PollingViewModelModule.kt` | Passes `ApiConfiguration.State` through contract args instead of reading `PaymentConfiguration` |
| `common/model/CommonConfiguration.kt` | Added `val apiConfiguration: ApiConfiguration.State?` field for merchant-provided credentials |
| `common/nfcscan/NfcScanningViewModelComponent.kt` | Includes `ApiRequestOptionsModule` in component graph |
| `common/nfcscan/analytics/NfcScanningEventReporterModule.kt` | Includes `ApiRequestOptionsModule` in component graph |
| `checkout/CheckoutController.kt` | Uses `() -> ApiRequest.Options` instead of eager `ApiRequest.Options` |
| `checkout/injection/CheckoutControllerComponent.kt` | Uses `() -> ApiRequest.Options` instead of eager `ApiRequest.Options` |
| `checkout/injection/CheckoutModule.kt` | Uses `() -> ApiRequest.Options` instead of eager `ApiRequest.Options` |

### `financial-connections` (3 files)
| File | Change |
|------|--------|
| `financialconnections/di/FinancialConnectionsSheetConfigurationModule.kt` | Replaced `@Named` key/account injections with `() -> ApiConfiguration.State` |
| `financialconnections/di/FinancialConnectionsSheetSharedModule.kt` | Replaced `@Named` key/account injections with `() -> ApiConfiguration.State` |
| `financialconnections/di/NamedConstants.kt` | Removed — constants moved to use `ApiConfiguration.State` |

### `financial-connections-lite` (1 file)
| File | Change |
|------|--------|
| `financialconnections/lite/repository/FinancialConnectionsLiteRepository.kt` | Replaced `@Named` key/account injections with `() -> ApiConfiguration.State` |

### `payment-method-messaging` (2 files)
| File | Change |
|------|--------|
| `paymentmethodmessaging/element/PaymentMethodMessagingCoordinator.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` with `() -> ApiConfiguration.State` |
| `paymentmethodmessaging/element/PaymentMethodMessagingModule.kt` | Replaced `@Named(PUBLISHABLE_KEY)`/`@Named(STRIPE_ACCOUNT_ID)` with `() -> ApiConfiguration.State` |

### `crypto-onramp` (4 files)
| File | Change |
|------|--------|
| `crypto/onramp/OnrampInteractor.kt` | Uses `() -> ApiConfiguration.State` instead of named key/account providers |
| `crypto/onramp/OnrampPresenterCoordinator.kt` | Uses `() -> ApiConfiguration.State` instead of named key/account providers |
| `crypto/onramp/di/OnrampModule.kt` | Uses `ApiConfigurationFromPaymentConfigurationModule` |
| `crypto/onramp/repositories/CryptoApiRepository.kt` | Uses `() -> ApiConfiguration.State` |

### `stripecardscan` (4 files)
| File | Change |
|------|--------|
| `stripecardscan/cardscan/CardScanActivity.kt` | Receives `ApiConfiguration.State` in configuration instead of reading `PaymentConfiguration.getInstance()` |
| `stripecardscan/cardscan/CardScanConfiguration.kt` | Receives `ApiConfiguration.State` in configuration instead of reading `PaymentConfiguration.getInstance()` |
| `stripecardscan/cardscan/CardScanSheet.kt` | Receives `ApiConfiguration.State` in configuration instead of reading `PaymentConfiguration.getInstance()` |
| `stripecardscan/di/CardScanModule.kt` | Receives `ApiConfiguration.State` in configuration instead of reading `PaymentConfiguration.getInstance()` |

### `stripecardscan-example` (1 file)
| File | Change |
|------|--------|
| `stripecardscan/example/CardScanDemoActivity.kt` | Receives `ApiConfiguration.State` in configuration instead of reading `PaymentConfiguration.getInstance()` |

---

## Classes That Construct `ApiConfiguration.State` from `PaymentConfiguration.getInstance()`

These are the places in production code where `ApiConfiguration.State` is manually constructed by reading from the `PaymentConfiguration` global singleton. They represent the "entry points" where credentials enter the system from merchant configuration.

### Via DI Module (single canonical path)

| File | Context |
|------|---------|
| `payments-core/.../PaymentConfigurationModule.kt` | Provides `PaymentConfiguration` to DI graphs |
| `payments-core/.../ApiConfigurationFromPaymentConfigurationModule.kt` | Converts `PaymentConfiguration` → `() -> ApiConfiguration.State` for standalone components |
| `paymentsheet/.../ApiConfigurationResolver.kt` | Fallback resolver in `PaymentElementLoader` — reads `PaymentConfiguration` when `CommonConfiguration.apiConfiguration` is null |

### Direct Construction in `payments-core`

| File | Context |
|------|---------|
| `GooglePayPaymentMethodLauncher.kt` | Constructs `ApiConfiguration.State` for the Google Pay payment method launcher contract |
| `InternalGooglePayPaymentMethodLauncher.kt` | Internal launcher setup — reads config for analytics and GPay config |
| `PaymentLauncherFactory.kt` | Factory methods (`createForPaymentSheet`, `createForActivity`) build `ApiConfiguration.State` from `PaymentConfiguration.getInstance()` |
| `CardWidgetViewModel.kt` | ViewModel factory reads `PaymentConfiguration` to create `ApiConfiguration.State` for CBC eligibility check |
| `PaymentSheetActivity.kt` (in paymentsheet) | Reads `PaymentConfiguration.getInstance()` to determine `isLiveMode` for status bar color |
| `GooglePayLauncher.kt` | Multiple constructor overloads read `PaymentConfiguration.getInstance().publishableKey` for `publishableKeyProvider` lambdas |
| `GooglePayLauncherActivity.kt` | Activity reads `PaymentConfiguration.getInstance().publishableKey` at launch |
| `GooglePayLauncherViewModel.kt` | ViewModel factory reads `PaymentConfiguration` for fallback construction |
| `GooglePayPaymentMethodLauncherViewModel.kt` | Constructs `ApiConfiguration.State` from args or `PaymentConfiguration` |
| `PaymentLauncherConfirmationActivity.kt` | Reads `PaymentConfiguration.getInstance().publishableKey` for component construction |
| `StripeBrowserLauncherViewModel.kt` | Factory reads `PaymentConfiguration.getInstance()` for analytics and API options |
| `PaymentAuthWebViewActivity.kt` | Reads `PaymentConfiguration.getInstance().publishableKey` for analytics |
| `IssuingCardPinService.kt` | Factory reads `PaymentConfiguration.getInstance()` for API setup |
| `CardNumberEditText.kt` | Reads `PaymentConfiguration.getInstance().publishableKey` for card account range lookups |
| `DefaultCardAccountRangeRepositoryFactory.kt` | Factory reads `PaymentConfiguration.getInstance()` for account range API calls |
| `StripePaymentController.kt` | Constructs `ApiConfiguration.State` from publishable key param (not `getInstance` directly, but from callers that read it) |

### Direct Construction in `paymentsheet`

| File | Context |
|------|---------|
| `CheckoutController.kt` | Reads `PaymentConfiguration.getInstance()` to construct `ApiConfiguration.State` for checkout component |
| `CustomerAdapter.kt` | Reads `PaymentConfiguration.getInstance()` for customer adapter setup |
| `CustomerSheetViewModel.kt` | Factory reads `PaymentConfiguration.getInstance()` for component construction |
| `CustomerSheetHacks.kt` | Reads `PaymentConfiguration.getInstance()` for customer sheet initialization (2 call sites) |
| `PaymentSheetActivity.kt` | Reads `PaymentConfiguration.getInstance()` to check `isLiveMode` for error display |

### Direct Construction in Other Modules

| File | Context |
|------|---------|
| `financial-connections/.../FinancialConnectionsSheetConfigurationModule.kt` | Constructs `ApiConfiguration.State` from configuration params (publishableKey passed in) |
| `payments-core/.../PassiveChallengeViewModel.kt` | Constructs from `args.publishableKey` |
| `payments-core/.../PassiveChallengeWarmerViewModel.kt` | Constructs from `args.publishableKey` |
| `payments-core/.../IntentConfirmationChallengeComponent.kt` | Constructs from component args |
| `payments-core/.../Stripe3ds2TransactionViewModel.kt` | Constructs from args |
| `payments-core/.../CollectBankAccountModule.kt` | Constructs from args publishableKey |
| `paymentsheet/.../AddressElementViewModel.kt` | Factory reads `PaymentConfiguration.getInstance()` for autocomplete |
| `paymentsheet/.../AutocompleteViewModel.kt` | Factory reads `PaymentConfiguration.getInstance()` for places API |
| `paymentsheet/.../PollingViewModel.kt` | Factory reads from contract args |
| `paymentsheet/.../LinkControllerModule.kt` | Constructs from `LinkControllerInteractor.configuration` (publishableKey/stripeAccountId) |
| `paymentsheet/.../CommonConfiguration.kt` | Extension function constructs from `LinkController.Configuration.State` |
