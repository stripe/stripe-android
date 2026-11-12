# Checkout Sessions bug bash — 2026-11-11

Two new issues were confirmed against `cf7dd4b99965a4e754abdadacf4f337e764f6e6a` and reproduced on a physical Pixel 6a running Android 16/API 36.

| Finding | Report | Evidence and reproduction |
| --- | --- | --- |
| SEPA offers a save checkbox when PMO SFU `none` prevents saving | [Report](../../ISSUE_confirm_sepa_none_offers_ineffective_save_checkbox.md) | [Native screenshots, video, backend controls, and regression](../confirm/RUN_NOTES.md) |
| Saved Amex exposes an accessibility label saying `Unknown` | [Report](../../ISSUE_lpms_saved_amex_accessibility_unknown_brand.md) | [Native screenshot/XML and focused regression](../lpms/RUN_NOTES.md) |

The seven topics were tested sequentially: customer logic, adaptive pricing, automatic tax, email collection, LPMs, order summary, and confirmation/SFU. The native follow-up completed 14 test payments, including a Bancontact authorization and return to the app. [Full coverage and remaining gaps](RUN_NOTES.md).

Reproduction scripts and evidence-only Kotlin tests/patches are included alongside each topic's notes. Follow the setup and commands there. The regressions for the two defects are expected to fail on the recorded source revision; no SDK fix is included. Live probes create synthetic test-mode backend objects.

This sharing snapshot includes the two new reports and their supporting seven-topic investigation. Earlier local billing/shipping investigations and the unrelated `AGENTS.md` edit are excluded. References to that earlier work are historical context, not additional findings. References in run notes to files being local or uncommitted describe the state before this publication step.

Credentials and the physical device serial are redacted; home-directory paths are shown relative to `~`. The recorded checkout behavior and assertions are unchanged.
