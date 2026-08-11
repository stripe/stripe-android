# Stacked PR Plan: API Configuration Migration

Total: ~5,700 lines changed (excluding the unrelated `LinkTest.kt` androidTest rewrite of ~2,100 lines which should be a separate PR).

## Dependency Order

```
PR 1 (foundation) 
  ↓
PR 2 (payments-core migration)
  ↓
PR 3 (paymentsheet DI + loader)
  ↓
PR 4 (Link requestOptions threading)
  ↓
PR 5 (TapToAdd + RetrieveCustomerEmail)
  ↓
PR 6 (paymentsheet remaining: customersheet, confirmation, satellite modules)
  ↓
PR 7 (cleanup: remove old constants + docs)
```

---

## PR 1: Introduce `ApiConfiguration.State` and `ApiConfigurationFromPaymentConfigurationModule`

**~250 lines** | Base: `master`

Additive-only. Introduces the new types without removing anything old.

**Files:**
- `stripe-core/…/ApiConfiguration.kt` — **NEW** class
- `payments-core/…/ApiConfigurationFromPaymentConfigurationModule.kt` — **NEW** module (provides `() -> ApiConfiguration.State` from `PaymentConfiguration`)
- `payments-core/…/ApiRequestOptionsModule` — inside the same new file (provides `() -> ApiRequest.Options` and eager `ApiRequest.Options`)
- `payments-core/…/PaymentsFraudDetectionDataRepositoryFactory.kt` — add `publishableKeyProvider` param
- `payments-core/…/PaymentConfiguration.kt` — pass publishable key to fraud detection in `init()`
- `payments-core/…/ErrorReporter.kt` — add `publishableKeyProvider` to `createFallbackInstance`
- `payments-core/…/PaymentAnalyticsRequestFactory.kt` — constructor takes `() -> String` instead of eager `String`

**Key principle:** Old `@Named(PUBLISHABLE_KEY)` / `@Named(STRIPE_ACCOUNT_ID)` still work. Nothing is removed. New code can use either path.

---

## PR 2: Migrate `payments-core` components to `() -> ApiConfiguration.State`

**~450 lines** | Base: PR 1

Migrates all `payments-core` DI components and classes from `@Named` string providers to `() -> ApiConfiguration.State`.

