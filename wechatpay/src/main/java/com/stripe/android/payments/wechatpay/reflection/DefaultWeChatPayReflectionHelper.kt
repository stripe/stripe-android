package com.stripe.android.payments.wechatpay.reflection

import android.content.Context
import android.content.Intent
import android.util.Log
import com.stripe.android.model.WeChat
import com.stripe.android.payments.wechatpay.WeChatPayAuthActivity
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class DefaultWeChatPayReflectionHelper : WeChatPayReflectionHelper {
    override fun isWeChatPayAvailable(): Boolean {
        return try {
            Class.forName("com.tencent.mm.opensdk.openapi.IWXAPIEventHandler")
            true
        } catch (e: Exception) {
            Log.e(TAG, "WeChatPay dependency not found")
            false
        }
    }

    /**
     * Calls "WXAPIFactory.createWXAPI(Context var0, String var1): IWXAPI"
     */
    override fun createWXAPI(context: Context): Any {
        val wxApiFactoryClassCreateMethod =
            Class.forName("com.tencent.mm.opensdk.openapi.WXAPIFactory")
                .getMethod("createWXAPI", Context::class.java, String::class.java)
        return wxApiFactoryClassCreateMethod.invoke(null, context, null)!!
    }

    /**
     * Calls "IWXAPI.registerApp(String var1): Boolean"
     */
    override fun registerApp(iWxApi: Any, appId: String?): Boolean {
        val iWxApiRegisterAppMethod = Class.forName("com.tencent.mm.opensdk.openapi.IWXAPI")
            .getMethod("registerApp", String::class.java)
        return iWxApiRegisterAppMethod.invoke(iWxApi, appId) as Boolean
    }

    /**
     * Calls "IWXAPI.sendReq(BaseReq var1): Boolean"
     */
    override fun sendReq(iWxApi: Any, baseReq: Any): Boolean {
        val iWxApiSendReqMethod = Class.forName("com.tencent.mm.opensdk.openapi.IWXAPI")
            .getMethod("sendReq", Class.forName("com.tencent.mm.opensdk.modelbase.BaseReq"))
        return iWxApiSendReqMethod.invoke(iWxApi, baseReq) as Boolean
    }

    /**
     * Creates "com.tencent.mm.opensdk.modelpay.PayReq" with a [WeChat] object
     * and have "PayReq.Options.callbackClassName" set as [WeChatPayAuthActivity]'s name
     */
    override fun createPayReq(weChat: WeChat): Any {
        val payReqClass = Class.forName("com.tencent.mm.opensdk.modelpay.PayReq")

        val payReqAppId = payReqClass.getField("appId")
        val payReqPartnerId = payReqClass.getField("partnerId")
        val payReqPrepayId = payReqClass.getField("prepayId")
        val payReqPackageValue = payReqClass.getField("packageValue")
        val payReqNonceStr = payReqClass.getField("nonceStr")
        val payReqTimeStamp = payReqClass.getField("timeStamp")
        val payReqSign = payReqClass.getField("sign")
        val payReqOptions = payReqClass.getField("options")

        val payReqOptionsClass = Class.forName("com.tencent.mm.opensdk.modelpay.PayReq\$Options")
        val payReqOptionsCallbackClassName = payReqOptionsClass.getField("callbackClassName")

        val payReqOptionsInstance = payReqOptionsClass.getConstructor().newInstance().also {
            payReqOptionsCallbackClassName.set(it, WeChatPayAuthActivity::class.java.name)
        }

        return payReqClass.getConstructor().newInstance().also {
            payReqAppId.set(it, weChat.appId)
            payReqPartnerId.set(it, weChat.partnerId)
            payReqPrepayId.set(it, weChat.prepayId)
            payReqPackageValue.set(it, weChat.packageValue)
            payReqNonceStr.set(it, weChat.nonce)
            payReqTimeStamp.set(it, weChat.timestamp)
            payReqSign.set(it, weChat.sign)
            payReqOptions.set(it, payReqOptionsInstance)
        }
    }

    override fun createIWXAPIEventHandler(handler: InvocationHandler): Any {
        val iWXAPIEventHandlerClass = Class.forName(
            "com.tencent.mm.opensdk.openapi.IWXAPIEventHandler"
        )
        return Proxy.newProxyInstance(
            iWXAPIEventHandlerClass.classLoader,
            arrayOf(iWXAPIEventHandlerClass),
            handler
        )
    }

    /**
     * Calls "IWXAPI.handleIntent(Intent var1, IWXAPIEventHandler var2): Boolean"
     */
    override fun handleIntent(weChatApi: Any, intent: Intent?, iWXAPIEventHandler: Any): Boolean {
        val handleIntentMethod = Class.forName("com.tencent.mm.opensdk.openapi.IWXAPI")
            .getMethod(
                "handleIntent",
                Intent::class.java,
                Class.forName(
                    "com.tencent.mm.opensdk.openapi.IWXAPIEventHandler"
                )
            )

        return handleIntentMethod(weChatApi, intent, iWXAPIEventHandler) as Boolean
    }

    /**
     * Calls "com.tencent.mm.opensdk.modelpay.PayResp.PayResp#errorCode"
     */
    override fun getRespErrorCode(payResp: Any): Int {
        val payRespErrorCode = Class.forName("com.tencent.mm.opensdk.modelpay.PayResp")
            .getField("errCode")
        return payRespErrorCode.get(payResp) as Int
    }

    private companion object {
        val TAG = DefaultWeChatPayReflectionHelper::class.java.simpleName
    }
}
