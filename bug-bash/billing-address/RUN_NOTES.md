# Billing-address bug bash: 2026-11-11

Published with the [shared report index](../checkout-sessions/README.md) on 2026-11-12 at the user's request. This is the earlier billing-address finding, separate from the two findings in the later seven-topic run. The recorded regression was not rerun during publication; its local home-directory path was normalized to `~` for sharing.

Source: `cf7dd4b99965a4e754abdadacf4f337e764f6e6a` on `master`. Device: physical Pixel 6a, Android 16/API 36. App: `com.stripe.android.paymentsheet.example`, built from this checkout. All backend work used test-mode merchants and synthetic customer/card details.

One confirmed bug is documented in [ISSUE_billing_address_saved_card_edit_omits_tax_fields.md](../../ISSUE_billing_address_saved_card_edit_omits_tax_fields.md). A billing-tax checkout accepts a saved card's France -> US edit with only ZIP, then filters out the card because the US street/city/state are missing. Do not file a duplicate for the resulting lost selection or generic confirmation failure; those are consequences of this root cause.

The standalone [reproduction](repro_tax_billing_edit.py) provisions a fresh French saved card, supplies `customer_update[address]=auto` through a real-backend relay, drives the example UI, and verifies the lost selection and confirmation failure. It then fails the intended-behavior assertion for missing Address, City, and State. The independent run took about 87 seconds; see [result](regression-result.txt). Build/install/run instructions are in its docstring. It uses Python's standard library and adb, restores the playground preference file, and removes its own relay mapping. No SDK source was changed.

| Configuration or action | Observed outcome |
| --- | --- |
| Guest, card only, required full billing, no automatic tax | Full address collected; test payment completed |
| Select the public 510 Townsend St, San Francisco suggestion | Street, city, state, and ZIP populated |
| Continue, then reopen the new-card form | Entered card and billing values retained |
| Change US to Canada while retaining ZIP `94103` | Postal validation rejected the US ZIP |
| Cancel that invalid edit | Prior valid selection remained; payment completed |
| Saved US card with only country/ZIP, automatic billing collection | Card available and selected |
| Same saved card, required full billing | Card excluded; backend init returned an empty payment-method list |
| Automatic tax using a stored customer address (`address_source=customer`) | Saved-card billing edits did not change the tax source; this was not the target billing-tax case |
| Automatic billing, billing-based tax, saved card France -> US | Confirmed bug: editor omitted fields, saved card disappeared, selection cleared |
| Required full billing with automatic tax and separately supplied shipping defaults | New-card full address produced tax and the test payment completed; shipping details remained present |

Rejected or incomplete leads worth preserving:

- The incomplete saved card's absence with **required** billing was correct backend filtering, not an SDK disappearance bug. A direct init comparison established the difference.
- Automatic tax with a returning customer lacking an address and without `customer_update[address]=auto` failed session creation. Correct the integration preconditions before testing SDK behavior.
- A stored customer address caused tax to use `address_source=customer`. Do not infer that changing a payment method must recalculate billing-based tax in that configuration.
- A partial US billing default reached the form on the preinstalled app, but that check was not repeated on the newly built commit. It is not completed coverage for this revision.
- A same-as-shipping checkbox was not offered by the tested checkout configuration. Copy/toggle behavior was not exercised; no bug was filed for absent preview functionality.

Coverage was centered on cards. Express Checkout/Google Pay/Link payment flows, other local payment methods, accessibility, rotation/process restoration, and broad locale/postal matrices were not systematically tested. Wallet autofill prompts were interruptions, not payment-flow coverage. This run does not establish correctness in those areas.

The root-cause path is recorded in the issue. Useful entry points are `DefaultEmbeddedUpdateScreenInteractorFactory`, `DefaultEditCardDetailsInteractor`, `BillingDetailsForm`, `BillingDetailsEntry`, `SavedPaymentMethodMutator`, `ManageLaunchSavedPaymentMethodSelector`, `CheckoutSheetLauncher`, `CheckoutStateLoader`, and `DefaultPaymentMethodFilter`. In particular, the editor's `requiresBillingAddressForAutomaticTax=false` disagreed with the saved-card filter's country-specific requirements.

Evidence lives in this directory: three screenshots, the device recording, a backend snapshot proving the card remains saved with incomplete US details, the regression output, and its UI hierarchy. The recording covers the country edit and Save; the separate screenshots and automated test establish the final disappearance and confirmation failure. The report links the relevant files.

At the end of the run, the temporary relay was stopped, its adb mapping and device scratch files were removed, and the playground preferences created by the run were removed (the file had originally been absent). The example app was left at its main menu. The installed debug build has `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true` from relay testing; a normal build can restore the default cleartext setting. Test-mode backend objects were retained. Reports, reproductions, and evidence are uncommitted. Earlier `/tmp/checkout_*` helpers are disposable and unnecessary for the repository-local reproduction.