**Files:**
- `payments-core/…/PaymentConfigurationModule.kt` — remove `@Named` providers (keep only `providePaymentConfiguration`)
- `payments-core/…/StripeRepositoryModule.kt` — update comment
- `payments-core/…/StripeApiRepository.kt` — use `publishableKeyProvider` for fraud detection
- `payments-core/…/PaymentLauncherViewModelFactoryComponent.kt` — replace `@Named` BindsInstance with `apiConfigurationProvider`
- `payments-core/…/PaymentLauncherModule.kt` — updated bindings
- `payments-core/…/StripePaymentLauncher.kt` — use `apiConfigurationProvider`
- `payments-core/…/StripePaymentLauncherAssistedFactory.kt` — updated factory
- `payments-core/…/PaymentLauncherFactory.kt` — construct `ApiConfiguration.State`
- `payments-core/…/PaymentLauncherConfirmationActivity.kt` — pass config
- `payments-core/…/PaymentLauncherViewModel.kt` — use `apiConfigProvider`
- `payments-core/…/GooglePayJsonFactory.kt` — use `apiConfigProvider`
- `payments-core/…/GooglePayLauncher.kt` — lazy `publishableKeyProvider`
- `payments-core/…/GooglePayPaymentMethodLauncher.kt` — construct `ApiConfiguration.State`
- `payments-core/…/GooglePayLauncherActivity.kt` — pass config
- `payments-core/…/GooglePayLauncherViewModel.kt` — updated
- `payments-core/…/GooglePayPaymentMethodLauncherActivity.kt` — pass config
- `payments-core/…/GooglePayPaymentMethodLauncherContractV2.kt` — updated
- `payments-core/…/GooglePayPaymentMethodLauncherViewModel.kt` — construct `ApiConfiguration.State`
- `payments-core/…/GooglePayPaymentMethodLauncherModule.kt` — include `ApiRequestOptionsModule`
- `payments-core/…/GooglePayPaymentMethodLauncherViewModelFactoryComponent.kt` — replace BindsInstance
- `payments-core/…/GooglePayRepositoryFactory.kt` — take `ApiConfiguration.State`
- `payments-core/…/GooglePayRepository.kt` — optional `apiConfiguration` param
- `payments-core/…/InternalGooglePayPaymentMethodLauncher.kt` — pass lazy key
- `payments-core/…/NextActionHandlerComponent.kt` — replace BindsInstance
- `payments-core/…/Stripe3ds2TransactionViewModelFactoryComponent.kt` — replace BindsInstance
- `payments-core/…/DefaultPaymentNextActionHandlerRegistry.kt` — use `apiConfigProvider`
- `payments-core/…/Stripe3DS2NextActionHandler.kt` — use `apiConfigProvider`
- `payments-core/…/Stripe3ds2TransactionViewModel.kt` — construct `ApiConfiguration.State`
- `payments-core/…/WebIntentNextActionHandler.kt` — use `apiConfigProvider`
- `payments-core/…/VoucherNextActionHandler.kt` — use `apiConfigProvider`
- `payments-core/…/IntentConfirmationChallengeNextActionHandler.kt` — updated
- `payments-core/…/IntentConfirmationChallengeComponent.kt` — updated
- `payments-core/…/PassiveChallengeComponent.kt` — updated
- `payments-core/…/PassiveChallengeViewModel.kt` — construct `ApiConfiguration.State` from args
- `payments-core/…/PassiveChallengeWarmerActivityComponent.kt` — updated
- `payments-core/…/PassiveChallengeWarmerViewModel.kt` — construct `ApiConfiguration.State`
- `payments-core/…/PaymentFlowResultProcessor.kt` — use `apiConfigProvider`
- `payments-core/…/StripePaymentController.kt` — construct `ApiConfiguration.State`
- `payments-core/…/CollectBankAccountComponent.kt` — replace BindsInstance
- `payments-core/…/CollectBankAccountModule.kt` — construct `ApiConfiguration.State`
- `payments-core/…/DefaultCardAccountRangeRepositoryFactory.kt` — use `ApiConfiguration.State`
- `payments-core/…/DefaultIntentStatusPoller.kt` — use `apiConfigProvider`
- `payments-core/…/CardWidgetViewModel.kt` — use `apiConfigProvider`
- `payments-core/…/PaymentAuthWebViewActivity.kt` — read from intent/config
- `payments-core/…/PaymentAuthWebViewActivityViewModel.kt` — updated
- `payments-core/…/StripeBrowserLauncherActivity.kt` — read from config
- `payments-core/…/StripeBrowserLauncherViewModel.kt` — construct `ApiConfiguration.State`
- `payments-core/…/IssuingCardPinService.kt` — use `ApiConfiguration.State`
- `stripe-core/…/ApiRequest.kt` — remove `@Inject` constructor
- `stripe-core/…/NamedConstants.kt` — remove `PUBLISHABLE_KEY` and `STRIPE_ACCOUNT_ID`
- Related test updates

**Note:** This is the largest PR (~450 lines production + ~186 lines tests). Could be split further (Google Pay alone vs payment launcher vs auth handlers) if needed.

---

## PR 3: PaymentSheet DI infrastructure + `PaymentElementLoader`

**~450 lines** | Base: PR 2

Introduces `ApiConfigurationModule`, `ApiConfigurationResolver`, `PaymentMethodMetadata.apiConfiguration`, and the loader changes.

