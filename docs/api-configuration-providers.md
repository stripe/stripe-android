# ApiConfiguration Migration — Complete Summary

## What Was Done

Replaced all internal usage of `PaymentConfiguration`, `@Named(PUBLISHABLE_KEY)`, and `@Named(STRIPE_ACCOUNT_ID)` with `ApiConfiguration.State` as the single internal credential type. The merchant-facing API (`PaymentConfiguration.init()`) is unchanged.

### Key Design Decisions

1. **`() -> ApiConfiguration.State` everywhere in Dagger** — All DI-provided instances use function types, never direct values. This guarantees lazy resolution — credentials are never accessed at Dagger graph construction time.

2. **`ApiConfigurationResolver`** — The single bridge between `PaymentConfiguration` (global singleton) and `ApiConfiguration.State`. Used only by the loaders before metadata exists.

3. **`PaymentMethodMetadata.apiConfiguration`** — After loading, `ApiConfiguration.State` is stored in `PaymentMethodMetadata` and flows through the paymentsheet graph via `ApiConfigurationModule`.

4. **No `@Named` constants** — `PUBLISHABLE_KEY` and `STRIPE_ACCOUNT_ID` constants deleted from `stripe-core/NamedConstants.kt`. All consumers use `ApiConfiguration.State` or `() -> ApiConfiguration.State`.

5. **`ApiRequest.Options` also lazy** — `ApiRequestOptionsModule` provides `() -> ApiRequest.Options`, not `ApiRequest.Options` directly.

---

## How ApiConfiguration.State Is Provided

### 1. Paymentsheet `ApiConfigurationModule` (from PaymentMethodMetadata)

```kotlin
@Provides
fun provideApiConfigurationStateProvider(
    paymentMethodMetadata: Provider<PaymentMethodMetadata?>
): () -> ApiConfiguration.State {
    return { requireNotNull(paymentMethodMetadata.get()).apiConfiguration }
}
```

The function is never called at construction time — only when a consumer invokes it during an operation (after metadata is loaded).

**Components using this module:**
- `PaymentSheetLauncherComponent`
- `PaymentOptionsViewModelFactoryComponent`
- `FlowControllerStateComponent`
- `EmbeddedCommonModule` (in Embedded components)
- `LinkControllerModule` (in `LinkControllerComponent`)
- `TapToAddViewModelComponent`

**Inline providers (same source, returns `() -> ApiConfiguration.State`):**
- `NativeLinkModule` — `{ paymentMethodMetadata.apiConfiguration }`
- `NfcScanningEventReporterModule` — `{ paymentMethodMetadata.apiConfiguration }`

### 2. `ApiConfigurationResolver` (PaymentElementLoader / CustomerSheetLoader)

Resolves from `PaymentConfiguration` singleton. Used ONLY by the loaders to construct `ApiConfiguration.State` before metadata exists:

```kotlin
internal class ApiConfigurationResolver @Inject constructor(
    private val paymentConfiguration: Provider<PaymentConfiguration>,
) {
    fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        if (apiConfiguration != null) return apiConfiguration
        val config = paymentConfiguration.get()
        return ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }
}
```

**Consumers:**
- `DefaultPaymentElementLoader` — resolves once at start of `load()`, passes to all methods
- `DefaultCustomerSheetLoader` — same pattern

### 3. `ApiConfigurationFromPaymentConfigurationModule` (standalone components)

For modules that don't have `PaymentMethodMetadata` and aren't in the paymentsheet graph:

```kotlin
@Provides
fun provideApiConfigurationStateProvider(
    paymentConfiguration: Provider<PaymentConfiguration>
): () -> ApiConfiguration.State {
    return {
        val config = paymentConfiguration.get()
        ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }
}
```

**Components using this module:**
- `OnrampModule` (crypto-onramp)
- `PaymentMethodMessagingModule` (payment-method-messaging)

### 4. `@BindsInstance apiConfigurationProvider: () -> ApiConfiguration.State`

