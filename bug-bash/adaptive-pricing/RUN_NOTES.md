# Adaptive Pricing / Currency Selector — 2026-11-12

## Outcome

No confirmed SDK defect. Extended the existing [Android docs API-drift report](../../ISSUE_docs_foundation_android_guide_does_not_compile.md) with the Adaptive Pricing guide's repeated CheckoutController/Result<Unit> setup. No new ISSUE report, production edit, test edit, build, commit, staging, push, remote report or external message.

EUR checkout completed with test Visa after selecting the card in USD and switching back to EUR. Independent pre-confirm retrieve records presentment_currency=eur,total11288; the same Session hash f10518fddaba subsequently polled succeeded/complete/paid. The server creation object remains USD12000 as documented. No final Charge/PaymentIntent or webhook retrieval was available; this evidence does not independently inspect the charge's presentment_details.

## Environment and setup

- SDK HEAD8cbb695eabcafa957a86c24e1f025643979a500a; tracked source initially and finally unchanged. Physical Pixel6a2B211JEGR05252/API36; no emulator. Parent owns wake/UNKNOWN keepalive; this agent alone owned meaningful UI.
- Existing installed baseDebug, DIRECTLY_TO_PLAYGROUND=false and STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true, retained without rebuilding/installing.
- Read root AGENTS.md, bug-bash skill and Android reference, docs brief/index/backend routes, [AP excerpts](../docs-driven/ADAPTIVE_PRICING_DOCS_EXCERPTS.md), Express/tax handoffs and five existing root ISSUE reports. No test skills needed because no test code was written or run.
- Guide source partials/adaptive-pricing.md at fe9c832f73564dbefefdcc0ec393772bf73d2f0b. Parent supplied verified common/Android source excerpts, including +location_XX testing and server/client currency distinction. No browser used.
- Backend https://stp-mobile-playground-backend-v7.stripedemos.com, US test merchant, creation2026-08-26.preview. Loopback relay18773 adds adaptive_pricing.enabled explicitly to the normal playground request; all real response bodies forwarded unchanged. pk_test_/cs_test_ assertions; raw credentials/private responses only in temporary scratch, deleted at cleanup.
- Guest, no saving/tax/shipping, USD120 cart (2x3500 shirt+5000 hoodie). Payment Element configured, both embedded wallets Never, Express unconfigured, Currency Selector configured and rendered by example's Session availability guard. LabelContent.AMOUNT. [FR config](fr.json) sets fixed adaptive+location_FR@example.com and card/link server types. No device locale/country change.
- Probe independently calls checkout_client retrieve/poll with mobile/locale parameters and Stripe-Version2020-03-02. Sanitized evidence includes Session hashes, creation currency, presentment/integration currency, totals, item amounts and backend ordered PM types. Initial probe predates expanded allowlist; later same-Session probes supply full presentment evidence.

## Actual coverage

| Configuration/action | Result/evidence |
| --- | --- |
| Enabled FR localized control | EUR112.88, shirt65.85+hoodie47.03; caption1USD=0.9406EUR. [UI](fr-initial.png), [initial probe](backend-initial.json). |
| FR EUR→USD | USD120, shirt70+hoodie50; bank fees caption. [Backend](backend-usd.json) presentment USD, integration USD; EUR alternative preserved. |
| Select new Visa in USD | Test4242424242424242,12/30,CVC123,US94103. Selected card in [USD UI](usd-card-selected.png). No save or personal-wallet action. |
| Selected USD card→EUR | Selection retained, all amounts EUR. [UI](eur-card-selected.png), [backend](backend-eur-card.json) presentment EUR11288, integration USD, card still available. |
| Confirm EUR | [SDK completed](eur-paid.png), [same-Session terminal backend](backend-eur-paid.json) succeeded/complete/paid/livemodefalse. Retrieve after completion returns expected checkout_not_active_session; poll remains available. |
| [Japan, automatic PMs](jp.json) | JPY19952, shirt11639+hoodie8313, no fractional yen; caption1USD=166.2666JPY. [UI](jp-initial.png), [backend](backend-jp.json). |
| JPY→USD→JPY | [USD backend](backend-jp-usd.json)12000; [return](backend-jp-back.json)19952. Available types expand in USD and contract in JPY. First pass checked backend around actual native toggles; second fresh JP Session captured visible payment methods below. |
| JP second Session, USD PM expansion | [UI](jp-usd-methods.png) shows Card,Amazon Pay,Alipay,US bank account,Cash App Pay,WeChat Pay,Crypto matching [backend](backend-jp-usd-methods.json) minus intentionally disabled wallets. No unsupported PM selected/confirmed. |
| Same JP Session→JPY PM contraction | [UI](jp-return-methods.png) only Card; [backend](backend-jp-return-methods.json) card/apple_pay/google_pay/link. Disabled wallets remain absent. Both localized cart amounts restored. |
| [US same-currency](us-ineligible.json), server AP enabled | Selector/disclosure absent, totalUSD120; no reserved selector gap. [UI](us-ineligible.png), [backend](backend-us-ineligible.json) adaptive_pricing_info=null and presentmentUSD. Automatic-method native list includes US-supported methods. |
| FR, server AP explicitly disabled | Same fr.json with relay disabled marker. Selector/disclosure absent and cartUSD120; no reserved selector gap. [UI](fr-disabled.png), [backend](backend-fr-disabled.json) creation.enabledfalse, adaptive_pricing_info=null. |

