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
| `payments/core/injection/StripeRepositoryModule.kt` | Updated to use `() -> ApiConfiguration.State` |
| `payments/core/injection/PaymentLauncherModule.kt` | Updated DI bindings |
| `payments/core/injection/PaymentLauncherViewModelFactoryComponent.kt` | Updated component includes |
| `payments/core/injection/NextActionHandlerComponent.kt` | Updated component includes |
| `payments/core/injection/Stripe3ds2TransactionViewModelFactoryComponent.kt` | Updated component includes |
| **Networking** | |
| `networking/StripeApiRepository.kt` | Uses `() -> ApiConfiguration.State` instead of named strings |
| `networking/PaymentAnalyticsRequestFactory.kt` | Constructor takes `() -> ApiConfiguration.State` |
| `PaymentsFraudDetectionDataRepositoryFactory.kt` | Now takes `publishableKeyProvider: () -> String` param |
| `PaymentConfiguration.kt` | `init()` passes publishable key to fraud detection |
| **Payment Launchers** | |
| `payments/paymentlauncher/StripePaymentLauncher.kt` | Uses `ApiConfiguration.State` |
| `payments/paymentlauncher/StripePaymentLauncherAssistedFactory.kt` | Updated factory params |
| `payments/paymentlauncher/PaymentLauncherViewModel.kt` | Updated dependencies |
| `payments/paymentlauncher/PaymentLauncherFactory.kt` | Updated factory construction |
| `payments/paymentlauncher/PaymentLauncherConfirmationActivity.kt` | Updated activity setup |
| **Google Pay** | |
| `googlepaylauncher/GooglePayLauncher.kt` | Uses `ApiConfiguration.State` |
| `googlepaylauncher/GooglePayPaymentMethodLauncher.kt` | Uses `ApiConfiguration.State` |
| `googlepaylauncher/GooglePayLauncherActivity.kt` | Updated |
| `googlepaylauncher/GooglePayLauncherViewModel.kt` | Updated |
| `googlepaylauncher/GooglePayPaymentMethodLauncherActivity.kt` | Updated |
| `googlepaylauncher/GooglePayPaymentMethodLauncherContractV2.kt` | Updated |
| `googlepaylauncher/GooglePayPaymentMethodLauncherViewModel.kt` | Updated |
| `googlepaylauncher/GooglePayRepository.kt` | Updated |
| `googlepaylauncher/InternalGooglePayPaymentMethodLauncher.kt` | Updated |
| `googlepaylauncher/injection/GooglePayPaymentMethodLauncherModule.kt` | Updated |
| `googlepaylauncher/injection/GooglePayPaymentMethodLauncherViewModelFactoryComponent.kt` | Updated |
| `googlepaylauncher/injection/GooglePayRepositoryFactory.kt` | Updated |
| **Other** | |
| `GooglePayJsonFactory.kt` | Uses `ApiConfiguration.State` |
| `IssuingCardPinService.kt` | Updated |
| `StripePaymentController.kt` | Updated |
| `cards/DefaultCardAccountRangeRepositoryFactory.kt` | Updated |
| `payments/PaymentFlowResultProcessor.kt` | Updated |
| `payments/StripeBrowserLauncherActivity.kt` | Updated |
| `payments/StripeBrowserLauncherViewModel.kt` | Updated |
| `payments/bankaccount/di/CollectBankAccountComponent.kt` | Updated |
| `payments/bankaccount/di/CollectBankAccountModule.kt` | Updated |
| `payments/core/analytics/ErrorReporter.kt` | Takes `publishableKeyProvider` in fallback factory |
| `payments/core/authentication/DefaultPaymentNextActionHandlerRegistry.kt` | Updated |
| `payments/core/authentication/VoucherNextActionHandler.kt` | Updated |
| `payments/core/authentication/WebIntentNextActionHandler.kt` | Updated |
| `payments/core/authentication/threeds2/Stripe3DS2NextActionHandler.kt` | Updated |
| `payments/core/authentication/threeds2/Stripe3ds2TransactionViewModel.kt` | Updated |
| `challenge/confirmation/IntentConfirmationChallengeNextActionHandler.kt` | Updated |
| `challenge/confirmation/di/IntentConfirmationChallengeComponent.kt` | Updated |
| `challenge/passive/PassiveChallengeComponent.kt` | Updated |
| `challenge/passive/PassiveChallengeViewModel.kt` | Updated |
| `challenge/passive/warmer/activity/PassiveChallengeWarmerActivityComponent.kt` | Updated |
| `challenge/passive/warmer/activity/PassiveChallengeWarmerViewModel.kt` | Updated |
| `polling/DefaultIntentStatusPoller.kt` | Updated |
| `view/CardWidgetViewModel.kt` | Updated |
| `view/PaymentAuthWebViewActivity.kt` | Updated |
| `view/PaymentAuthWebViewActivityViewModel.kt` | Updated |

