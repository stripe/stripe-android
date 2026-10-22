# Load completion to first content draw

PaymentSheet emits `mc_load_to_first_frame` once per presentation when its first
loaded screen is drawn. The event's `duration` is a floating-point number of
seconds. It includes the existing integration metadata, including
`payment_method_orientation`, so horizontal and vertical layouts can be compared
separately.

The interval starts on the loading worker immediately after `PaymentElementLoader.load`
returns a successful result. It ends after Compose's `drawContent()` returns for
the loaded screen's content column, including the primary button. It includes
any intervening dispatch delay, main-thread initialization, composition,
measurement, layout, and CPU drawing work. The loading spinner cannot end the
interval. Analytics is posted after drawing, using the duration already captured
at the endpoint.

This event measures the first loaded content draw for each PaymentSheet
presentation. It does not measure network loading, GPU completion, SurfaceFlinger
presentation, animation completion, or missed frame deadlines. A drawing callback
is not proof that pixels have reached the display. Use FrameTimeline in a release
system trace to investigate those additional stages and jank. The existing
navigation-based sheet-show events do not establish a draw boundary.

Failed loads and sheets dismissed before loaded content is drawn produce no
sample. Later draws, navigation, and Activity recreation with the same ViewModel
do not produce additional samples. A new presentation has its own timer. The
metric applies to PaymentSheet's complete flow; FlowController and embedded
payment elements have different loading and presentation lifecycles.

For comparisons, report distributions of this duration with the same release
build configuration, payment methods, layout, and CAPTCHA configuration. Separate
fresh-process presentations from later presentations, and record device, Android,
and WebView versions. Debug and Robolectric timings are correctness checks, not
release performance measurements. No performance optimizations are included in
this measurement change.