All9 retained PNGs opened and visually inspected. No video. No personal wallet content captured or wallet invoked. No autofill modal arose during synthetic card input. Native helpers inspect fresh hierarchy for each labeled tap. Initial Pillow-based capture helper lacked its old venv; switched to raw native screenshots guarded against external GMS/autofill, with wallets explicitly disabled. No failed capture retained.

## Rejected leads and coverage limits

- Missing expanded exchange disclosure is expected for these responses: backend supplies caption only, no details. Tapping FR caption has no effect; CurrencySelectorToggle.CurrencyDisclosure adds clickability/chevron only if exchangeRateDetails is nonnull. No expanded-content variant was tested or manufactured.
- Server USD versus client EUR is the documented integration/presentment distinction. No currency mismatch report.
- Backend unit amount rounding can yield3292x2 versus line subtotal6585 in EUR. App uses backend line subtotal65.85 and total112.88 consistently. No client arithmetic defect; no independent FX-rate correctness claim.
- Automatic-method changes agree with backend availability and configured wallets. No claim that disappearing unsupported payment methods are bugs. Card preservation verified for USD/EUR; selected non-card invalidation was not exercised.
- Guide's stale controller/Result<Unit> repeats existing compiler-tested root. Current Session.totals.total.amount snippet is valid. No redundant compiler run. [GitHub --state all adaptive pricing search](github-duplicate-search.json) empty; rechecked local reports.
- Not tested: client-unconfigured selector, raw unconditional unavailable Content sizing, additional currencies/three-decimal currencies, alternate settlement merchant, Dashboard enablement, full eligibility restrictions, refunds, JPY payment, localized tax/shipping/discounts, new email geographic changes, saved PMs, network failure/retry, rapid overlapping toggles, rotation/process death, accessibility/focus matrix, expanded disclosure with actual backend details. Earlier agents own tax/shipping and discounts prerequisite gaps; no repeat of them here.
- Payment proof is pre-confirm presentment retrieval plus same-Session paid poll and SDK completion, not final Charge/PI presentment field or webhook fulfillment inspection.

## Reuse and cleanup

`device.py` and `backend_probe.py` adapted from Express; no dependency on its scratch/venv. Privately back up original preferences to /tmp/checkout-adaptive/original-settings.xml before settings replacement. Start relay18773, add owned adb reverse, apply fr.json/jp.json/us-ineligible.json, launch main menu→CheckoutElementsController Example→Create Checkout Session. A private /tmp/checkout-adaptive/disabled marker sets server adaptive_pricing.enabled=false for the next Session; absence enables it. Every creation replaces latest private Session credentials. Do not print those credentials. Probe outputs sanitized values only.

Original whole prefs restored and byte-for-byte verified; app force-stopped. Relay exec26195 stopped, owned reverse18773 removed, /sdcard/checkout-adaptive.xml removed, private /tmp/checkout-adaptive removed. No recording or owned mapping remains. [Cleanup evidence](cleanup.json). Synthetic backend Sessions/payment remain as legitimate test objects. Parent keepalive untouched. Device released; installed cleartext-enabled baseDebug unchanged.
