package com.stripe.android.payments.wechatpay

import com.stripe.android.model.parsers.PaymentIntentJsonParser
import org.json.JSONObject

object PaymentIntentFixtures {
    private val PARSER = PaymentIntentJsonParser()

    private val PI_REQUIRES_BLIK_AUTHORIZE_JSON = JSONObject(
        """
        {
          "id": "pi_1IVmwXFY0qyl6XeWwxGWA04D",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "amount_subtotal": 1099,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_1IVmwXFY0qyl6XeWwxGWA04D"
          },
          "client_secret": "pi_1IVmwXFY0qyl6XeWwxGWA04D_secret_4U8cSCdPefr8LHtPsKvA3mcQz",
          "confirmation_method": "automatic",
          "created": 1615939737,
          "currency": "pln",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": {
            "type": "blik_authorize"
          },
          "on_behalf_of": null,
          "payment_method": "pm_1IVnI3FY0qyl6XeWxJFdBh2g",
          "payment_method_options": {
            "blik": {
            }
          },
          "payment_method_types": [
            "blik"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_action",
          "total_details": {
            "amount_discount": 0,
            "amount_tax": 0
          },
          "transfer_data": null,
          "transfer_group": null
        }
        """.trimIndent()
    )

    internal val PI_REQUIRES_BLIK_AUTHORIZE = PARSER.parse(PI_REQUIRES_BLIK_AUTHORIZE_JSON)!!

    private val PI_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON = JSONObject(
        """
        {
          "id": "pi_1IlJH7BNJ02ErVOjm37T3OUt",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_1IlJH7BNJ02ErVOjm37T3OUt"
          },
          "client_secret": "pi_1IlJH7BNJ02ErVOjm37T3OUt_secret_vgMExmjvESdtPqddHOSSSDip2",
          "confirmation_method": "automatic",
          "created": 1619638941,
          "currency": "usd",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "some_random_string",
              "package": "Sign=WXPay",
              "partner_id": "wx65997d6307c3827d",
              "prepay_id": "test_transaction",
              "sign": "8B26124BABC816D7140034DDDC7D3B2F1036CCB2D910E52592687F6A44790D5E",
              "timestamp": "1619638941"
            }
          },
          "on_behalf_of": null,
          "payment_method": "pm_1IlJH7BNJ02ErVOjxKQu1wfH",
          "payment_method_options": {
            "wechat_pay": {
            }
          },
          "payment_method_types": [
            "wechat_pay"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_action",
          "transfer_data": null,
          "transfer_group": null
        }
        """.trimIndent()
    )

    internal val PI_REQUIRES_WECHAT_PAY_AUTHORIZE =
        PARSER.parse(PI_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON)!!

    private val PI_NO_NEXT_ACTION_DATA_JSON = JSONObject(
        """
        {
          "id": "pi_1IVmwXFY0qyl6XeWwxGWA04D",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "amount_subtotal": 1099,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_1IVmwXFY0qyl6XeWwxGWA04D"
          },
          "client_secret": "pi_1IVmwXFY0qyl6XeWwxGWA04D_secret_4U8cSCdPefr8LHtPsKvA3mcQz",
          "confirmation_method": "automatic",
          "created": 1615939737,
          "currency": "pln",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": {
          },
          "on_behalf_of": null,
          "payment_method": "pm_1IVnI3FY0qyl6XeWxJFdBh2g",
          "payment_method_options": {
            "blik": {
            }
          },
          "payment_method_types": [
            "blik"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_action",
          "total_details": {
            "amount_discount": 0,
            "amount_tax": 0
          },
          "transfer_data": null,
          "transfer_group": null
        }
        """.trimIndent()
    )

    internal val PI_NO_NEXT_ACTION_DATA = PARSER.parse(PI_NO_NEXT_ACTION_DATA_JSON)!!
}
