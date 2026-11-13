# Discounts — docs-driven bug bash

## Outcome

Discounted control blocked by deployed backend prerequisites. No SDK behavioral defect confirmed. One documentation availability report, plus additional compiler evidence in the existing Android API-drift report. Do not count provisioned fixtures as exercised discount behavior.

## Environment

- SDK HEAD8cbb695eabcafa957a86c24e1f025643979a500a; tracked source unchanged. Pixel6a2B211JEGR05252/API36 exclusively; no emulator.
- Reused installed baseDebug with DIRECTLY_TO_PLAYGROUND=false and STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true. No build/install/Gradle needed.
- Read AGENTS, bug-bash/Android reference, docs-driven brief/index/discount excerpts, all three prior reports, Express and saved-method setup notes. Applied write-unit-tests skill for focused contract probes; build delegation skill read, no Gradle commands required.
- Backend US merchant, version2026-08-26.preview. Test-mode key/session prefixes checked. Source-verified provisioning routes in [BACKEND_PROXY_ROUTES](../docs-driven/BACKEND_PROXY_ROUTES.md). No custom keys or gates changed.
- Loopback18771 relay forwarded real responses unchanged and added creation overrides. Secrets/raw retrieval kept only privately in /tmp and deleted during cleanup.

## Actual coverage

Base [configuration](base.json): guest, synthetic discounts.bugbash@example.com, USD120 cart (two $35 shirts and $50 hoodie), card only, no shipping/tax/saving/wallets. [Creation request](creation-allow-promotion.json) is actual sanitized request, ui_mode=mobile_elements, items/one_time_price, no mode.

| Case | Actual result |
| --- | --- |
| Provision fresh coupons and promotion codes | Successfully created25%, USD1000 fixed,10% restricted to USD200 minimum,100% fixtures. [Objects](fixtures.json), [reproducer](provision.py). Only25% was attempted at runtime. |
| Create Session with documented allow_promotion_codes=true | HTTP400: cannot pass allow_promotion_codes in modeless mode. [Inspected screenshot](allow-promotion-error.png). |
| Remove only flag and retry on device | Initializes real Session with subtotal/total12000. |
| Apply fresh25% code lowercase via public UI | Error unknown parameter promotion_code. [Inspected screenshot](apply-unknown-parameter.png). Independent [backend](backend-percent.json) open/unpaid/not_started, total12000. Case-insensitivity cannot be assessed because parameter validation fails first. |
| Create with server discounts:[{coupon:fresh25%}] | HTTP400: cannot pass discounts in modeless mode. [Inspected screenshot](server-coupon-error.png). |
| Minimal backend-only identical-request comparisons | [Probe](backend_contract_probe.py) passed assertions: plain creation200, flag-only400, coupon-only400. Uppercase valid apply and empty remove both400 parameter_unknown/promotion_code. [Results](backend-contract-results.json). No payment submitted. |
| Android docs API contract | [Compiler probe](compile_docs_probe.py) assertion pass. Actual API exit0; isolated documented onSuccess/totalSummary accesses each expected exit1. [Output](docs-compiler-results.txt). Uses cached Kotlin compiler and built SDK, no SDK source modification. |

All three screenshots opened and visually inspected; no external wallet UI or personal data captured. No recording. After failed coupon creation, an exploratory retrieval initially sampled the prior successful Session; its misleading server-percent filename was deleted and it is not evidence of discounted creation.

## Findings and rejected leads

- New [documentation availability report](../../ISSUE_discounts_guide_omits_backend_availability_prerequisites.md): promised creation/apply paths unavailable on this test merchant and current protocol. Backend prerequisites/incomplete rollout, not a newly established SDK regression. [Verified backend schema/gates](BACKEND_CONTRACT_EVIDENCE.md).
- Extended [existing Android API-drift report](../../ISSUE_docs_foundation_android_guide_does_not_compile.md), no duplicate report. Docs reference totalSummary and onSuccess; actual Session has totals and separate discountAmounts, operations return OperationResult.
- Raw codegen checkout_items/one_time_price_item differs from actual working creation items/one_time_price. Rendered codegen transformation not verified, therefore no report about raw argument spelling.
- Codes were freshly created and accepted by provisioning backend. Unknown-parameter response occurs before coupon validity/restrictions, so no invalid-code/case-sensitive/expired-code bug inferred.
- GitHub issue search with GH_HOST=github.com, --state all, checkout promotion discount returned [none](github-duplicate-search.json). Local reports rechecked for distinct root causes.

## Gaps

No successful discounted Session or payment: fixed/percentage arithmetic, rounding, replace/remove/reapply, invalid-code recovery preserving existing discount, selected-card preservation, minimum/expiry/customer/product/max-redemption restrictions, zero/below-minimum total, and correct discounted confirmation remain blocked and untested. Do not infer that these pass from coupon provisioning or parser/source support. No prerequisite bypass attempted.

## Cleanup and handoff

[Cleanup](cleanup.json): restored entire original prefs byte-for-byte, force-stopped app, removed reverse18771 and /sdcard/checkout-discounts.xml, stopped owned relay, deleted /tmp/checkout-discounts including keys/secrets/raw payload/settings backup and local __pycache__. No recorder. Parent keepalive untouched. Physical device released. Installed cleartext baseDebug flags unchanged. No tracked tests/fixes, patches, commits, staging, push, remote issue filing, or external messaging.

Reuse helpers after privately recreating /tmp/checkout-discounts and backing up prefs as original-settings.xml. Run provision.py to create new supported fixtures; backend_contract_probe.py relies on private fixtures.json. Start backend_probe.py serve and reverse18771 for UI, settings base.json then launch. Current backend does not support required discount flow on this merchant; enablement must be resolved outside this run before behavioral testing.
