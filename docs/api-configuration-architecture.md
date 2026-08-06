## How `ApiConfiguration` is Provided Throughout the SDK

### Overview

`ApiConfiguration.State` is the internal credential holder (publishable key + optional stripe account ID) that replaces the old `@Named(PUBLISHABLE_KEY)` / `@Named(STRIPE_ACCOUNT_ID)` string injections. It is never injected as a raw value — it's always provided as `() -> ApiConfiguration.State` (a lazily-invoked function type) to avoid resolution before credentials are available.

---

### Core Classes

**`ApiConfiguration`** (`stripe-core`) — Builder-pattern class with an inner `State` Parcelable data class:
```kotlin
data class State(
    val publishableKey: String,
    val stripeAccountId: String?
) : Parcelable {
    fun isLiveMode(): Boolean = !publishableKey.startsWith("pk_test")
}
```

**`ApiConfiguration.State`** is stored on:
- `PaymentMethodMetadata.apiConfiguration` — set after elements session loads
- `CommonConfiguration.apiConfiguration: ApiConfiguration.State?` — nullable, set from merchant config or LinkController config

---

### Dagger Modules That Provide `() -> ApiConfiguration.State`

There are **three different modules** that provide `() -> ApiConfiguration.State`, used in different component graphs depending on what credential source is available:

#### 1. `ApiConfigurationModule` (paymentsheet)
**Used by:** `PaymentSheetLauncherComponent`, `FlowControllerStateComponent`, `EmbeddedCommonModule`, `PaymentOptionsViewModelFactoryComponent`, `NfcScanningViewModelComponent`

```kotlin
@Provides
fun provideApiConfigurationStateProvider(
    paymentMethodMetadata: Provider<PaymentMethodMetadata?>
): () -> ApiConfiguration.State {
    return { requireNotNull(paymentMethodMetadata.get()).apiConfiguration }
}
```
Reads from `PaymentMethodMetadata` which is populated after loading. The lambda is lazy — only called after metadata exists.

#### 2. `ApiConfigurationFromPaymentConfigurationModule` (payments-core)
**Used by:** Standalone components outside paymentsheet (e.g., `PaymentMethodMessagingComponent`, `StripePaymentLauncherComponent`, `PaymentLauncherComponent`)

```kotlin
@Provides
fun provideApiConfigurationStateProvider(
    paymentConfiguration: Provider<PaymentConfiguration>
): () -> ApiConfiguration.State {
    return {
        val config = paymentConfiguration.get()
        ApiConfiguration.State(publishableKey = config.publishableKey, stripeAccountId = config.stripeAccountId)
    }
}
```
Reads from `PaymentConfiguration` singleton (merchant must have called `PaymentConfiguration.init()` first).

#### 3. `LinkControllerModule` (inline provider)
**Used by:** `LinkControllerComponent` (standalone link flow, crypto-onramp)

```kotlin
@Provides
fun provideApiConfigurationStateProvider(
    interactor: Provider<LinkControllerInteractor>
): () -> ApiConfiguration.State {
    return {
        val config = requireNotNull(interactor.get().configuration)
        ApiConfiguration.State(publishableKey = config.publishableKey, stripeAccountId = config.stripeAccountId)
    }
}
```
Reads from the `LinkControllerInteractor.configuration` field, which is set at the start of `configure()` before any loading occurs. Uses `Provider<LinkControllerInteractor>` to break a dependency cycle.

#### 4. `TapToAddViewModelComponent` (inline providers)
**Used by:** `TapToAddViewModelComponent` (standalone component for tap-to-add flow)

```kotlin
@Provides
fun providesApiConfigurationProvider(
    paymentMethodMetadata: PaymentMethodMetadata
): () -> ApiConfiguration.State = { paymentMethodMetadata.apiConfiguration }
```
Reads directly from `@BindsInstance PaymentMethodMetadata` — available immediately at component creation (no timing issue, no ViewModel reference).

---

### `ApiRequestOptionsModule` (payments-core)

Converts `() -> ApiConfiguration.State` into `() -> ApiRequest.Options` and eagerly-resolved `ApiRequest.Options`:

```kotlin
@Provides
fun provideApiRequestOptionsProvider(
    apiConfigurationProvider: () -> ApiConfiguration.State
): () -> ApiRequest.Options { ... }

@Provides
fun provideApiRequestOptions(
    apiRequestOptionsProvider: () -> ApiRequest.Options
): ApiRequest.Options = apiRequestOptionsProvider()
```

