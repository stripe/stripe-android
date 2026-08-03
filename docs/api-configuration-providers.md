# ApiConfiguration Migration — Current State

## Summary of Changes

Replaced internal usage of `PaymentConfiguration`, `@Named(PUBLISHABLE_KEY)`, and `@Named(STRIPE_ACCOUNT_ID)` with `ApiConfiguration.State` across the SDK. The merchant-facing API (`PaymentConfiguration.init()`) is unchanged. `ApiConfiguration.State` is the internal credential source for all payment operations.

---

## How ApiConfiguration.State Flows Through PaymentSheet Integrations

### The Load Flow (PaymentSheet / FlowController / Embedded)

```
1. Merchant calls PaymentConfiguration.init(context, pk, accountId)
2. Merchant creates PaymentSheet / FlowController / EmbeddedPaymentElement
3. ViewModel is created → Dagger component is built
   - ApiConfigurationModule provides ApiConfiguration.State from PaymentMethodMetadata?
   - At this point, metadata is NULL (not loaded yet)
   - ⚠️ Any eager consumer of ApiConfiguration.State will crash here
4. PaymentElementLoader.load() is called
   - ApiConfigurationResolver resolves ApiConfiguration.State from PaymentConfiguration
   - Passes it to ElementsSessionLoader → ElementsSessionRepository (as method param)
   - Passes it to PaymentMethodMessagingPromotionsHelper (as method param)
   - Stores it in PaymentMethodMetadata.apiConfiguration
5. After load completes, PaymentMethodMetadata is set on the ViewModel
   - ApiConfigurationModule can now resolve: metadata.apiConfiguration
   - All lazy consumers (Provider<ApiConfiguration.State>) now work
```

### Known Issue: Pre-Load Consumers

Classes that are eagerly constructed at step 3 and need `ApiConfiguration.State` will crash if they resolve it immediately. Current mitigations:
- `NativeLinkActivityContract` / `WebLinkActivityContract` — inject `Provider<ApiConfiguration.State>` (lazy)
- `CheckoutSessionRepository` — inject `Provider<ApiConfiguration.State>` (lazy, called only during operations)
- `ElementsSessionRepository` — receives `apiConfiguration` as method parameter from the loader
- `PaymentMethodMessagePromotionsHelper` — receives `apiConfiguration` as method parameter

**Remaining problem:** Event reporters call `onInit` at ViewModel creation (step 3). `PaymentAnalyticsRequestFactory` resolves the publishable key lazily via `publishableKeyProvider = { apiConfigProvider.get().publishableKey }`, but `apiConfigProvider.get()` still calls `ApiConfigurationModule.provideApiConfigurationState()` which does `requireNotNull(metadata)` → crash if analytics fires before load.

---

## Where ApiConfiguration.State Is Provided

### 1. Paymentsheet `ApiConfigurationModule` (from PaymentMethodMetadata)

```kotlin
@Provides
fun provideApiConfigurationState(
    paymentMethodMetadata: Provider<PaymentMethodMetadata?>
): ApiConfiguration.State {
    return requireNotNull(paymentMethodMetadata.get()).apiConfiguration
}
```

**Used by components with loaded state:**
- `PaymentSheetLauncherComponent`
- `PaymentOptionsViewModelFactoryComponent`
- `FlowControllerStateComponent`
- `EmbeddedCommonModule`
- `LinkControllerModule`
- `TapToAddViewModelComponent`

**Inline providers (same source):**
- `NativeLinkModule` — `paymentMethodMetadata.apiConfiguration`
- `NfcScanningEventReporterModule` — `paymentMethodMetadata.apiConfiguration`

### 2. `ApiConfigurationResolver` (PaymentElementLoader / CustomerSheetLoader)

Resolves from `PaymentConfiguration` singleton. Used ONLY by the loaders before metadata exists:
- `DefaultPaymentElementLoader` — resolves once, passes to all pre-load methods
- `DefaultCustomerSheetLoader` — same pattern

### 3. `@BindsInstance` at component creation (from `PaymentConfiguration.getInstance()`)

Components that don't have metadata and are created at entry points:
- `CheckoutControllerComponent` — `CheckoutController.Builder.build()`
- `CustomerSheetViewModelComponent` — `CustomerSheetViewModel.Factory`
- `CustomerAdapterDataSourceComponent` — `CustomerSheetHacks.initialize()`
- `CustomerSessionDataSourceComponent` — `CustomerSheetHacks.initialize()`
- `StripeCustomerAdapterComponent` — `CustomerAdapter.create()`
- `AddressElementViewModelFactoryComponent` — `AddressElementViewModel.Factory` (from `AddressElementActivityContract.Args.publishableKey`)
- `AutocompleteViewModelFactoryComponent` — `AutocompleteViewModel.Isolated` (from `AutocompleteContract.Args.publishableKey`)
- `PollingComponent` — `PollingViewModel.Factory` (from `PollingContract.Args.publishableKey`)