### `payments-ui-core` (3 files)
| File | Change |
|------|--------|
| `ui/core/cardscan/CardScanStripeLauncher.kt` | Updated |
| `ui/core/cardscan/RememberCardScanLauncher.kt` | Updated |
| `ui/core/elements/CardScanAction.kt` | Updated |

### `paymentsheet` (108 files)
| File | Change |
|------|--------|
| **DI / Injection** | |
| `paymentsheet/injection/ApiConfigurationModule.kt` | **NEW** — Provides `() -> ApiConfiguration.State` from `PaymentMethodMetadata` |
| `paymentsheet/injection/ApiConfigurationResolver.kt` | **NEW** — Resolves API config with `PaymentConfiguration` fallback |
| `paymentsheet/injection/PaymentSheetLauncherComponent.kt` | Includes new modules |
| `paymentsheet/injection/PaymentSheetLauncherModule.kt` | Updated providers |
| `paymentsheet/injection/LinkHoldbackExposureModule.kt` | Updated — removed `apiRequestOptionsProvider` |
| `paymentsheet/injection/PaymentOptionsViewModelFactoryComponent.kt` | Updated |
| `paymentsheet/injection/PaymentSheetAutocompleteModule.kt` | Updated |
| `paymentsheet/injection/AddressElementViewModelFactoryComponent.kt` | Updated |
| `paymentsheet/injection/AddressElementViewModelModule.kt` | Updated |
| `paymentsheet/injection/AutocompleteViewModelFactoryComponent.kt` | Updated |
| `paymentsheet/injection/AutocompleteViewModelModule.kt` | Updated |
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
| `link/WebLinkActivityContract.kt` | Updated |
| `link/ui/paymentmenthod/PaymentMethodViewModel.kt` | Updated |
| **TapToAdd** | |
| `common/taptoadd/TapToAddIsSimulatedProvider.kt` | `get(isLiveMode: Boolean)` — removed `apiConfigProvider` |
| `common/taptoadd/TapToAddConnectionManager.kt` | Removed `apiConfigProvider`; `isSupported` is now a function; `ConnectionConfig` has `publishableKey`/`isLiveMode` |
| `common/taptoadd/TapToAddConnectionModule.kt` | Removed `apiConfigProvider` from provider |
| `common/taptoadd/TapToAddCollectionHandler.kt` | Removed `apiConfigurationProvider` from constructor; reads from `metadata.apiConfiguration` |
| `common/taptoadd/TapToAddModule.kt` | Updated |
| `common/taptoadd/TapToAddViewModelComponent.kt` | Updated |
| `common/taptoadd/CreateCardPresentSetupIntentCallbackRetriever.kt` | Updated |
| **Loading / State** | |
| `paymentsheet/state/PaymentElementLoader.kt` | Resolves API config early; passes to `createLinkState`, `tapToAddConnectionStarter`, `eventReporter` |
| `paymentsheet/state/CreateLinkState.kt` | Accepts `apiConfiguration` param; threads to `LinkConfiguration` |
| `paymentsheet/state/RetrieveCustomerEmail.kt` | Takes `stripeAccountId` param instead of `apiConfigProvider` |
| `paymentsheet/state/TapToAddConnectionStarter.kt` | `isSupported(publishableKey, isLiveMode)` and `start(config, publishableKey, isLiveMode)` |
| `paymentsheet/state/TapToAddAvailabilityFactory.kt` | `isAvailable(...)` now takes `publishableKey`/`isLiveMode` |
| `paymentsheet/state/LoadSession.kt` | Updated |
| `paymentsheet/state/PaymentMethodMessagingPromotionsHelper.kt` | Updated |
| **Analytics** | |
| `paymentsheet/analytics/EventReporter.kt` | `onLoadStarted` and `onLoadFailed` accept `publishableKey` |
| `paymentsheet/analytics/DefaultEventReporter.kt` | Passes `publishableKey` to `fireEvent` |
| `common/analytics/experiment/LogLinkHoldbackExperiment.kt` | Injects `apiConfigProvider`; passes options to lookup call |
| **Repositories** | |
| `paymentsheet/repositories/ElementsSessionRepository.kt` | Updated |
| `paymentsheet/repositories/CustomerApiRepository.kt` | Updated |
| `paymentsheet/repositories/CustomerRepository.kt` | `retrieveCustomer` takes `stripeAccountId` |
| `paymentsheet/repositories/SavedPaymentMethodRepository.kt` | Updated |
| `paymentsheet/repositories/CheckoutSessionRepository.kt` | Updated |
| **Payment Method Metadata** | |
| `lpmfoundations/paymentmethod/PaymentMethodMetadata.kt` | Added `apiConfiguration: ApiConfiguration.State` field |
| `lpmfoundations/paymentmethod/definitions/CardDefinition.kt` | Updated |
| **Confirmation** | |
| `paymentelement/confirmation/intent/IntentConfirmationModule.kt` | Updated |
| `paymentelement/confirmation/intent/DeferredIntentConfirmationInterceptor.kt` | Updated |
| `paymentelement/confirmation/intent/DeferredIntentCallbackRetriever.kt` | Updated |
| `paymentelement/confirmation/intent/CheckoutSessionConfirmationInterceptor.kt` | Updated |
| `paymentelement/confirmation/intent/ConfirmationTokenConfirmationInterceptor.kt` | Updated |
| `paymentelement/confirmation/intent/CustomerSheetConfirmationInterceptor.kt` | Updated |
| `paymentelement/confirmation/intent/SharedPaymentTokenConfirmationInterceptor.kt` | Updated |
| `paymentelement/confirmation/gpay/GooglePayConfirmationDefinition.kt` | Updated |
| `paymentelement/confirmation/challenge/PassiveChallengeConfirmationDefinition.kt` | Updated |
| **Embedded** | |
| `paymentelement/embedded/EmbeddedCommonModule.kt` | Updated |
| `paymentelement/embedded/content/EmbeddedPaymentElementViewModel.kt` | Updated |
| `paymentelement/embedded/content/EmbeddedPaymentElementViewModelComponent.kt` | Updated |
| `paymentelement/embedded/sheet/EmbeddedSheetComponent.kt` | Updated |
| **CustomerSheet** | |
| `customersheet/CustomerAdapter.kt` | Updated |
| `customersheet/CustomerSheet.kt` | Updated |
| `customersheet/CustomerSheetLoader.kt` | Updated |
| `customersheet/CustomerSheetViewModel.kt` | Updated |
| `customersheet/StripeCustomerAdapter.kt` | Updated |
| `customersheet/util/CustomerSheetHacks.kt` | Updated |
| `customersheet/data/CustomerAdapterDataSource.kt` | Updated |
| `customersheet/data/CustomerSessionElementsSessionManager.kt` | Updated |
| `customersheet/data/CustomerSessionPaymentMethodDataSource.kt` | Updated |
| `customersheet/data/CustomerSessionSavedSelectionDataSource.kt` | Updated |
| `customersheet/data/injection/CustomerAdapterDataSourceComponent.kt` | Updated |
| `customersheet/data/injection/CustomerSessionDataSourceComponent.kt` | Updated |
| `customersheet/data/injection/CustomerSheetDataSourceCommonModule.kt` | Updated |
| `customersheet/injection/CustomerSheetDataCommonModule.kt` | Updated |
| `customersheet/injection/CustomerSheetViewModelComponent.kt` | Updated |
| `customersheet/injection/CustomerSheetViewModelModule.kt` | Updated |
| `customersheet/injection/StripeCustomerAdapterComponent.kt` | Updated |
| **Other PaymentSheet** | |
| `paymentsheet/PaymentSheetActivity.kt` | Updated |
| `paymentsheet/PaymentSheetViewModel.kt` | Updated |
| `paymentsheet/flowcontroller/FlowControllerStateComponent.kt` | Updated |
| `paymentsheet/flowcontroller/FlowControllerViewModel.kt` | Updated |
| `paymentsheet/viewmodels/BaseSheetViewModel.kt` | Updated |
| `paymentsheet/addresselement/AddressElementViewModel.kt` | Updated |
| `paymentsheet/addresselement/AutocompleteContract.kt` | Updated |
| `paymentsheet/addresselement/AutocompleteLauncher.kt` | Updated |
| `paymentsheet/addresselement/AutocompleteViewModel.kt` | Updated |
| `paymentsheet/addresselement/DefaultStripeAutocompleteRepository.kt` | Updated |
| `paymentsheet/addresselement/PaymentElementAutocompleteAddressInteractor.kt` | Updated |
| `paymentsheet/paymentdatacollection/ach/USBankAccountForm.kt` | Updated |
| `paymentsheet/paymentdatacollection/ach/USBankAccountFormArguments.kt` | Updated |
| `paymentsheet/paymentdatacollection/ach/USBankAccountFormViewModel.kt` | Updated |
| `paymentsheet/paymentdatacollection/ach/di/USBankAccountFormViewModelModule.kt` | Updated |
| `paymentsheet/paymentdatacollection/polling/PollingActivity.kt` | Updated |
| `paymentsheet/paymentdatacollection/polling/PollingContract.kt` | Updated |
| `paymentsheet/paymentdatacollection/polling/PollingViewModel.kt` | Updated |
| `paymentsheet/paymentdatacollection/polling/PollingNextActionHandler.kt` | Updated |
| `paymentsheet/paymentdatacollection/polling/di/PollingComponent.kt` | Updated |
| `paymentsheet/paymentdatacollection/polling/di/PollingViewModelModule.kt` | Updated |
| `common/model/CommonConfiguration.kt` | Added `apiConfiguration` field |
| `common/nfcscan/NfcScanningViewModelComponent.kt` | Updated |
| `common/nfcscan/analytics/NfcScanningEventReporterModule.kt` | Updated |
| `checkout/CheckoutController.kt` | Updated |
| `checkout/injection/CheckoutControllerComponent.kt` | Updated |
| `checkout/injection/CheckoutModule.kt` | Updated |