Included in all components that need to make API requests.

---

### `ApiConfigurationResolver` (paymentsheet)

Bridge class used **only** in `PaymentElementLoader` to resolve credentials before metadata exists:

```kotlin
class ApiConfigurationResolver @Inject constructor(
    private val paymentConfiguration: Provider<PaymentConfiguration>,
) {
    fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        if (apiConfiguration != null) return apiConfiguration
        val config = paymentConfiguration.get()
        return ApiConfiguration.State(publishableKey = config.publishableKey, stripeAccountId = config.stripeAccountId)
    }
}
```

At the start of `PaymentElementLoader.load()`, it resolves from `CommonConfiguration.apiConfiguration` if set, otherwise falls back to `PaymentConfiguration`. This resolved value is then threaded through the load flow as a method parameter.

---

### Flow: How Credentials Propagate in PaymentSheet

```
1. Merchant calls PaymentSheet.Configuration (sets nothing about keys)
   OR future: passes ApiConfiguration directly

2. PaymentElementLoader.load() starts:
   - apiConfigurationResolver.resolve(commonConfig.apiConfiguration)
     -> returns ApiConfiguration.State (from config or PaymentConfiguration fallback)
   - eventReporter.onInit(apiConfiguration.publishableKey)
   - Passes apiConfiguration to elementsSessionLoader, googlePay, etc.

3. Elements session loads -> PaymentMethodMetadata is created with apiConfiguration field

4. PaymentMethodMetadata stored on ViewModel's StateFlow
   - PaymentSheetLauncherModule.providePaymentMethodMetadataFlow() -> @Singleton StateFlow
   - PaymentSheetLauncherModule.providePaymentMethodMetadata(flow) -> reads flow.value

5. ApiConfigurationModule's lambda -> reads metadata.apiConfiguration when invoked
   (only invoked AFTER metadata is populated)
```

---

### Memory Leak Fix: `PaymentSheetLauncherModule`

The `providePaymentMethodMetadata` method previously took `PaymentSheetViewModel` directly, causing Dagger's generated factory to hold `Provider<PaymentSheetViewModel>`. Any lambda capturing `Provider<PaymentMethodMetadata?>` (e.g., from `ApiConfigurationModule`) would retain the ViewModel.

**Fix:** Split into two providers:
```kotlin
@Provides @Singleton
fun providePaymentMethodMetadataFlow(viewModel: PaymentSheetViewModel): StateFlow<PaymentMethodMetadata?> {
    return viewModel.paymentMethodMetadata  // extracted once, cached
}

@Provides
fun providePaymentMethodMetadata(flow: StateFlow<PaymentMethodMetadata?>): PaymentMethodMetadata? {
    return flow.value  // factory holds Provider<StateFlow>, not Provider<ViewModel>
}
```

Now the reference chain is: lambda -> Provider<PaymentMethodMetadata?> -> factory holding Provider<StateFlow<...>> -> StateFlow. The StateFlow doesn't reference the ViewModel back, so the ViewModel can be GC'd.

---

### Key Consumers of `() -> ApiConfiguration.State`

| Consumer | What it does with it |
|----------|---------------------|
| `StripeApiRepository` | `publishableKeyProvider = { apiConfigProvider().publishableKey }` |
| `PaymentAnalyticsRequestFactory` | Signs analytics requests with publishable key |
| `LinkApiRepository` | Creates `ApiRequest.Options` for Link API calls |
| `DefaultTapToAddIsSimulatedProvider` | Checks `isLiveMode()` to determine simulated mode |
| `TapToAddConnectionModule` | Passes to connection manager for Terminal SDK init |
| `TapToAddModule` | Passes to `TapToAddCollectionHandler.create()` |
| `CheckoutSessionRepository` | Creates API options for checkout session calls |
| `DefaultSavedPaymentMethodRepository` | Creates API options for saved PM operations |
| `AttestationConfirmationDefinition` | Creates API options for attestation flows |
| `DeferredIntentCallbackRetriever` | Checks `isLiveMode()` for error message logic |

---

### Where `PaymentConfiguration` is Still Used

- `ApiConfigurationResolver` — fallback when `CommonConfiguration.apiConfiguration` is null (merchant hasn't passed it explicitly)
- `ApiConfigurationFromPaymentConfigurationModule` — for standalone components outside paymentsheet
- `OnrampInteractor.configure()` — calls `PaymentConfiguration.init()` before `linkController.configure()` so that downstream `PaymentConfiguration.getInstance()` calls work
