# Configuring beta API headers

The public-preview configuration applies to PaymentSheet, FlowController, and Embedded Payment Element. Opt in to `ApiConfigurationPreview` and pass the same configuration through the element's builder:

```kotlin
@OptIn(ApiConfigurationPreview::class)
fun paymentSheetConfiguration(publishableKey: String): PaymentSheet.Configuration {
    return PaymentSheet.Configuration.Builder("Example, Inc.")
        .apiConfiguration(
            ApiConfiguration(publishableKey)
                .stripeAccountId(null)
                .betas(setOf("example_beta=v1"))
        )
        .build()
}
```

FlowController accepts that PaymentSheet configuration. Embedded Payment Element uses the same `apiConfiguration(...)` setter on its configuration builder.

Each configured Stripe API request includes the tokens in `Stripe-Version`, alongside the request producer's existing version and built-in tokens. Arbitrary exact strings are supported, including the `=v1` suffix. Added tokens are sorted and exact duplicates removed. An empty set keeps the existing header unchanged. Calling `betas(...)` again replaces the builder's set; it does not change an already built configuration or request.

Customer ephemeral keys, Link custom keys, and 3DS2 next-action keys retain the originating beta policy while preserving their existing connected-account behavior. Analytics and fraud telemetry do not receive this API negotiation header. Standalone legacy integrations start with no configured flags.

Tap to Add calls made through this SDK's Stripe repository carry the configuration. Calls made internally by the separate Terminal SDK use Terminal's transport; its exposed API here does not provide a beta-header hook.
