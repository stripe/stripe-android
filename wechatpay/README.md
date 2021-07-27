# Stripe Android SDK WeChatPay module
This module provides support for `PaymentIntent` with `wechat_pay` as its payment method.
 
# Overview
* This module will provide a `PaymentAuthenticator` implementation to authenticate a [PaymentIntent](https://stripe.com/docs/api/payment_intents) with [next_action.wechat_pay_redirect_to_android_app](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action-wechat_pay_redirect_to_android_app) by invoking the WeChat Pay SDK and passing the required parameters from that hash.

* No extra code is needed when using this module, however you will need to declare dependency on the actual WeChat Pay SDK, as this module uses reflection to invoke it.

# Usage
* In app/build.gradle, add these dependencies:
    ```gradle
    dependencies {
      // Stripe dependencies, make sure they have the same $stripe_sdk_version
      implementation 'com.stripe:stripe-android:$stripe_sdk_version' // Main Stripe SDK
      implementation 'com.stripe:stripe-wechatpay:$stripe_sdk_version' // WeChat Pay module
      // WeChat Pay SDK, make sure 6.7.0 is used
      implementation 'com.tencent.mm.opensdk:wechat-sdk-android-without-mta:6.7.0'
    }
    ```

* Then in your app, just confirm and capture the result of the `PaymentIntent` with WeChat Pay like others (see a complete example [here](https://stripe.com/docs/payments/integration-builder)), the WeChat app on the phone will be opened to confirm the payment.
    ```kotlin
    class CheckoutActivity : AppCompatActivity() {
        private fun startCheckout() {
            // create paymentMethod with WeChat Pay type
            val weChatPaymentMethodCreateParams = PaymentMethodCreateParams.createWeChatPay()
            // confirm the intent with the param
            val confirmParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodCreateParams(weChatPaymentMethodCreateParams, clientSecret)
            stripe.confirmPayment(this, confirmParams)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (stripe.isPaymentResult(requestCode, data)) {
                lifecycleScope.launch {
                    runCatching {
                        stripe.getPaymentIntentResult(requestCode, data!!).intent
                    }.fold( /* retrieve success/failure result */ )
                }
            }
        }
    }
    ```

# Notice
1. Make sure your Android app is registered in [WeChat Open Platform](https://open.weixin.qq.com/) and its package name (应用包名) from `AndroidManifest.xml` and signature (应用签名) correctly uploaded.
    > Note: Use the __GenSignature__ tool provided by WeChat [here](https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5) to generate a signature, note the signature will differ from debug build to release build.
2. Make sure the WeChat app installed on your test phone is logged in with an account with WeChat Pay enabled.
3. Make sure to create a Stripe `PaymentIntent` with a Stripe _live_ key - the `PaymentIntent` created by a _test_ key will have dummy WeChat Pay parameters that won't be recognized by WeChat.
4. Apart from adding the dependencies, you __don't__ need to write any additional code with WeChat Pay SDK (e.g the `WXPayEntryActivity` or any callbacks from the WeChat Pay [doc](https://pay.weixin.qq.com/wechatpay_guide/help_docs.shtml)), the module will do those for you.
