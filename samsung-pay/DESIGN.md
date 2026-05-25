# Samsung Pay Integration — Design Overview

## Module Structure

The integration lives in the `samsung-pay` module and plugs into `paymentsheet` via a confirmation definition. It wraps Samsung's `samsungpay_2.22.00.jar` SDK.

---

## Two Modes

### 1. `SamsungPayLauncherActivity` — Full Payment Mode (Intent-based)

**Purpose:** End-to-end payment flow. The merchant passes a PaymentIntent `clientSecret`, and the SDK handles everything — Samsung Pay UI, credential collection, and (eventually) Stripe confirmation.

**Public API:** `SamsungPayLauncher`
- Merchant calls `presentForPaymentIntent(clientSecret)`
- Result is a simple `SamsungPayResult` (Completed / Canceled / Failed)

**Status:** Partially stubbed. The activity has TODO comments — it doesn't yet fetch the PI from Stripe to get amount/currency, and it doesn't yet call `PaymentController` to confirm with the credential. Hardcoded to USD/$0.

**Use case:** Drop-in "pay with Samsung Pay" button in a custom integration. Analogous to `GooglePayLauncher` (not `GooglePayPaymentMethodLauncher`).

---

### 2. `SamsungPayPaymentMethodLauncherActivity` — PaymentMethod-only Mode

**Purpose:** Opens Samsung Pay, collects the credential, then creates a Stripe `PaymentMethod` and returns it. Does **not** confirm a PI — that's the caller's responsibility.

**Public API:** `SamsungPayPaymentMethodLauncher`
- Merchant calls `present(currencyCode, amount, orderNumber)`
- Result is `Result.Completed(paymentMethod)` / `Result.Canceled` / `Result.Failed(error, errorCode)`

**Status:** Fully wired. The ViewModel calls `stripe.createPaymentMethodSynchronous(params)` with the Samsung Pay credential and returns the `PaymentMethod`.

**Use case:** PaymentSheet integration. The `SamsungPayConfirmationDefinition` uses this mode — it gets back a `PaymentMethod`, wraps it in `PaymentMethodConfirmationOption.Saved`, and feeds it back into the confirmation pipeline (which then confirms the PI separately via the standard flow).

---

## Flow Diagrams

### Mode 1: Full Payment (`SamsungPayLauncher`)

```
Merchant Activity
  → SamsungPayLauncher.presentForPaymentIntent(clientSecret)
    → ActivityResultLauncher<SamsungPayLauncherContract.Args>
      → SamsungPayLauncherActivity (transparent)
        → [TODO: fetch PI from Stripe API]
        → BaseSamsungPayActivity.startSamsungPay(...)
          → PaymentManager.startInAppPayWithCustomSheet(...)
            → Samsung Pay UI (system sheet)
              → onSuccess(credential)
                → [TODO: confirm PI with PaymentController]
                → finish with SamsungPayResult.Completed
              → onFailure(errorCode)
                → finish with SamsungPayResult.Failed
    → ResultCallback.onResult(SamsungPayResult)
```

### Mode 2: PaymentMethod-only (`SamsungPayPaymentMethodLauncher`)

```
Merchant Activity / PaymentSheet
  → SamsungPayPaymentMethodLauncher.present(currency, amount, orderNumber)
    → ActivityResultLauncher<SamsungPayPaymentMethodLauncherContract.Args>
      → SamsungPayPaymentMethodLauncherActivity (transparent)
        → BaseSamsungPayActivity.startSamsungPay(...)
          → PaymentManager.startInAppPayWithCustomSheet(...)
            → Samsung Pay UI (system sheet)
              → onSuccess(credential)
                → ViewModel.createPaymentMethod(credential)
                  → Stripe.createPaymentMethodSynchronous(samsungPayParams)
                  → finish with Result.Completed(paymentMethod)
              → onFailure(errorCode)
                → finish with Result.Failed(error, errorCode)
    → ResultCallback.onResult(Result)
```

### PaymentSheet Integration (via Mode 2)

```
PaymentSheet Confirmation Pipeline
  → SamsungPayConfirmationDefinition.action() → Launch
  → SamsungPayConfirmationDefinition.launch()
    → SamsungPayPaymentMethodLauncherContract.Args(config, currency, amount, orderNumber)
      → [Mode 2 flow above]
  → SamsungPayConfirmationDefinition.toResult()
    → Result.Completed → NextStep(PaymentMethodConfirmationOption.Saved)
      → Re-enters confirmation pipeline → confirms PI with the PaymentMethod
    → Result.Canceled → Canceled
    → Result.Failed → Failed
```

