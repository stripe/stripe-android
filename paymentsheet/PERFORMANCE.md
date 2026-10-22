# First PaymentSheet presentation

Measure first presentation in a minified release application with a fresh process.
Keep network conditions and PaymentSheet configuration consistent between runs.
Debug builds include substantial cold class verification and unoptimized Compose
execution; instrumentation adds its own synchronization and mocking costs.
Debug frame durations do not predict release frame durations.

The `src/main/baseline-prof.txt` files in `paymentsheet`, `payments-ui-core`, and
`stripe-ui-core` contain manually curated rules for the first-presentation path.
They cover activity initialization, form composition, and field layout, including
nested classes and generated callbacks. These complement the profiles supplied by
Compose. They are packaged into the libraries so consuming release applications
can merge and compile them ahead of execution. They do not remove debug runtime
verification costs, and packaging alone does not establish a performance gain.
Installation and compilation matter: sideloading a release APK does not by itself
guarantee that ART has compiled its bundled profile before the first launch.

When changing the startup path:

1. Record a representative release workload, including cold presentation, card
   entry, address fields, and Link. Prefer generated profiles from that workload
   when updating coverage. Keep library rules scoped to the module that owns the
   classes and avoid whole-package rules.
2. Build `:paymentsheet:bundleReleaseAar`,
   `:payments-ui-core:bundleReleaseAar`, and `:stripe-ui-core:bundleReleaseAar`.
   Verify each AAR contains `baseline-prof.txt` and its class patterns still match
   the module's `classes.jar`.
3. Build `:paymentsheet-example:assembleBaseRelease`. Verify the release APK
   contains `assets/dexopt/baseline.prof` and `assets/dexopt/baseline.profm`.
4. On a dedicated test device, compare repeated fresh-process presentations with
   and without profile compilation. Confirm the actual package compilation state
   before launching; a successful compile command alone is insufficient. Collect
   FrameTimeline data or Macrobenchmark frame metrics and report distributions,
   rather than inferring a dropped-frame count from composition duration.

Use the repository's managed-device tasks for functional instrumentation checks.
Compose tests are useful for correctness but their test clock is not a substitute
for production frame timing.

Include sessions with passive CAPTCHA enabled when profiling presentation. The
warmer also creates a WebView: its first initialization is a separate cold path
from form composition and can still require native work on Main. CAPTCHA setup
waits for WebKit's asynchronous startup callback, which moves supported provider
work to a worker before constructing the view. Older WebView providers can still
have substantial UI-thread startup work; baseline profiles do not eliminate it.
