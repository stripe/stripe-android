# Tax — docs-driven bug bash, 2026-11-12

## Outcome

One confirmed SDK root: [saved-card editor omits automatic-tax fields](../../ISSUE_tax_saved_card_edit_omits_tax_fields.md). Automatic billing's new-card form collects US tax fields, but the saved-card editor does not; France-only→US94103 succeeds then becomes locally unselectable. A focused factory regression failed as intended and its Full control passed. Existing [Android API-drift report](../../ISSUE_docs_foundation_android_guide_does_not_compile.md) extended with the tax guide's repeated totalSummary/taxAmounts mismatch; no duplicate docs report.

## Environment and contracts

- HEAD8cbb695eabcafa957a86c24e1f025643979a500a. Physical Pixel6a2B211JEGR05252/API36, native adb only. Initial tracked diff empty. No emulator, rebuild/install, commit/staging/push, fixes, remote reports or messages.
- Installed baseDebug retained: DIRECTLY_TO_PLAYGROUND=false, STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true. Parent owns wake/UNKNOWN keepalive.
- Read AGENTS.md, bug-bash skill, both Android/tax-saved-card references, docs brief/index/backend routes/[tax excerpts](../docs-driven/TAX_DOCS_EXCERPTS.md), shipping/saved-methods handoffs and existing ISSUE reports. Used write-unit-tests and delegate-low-reasoning-work for focused validation; Compose skill read, no Compose test added.
- Docs collect-taxes Android source revisionffad89daddb431f7a5340bcbea2dbd38234db7c5: automatic tax, tax code/behavior, billing auto minimum vs required full, current Session totals. Shipping tax was already covered and its SF/LA matrix was not repeated here.
- Real backend https://stp-mobile-playground-backend-v7.stripedemos.com; automatic tax chooses Merchant.US_TAX/us_tax in current example source. Test-only prefixes/livemode asserted. California billing yielded ready and positive tax; merchant registration Dashboard/global tax-settings APIs were not independently audited. No legal tax-rate correctness assertion.
- [auto.json](auto.json) uses New Customer, card only, no wallets/shipping, synthetic fixed tax.bugbash@example.com, automatic tax enabled, optional save. Relay127.0.0.1:18772 additionally supplies billing_address_collection=auto and customer_update.address=auto. This forces backend address_source=billing instead of relying on Customer.address. Original Customer created without address; first payment updates Customer address. Subsequent Session responses still explicitly report billing, and selecting FR-only/US-only PM changes tax; no inference that fixed Customer address controls tax.
- Default cart:2x3500 T-shirts +1x5000 hoodie, USD12000, general tax_code txcd_99999999 on both, tax_behavior exclusive. Final control changes both to inclusive and billing collection to required: [overrides](required-inclusive-overrides.json).
- Backend probe stores raw payloads/keys/Customer IDs privately; retained evidence hashes IDs and allowlists fields. Creation Stripe-Version2026-08-26.preview, independent checkout-client retrieval/poll2020-03-02 with locale/mobile params. Actual responses pass through relay unchanged. No raw device HTTP interception claimed.

## Actual coverage

| Case | Result |
| --- | --- |
| Auto billing, fresh Customer, missing location | [Backend](backend-initial.json): billing/requires_address,total12000. US form displays line1/city/state/ZIP; valid card alone leaves Continue disabled. [Screenshot](auto-missing-address.png). |
| Full US new-card address under auto |510 Townsend Street,San Francisco,CA94103,US. [Form](auto-full-address.png), save checked. [Backend](backend-auto-selected.json) ready,tax_exclusive1035,total13035; [UI](auto-tax-selected.png) matches. |
| Confirm taxed billing payment | Same Session3da40b3b7de4 [paid/complete/succeeded](backend-auto-paid.json), [UI](auto-paid.png). |
| Return saved full-US card | [Backend](backend-returned.json) same Customer hash80a14edd2885, PM8cffc4c453da, all US address fields, always redisplay. Saved-card chooser offered it. |
| Edit saved US ZIP94103→90012 | [Editor](saved-auto-zip-edit.png) offers only country/ZIP. [Backend](backend-saved-zip-edit.json) persists90012 while retaining prior SF street/city/state. No disappearance established in this control; stopped before confirmation. This inconsistent address is not treated as a tax-rate defect. |
| Provision independent FR-only card | [Helper](provision_fr.py): publishable test-key PM create with tok_visa and countryFR; test backend attach to same Customer, HTTP200. No full address inherited. Explicit redisplay filters always/limited/unspecified. [Backend](backend-fr-returned.json) returns only newest FR-only PM e0903022833a; app offers it. Prior same-card fixture absent in response, so no local filtering allegation about that older PM. |
| Saved FR-only→US94103 | Save accepted with missing US street/city/state; backend [persisted](backend-fr-to-us-saved.json). DONE→select Visa closes sheet; tax updates to1035 but selection disappears. Confirm fails; reopen shows Add a card. [Final backend](backend-fr-to-us-confirm.json) retains PM and stays open/unpaid/not_started. Report links exact screenshots. |
| Required billing with incomplete saved PM | New Session8da02d6b81bd [backend](backend-required-inclusive-initial.json) omits PM entirely and requires address. This is backend filtering, unlike previous auto Session's locally hidden PM. |
| Required new card, inclusive prices | Full address form uses autocomplete; chose510 Townsend St/SF suggestion, which populates city,state,ZIP and optional line2. [Full form](required-full-address.png). |
| Inconsistent ZIP00000 with valid SF street/city/state | Continue accepted; backend tax ready/inclusive953,total12000. [Probe](backend-required-invalid-zip.json). This did not trigger an invalid-location error; other full fields remained valid, so no SDK validation/tax defect inferred. |
| Correct same new card ZIP back to94103 | Reopen retains card/full address. Continue preserves usable selection; [backend](backend-required-valid.json) inclusive953,total12000, [UI](required-inclusive-selected.png) shows “Sales Tax (included)”$9.53,total$120.00. |
| Confirm inclusive control | Same Session [paid/complete/succeeded](backend-required-paid.json), [UI](required-inclusive-paid.png). Save choice unchecked. |

