package com.stripe.android.payments.wechatpay

import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.WeChat
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private const val TAG = "WeChatPayApi"
private const val WECHAT_PAY_ERR_OK = BaseResp.ErrCode.ERR_OK
private const val WECHAT_PAY_ERR_USER_CANCEL = BaseResp.ErrCode.ERR_USER_CANCEL

internal interface WeChatPayLauncher {

    sealed interface Result {

        val outcome: Int
        val exception: Throwable?

        object Success : Result {
            override val outcome: Int = StripeIntentResult.Outcome.SUCCEEDED
            override val exception: Throwable? = null
        }

        object Canceled : Result {
            override val outcome: Int = StripeIntentResult.Outcome.CANCELED
            override val exception: Throwable? = null
        }

        data class Failure(
            val message: String,
        ) : Result {
            override val outcome: Int = StripeIntentResult.Outcome.FAILED
            override val exception: Throwable = IllegalStateException(message)
        }
    }

    val result: SharedFlow<Result>

    fun launchWeChatPay(weChat: WeChat?)

    companion object {

        fun create(
            activity: AppCompatActivity,
        ): WeChatPayLauncher? {
            val isWeChatPayAvailable = runCatching {
                Class.forName("com.tencent.mm.opensdk.openapi.IWXAPIEventHandler")
            }.onFailure {
                Log.e(TAG, "WeChatPay dependency not found")
            }.isSuccess

            return if (isWeChatPayAvailable) {
                RealWeChatPayLauncher(activity)
            } else {
                null
            }
        }
    }
}

internal class RealWeChatPayLauncher(
    private val activity: AppCompatActivity,
) : WeChatPayLauncher {

    private val api: IWXAPI by lazy {
        WXAPIFactory.createWXAPI(activity.applicationContext, null, false)
    }

    private val _result = MutableSharedFlow<WeChatPayLauncher.Result>(replay = 1)
    override val result: SharedFlow<WeChatPayLauncher.Result> = _result

    init {
        activity.addOnNewIntentListener(this::handleIntent)

        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activity.removeOnNewIntentListener(this@RealWeChatPayLauncher::handleIntent)
                    super.onDestroy(owner)
                }
            }
        )
    }

    private fun handleIntent(intent: Intent?) {
        val eventHandler = object : IWXAPIEventHandler {

            override fun onReq(req: BaseReq?) {
                // Nothing to do here
            }

            override fun onResp(resp: BaseResp?) {
                val errorCode = resp?.errCode
                Log.d(TAG, "onResp with errCode: $errorCode")

                val result = when (errorCode) {
                    WECHAT_PAY_ERR_OK -> WeChatPayLauncher.Result.Success
                    WECHAT_PAY_ERR_USER_CANCEL -> WeChatPayLauncher.Result.Canceled
                    null -> failedWithUnknownError()
                    else -> failedWithError(errorCode)
                }

                postResult(result)
            }
        }

        api.handleIntent(intent, eventHandler)
    }

    override fun launchWeChatPay(weChat: WeChat?) {
        if (weChat == null) {
            postResult(failedToFetchInfo())
            return
        }

        val registered = api.registerApp(weChat.appId)
        if (!registered) {
            postResult(failedToRegister())
            return
        }

        val installed = isInstalled()
        if (!installed) {
            postResult(appNotInstalled())
            return
        }

        val sent = api.sendReq(weChat.toPayRequest())
        if (!sent) {
            postResult(failedToSendRequest())
        }
    }

    private fun isInstalled(): Boolean {
        return runCatching {
            activity.packageManager.getPackageInfo("com.tencent.mm", 0)
        }.isSuccess
    }

    private fun postResult(result: WeChatPayLauncher.Result) {
        Log.d(TAG, "Finish with result $result")
        _result.tryEmit(result)
    }

    private fun failedToFetchInfo(): WeChatPayLauncher.Result {
        return unableToLaunchWeChat(R.string.stripe_unable_to_launch_wechat_failed_to_fetch_info)
    }

    private fun failedToRegister(): WeChatPayLauncher.Result {
        return unableToLaunchWeChat(R.string.stripe_unable_to_launch_wechat_failed_to_register)
    }

    private fun appNotInstalled(): WeChatPayLauncher.Result {
        return unableToLaunchWeChat(R.string.stripe_unable_to_launch_wechat_not_installed)
    }

    private fun failedToSendRequest(): WeChatPayLauncher.Result {
        return unableToLaunchWeChat(R.string.stripe_unable_to_launch_wechat_failed_to_send_request)
    }

    private fun unableToLaunchWeChat(@StringRes reason: Int): WeChatPayLauncher.Result {
        val reasonText = activity.getString(reason)
        val message = activity.getString(R.string.stripe_unable_to_launch_wechat, reasonText)
        return WeChatPayLauncher.Result.Failure(message)
    }

    private fun failedWithError(error: Int?): WeChatPayLauncher.Result {
        return WeChatPayLauncher.Result.Failure(
            message = activity.getString(R.string.stripe_wechat_failed_with_error, error),
        )
    }

    private fun failedWithUnknownError(): WeChatPayLauncher.Result {
        return WeChatPayLauncher.Result.Failure(
            message = activity.getString(R.string.stripe_wechat_failed_unknown_error),
        )
    }
}

private fun WeChat.toPayRequest(): PayReq {
    val weChat = this
    return PayReq().apply {
        appId = weChat.appId
        partnerId = weChat.partnerId?.takeUnless { it.isBlank() } ?: "test"
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
