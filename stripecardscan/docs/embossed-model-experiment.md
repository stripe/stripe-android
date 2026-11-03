# Experimental compact embossed-card OCR

This draft replaces the card-number SSD with an experimental 235,816-byte model
and adapts its decoder to a single feature grid. The example app installs separately
as **Embossed Card Scan**, opens the scan demo directly, and defaults to SSD only.
Leave both checkboxes unchecked and tap **Scan card** to test this model.

**Not ready to merge:** both existing printed-fixture accuracy cases regress.
Their assertions have deliberately been retained. Initial physical-card testing
was encouraging, but it is not a representative accuracy benchmark.

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

Both decoder unit tests passed. On a Pixel 10 Pro XL running Android 17 / API 37,
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