All12 retained PNGs were opened and visually inspected. No video. Personal autofill/save prompts dismissed with No thanks/Not now, never captured or selected. Screenshot helper refused wallet prompts during two attempted captures; those captures were retried only after dismissal.

## Focused validation and reachable cause

[Regression patch](saved-card-tax-fields-regression.patch) exercises real DefaultEmbeddedUpdateScreenInteractorFactory→DefaultUpdatePaymentMethodInteractor→BillingDetailsForm with tax-enabled BILLING metadata, changes countryFR→US and checks required fields are not hidden. Automatic fails, Full passes. Parent executed exact Gradle command because low-cost executor delegation was unusable in this session. [Result](regression-results.txt), [XML](regression-results.xml):2tests/1expected failure/0errors/skips,exit1,35s. No production edits or test left installed in source; original file restored exactly. This test intentionally isolates editor field contract; device+backend evidence establishes downstream filtering/confirmation impact.

Factory only forwards generic address mode. BillingDetailsForm calls cardBillingAddressCollectionMode with requiresBillingAddressForAutomaticTax=false. DefaultPaymentMethodFilter applies hasSufficientBillingDetailsForAutomaticTax on reload; US requires line1/city/state/ZIP. Backend accepted country/ZIP for tax computation but the SDK discards that PM because its own stronger field requirement cannot be met through the editor. No rate or backend-removal claim.

GitHub --state all duplicate searches: [checkout tax billing](github-duplicate-search.json), [saved card tax](github-saved-tax-duplicate-search.json), both empty. Existing root ISSUE reports contain no tax editor root. Skill historical report path absent; this reproduction is independently observed at current revision.

## Limits / rejected leads

- No legal-registration audit or varying product-category/exemption matrix; verified explicit general code and exclusive/inclusive behavior against real backend amounts. Amount evidence is before-confirm retrieval plus same-Session terminal poll, not charge/PaymentIntent amount retrieval or webhook fulfillment.
- No fresh direct init capture; backend distinction uses independent checkout-client retrieve immediately around actions. No outgoing update request packet capture; UI+persisted PaymentMethod fields and source mapping establish updates.
- No invalid-location backend-error retry;00000 with other valid fields remained tax-ready. Missing fields validated by disabled Continue and the saved-card root above.
- No additional billing/shipping precedence matrix (shipping source established by prior topic), customer-address source variant, saved full-address edit under required on device, cancellations/network outage, race/rotation/process death, multiple controllers, alternative methods, other tax countries or wallets. Required saved-editor field visibility covered only by focused unit control.
- FR zero tax is not reported as wrong: merchant's tax coverage differs by location. Missing older same-fingerprint PM is backend response behavior; not an SDK omission.
- Country menu pins US at the top, not alphabetically near UK. Initial bounded forward scroll overshot; reversed to top. This cost time but is not a product bug. Helper remains exploratory, no production change.
- Tax IDs/checkout phone/shipping options excluded by preview docs; discounts unavailable prerequisite issue not retried. No broad unrelated behavior retested.

## Cleanup

Original whole playground preferences restored byte-for-byte and app force-stopped. Owned relay (exec30107) stopped; reverse18772 and /sdcard/checkout-tax.xml removed. Private /tmp/checkout-tax (keys, Session secrets, Customer IDs, raw responses, preferences backup) and __pycache__ removed. [Cleanup](cleanup.json). Tracked source/test diff empty; parent keepalive untouched. Device released. Installed cleartext-enabled baseDebug remains unchanged. Synthetic backend test objects remain.

Reuse: privately back up original preferences to /tmp/checkout-tax/original-settings.xml; start backend_probe.py serve and reverse18772; load auto.json through device.py settings, with auto-overrides.json copied privately to overrides.json. After first Customer/Session, provision_fr.py attaches test FR card; return and create next Session. The playground persists New Customer as Returning, so do not reapply New unless intentionally starting a fresh Customer. No credentials in retained helpers. Restore original preferences and remove mapping/scratch afterwards.
