# Saved payment methods — docs-driven bug bash

## Outcome

No confirmed SDK or documentation defect in the exercised subset. Optional consent, unconditional saving, default/explicit redisplay filters, saved-card payment, and enabled removal behaved as expected. No new ISSUE report or regression test was needed.

## Environment and contracts

- SDK HEAD `8cbb695eabcafa957a86c24e1f025643979a500a`; tracked source unchanged. Physical Pixel 6a `2B211JEGR05252`, API36. No emulator used.
- Reused installed baseDebug, `DIRECTLY_TO_PLAYGROUND=false`, `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No Gradle build or SDK test edits.
- Read root AGENTS.md, bug-bash skill and both Android/tax-and-saved-cards references, docs-driven brief/index, saved-method excerpts, Express/email handoffs, and all three existing root ISSUE reports.
- Contract: [source-verified excerpts](../docs-driven/SAVED_METHODS_DOCS_EXCERPTS.md), common setup.md lines174–242 at docs revision `4ed3418fb5a9b0af2cf2dcdca3aefb89b5b400cf`. Existing Customer association, optional save → always, off_session → limited, default excludes limited, explicit always+limited includes it.
- Real test backend US merchant. All payments $120 USD, card4242424242424242, expiry12/30, CVC123, US94103, synthetic email saved.methods.bugbash@example.com. No shipping, automatic tax, adaptive pricing, Express wallets, or Link. Same email does not identify the Customer; hashes in backend evidence establish distinct/fixed IDs.
- Loopback relay127.0.0.1:18770 forwarded create_customer, publishable_key, create_checkout_session (and allowed attach_payment_method, unused). Test-key/session prefixes asserted. Overrides change only creation request parameters; actual backend responses pass through unchanged. Creation version2026-08-26.preview. Independent checkout-client poll/retrieve version2020-03-02 includes SDK locale/mobile-session/app parameters. Retained probes omit keys, secrets, and raw Customer/PM IDs.
- GitHub issue search with `GH_HOST=github.com`, `--state all`, query `checkout saved payment methods` found only unrelated closed5780: [result](github-duplicate-search.json). Local reports checked; no duplicate candidate.

## Actual coverage

Base [optional-save.json](optional-save.json) explicitly chooses New Customer and optional saving. Playground changes New to Returning after creation; reapply the scenario and restart to create another independent Customer. [Creation parameters](creation-configurations.json) preserve checked/unconditional configurations without secrets.

| Case | Observed result/evidence |
| --- | --- |
| New Customer A, optional-save enabled, checkbox checked | [Checked form](optional-checked.png). Confirm succeeded; independent [poll](backend-optional-checked.json) Session cbdfcc0f1e7a complete/paid/succeeded. |
| Next Session for A, default redisplay | [Backend](backend-returned.json) returns one test PM with allow_redisplay always and matching Customer association. [UI](returned.png) renders/selects4242. |
| Confirm A's redisplayed saved card | No card re-entry needed. [Terminal poll](backend-saved-card-payment.json), Session8add44fc5cad complete/paid/succeeded. |
| Fresh Customer B, optional-save enabled, checkbox left unchecked | [Unchecked form](optional-unchecked.png). [Terminal poll](backend-unchecked-paid.json), Session71482598d191 paid. Subsequent Session for B with filters always/limited/unspecified returns zero methods: [backend](backend-unchecked-next-all-filters.json). This establishes non-redisplay through all supported categories; no secret-key PaymentMethod-list query was available to independently prove physical detachment. |
| Fresh Customer C, payment_settings.setup_future_usage=off_session, save choice disabled | [Form](unconditional-form.png) has future-charge disclosure and no save checkbox. [Terminal poll](backend-unconditional-paid.json), Session5709565351bf paid. |
| Next Session for C, no setup_future_usage and default redisplay filter | [Backend](backend-limited-default.json) has zero customer.payment_methods; [UI](limited-default.png) has no saved card. This is server filtering, not local SDK filtering. |
| Another C Session, allow_redisplay_filters=[always,limited], payment_method_remove enabled | [Backend](backend-limited-included.json) returns the same Customer with one matching test PM, allow_redisplay limited. [UI](limited-included.png) renders/selects4242. |
| Edit → Remove → Cancel | Removal confirmation appeared; Cancel returned to Manage card with Remove still available. Then repeated Remove and confirmed. No dedicated screenshot/backend snapshot of canceled state retained. |
| Confirm Remove | [UI](removed.png) clears selected card. Same Session [backend](backend-removed.json) returns zero methods; next Session with same always+limited filter also returns zero: [backend](backend-removed-next.json). The remove option is an exposed playground/server feature; the supplied saving docs excerpt does not itself promise a removal API. |

All seven retained screenshots were opened and visually inspected. No recording needed. Four actual payments have terminal backend proof, not only an SDK label. Completed Session retrieve returns checkout_not_active_session, expected; terminal poll remains available.

## Source checks and rejected leads

- CheckoutSessionResponseJsonParser reads payment_method_save/remove into save offer and Customer removal capability. CheckoutSessionConfirmationInterceptor passes the card's shouldSave only when saving is enabled. ConfirmCheckoutSessionParams serializes save_payment_method. Repository detachPaymentMethod posts payment_method_to_detach and reloads response.
- Missing limited card under the default filter was intentionally verified against raw **retrieve** customer.payment_methods (not a separately captured init payload). Explicit filter supplies the card and the SDK displays it. No inference of a local filtering bug.
- The first attempt to start the unchecked control reused A: the playground automatically persists a New Customer as Returning. Observed saved-card chooser exposed the setup mistake before entering/paying a card. Reapplied New scenario/restarted, then ran B independently. That extra open A Session is not counted as an unchecked test or SDK failure.
- No existing email-selection bug retested; fixed synthetic Customer email used throughout. No tax/card-address-edit scenario inferred from this run.
- Personal autofill/save prompts were dismissed with No thanks/Not now. No personal wallet card selected, captured, changed, or submitted.

## Gaps

- Did not exercise customer_creation=always with Customer generated at completion. Fresh Customers were explicitly created before Session creation; existing Customer IDs were then reused.
- Did not independently exercise per-card setup_future_usage alternative, optional-save alongside unconditional saving, other payment methods/cards, mandatory billing/tax/shipping, several saved methods, expired/declined saved cards, CVC recollection, rotation/process death, or remove-disabled UI behavior.
- Unchecked non-saving and removal were verified through checkout-client Customer lists, not secret-key global PM/customer APIs or webhook payloads. No fulfillment assertion.
- No focused SDK test added because no concrete failing candidate remained. Scripts are exploratory real-backend/device tools, not production or unit-test changes.

## Cleanup and reuse

- Original entire playground preferences restored byte-for-byte and app force-stopped: [cleanup](cleanup.json). No rotation/settings change made outside app preferences. Parent keepalive untouched.
- Removed owned reverse18770 and `/sdcard/checkout-saved-methods.xml`; stopped owned relay PID43614. Deleted private `/tmp/checkout-saved-methods` (keys, Session secrets, raw payloads, Customer IDs, settings backup) and generated __pycache__. No recorder or owned mapping remains.
- Physical device released. Installed cleartext-enabled baseDebug remains unchanged. Tracked git diff empty. No staging, commits, pushes, remote reports, messages, or fixes. Synthetic backend test objects remain.
- To reuse, privately create `/tmp/checkout-saved-methods` and back up original prefs there as original-settings.xml, then run backend_probe.py serve and reverse18770. device.py settings replaces scenario keys using source defaults. overrides.json in private scratch may overlay creation fields. next_session.py copies last creation and customer_id for backend-only follow-ups; it changes the helper's current Session secret, not the device's active Session. Every fresh New scenario must be reapplied because playground persists Returning.
