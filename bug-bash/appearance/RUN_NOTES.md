# Appearance — 2026-11-12

## Outcome and environment

No confirmed SDK defect or new documentation API mismatch. No ISSUE_appearance report, regression test, production edit, build, installation, staging, commit, push, remote filing, or external message. The existing docs API-drift report already owns the stale controller name; the appearance APIs exercised here exist in the current playground wiring.

SDK HEAD `8cbb695eabcafa957a86c24e1f025643979a500a`. Physical Pixel 6a `2B211JEGR05252`, API36. Reused installed baseDebug with `DIRECTLY_TO_PLAYGROUND=false` and `STRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true`. No emulator or instrumentation tasks. Parent wake/UNKNOWN keepalive remained untouched. Read bug-bash skill/Android reference, root instructions, docs brief/index/[appearance excerpts](../docs-driven/APPEARANCE_DOCS_EXCERPTS.md), adaptive-pricing/shipping/Express handoffs and six root report summaries. No test code; no test/build skill execution required. Native adb; no UI code modifications, browser, or rendered-web tooling.

All configuration is existing playground preference wiring in `CheckoutPlaygroundConfigurationFactory.kt`; full inputs are linked below. Guest US test merchant, fixed `adaptive+location_FR@example.com`, card/link allowed, wallets disabled, no saving/tax, Payment/Shipping/Currency configured, server shipping collection enabled. Loopback relay127.0.0.1:18775 adds `adaptive_pricing.enabled=true` to real backend creation requests; real responses unchanged. Three sessions created successfully. Test key/session prefixes asserted; credentials retained only in private temporary scratch and removed. No payment confirmation needed for this appearance pass; prior AP/shipping passes own payment proof.

## Actual coverage

| Configuration/action | Observed result/evidence |
| --- | --- |
| [Automatic palette + CURRENCY_CODE + OpenSans](automatic-code.json), system dark | Inline Card label uses configured text; Currency EUR/USD labels, magenta border, rounded corners, distinct selected cream/background blue, custom text/disclosure font render. [Initial](dark-code.png). Selector bounds [42,814][1038,941], 12dp vertical padding. |
| Inline Card opens Payment sheet | Dark navy surface/component, pale borders/text, orange primary/icon, normal layout. [Dark card](dark-card.png). |
| Open Payment sheet, system dark→light | Same sheet remains open with cream surface/mint components/green foreground and primary. [Light card](light-card.png). Android notification prompt appeared after recreation; Back dismissed it without granting/denying. |
| Incomplete card4242, move focus to CVC | Custom magenta error underline, number and message; numeric keyboard visible, form/action readable. [Error + keyboard](light-card-error-keyboard.png). |
| Dismiss then Select entry point | Opens same customized Add a card sheet with empty inputs; [Select sheet](light-select-sheet.png). This card-only configuration does not expose a separate multi-method chooser. |
| Shipping custom titles, system light | `Delivery address` and `Save delivery address` match configuration. Blank save produces custom error text/underlines. [Light shipping errors](light-shipping-error.png). |
| Shipping open, system light→dark | Open form and errors retained; dark configured surface/component and pink error. [Dark errors](dark-shipping-error.png). |
| Shipping Address123 Main Street→Enter address manually | Real autocomplete suggestions appear; manual full US form retains line1. No address submitted. |
| Shipping font_scale1.3 | Full manual form, labels, error and custom action fit portrait. [Large font](dark-shipping-large-font.png). |
| Same form, landscape | Scrolling reveals city/ZIP/state/error/custom action; no inaccessible bottom action. [Landscape scrolled](dark-shipping-landscape-scrolled.png). Restored rotation/font immediately afterward. |
| [Payment AlwaysLight, Shipping AlwaysDark, AMOUNT+Cursive](forced-amount.json), dark system | Currency displays EUR112.88/USD120.00 with actual bundled cursive.ttf font. [Amount labels](forced-amount.png). Payment sheet stays light despite system dark. [Forced light](forced-light-on-dark.png). |
| Amount EUR→USD | Selected background/checkmark/text colors swap, amount and disclosure update. [USD UI](amount-usd.png). [Independent backend retrieve](backend-amount-usd.json) presentmentUSD/integrationUSD,total12000, EUR alternative11288. |
| Same forced configuration, system light | Shipping stays dark. [Forced dark](forced-dark-on-light.png). An unrelated notification prompt recurred and was dismissed with Back. |
| [AUTOMATIC label + default font +4dp padding](automatic-label.json), light system | Amount labelsEUR112.88/USD120.00, bold selected label, same configured shape/colors. [Automatic](automatic-label.png). Bounds[42,814][1038,899]. |

All14 retained PNGs opened and visually inspected. No video; no personal wallet captures/actions. Compare configured colors and roles visually; no claim of exact screenshot RGB equality. A raw PNG pixel-count experiment found screenshot RGB values differ from requested ARGB values (e.g. dark surface screenshot26/36/52 vs requested23/36/53), and was discarded rather than interpreted as an SDK bug; color-space/display conversion was not independently investigated. Font/padding changed together between variants, so differing heights are not attributed solely to padding. Actual configured font resources exist (`opensans.ttf`, `cursive.ttf`).

## Rejected leads and limits

- Forced-light inline Payment content on the dark merchant page has dark text/icon against its host background. The same forced-light sheet is readable. This pass did not establish an SDK contract requiring the embedded container to paint its own surface; merchant-owned background integration is not reported as a verified SDK defect.
- Notification permission prompt and initial label-tap races after recreation were unrelated example/Android interruptions. Dismissed with Back, observed state again. A mistimed CODE→USD attempt had no matching node and did not execute; its screenshot/probe were discarded. Actual switch coverage is the successful AMOUNT→USD above.
- AUTOMATIC currently renders amounts, consistent with Stripe choosing the representation. It need not differ from AMOUNT.
- Existing [GitHub --state all search](github-duplicate-search.json) found older unrelated appearance/address issues8021/10792/9444. No confirmed new root to report. No additional compiler probe: current documented appearance methods used here are present; placeholder merchant font resources are not SDK defects.
- Not covered: exhaustive color/shape/property combinations, every payment method or saved method, full Payment typography options, Shipping successful save with these exact custom colors, payment confirmation, Currency error dangerColor, live config mutation, selected-card preservation under these exact styles, JPY with custom appearance, TalkBack, extreme font scales, rapid rotations, process death, standalone Shipping inline rendering (public entry tested is sheet). Express styling was already covered by prior pass. No discounts/tax-root/migration retest.

## Cleanup and reuse

[device.py](device.py) and [backend_probe.py](backend_probe.py) are reusable local helpers adapted from AP. Before reuse, privately back up complete original preferences into `/tmp/checkout-appearance/original-settings.xml`; start relay18775/add its reverse mapping, apply one config, launch main→CheckoutElementsController Example→Create Checkout Session. Never print backup or private backend credentials.

Exact original settings: night=yes,font_scale1.0,accelerometer_rotation0,user_rotation0. Restored all four, restored whole original app prefs byte-for-byte, force-stopped app, removed `/sdcard/checkout-appearance.xml`, stopped relay exec28058, removed owned reverse18775 (none remain), and removed private scratch. [Cleanup evidence](cleanup.json). No recorder or source patch. Original installed cleartext baseDebug unchanged. Physical device released to parent for next sequential topic.