### `financial-connections` (3 files)
| File | Change |
|------|--------|
| `financialconnections/di/FinancialConnectionsSheetConfigurationModule.kt` | Updated |
| `financialconnections/di/FinancialConnectionsSheetSharedModule.kt` | Updated |
| `financialconnections/di/NamedConstants.kt` | Removed |

### `financial-connections-lite` (1 file)
| File | Change |
|------|--------|
| `financialconnections/lite/repository/FinancialConnectionsLiteRepository.kt` | Updated |

### `payment-method-messaging` (2 files)
| File | Change |
|------|--------|
| `paymentmethodmessaging/element/PaymentMethodMessagingCoordinator.kt` | Updated |
| `paymentmethodmessaging/element/PaymentMethodMessagingModule.kt` | Updated |

### `crypto-onramp` (4 files)
| File | Change |
|------|--------|
| `crypto/onramp/OnrampInteractor.kt` | Updated |
| `crypto/onramp/OnrampPresenterCoordinator.kt` | Updated |
| `crypto/onramp/di/OnrampModule.kt` | Uses `ApiConfigurationFromPaymentConfigurationModule` |
| `crypto/onramp/repositories/CryptoApiRepository.kt` | Uses `() -> ApiConfiguration.State` |

### `stripecardscan` (4 files)
| File | Change |
|------|--------|
| `stripecardscan/cardscan/CardScanActivity.kt` | Updated |
| `stripecardscan/cardscan/CardScanConfiguration.kt` | Updated |
| `stripecardscan/cardscan/CardScanSheet.kt` | Updated |
| `stripecardscan/di/CardScanModule.kt` | Updated |

### `stripecardscan-example` (1 file)
| File | Change |
|------|--------|
| `stripecardscan/example/CardScanDemoActivity.kt` | Updated |
