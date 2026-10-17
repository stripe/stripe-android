# Test Autofill Service

A minimal `AutofillService` for debug builds that provides hardcoded test data for autofill-enabled fields. This removes the dependency on Google Play Services or a signed-in Google account for testing autofill behavior.

## Setup

1. Install the debug build on a device or emulator
2. Open autofill settings via one of:
   - **Pixel/Stock Android**: Settings > Passwords & accounts > Autofill service
   - **Older versions (API 26-29)**: Settings > System > Languages & input > Advanced > Autofill service
   - **adb shortcut**:
     ```
     adb shell am start -a android.settings.REQUEST_SET_AUTOFILL_SERVICE
     ```
3. Select **PaymentSheet Example** as the autofill provider

## Usage

Tap into any text field that declares a `ContentType` via Compose semantics. The service matches fields by their autofill hints and presents a suggestion with hardcoded test data (e.g. "123 Main Street", "San Francisco", "4242424242424242").

## Test data

| Hint | Value |
|------|-------|
| streetAddress | 123 Main Street |
| extendedAddress | Apt 4B |
| addressLocality | San Francisco |
| addressRegion | CA |
| addressCountry | US |
| postalCode | 94105 |
| personName | Jane Doe |
| emailAddress | test@example.com |
| phoneNational | 4155551234 |
| creditCardNumber | 4242424242424242 |
| creditCardSecurityCode | 123 |
| creditCardExpirationDate | 12/28 |

## Extending

To support new fields, add entries to the `TEST_DATA` map in `TestAutofillService.kt`. The keys must match the hint strings from `androidx.autofill.HintConstants` (which Compose's `ContentType` maps to internally).