**Files:**
- `paymentsheet/…/ApiConfigurationModule.kt` — **NEW** (provides from `PaymentMethodMetadata`)
- `paymentsheet/…/ApiConfigurationResolver.kt` — **NEW** (fallback resolver)
- `paymentsheet/…/PaymentSheetLauncherComponent.kt` — include new modules
- `paymentsheet/…/PaymentSheetLauncherModule.kt` — provide metadata from StateFlow
- `paymentsheet/…/FlowControllerStateComponent.kt` — include new modules
- `paymentsheet/…/EmbeddedCommonModule.kt` — include new modules
- `paymentsheet/…/PaymentOptionsViewModelFactoryComponent.kt` — include new modules
- `paymentsheet/…/PaymentMethodMetadata.kt` — add `apiConfiguration` field
- `paymentsheet/…/CommonConfiguration.kt` — add `apiConfiguration` field
- `paymentsheet/…/PaymentElementLoader.kt` — resolve API config early, pass to `createLinkState`, `eventReporter`, `tapToAddConnectionStarter`
- `paymentsheet/…/CreateLinkState.kt` — accept `apiConfiguration` param, thread to `buildLinkConfiguration`
- `paymentsheet/…/EventReporter.kt` — add `publishableKey` to `onLoadStarted`/`onLoadFailed`
- `paymentsheet/…/DefaultEventReporter.kt` — pass key to `fireEvent`
- `paymentsheet/…/RetrieveCustomerEmail.kt` — take `stripeAccountId` param, remove `apiConfigProvider`
- `paymentsheet/…/CustomerRepository.kt` — add `stripeAccountId` to `retrieveCustomer`
- `paymentsheet/…/CustomerApiRepository.kt` — remove `Provider<PaymentConfiguration>`, use explicit param
- `paymentsheet/…/ElementsSessionRepository.kt` — use `() -> ApiRequest.Options`
- `paymentsheet/…/SavedPaymentMethodRepository.kt` — use `() -> ApiRequest.Options`
- `paymentsheet/…/CheckoutSessionRepository.kt` — use `() -> ApiRequest.Options`
- Related test updates (`DefaultPaymentElementLoaderTest`, `DefaultCreateLinkStateTest`, `DefaultRetrieveCustomerEmailTest`, `DefaultEventReporterTest`, `FakeLoadingEventReporter`)

---

## PR 4: Link `requestOptions` pass-through

**~400 lines** | Base: PR 3

Threads `requestOptions` as explicit parameter through all `LinkRepository` methods.

**Files:**
- `link/LinkConfiguration.kt` — add `apiConfiguration` field + `requestOptions` property
- `link/repositories/LinkRepository.kt` — add `requestOptions: ApiRequest.Options` to all 21 methods
- `link/repositories/LinkApiRepository.kt` — use passed `requestOptions`; remove `apiRequestOptionsProvider`
- `link/account/DefaultLinkAccountManager.kt` — pass `config.requestOptions` to all calls
- `link/account/DefaultLinkAuth.kt` — pass `config.requestOptions` to all calls
- `link/injection/NativeLinkComponent.kt` — remove `publishableKeyProvider`/`stripeAccountIdProvider` BindsInstance
- `link/injection/NativeLinkModule.kt` — provide from `PaymentMethodMetadata`
- `link/injection/LinkControllerComponent.kt` — include `ApiRequestOptionsModule`
- `link/injection/LinkControllerModule.kt` — provide from `LinkControllerInteractor.configuration`
- `link/LinkActivityViewModel.kt` — remove key providers from component creation
- `link/LinkControllerInteractor.kt` — expose `configuration`; remove `PaymentConfiguration.init()`
- `link/NativeLinkActivityContract.kt` — use injected `apiConfigurationProvider`
- `link/WebLinkActivityContract.kt` — updated
- `link/ui/paymentmenthod/PaymentMethodViewModel.kt` — use `PaymentMethodMetadata.apiConfiguration`
- `common/analytics/experiment/LogLinkHoldbackExperiment.kt` — inject `apiConfigProvider`, pass to lookup
- `paymentsheet/injection/LinkHoldbackExposureModule.kt` — remove `apiRequestOptionsProvider` from construction
- Related tests (`FakeLinkRepository`, `LinkApiRepositoryTest`, `DefaultLinkAccountManagerTest`, `DefaultLinkAuthTest`, `LogLinkGlobalHoldbackExposureTest`)

---

## PR 5: TapToAdd explicit params + remaining PaymentElementLoader callers

**~300 lines** | Base: PR 4

Removes `apiConfigProvider` from TapToAdd, passes `publishableKey`/`isLiveMode` explicitly.

