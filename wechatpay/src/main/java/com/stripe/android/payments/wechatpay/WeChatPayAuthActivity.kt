package com.stripe.android.payments.wechatpay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.StripeException
import com.stripe.android.model.WeChat
import com.stripe.android.payments.PaymentFlowResult
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.modelpay.PayResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory

internal class WeChatPayAuthActivity : AppCompatActivity(), IWXAPIEventHandler {
    @VisibleForTesting
    internal var weChatApiProvider = { WXAPIFactory.createWXAPI(this, null) }

    private lateinit var weChatApi: IWXAPI

    private var clientSecret: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            requireNotNull(
                WeChatPayAuthContract.Args.fromIntent(intent),
                { NULL_ARG_MSG }
            ).let {
                weChatApi = weChatApiProvider()
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
        weChatApi.handleIntent(intent, this)
        Log.d(TAG, "onNewIntent - callback from IWXAPI")
    }

    override fun onReq(req: BaseReq?) {}

    override fun onResp(resp: BaseResp?) {
        if (resp is PayResp) {
            Log.d(TAG, "onResp with errCode: ${resp.errCode}")
            when (resp.errCode) {
                BaseResp.ErrCode.ERR_OK -> {
                    // Yes, wechat use errorCode to tell you payment is successful
                    finishWithResult(StripeIntentResult.Outcome.SUCCEEDED)
                }
                BaseResp.ErrCode.ERR_USER_CANCEL -> {
                    finishWithResult(StripeIntentResult.Outcome.CANCELED)
                }
                else -> {
                    finishWithResult(
                        StripeIntentResult.Outcome.FAILED,
                        IllegalStateException("$FAILS_WITH_CODE ${resp.errCode}")
                    )
                }
            }
        } else {
            finishWithResult(
                StripeIntentResult.Outcome.FAILED,
                IllegalStateException(NULL_RESP)
            )
        }
    }

    private fun launchWeChat(weChat: WeChat) {
        if (weChatApi.registerApp(weChat.appId)) {
            Log.d(TAG, "registerApp success")
            weChatApi.sendReq(createPayReq(weChat))
        } else {
            Log.d(TAG, "registerApp failure")
            finishWithResult(
                StripeIntentResult.Outcome.FAILED,
                IllegalStateException(REGISTRATION_FAIL)
            )
        }
    }

    private fun createPayReq(weChat: WeChat): PayReq {
        return PayReq().apply {
            appId = weChat.appId
            partnerId = weChat.partnerId
            prepayId = weChat.prepayId
            packageValue = weChat.packageValue
            nonceStr = weChat.nonce
            timeStamp = weChat.timestamp
            sign = weChat.sign
            options = PayReq.Options().apply {
                callbackClassName = WeChatPayAuthActivity::class.java.name
            }
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
    }
}
