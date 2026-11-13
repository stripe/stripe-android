# Backend discount protocol and prerequisite evidence

Parent read Sourcegraph on 2026-11-12, revision `001bc4512d28a43acf2edefa2491bd853cd84372`. Source is `stripe-internal/mint`.

## Checkout-client update schema

https://stripe.sourcegraphcloud.com/stripe-internal/mint@001bc4512d28a43acf2edefa2491bd853cd84372/-/blob/pay-server/lib/payment_pages/api/checkout_client/update_params/method_params.rb

The complete file is116 lines. Fields: id, client_secret, locale, timezone, mobile_session_id, mobile_app_id; mutation fields tax_region, adaptive_pricing, payment_method_to_detach, payment_method_to_update, checkout_item_to_update, checkout_item_to_remove. The exclusive-mutation list at106–113 contains exactly those six mutations. There is **no promotion_code field**.

Update endpoint binds that schema via `api_params UpdateParams::MethodParams` at update_method.rb27, and invokes `Private::PaymentPageUpdateRequest.build` against the underlying PaymentPage::UpdateMethod at34–40:
https://stripe.sourcegraphcloud.com/stripe-internal/mint@001bc4512d28a43acf2edefa2491bd853cd84372/-/blob/pay-server/lib/payment_pages/api/checkout_client/update_method.rb

The checkout-client adjustable-quantity discount tests seed promo by POST `/v1/payment_pages/{session_id}` with promotion_code, not through checkout_client. Those tests gate merchant with checkout_unified_mode_development and checkout_item_one_time_price. This is source context, not authorization to bypass gates or substitute another payment API in the SDK test.

## Modeless creation prerequisites

https://stripe.sourcegraphcloud.com/stripe-internal/mint@001bc4512d28a43acf2edefa2491bd853cd84372/-/blob/pay-server/lib/payment_pages/api/validators/discounts.rb?L519-539

Exact source logic at529–538:
```ruby
when Core::Enum::CheckoutSessionMode::MODELESS
  if !Opus::Gating::Check.checkout_unified_mode_development?(merchant)
    raise(
      Errors::CheckoutAPIError.new(
        :checkout_mode_invalid_parameter,
        param: :discounts,
        format: {param: "discounts", mode: mode.serialize}
      )
    )
  end
```

Source search also verified checkout_configuration_settings.rb33–46: validate_allow_promotion_codes! returns if flag nil; payment/subscription support it; setup doesn't; modeless uses the same checkout_unified_mode_development merchant gate. It raises checkout_mode_invalid_parameter otherwise. Gate-off creation tests assert400 for allow_promotion_codes true AND false. No gates were changed during this run.

Generic docs preview-access partial contains only an elements_beta_preview email-access request (lines1–4): “Mobile Checkout Elements is in private preview. Share your email address to request access.” It does not document discount-specific gates/protocol prerequisites.

## Interpretation limits

Real request comparisons in this topic establish current test-merchant behavior. Server flags/coupon creation rejection is a prerequisite gap; checkout-client promotion mutation lacks protocol support in the inspected schema. Do not report known unfinished/gated preview capability as a newly discovered SDK regression or claim all merchants behave identically. A documentation report can explain that the advertised recipe does not state these prerequisites and fails in an otherwise working Mobile Checkout Elements setup. Remaining real discounted payment/rounding/constraint scenarios are untested because no discounted control can be established in this environment.
