# Server-controlled compact embossed-card OCR experiment

This draft bundles the shipped SSD and the experimental compact SSD so Elements
Session can assign a model per session. The original model remains the default.
The compact model is promising on synthetic embossed cards but regresses both
existing printed fixtures; broad physical-card accuracy is still unestablished.

## Assignment and rollout contract

The server returns the following in the Elements Session response:

```json
{
  "flags": {
    "elements_mobile_allow_stripecardscan": true,
    "elements_mobile_cardscan_disable_ssdocr": false,
    "elements_mobile_cardscan_use_compact_ssdocr": true
  }
}
```

`elements_mobile_cardscan_use_compact_ssdocr` selects compact when true and original
when false or omitted. PaymentElement and CustomerSheet carry this assignment into
the scanner configuration across the Activity boundary. Direct SDK demo callers can
set `CardScanConfiguration(useCompactSsdOcr = true, elementsSessionId = null)`.
The example's existing default configuration selects original.

There is no client-side bucketing or model download. Each scan loads the selected
bundled artifact with its matching decoder dimensions. Managers are scoped to each
scan so a prior scan's cached fetcher cannot affect the next assignment. A load
failure does not silently switch SSD models. Existing ML Kit behavior is unchanged;
keep `elements_mobile_cardscan_use_mlkit` and the SSD-disable flag consistent across
arms, or segment by them.

Server work must register/emit the new flag and assign eligible sessions to control
or treatment. Choose rollout eligibility and allocation server-side. Returning false
or omitting the flag rolls new sessions back to original; an already configured scan
keeps its assignment. Older SDKs ignore this flag, so restrict analysis to SDK versions
that support it. This Android PR does not activate a production experiment.

Both artifacts ship during the experiment: **235,816 additional raw asset bytes**
compared with shipping original alone. APK download savings require retiring the
control model after evaluation. Actual compressed APK size is a separate metric.

## Measurement

The scanner emits `cardscan_presented` after Activity setup, before camera permission
and Google Play TFLite initialization. It identifies an attempt even if scanning
cannot start. Presentation, camera-ready, milestone and terminal events share the
same attempt ID. Repeated camera callbacks and duplicate terminal results are
ignored; late recognition cannot replace a canceled Activity result.

Every `cardscan_*` event from the scanner includes these fields:

| Parameter | Meaning |
|---|---|
| `scan_session_id` | UUID for one scan attempt; repeated attempts get distinct IDs |
| `ssd_model` | Configured assignment: `original` or `compact` |
| `ssd_model_version` | `1.1.1.16` or `embossed-fine-w8-2026-11-02` |
| `ssd_ocr_enabled` | Whether the configuration enables SSD OCR |
| `mlkit_enabled` | Whether ML Kit OCR is enabled alongside SSD |
| `elements_session_id` | Existing session identifier, when available |

`ssd_model` is assignment attribution, not evidence of successful model execution.
It is present on failure and cancellation events too, including permission denial
before a scan starts. No card number, expiry, name or image is added to analytics.

Join events by `scan_session_id`, group by model/version and matching OCR settings:

- Completion rate: attempts with `cardscan_success` / attempts with
  `cardscan_presented`.
- Failure and cancellation rates: corresponding terminal events joined to presented
  attempts; inspect `cancellation_reason`. Report the fraction that reach
  `cardscan_scan_started` separately to distinguish permission/runtime failures
  from recognition failures.
- Time to completion: p50/p90 of `cardscan_success.duration` (seconds), alongside
  completion rate so slow or unsuccessful scans are not hidden by survivor bias.
- Time to first SSD PAN: `cardscan_darknite_found_pan.duration`, retaining the
  existing event name for both SSD variants. Also compare the fraction of started
  attempts that reach this milestone. This measures time to a usable SSD PAN,
  including acquisition/preprocessing; it is not individual inference latency.
  Timing starts when the camera stream becomes available, excluding permission UI.
  Camera stream restarts during an active attempt preserve its ID and start time.
- When ML Kit is enabled: compare its milestones and `cardscan_models_disagree`
  rates to understand cases where the combined scanner masks SSD differences.

SDK/OS/device mix should be checked between arms. Analytics ingestion must retain
these new parameters and a dashboard/query must be configured separately. Success
means the scanner accepted a PAN; it does not establish that the digits are correct.
False accepts and embossed/printed subgroup accuracy require labeled evaluation or
separate correction signals. This experiment cannot infer card style from telemetry.

## Artifact and runtime contract

| Property | Original | Compact Fine |
|---|---:|---:|
| File bytes | 1,659,924 | 235,816 (85.79% smaller) |
| Parameters | 808,650 | 182,765 |
| Calculated convolution MACs | 447,749,064 | 307,330,248 |
| Anchors | 3,420 | 2,736 |
| Class output | `[1,37620]` | `[1,30096]` |
| Box output | `[1,13680]` | `[1,10944]` |

SHA-256: `a87452ad7ce36c7e65b65c248927476e6fbc8261e886544f0c11cff443282fa9`.
The input remains RGB FLOAT32 `[1,375,600,3]`, normalized as
`(pixel - 127.5) / 128.5`. The original 38-by-24 fine-grid anchor geometry,
class ordering and flattened output ordering are preserved. The 19-by-12 coarse
grid is removed. The decoder uses stable softmax by subtracting the maximum logit.
PAN validation and multi-frame scan decisions are unchanged.

