# Shipping Address Element bug bash — 2026-11-12

## Outcome

No confirmed SDK or new documentation defect. No ISSUE_shipping report or regression test was needed. Required shipping successfully completed a real test-mode card checkout. US/Canada restriction, autocomplete, manual entry, blank/incomplete validation, save/dismiss, reopen/prefill, edit, explicit cancellation, and shared selected-card state worked in the cases below. Automatic shipping tax and totals refreshed from real backend responses.

## Environment and source contracts

- SDK HEAD `8cbb695eabcafa957a86c24e1f025643979a500a`; tracked source unchanged. Initial changes were the two foundation docs reports and existing bug-bash evidence. No staging, commit, push, remote issue filing, external messages, or fixes.
- Physical Pixel 6a `2B211JEGR05252`, API36. Native adb only, exclusively owned meaningful UI during this topic. Parent owns the wake/UNKNOWN keepalive. No emulator, instrumentation task, Gradle command, rebuild, or install in this topic.
- Reused installed baseDebug with `DIRECTLY_TO_PLAYGROUND=false`, `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true` from Express. These flags remain installed.
- Read root AGENTS.md, bug-bash SKILL.md and Android reference, docs-driven brief/index, both existing root ISSUE reports, relevant Express handoff, and tax-and-saved-cards reference before enabling automatic tax.
- Source-verified Android/common docs: [SHIPPING_DOCS_EXCERPTS.md](../docs-driven/SHIPPING_DOCS_EXCERPTS.md), Markdoc revision `4ed3418fb5a9b0af2cf2dcdca3aefb89b5b400cf`. Contracts: configure Shipping Address Element before presenting; allowed countries are a server precondition; save updates controller Session and dismisses; read updated shipping/tax/totals from the shared controller. Shipping options and checkout phone collection are preview exclusions.
- GitHub duplicate search: `GH_HOST=github.com gh issue list --repo stripe/stripe-android --state all --search 'checkout shipping' --limit 30 --json number,title,url`. Returned unrelated issues12912/6601/3719/534; no matching Shipping Address Element issue. Existing docs API-name/fold drift already covers the stale CheckoutController name in the shipping snippet; no duplicate report.
- Test backend: `https://stp-mobile-playground-backend-v7.stripedemos.com`. Owned relay [backend_probe.py](backend_probe.py) bound only127.0.0.1:18768; forwarded publishable-key and create-session requests, and changed only `shipping_address_collection.allowed_countries` to `[US, CA]`. It rejects other routes. Asserted test key/Session prefixes; creation uses backend SDK request version2026-08-26.preview. Credentials remained in private `/tmp/checkout-shipping/backend-private.json` and were deleted after cleanup.

## Actual configurations and results

Both scenarios: guest, fixed synthetic email `shipping.bugbash@example.com`, save-payment-method false, card-only allowed type, Payment Element configured, wallets disabled, Express unconfigured, Shipping Address Element configured, USD120 cart (2×$35 shirt+$50 hoodie), no billing-required override, discounts, or adaptive pricing.

### No-tax shipping control

[required-us-ca.json](required-us-ca.json), US merchant, automatic tax false. Session hash4441d2c27268.

| Action | Observed result/evidence |
| --- | --- |
| Create required-shipping Session | Initial `No shipping address`; backend open/unpaid/not_started, total12000, collection allowed countries exactlyUS/CA. [Initial backend](backend-initial.json). |
| Tap Save address on empty form | Sheet remains open; required fields highlighted and “This field cannot be blank.” shown. No address saved. Country menu screenshot also retains these validation markings. |
| Open Country or region | Exactly United States and Canada, matching backend restriction. [Inspected screenshot](country-restriction.png). |
| Enter Shipping Tester and510 Townsend Street | Real Stripe-hosted autocomplete returned San Francisco/CA/USA and other matches; selected San Francisco suggestion. Expanded fields contained510 Townsend Street, San Francisco, California,94103,US. |
| Add Suite200 and Save address | Sheet dismissed; controller Session rendered recipient, line1/line2, city, state, postal code, country correctly. [UI](us-saved.png), [hierarchy](us-saved-ui.jsonl). |
| Reopen Shipping Address Element | Full saved US address and Suite200 prefilled. No second Session created. |
| Change line2 to UNSAVED Suite999; tap Close | Sheet explicitly closed; Session still Suite200. [Hierarchy](canceled-edit-ui.jsonl). No dismissal inferred from a timeout. |
| Select test Visa4242 | Card4242424242424242, expiry12/30, CVC123, billing US/94103; Continue selected it. No confirm failure or email update before shipping edit. |
| Edit country US→Canada | Dropdown constrained to same two countries; Canadian fields appeared. Old US ZIP94103 showed “Your postal code is incomplete.” under Canadian rules. Province initially Alberta; deliberately selected Ontario. |
| Save full Canadian address | Shipping Tester,100 Queen St W,Unit12,Toronto,ON,M5H2N2,CA. Session updated all fields and selected Visa4242 remained. [Inspected UI](ca-edited-card-selected.png), [same Session backend before confirm](backend-ca-before-confirm.json). |
| Confirm once | SDK `Payment complete!`; [UI](ca-payment-complete.png). Independent [poll](backend-ca-payment.json): HTTP200, confirmation_status=succeeded, status=complete, payment_status=paid, livemode=false. No reselect needed after shipping edit. |

### Automatic-tax shipping control

[tax-us-ca.json](tax-us-ca.json), merchant `us_tax`, automatic tax true, otherwise same setup. Session hashce561fbfce35. This scope deliberately stops before a deep billing/tax matrix.