Components created at runtime entry points. The caller passes a lambda:

| Component | Caller | Source of key |
|-----------|--------|--------------|
| `PaymentLauncherViewModelFactoryComponent` | `PaymentLauncherViewModel.Factory` | `PaymentLauncherContract.Args` |
| `NextActionHandlerComponent` | `DefaultPaymentNextActionHandlerRegistry.createInstance()` | Passed from caller |
| `Stripe3ds2TransactionViewModelFactoryComponent` | `Stripe3ds2TransactionViewModel.Factory` | Args |
| `PassiveChallengeComponent` | `PassiveChallengeViewModel.Factory` | Args |
| `PassiveChallengeWarmerActivityComponent` | `PassiveChallengeWarmerViewModel.Factory` | Args |
| `CollectBankAccountComponent` | `CollectBankAccountModule` | Args |
| `IntentConfirmationChallengeComponent` | Config module | Args |
| `CheckoutControllerComponent` | `CheckoutController.Builder.build()` | `PaymentConfiguration.getInstance()` |
| `CustomerSheetViewModelComponent` | `CustomerSheetViewModel.Factory` | `PaymentConfiguration.getInstance()` |
| `PollingComponent` | `PollingViewModel.Factory` | `PollingContract.Args` |
| `NativeLinkComponent` | `NativeLinkModule` | `PaymentMethodMetadata` |
| `AutocompleteViewModelFactoryComponent` | `AutocompleteViewModel.Isolated` | `AutocompleteContract.Args` |
| `AddressElementViewModelFactoryComponent` | `AddressElementViewModel.Factory` | `AddressElementActivityContract.Args` |
| `GooglePayPaymentMethodLauncherViewModelFactoryComponent` | `GooglePayPaymentMethodLauncherViewModel.Factory` | `PaymentConfiguration.getInstance()` (lazy) |

### 5. Customer Sheet data source components

```kotlin
@BindsInstance apiConfigurationProvider: () -> ApiConfiguration.State
```

| Component | Caller |
|-----------|--------|
| `CustomerAdapterDataSourceComponent` | `CustomerSheetHacks.initialize()` |
| `CustomerSessionDataSourceComponent` | `CustomerSheetHacks.initialize()` |
| `StripeCustomerAdapterComponent` | `CustomerAdapter.create()` |

All pass `{ PaymentConfiguration.getInstance(context).let { ... } }` — lazy, deferred until needed.

### 6. `ApiRequestOptionsModule`

Converts `() -> ApiConfiguration.State` into `() -> ApiRequest.Options`:

```kotlin
@Provides
fun provideApiRequestOptionsProvider(
    apiConfigurationProvider: () -> ApiConfiguration.State
): () -> ApiRequest.Options {
    return {
        val state = apiConfigurationProvider()
        ApiRequest.Options(apiKey = state.publishableKey, stripeAccount = state.stripeAccountId)
    }
}
```

### 7. Other modules

| Module | Source |
|--------|--------|
| `FinancialConnectionsSheetConfigurationModule` | `FinancialConnectionsSheetConfiguration` (bound directly) |
| `stripecardscan/CardScanModule` | `CardScanConfiguration.publishableKey` (passed via Intent) |

---

## Where PaymentConfiguration Is Still Used

### In paymentsheet (all via `ApiConfigurationResolver` or entry-point lambdas):

| Location | Why |
|----------|-----|
| `ApiConfigurationResolver` | Bridge: resolves `ApiConfiguration.State` from singleton for loaders |
| `PaymentSheetActivity` | `isLiveMode` check before load (for error display) |
| `AddressLauncher.present()` | Entry point: sources key for `AddressElementActivityContract.Args` |
| `CheckoutController.Builder.build()` | Entry point: `@BindsInstance` lambda for component |
| `CustomerSheetViewModel.Factory` | Entry point: `@BindsInstance` lambda for component |
| `CustomerSheetHacks.initialize()` | Entry point: `@BindsInstance` lambda for data source components |
| `CustomerAdapter.create()` | Entry point: `@BindsInstance` lambda for adapter component |
| `PaymentSheetCommonModule` (include) | Provides `PaymentConfiguration` for `ApiConfigurationResolver` |
| `EmbeddedCommonModule` (include) | Same |
| `CheckoutModule` (include) | Same |
| `CustomerSheetViewModelComponent` (include) | Same |

