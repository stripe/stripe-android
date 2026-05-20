# Samsung Pay SDK Quirks

Known quirks and gotchas when working with the Samsung Pay SDK (`com.samsung.android.sdk.samsungpay.v2`).

## Currency codes must be uppercase

`AmountBoxControl` requires ISO 4217 uppercase currency codes (e.g. `"USD"`, `"EUR"`).
Passing lowercase (e.g. `"usd"`) throws `IllegalArgumentException: usd is invalid currencyCode`.

Stripe's `PaymentIntent.currency` returns lowercase, so always call `.uppercase()` before passing to Samsung Pay.

## Order number is mandatory when Visa cards are allowed

`PaymentManager.startInAppPayWithCustomSheet()` throws `IllegalArgumentException: Order number is mandatory for VISA` if `CustomSheetPaymentInfo` does not have an order number set and Visa is in the allowed card brands.

Always call `.setOrderNumber(...)` on the `CustomSheetPaymentInfo.Builder`. We use the PaymentIntent ID as the order number.

## Order number only allows alphanumeric and hyphens

`setOrderNumber()` throws `IllegalArgumentException: Order number contains non-allowed character. Alphanumeric and hyphens(-) are allowed.` if the value contains underscores or other special characters.

Stripe PaymentIntent IDs contain underscores (e.g. `pi_xxx_secret_xxx`), so we sanitize by replacing non-allowed characters with hyphens via `replace(Regex("[^A-Za-z0-9-]"), "-")`.

## Merchant name must be non-empty

`PaymentManager.startInAppPayWithCustomSheet()` throws `IllegalArgumentException: You must set merchant name.` if the merchant name on `CustomSheetPaymentInfo` is empty or null. Always ensure a non-empty value is passed.

## `SpaySdk.ServiceType` must be set as a String

When building `PartnerInfo`, the service type must be set via:
```kotlin
bundle.putString(SpaySdk.PARTNER_SERVICE_TYPE, SpaySdk.ServiceType.INAPP_PAYMENT.toString())
```
Using the enum directly without `.toString()` will fail silently or crash.

## Samsung Pay callbacks run on the main thread

`CustomSheetTransactionInfoListener` callbacks (`onSuccess`, `onFailure`, `onCardInfoUpdated`) are invoked on the main thread. Any heavy work (e.g. Stripe API calls to create a PaymentMethod) must be dispatched to a background thread.

## `updateSheet()` must be called in `onCardInfoUpdated`

When the user selects a different card in the Samsung Pay sheet, `onCardInfoUpdated` fires. You must call `paymentManager.updateSheet(customSheet)` in this callback or the sheet will hang/crash.

## Samsung Pay is only available on Samsung devices

`GetSamsungPayStatus` will return `SPAY_NOT_SUPPORTED` on non-Samsung devices. The `com.samsung.android.spay` package query is declared in the manifest to support this check.

## `SPAY_NOT_READY` has multiple sub-reasons

When status is `SPAY_NOT_READY`, check `bundle.getInt(SamsungPay.EXTRA_ERROR_REASON)`:
- `ERROR_SPAY_SETUP_NOT_COMPLETED` — Samsung Pay is installed but not set up. Can prompt user via `activateSamsungPay()`.
- `ERROR_SPAY_APP_NEED_TO_UPDATE` — Samsung Pay app needs updating.
- Other codes — Samsung Pay is not usable on this device.