**Files:**
- `common/taptoadd/TapToAddIsSimulatedProvider.kt` — `get(isLiveMode: Boolean)`
- `common/taptoadd/TapToAddConnectionManager.kt` — remove `apiConfigProvider`; `isSupported` becomes function; `ConnectionConfig` gets fields
- `common/taptoadd/TapToAddConnectionModule.kt` — remove `apiConfigProvider`
- `common/taptoadd/TapToAddCollectionHandler.kt` — read from `metadata.apiConfiguration`
- `common/taptoadd/TapToAddModule.kt` — updated
- `paymentsheet/state/TapToAddConnectionStarter.kt` — accept params
- `paymentsheet/state/TapToAddAvailabilityFactory.kt` — accept params
- Related tests (`DefaultTapToAddIsSimulatedProviderTest`, `DefaultTapToAddConnectionManagerTest`, `TapToAddRetriableConnectionManagerTest`, `FakeTapToAddConnectionManager`, `TapToAddConnectionStarterTest`, `FakeTapToAddConnectionStarter`, `FakeTapToAddAvailabilityFactory`, `DefaultTapToAddAvailabilityFactoryTest`)

---

## PR 6: CustomerSheet, confirmation, satellite modules, remaining paymentsheet

**~450 lines** | Base: PR 5

Migrates all remaining consumers.

**Files:**
- **CustomerSheet** (110 lines prod + 68 lines tests): `CustomerAdapter`, `CustomerSheet`, `CustomerSheetLoader`, `CustomerSheetViewModel`, `StripeCustomerAdapter`, `CustomerSheetHacks`, `CustomerAdapterDataSource`, `CustomerSessionElementsSessionManager`, `CustomerSessionPaymentMethodDataSource`, `CustomerSessionSavedSelectionDataSource`, all injection components
- **Confirmation** (70 lines prod + 167 lines tests): `DeferredIntentConfirmationInterceptor`, `CheckoutSessionConfirmationInterceptor`, `ConfirmationTokenConfirmationInterceptor`, `CustomerSheetConfirmationInterceptor`, `SharedPaymentTokenConfirmationInterceptor`, `DeferredIntentCallbackRetriever`, `IntentConfirmationModule`, `GooglePayConfirmationDefinition`, `PassiveChallengeConfirmationDefinition`
- **Satellite modules** (162 lines): `financial-connections`, `financial-connections-lite`, `payment-method-messaging`, `crypto-onramp`, `stripecardscan`, `payments-ui-core`
- **Other paymentsheet** (remaining ~100 lines): address element, ACH, polling, embedded, checkout, flowcontroller, NFC scanning, `PaymentSheetActivity`, `BaseSheetViewModel`, `PaymentMethodMetadata`, `CardDefinition`

---

## PR 7: Documentation + unrelated `LinkTest.kt` rewrite

**~2,450 lines** | Base: PR 6

- `docs/api-configuration-architecture.md` — architecture documentation
- `docs/payment-config-phase-6-summary.md` — branch summary
- `paymentsheet/src/androidTest/…/LinkTest.kt` — unrelated integration test rewrite (2,100 lines)

---

## Line Count Summary

| PR | Description | Estimated Lines |
|----|-------------|-----------------|
| 1 | Foundation: `ApiConfiguration.State` + new modules | ~250 |
| 2 | `payments-core` migration | ~450 + 186 tests |
| 3 | PaymentSheet DI + loader + repositories | ~450 + 300 tests |
| 4 | Link `requestOptions` pass-through | ~250 + 150 tests |
| 5 | TapToAdd explicit params | ~150 + 100 tests |
| 6 | CustomerSheet + confirmation + satellite modules | ~350 + 250 tests |
| 7 | Docs + LinkTest rewrite | ~2,450 |

**Notes:**
- PR 2 is the largest production change (~450 lines). If it exceeds 500, split into: (a) Google Pay + payment launcher, (b) auth handlers + remaining.
- PR 7's `LinkTest.kt` rewrite is unrelated and could be merged independently at any time.
- Each PR should compile and pass tests independently.
- Tests always ship with the production code they cover (not in a separate PR).