### 4. From method parameters (passed by caller)

Used by pre-load repositories that run before metadata exists:
- `ElementsSessionRepository.get(apiConfiguration)` — from `PaymentElementLoader`
- `PaymentMethodMessagePromotionsHelper.fetchPromotionsAsync(intent, apiConfiguration)` — from `PaymentElementLoader`
- `USBankAccountFormViewModel.Args` — carries `publishableKey`/`stripeAccountId` from `PaymentMethodMetadata`

### 5. `GooglePayPaymentMethodLauncherViewModelFactoryComponent` (lazy provider)

```kotlin
@BindsInstance apiConfigurationProvider: () -> ApiConfiguration.State
```
Resolved lazily from `PaymentConfiguration.getInstance()` at ViewModel creation, but not called until the actual Google Pay flow runs.

---

## Where PaymentConfiguration Is Still Used in Paymentsheet

| Location | Why |
|----------|-----|
| `ApiConfigurationResolver` | Bridge: resolves `ApiConfiguration.State` from singleton for loaders |
| `PaymentSheetActivity` | `isLiveMode` check before load (for error display) |
| `AddressLauncher.present()` | Entry point: merchant hasn't passed ApiConfiguration yet |
| `CheckoutController.Builder.build()` | Entry point: passes to component as `@BindsInstance` |
| `CustomerSheetViewModel.Factory` | Entry point: passes to component as `@BindsInstance` |
| `CustomerSheetHacks.initialize()` | Entry point: passes to data source components |
| `CustomerAdapter.create()` | Entry point: passes to adapter component |
| `CustomerSheet` (error reporter) | Analytics fallback for error reporting |
| `PaymentSheetCommonModule` (include) | Provides `PaymentConfiguration` for `ApiConfigurationResolver` |
| `EmbeddedCommonModule` (include) | Same |
| `CheckoutModule` (include) | Same |
| `CustomerSheetViewModelComponent` (include) | Same |
| `DeferredIntentCallbackRetriever` | Comment reference only |

---

## Where PaymentConfiguration Is Still Used in payments-core

| Location | Why |
|----------|-----|
| `PaymentConfigurationModule` | Core module: provides `PaymentConfiguration` from singleton |
| `ApiConfigurationFromPaymentConfigurationModule` | Bridge: derives `ApiConfiguration.State` from `PaymentConfiguration` |
| `PaymentConfiguration.init()` | Merchant entry point (stores keys in singleton + SharedPrefs) |
| `DefaultFraudDetectionDataRepository` factory | Called from `PaymentConfiguration.init()` |
| `GooglePayPaymentMethodLauncher` | Reads at construction (merchant has already initialized) |
| `GooglePayPaymentMethodLauncherViewModel.Factory` | Lazy provider for component creation |
| `GooglePayLauncher` constructors | Reads at construction (merchant has already initialized) |
| `GooglePayLauncherViewModel` | Reads for component creation |
| `InternalGooglePayPaymentMethodLauncher` | Default param for analytics factory |
| `PaymentLauncherConfirmationActivity` | Fallback when intent args are null |
| `StripePaymentController` | Non-DI constructor, merchant passes key directly |
| `CardWidgetViewModel` | Standalone view, reads from singleton |
| `IssuingCardPinService` | Standalone service |
| Various `ErrorReporter.createFallbackInstance()` callers | Analytics fallback |

---

## Other Modules

| Module | How ApiConfiguration is provided |
|--------|--------------------------------|
| `crypto-onramp` | `ApiConfigurationFromPaymentConfigurationModule` (from singleton) |
| `payment-method-messaging` | `ApiConfigurationFromPaymentConfigurationModule` (from singleton) |
| `financial-connections` | `FinancialConnectionsSheetConfigurationModule` provides from `FinancialConnectionsSheetConfiguration` |
| `stripecardscan` | `CardScanConfiguration.publishableKey` (passed from `PaymentMethodMetadata`) |

---

## What's Next

1. **Add `ApiConfiguration` to merchant-facing configuration objects** (`PaymentSheet.Configuration`, `EmbeddedPaymentElement.Configuration`, `CustomerSheet.Configuration`)
2. **Thread merchant-provided `ApiConfiguration` through the load flow** — `ApiConfigurationResolver.resolve()` will prefer the merchant value over `PaymentConfiguration`
3. **Fix the onInit analytics timing issue** — Event reporters fire at ViewModel creation before metadata exists. Need to either defer analytics or source the key from the merchant config (which will be available at creation time once step 1 is done)
4. **Remove `PaymentConfiguration` dependency from paymentsheet entirely** — Once merchants pass `ApiConfiguration` directly, the singleton is no longer needed in the paymentsheet module