| Action | Observed result/evidence |
| --- | --- |
| Create without address | UI “Enter shipping address to calculate”; backend automatic_tax.address_source=shipping, enabled=true, status=requires_address, total12000. [UI](tax-before-address.png), [backend](backend-tax-initial.json). |
| Enter Tax Shipping Tester/510 Townsend Street, then Enter address manually | Explicit manual entry expanded the form and preserved typed street/name. Filled San Francisco,94103,California; line2 empty. This independently exercised manual fallback, while first scenario used a real autocomplete suggestion. |
| Save US address | Sheet dismissed, sales tax$10.35, total$130.35; shirt$76.04 and hoodie$54.31. Backend status=ready, tax_exclusive1035, percentage8.625, total13035. [UI](tax-us-saved.png), [backend](backend-tax-us-saved.json). |
| Edit to200 N Spring St,Los Angeles,CA90012,US; Save | Sheet dismissed; sales tax$12.30, total$132.30; shirt$77.17 and hoodie$55.13. Backend ready, tax_exclusive1230, percentage10.25, total13230. [UI](tax-la-saved.png), [backend](backend-tax-la-saved.json). |
| Edit ZIP to94103; tap Close without save | Controller Session preserved Los Angeles/90012; backend ready/tax1230/total13230 unchanged. [UI hierarchy](tax-canceled-ui.jsonl), [backend](backend-tax-canceled.json). |

All7 retained screenshots were opened and visually inspected. No video recorded. Only synthetic app data was retained; no wallet UI or personal autofill data captured.

## Reachable implementation and evidence boundaries

- Playground `ConfirmationControls` invokes the presenter's ShippingAddressElement; the Activity creates that presenter on the same controller as Payment Element. ShippingAddressElement.present reads existing collectedDetails and server allowedShippingCountries, then launches the address activity with those values. Its success result calls controller.commitShippingAddress; Canceled only resets presentation state.
- InputAddressViewModel.clickPrimaryButton rejects incomplete form values without calling its primary action. `CheckoutShippingPrimaryButtonAction` in `paymentsheet/injection/AddressElementViewModelModule.kt` delegates to CheckoutSessionTaxRegionUpdater. Tax updater calls repository.updateTaxRegion only when automatic tax is enabled and source=shipping; otherwise it returns the existing response and stores the collected address locally. Repository updateTaxRegion serializes country,line1,line2,city,state,postal_code. The independent real backend tax changes above establish successful server recalculation.
- CheckoutSessionMappers exposes locally collected name/address as public Session.shippingAddress. Confirmation obtains shipping details and CheckoutSessionConfirmationInterceptor converts them to ConfirmCheckoutSessionParams.Shipping; its parameter map includes shipping.name and shipping.address. This is the reachable source path for submitted fields.
- **Exact final server-persisted shipping fields were not independently retrieved.** Checkout-client retrieve returns collection config/tax/totals but no shipping detail object (retained field_names show this). After completion it returns checkout_not_active_session; poll remains usable. Screenshots prove controller Session values; source proves serialization path; terminal poll proves required-shipping payment succeeded. These do not constitute a raw confirmation request capture or server shipping-object readback.

## Rejected leads and gaps

- No selected-card reset after shipping save: Visa4242 selected before the Canadian edit stayed selected and confirmed successfully. This specifically avoids foundation's prior failed-confirm/email-update sequence; email/lifecycle specialist owns that separate lead.
- Initial address typing followed immediately by Tab hid autocomplete results; refocusing an unchanged query showed no results until another keystroke. With focus retained and debounce allowed, suggestions and manual entry worked. No claim of an autocomplete outage; no candidate remained.
- Non-tax save does not immediately persist a server shipping object: source intentionally stores locally until confirmation and only performs tax updates when needed. Docs' controller Session update works. No unsupported server-persistence promise reported.
- Shipping absent was already a passed foundation control; not rerun here. Required shipping with missing address at confirm, invalid/disallowed-country direct merchant API, concurrent update/present/confirm, rotation/process death, multiple controllers, and default-address configuration/reconfiguration were not tested. Reopening within the same controller Session was tested.
- Merchant own-UI `updateShippingAddress(name,address)` is public but has no runtime playground control. Source inspected only, no custom UI or direct invocation instrumented. No claim this alternative passed device validation.
- Tax Session did not select/confirm a card; combined tax+selected-card refresh remains for the tax specialist. No tax address failure/network retry, cross-border taxation, saved customer/card interaction, postal invalidity matrix, wallet shipping, shipping options, checkout phone collection, or fulfillment/webhook readback.

## Cleanup and reuse

- Backed up full original shared_prefs/CheckoutElementsControllerPlaygroundSettings.xml privately before changing settings. Restored exact bytes with run-as and compared byte-for-byte; app force-stopped. No other app/device settings changed.
- Removed owned adb reverse18768, stopped owned relay (exec session66203), removed `/sdcard/checkout-shipping.xml`; no recorder was started. Removed private Session credentials, settings backup, and temporary screenshot from `/tmp/checkout-shipping` after verification. Parent keepalive remains untouched. Device released to parent.
- Installed build remains cleartext-enabled baseDebug as above. Physical Pixel is free. Both backend Sessions are legitimate synthetic test objects; first complete/paid, second open/unpaid.
- Helpers are local exploratory tooling, not SDK fixes. To reuse, privately back up preferences to `/tmp/checkout-shipping/original-settings.xml` first; `device.py settings SCENARIO` replaces only the settings_v1 map with explicit scenario values/source defaults. Start `backend_probe.py serve` and add matching reverse18768 before creating a Session; `probe LABEL` prints only selected sanitized fields and a Session hash. Creating another Session replaces the helper's latest private Session. Restore and remove owned mapping/private files after use. No Pillow dependency for shipping screenshots; helper refuses wallet screens/buttons.
