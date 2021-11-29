package com.stripe.android.payments.wechatpay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.WeChat
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.wechatpay.reflection.DefaultWeChatPayReflectionHelper
import com.stripe.android.payments.wechatpay.reflection.WeChatPayReflectionHelper
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

internal class WeChatPayAuthActivity : AppCompatActivity(), InvocationHandler {
    // TODO(ccen): inject reflectionHelper as a singleton
    @VisibleForTesting
    internal var reflectionHelperProvider: () -> WeChatPayReflectionHelper =
        { DefaultWeChatPayReflectionHelper() }
    private val reflectionHelper by lazy { reflectionHelperProvider() }
    private val weChatApi by lazy { reflectionHelper.createWXAPI(this) }
    private val iWXAPIEventHandler by lazy { reflectionHelper.createIWXAPIEventHandler(this) }
    private var clientSecret: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            requireNotNull(
                WeChatPayAuthContract.Args.fromIntent(intent),
                { NULL_ARG_MSG }
            ).let {
                clientSecret = it.clientSecret
                launchWeChat(
                    it.weChat
                )
            }
        }.onFailure {
            Log.d(TAG, "incorrect Intent")
            finishWithResult(StripeIntentResult.Outcome.FAILED, it)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        reflectionHelper.handleIntent(weChatApi, intent, iWXAPIEventHandler)
        Log.d(TAG, "onNewIntent - callback from IWXAPI")
    }

    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        // IWXAPIEventHandler.onReq(BaseReq var1)
        if (method?.name == "onReq") {
            // no-op
        }
        // IWXAPIEventHandler.onResp(BaseResp var1)
        else if (method?.name == "onResp") {
            val resp = args?.get(0)
            resp?.let {
                val errorCode = reflectionHelper.getRespErrorCode(resp)
                Log.d(TAG, "onResp with errCode: $errorCode")
                when (errorCode) {
                    WECHAT_PAY_ERR_OK -> {
                        finishWithResult(StripeIntentResult.Outcome.SUCCEEDED)
                    }
                    WECHAT_PAY_ERR_USER_CANCEL -> {
                        finishWithResult(StripeIntentResult.Outcome.CANCELED)
                    }
                    else -> {
                        finishWithResult(
                            StripeIntentResult.Outcome.FAILED,
                            IllegalStateException("$FAILS_WITH_CODE $errorCode")
                        )
                    }
                }
            } ?: run {
                finishWithResult(
                    StripeIntentResult.Outcome.FAILED,
                    IllegalStateException(NULL_RESP)
                )
            }
        }
        return null
    }

    private fun launchWeChat(weChat: WeChat) {
        if (reflectionHelper.registerApp(weChatApi, weChat.appId)) {
            Log.d(TAG, "registerApp success")
            reflectionHelper.sendReq(weChatApi, reflectionHelper.createPayReq(weChat))
        } else {
            Log.d(TAG, "registerApp failure")
            finishWithResult(
                StripeIntentResult.Outcome.FAILED,
                IllegalStateException(REGISTRATION_FAIL)
            )
        }
    }

    private fun finishWithResult(
        @StripeIntentResult.Outcome flowOutcome: Int,
        exception: Throwable? = null
    ) {
        Log.d(TAG, "finishWithResult with flowOutcome: $flowOutcome")
        val paymentFlowResult = PaymentFlowResult.Unvalidated(
            clientSecret = clientSecret,
            flowOutcome = flowOutcome,
            exception = exception?.let { StripeException.create(exception) }
        )
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(paymentFlowResult.toBundle())
        )
        finish()
    }

    companion object {
        const val TAG = "WeChatPayAuthActivity"
        const val NULL_ARG_MSG = "WeChatPayAuthContract.Args is null"
        const val FAILS_WITH_CODE = "WeChatPay fails with errorCode:"
        const val NULL_RESP = "WeChatPay BaseResp is PayResp"
        const val REGISTRATION_FAIL = "WeChatPay registerApp fails"
        const val WECHAT_PAY_ERR_OK = 0 // BaseResp.ErrCode.ERR_OK
        const val WECHAT_PAY_ERR_USER_CANCEL = -2 // BaseResp.ErrCode.ERR_USER_CANCEL
    }
}