---

## Design Quirks

1. **Activity-as-host pattern.** Samsung's SDK callback (`CustomSheetTransactionInfoListener`) delivers results on the main thread via callback methods, not via `ActivityResult`. The transparent activities exist solely to receive these callbacks and translate them into `ActivityResult` parcels. This is because Samsung Pay's `PaymentManager` is tied to an `Activity` lifecycle.

2. **`BaseSamsungPayActivity` inheritance.** Both launcher activities inherit from a shared base that builds the `CustomSheetPaymentInfo` and starts the Samsung Pay sheet. Abstract methods (`onSamsungPaySuccess`/`onSamsungPayFailure`) let each subclass handle results differently.

3. **`hasLaunched` in SavedStateHandle.** The PaymentMethod activity uses a ViewModel flag to guard against re-launching Samsung Pay on configuration change (process death -> `onCreate` replay). The simpler `SamsungPayLauncherActivity` does not do this — potential bug on rotation.

4. **Readiness gating is duplicated.** Both `SamsungPayLauncher` and `SamsungPayPaymentMethodLauncher` independently check `GetSamsungPayStatus` on init and gate `present*()` behind `isReady`. The PaymentSheet path has a `skipReadyCheck` escape hatch (`@RestrictTo(LIBRARY_GROUP)`) since readiness is checked elsewhere in the PaymentSheet pipeline.

5. **`SamsungPayViewModel` is a dev/debug leftover.** It has hardcoded amounts ($0.60), `Log.d` statements, and isn't used by either launcher activity. Appears to be the original exploration prototype before the architecture was split into two modes.

6. **Example app uses a stale API.** `SamsungPayLauncherIntegrationActivity` references `Config(amount = 1205L)` and `SamsungPayResult.Success`/`.Cancel`/`.Failure` — these don't match the current `Config` constructor or the sealed interface variants (`Completed`/`Canceled`/`Failed`). The example hasn't been updated to match the refactor.

7. **Two different result types.** Mode 1 uses `SamsungPayResult` (a simple sealed interface). Mode 2 uses `SamsungPayPaymentMethodLauncher.Result` (a nested sealed class with error codes and a `PaymentMethod` payload). They're completely disjoint — no shared sealed hierarchy.

8. **PaymentSheet piggybacks on Mode 2 then confirms separately.** The `SamsungPayConfirmationDefinition.toResult()` returns `NextStep` with a `Saved` payment method, which re-enters the confirmation pipeline. This means Samsung Pay confirmation is a two-phase operation internally (unlike Google Pay which can do it in one shot).

9. **`cardBrandFilter` bridging.** The public `Config` takes a `List<CardBrand>` and wraps it in a private `AllowListCardBrandFilter`. The `@RestrictTo` constructor takes a raw `CardBrandFilter` directly — this lets PaymentSheet pass its own merchant-configured filter without conversion.

---

## Key Files

| File | Role |
|------|------|
| `samsung-pay/.../SamsungPayLauncher.kt` | Public API for Mode 1 (full payment) |
| `samsung-pay/.../SamsungPayPaymentMethodLauncher.kt` | Public API for Mode 2 (PM-only) |
| `samsung-pay/.../BaseSamsungPayActivity.kt` | Shared Samsung SDK interaction logic |
| `samsung-pay/.../SamsungPayLauncherActivity.kt` | Mode 1 host activity |
| `samsung-pay/.../SamsungPayPaymentMethodLauncherActivity.kt` | Mode 2 host activity |
| `samsung-pay/.../SamsungPayPaymentMethodLauncherViewModel.kt` | Creates PaymentMethod from credential |
| `samsung-pay/.../GetSamsungPayStatus.kt` | Device readiness check |
| `samsung-pay/.../Config.kt` | Merchant configuration (serviceId, merchantId, cardBrands) |
| `samsung-pay/.../SamsungFactory.kt` | Builds Samsung `PartnerInfo` |
| `paymentsheet/.../SamsungPayConfirmationDefinition.kt` | PaymentSheet pipeline adapter |
| `paymentsheet/.../SamsungPayButton.kt` | Compose button component |