### In payments-core:

| Location | Why |
|----------|-----|
| `PaymentConfigurationModule` | Core module: provides `PaymentConfiguration` from singleton |
| `ApiConfigurationFromPaymentConfigurationModule` | Bridge for standalone modules |
| `PaymentConfiguration.init()` | Merchant entry point |
| `GooglePayPaymentMethodLauncher` | Merchant-facing class, reads at construction (merchant has initialized) |
| `GooglePayPaymentMethodLauncherViewModel.Factory` | Lazy lambda for component |
| `GooglePayLauncher` constructors | Merchant-facing class |
| `GooglePayLauncherViewModel` | Component creation |
| `PaymentLauncherFactory.create(Context)` | Lazy lambda for `StripePaymentLauncher` |
| `PaymentLauncherConfirmationActivity` | Fallback when intent args are null |
| `CardWidgetViewModel` | Standalone view widget |
| `IssuingCardPinService` | Standalone service |
| Various `ErrorReporter.createFallbackInstance()` callers | Analytics fallback |

### In other modules:

| Module | Location | Why |
|--------|----------|-----|
| `crypto-onramp` | `ApiConfigurationFromPaymentConfigurationModule` | Bridge |
| `payment-method-messaging` | `ApiConfigurationFromPaymentConfigurationModule` | Bridge |
| `financial-connections` | `FinancialConnectionsSheetConfigurationModule` | Sources from `FinancialConnectionsSheetConfiguration` (not `PaymentConfiguration`) |

---

## Rationale

1. **Why `() -> ApiConfiguration.State` instead of direct values?** — Dagger eagerly resolves all `@Provides` methods when a consumer requests the type. If anything in a graph takes `ApiConfiguration.State` directly, it triggers resolution at component construction — before `PaymentConfiguration.init()` or `PaymentElementLoader.load()` has run. Function types are never called until the consumer explicitly invokes them.

2. **Why not just `Provider<ApiConfiguration.State>`?** — Dagger can't provide `Provider<T>` from a `@Provides` method. `Provider<T>` is a Dagger framework type. `() -> T` is a plain Kotlin type that Dagger treats as any other binding.

3. **Why keep `PaymentConfiguration` at entry points?** — Until merchants pass `ApiConfiguration` directly on their configuration objects, the global singleton is the only source of credentials at app startup. Entry points (ViewModel factories, Activity initialization) read from it lazily via lambdas.

4. **Why `ApiConfigurationResolver` instead of reading `PaymentConfiguration` directly?** — It's the preparation point for the future merchant API. When `ApiConfiguration` is added to `PaymentSheet.Configuration`, the resolver will prefer the merchant-supplied value. Only one class touches the singleton.

5. **Why store in `PaymentMethodMetadata`?** — After loading, credentials are frozen for the session. Storing in metadata makes them available to all post-load consumers without re-reading the singleton. It also ensures the correct credentials survive process death (parceled with metadata).

---

## What's Next

1. **Add `ApiConfiguration` to merchant-facing configuration objects** — `PaymentSheet.Configuration`, `EmbeddedPaymentElement.Configuration`, `CustomerSheet.Configuration`
2. **Update `ApiConfigurationResolver`** — Prefer the merchant-supplied value when available
3. **Fix the `onInit` analytics timing** — Event reporters fire at ViewModel creation before metadata exists. The `() -> ApiConfiguration.State` pattern means analytics requests defer resolution, but the actual event still needs a publishable key when fired.
4. **Remove `PaymentConfiguration` dependency from paymentsheet** — Once merchants pass `ApiConfiguration` directly, the singleton is no longer needed