Weights use per-output-channel INT8 storage and FLOAT32 dequantization/execution;
biases use FP16 storage. This is not fully integer inference. The calculated MAC
reduction comes from the architecture change; no Android speed, RAM or power
improvement has been established. Intermediate tensor names were stripped after
training; outputs matched the named export bitwise on all 32 development images.

## Training

The pretrained fine-grid backbone was retained, and the classification and box
heads were trained on 512 locally rendered fictional designs for 600 AdamW steps
(batch 16, learning rate 0.0003). Rendering used digit height fields with beveled
normals, directional lighting, specular highlights, approximate shadows, tipping,
texture, mild perspective and blur. This is a procedural relief approximation,
not a photographic card/camera simulation.

Supervision used generated digit labels and boxes, hard-negative mining at 3:1,
Smooth L1 box regression, and soft-label distillation from the original export
(temperature 2, weight 0.05). Anchors with IoU at least 0.35 were positive, with a
forced unique match for every digit; IoU between 0.15 and 0.35 was ignored. Weights
were quantized after head training. Training fonts were IBM Plex Mono Medium,
Courier New and Menlo. All generated PANs were 16 digits, began with 4, and occupied
one horizontal line. The model still relies on pretrained visual features.

## Evaluation

Development (32 designs) selected the candidate before final evaluation. The final
sets contain 256 new designs from the same renderer and 96 designs using held-out
font families/weights. PAN identities and design seeds were disjoint. The font set
includes related variants (Courier New Bold and IBM Plex Mono Light), plus American
Typewriter, so it is not exclusively unseen font families.

| Model | Generated test exact PAN | Font stress exact PAN | Existing printed fixtures |
|---|---:|---:|---:|
| Original | 113/256 | 32/96 | 2/2 |
| Compact Fine FP16 reference | 250/256 | 74/96 | 1/2 |
| Compact Fine INT8 (this PR) | 252/256 | 74/96 | 0/2 |
| 62,784-byte Nano experiment | 146/256 | 30/96 | 1/2 |

The generated test includes 227 embossed cases and 29 printed controls. On the
embossed subset, this candidate gets 223/227 versus the original's 102/227.
The four generated-test failures were two digit substitutions, one insertion and
one deletion. Font stress exposes limited coverage: American Typewriter 8/25,
Courier New Bold 35/37, and IBM Plex Mono Light 31/34.

These are single-image Python/TFLite decoder results, not end-to-end camera
acceptance rates. On 37 limited negative probes the candidate returned no digits
on 36 and one stray digit on one erased card. No wrong output passed the restricted
16-digit, first-digit-4 Luhn proxy. This does not establish a production false-accept
rate. Synthetic improvement does not prove physical-card accuracy.

Known regressions: the ordinary printed fixture returns an extra final `2` under
INT8, while the FP16 reference reads it correctly. Both compact Fine versions return
only `42422` from the stacked fixture. The existing `SSDOcrTest` assertions have not
been weakened or ignored. Broader physical-card, layout, font and PAN-length
validation is required before replacing the shipped model.

## Android validation and reproduction

```sh
./gradlew :stripecardscan-example:assembleDebug :stripecardscan:assembleDebugAndroidTest
./gradlew :stripecardscan:testDebugUnitTest --tests '*CompactModelDecoderTest'
```

The A/B wiring and attempt-lifecycle hardening pass all 110 scanner unit tests, 225 parser/metadata tests,
17 launcher/action tests and one Compose assignment-recomposition test. Both APKs
build and include the expected hashes for both artifacts. Repository-wide
`detekt apiCheck` passes without API baseline changes.

The managed `pixel2api33DebugAndroidTest` run attempted both `SSDOcrTest` and
`EmbossedModelDeviceTest`. All four tests failed before inference because its AOSP
ATD image lacks the Google Play TFLite module: `No acceptable module
com.google.android.gms.tflite_dynamite found` (local/remote version 0). This run
provides no recognition-accuracy evidence for either arm.

Bitrise ran all 16 scanner instrumentation tests successfully on its Google
Play-enabled Android 13 emulator for commit `9c5cd4c9ced`, covering both original
and compact model fixtures. This closes the runtime coverage gap for the model
selection wiring; the managed AOSP limitation above still applies locally.

Before the A/B wiring, both decoder unit tests passed. On a Pixel 10 Pro XL running Android 17 / API 37,
`EmbossedModelDeviceTest` loaded this artifact through the Google Play TFLite runtime
and decoded the included fictional embossed card correctly three times using the
actual SDK preprocessing and decoder. This is a runtime/integration smoke test,
not a physical-card benchmark. The synthetic asset is 600-by-375 with expected PAN
`4372265397654881` (renderer test seed 1).

For an explicitly selected connected device, install the Android-test APK and run:

```sh
adb -s SERIAL install -r stripecardscan/build/outputs/apk/androidTest/debug/stripecardscan-debug-androidTest.apk
adb -s SERIAL shell am instrument -w -r -e class com.stripe.android.stripecardscan.payment.ml.EmbossedModelDeviceTest com.stripe.android.stripecardscan.test/androidx.test.runner.AndroidJUnitRunner
adb -s SERIAL install -r stripecardscan-example/build/outputs/apk/debug/stripecardscan-example-debug.apk
```

Repository policy uses the managed-device task for routine instrumentation runs;
connected-device commands above are for deliberate physical-phone evaluation.
