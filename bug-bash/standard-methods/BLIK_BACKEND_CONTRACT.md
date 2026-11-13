# BLIK confirmation contract evidence

Parent read authoritative backend source at revision `5446557e0835417a195f441f35a90963d404f503` on 2026-11-12.

## Accepted Checkout Client confirm fields

https://stripe.sourcegraphcloud.com/stripe-internal/mint@5446557e0835417a195f441f35a90963d404f503/-/blob/pay-server/lib/payment_pages/api/checkout_client/confirm_params/method_params.rb

Complete file167 lines: id/client_secret/locale/mobile IDs; payment_method, payment_method_data, customer_data, collected_information; expected_payment_method_type/expected_amount; shipping/return_url; attribution/captcha/device signals. **No payment_method_options or confirmation_token field.**

Nested customer_data is email/name/address/has_fallback_payment_method only; no alternative BLIK options carrier:
https://stripe.sourcegraphcloud.com/stripe-internal/mint@5446557e0835417a195f441f35a90963d404f503/-/blob/pay-server/lib/payment_pages/api/checkout_client/confirm_params/customer_data_params.rb

## Forwarding to underlying Payment Page confirmation

https://stripe.sourcegraphcloud.com/stripe-internal/mint@5446557e0835417a195f441f35a90963d404f503/-/blob/pay-server/lib/payment_pages/api/checkout_client/private/payment_page_confirm_request.rb

Full file76 lines. build at12–42 forwards payment_method and payment_method_data, customer_data, collected_information/email, save flag, expected type/amount, shipping, return_url, captcha/device/attribution data. It does **not** construct or forward payment_method_options. There is no separate option transport elsewhere in this builder.

The BLIK docs' required code is `payment_method_options[blik][code]` (see BLIK_AND_MERCHANTS.md). Therefore the SDK's observed omission is only one side of the missing end-to-end carrier. A regression asserting preservation of entered options demonstrates SDK behavior, but doesn't establish that adding the field alone would be accepted by today's backend. The runtime missing-code error and offered BLIK method should be reported with this integrated scope. Schema absence alone doesn't establish an intentional preview exclusion; unlike the known discount gate, no explicit BLIK exclusion was found in this read.

If safe direct request comparison is done, retain sanitized results only and use a fresh test Session; don't change gates or substitute the legacy Payment Pages flow as Checkout Client success.
