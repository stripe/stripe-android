# Verified test backend proxy routes

Source read through Sourcegraph on 2026-11-12: `stripe-internal/sandbox-apps`, revision `48e8e831583fcbbe56de5163d90a56b87e178a32`, `stripe-mobile-playground-backend/server.js`, lines 147–249.

Source link: https://stripe.sourcegraphcloud.com/stripe-internal/sandbox-apps@48e8e831583fcbbe56de5163d90a56b87e178a32/-/blob/stripe-mobile-playground-backend/server.js?L147-249

Backend URL is confirmed by demo.yaml/README as https://stp-mobile-playground-backend-v7.stripedemos.com .

Common POST envelope:
```json
{"merchant":"US","stripe_version":"2026-08-26.preview","request_params":{}}
```

`request_params` is passed directly to the corresponding Stripe SDK operation; stripe_version optional. Configured merchants provide test-mode keys; do not use custom keys. These intentionally flexible routes are test fixtures, not a production integration pattern.

- GET /publishable_key?merchant=US -> {publishable_key: merchant.publicKey}
- POST /create_checkout_session -> stripe.checkout.sessions.create(requestParams, requestOptions)
- POST /create_customer -> stripe.customers.create(requestParams, requestOptions)
- **POST /create_coupon** -> stripe.coupons.create(requestParams, requestOptions), source199–212
- **POST /create_promotion_code** -> stripe.promotionCodes.create(requestParams, requestOptions), source214–227
- POST /attach_payment_method -> stripe.paymentMethods.attach(payment_method_id.trim(), requestParams, requestOptions); payment_method_id is separate top-level required field.

Routes return created object; errors via sendStripeProxyError. These routes were source-verified; actual deployed behavior must still be observed. No coupon update/delete or PM retrieval proxy is claimed by this inventory.
