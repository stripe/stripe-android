# Async payment evidence available in this setup

Parent verified test backend server.js endpoint inventory at revision48e8e831583fcbbe56de5163d90a56b87e178a32, lines125–145. It lists checkout, checkout_session, showcase, confirm_intent, customer_ephemeral_key, create_setup_intent, endpoints; test proxies create_checkout_session/customer/coupon/promotion_code, attach_payment_method, publishable_key. No PaymentIntent/Session retrieval proxy is declared. Do not invoke confirm_intent just to retrieve data; it mutates payment state.

Parent read full CheckoutClient::PollResource, revision `8df1a74cee5d3d259c5f8e3c5ae2e143277311eb`:
https://stripe.sourcegraphcloud.com/stripe-internal/mint@8df1a74cee5d3d259c5f8e3c5ae2e143277311eb/-/blob/pay-server/lib/payment_pages/api/checkout_client/poll_resource.rb

Fields: id, livemode, ui_mode, state, status, payment_status, confirmation_status. No PaymentIntent or PI secret/status.

Exact enum documentation:
- pending: recorded confirmation/existing processing hasn't produced observed completion/failure; after accepted confirm/next-action completion continue polling.
- succeeded: successful Checkout completion; update local Session status/payment_status and return success without retrieve. **Does not guarantee payment settlement, capture, or all background activation work.**
- failed: observed payment/setup failure or terminal Session state; return failure. Retrieve may provide details but can reject; no guarantee of retryability/retrievability.

Field documentation says it reflects current polling information, not a durable confirm-response log. Terminal Session failure precedes not_started. State/status alone must not decide confirmation outcome.

For async tests, SDK Completed and poll succeeded/complete/unpaid are not proof of paid settlement. A later paid transition or failed confirmation outcome is useful observed evidence. Without direct PI retrieval, don't claim exact PI processing→requires_payment_method transition. Unpaid alone also doesn't prove a decline. Signed webhook receipt/fulfillment remains untested.
