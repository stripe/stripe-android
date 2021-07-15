package com.stripe.android.payments.wechatpay.reflection

import android.content.Context
import android.content.Intent
import com.stripe.android.model.WeChat
import java.lang.reflect.InvocationHandler

/**
 * Interface to call WeChatPay SDK through reflection.
 */
interface WeChatPayReflectionHelper {
    /**
     * Checks if [WECHAT_PAY_GRADLE_DEP] dependency available.
     */
    fun isWeChatPayAvailable(): Boolean

    /**
     * Creates a "com.tencent.mm.opensdk.openapi.IWXAPI" instance.
     */
    fun createWXAPI(context: Context): Any

    /**
     * Registers the appId through a "com.tencent.mm.opensdk.openapi.IWXAPI" instance.
     */
    fun registerApp(iWxApi: Any, appId: String?): Boolean

    /**
     * Sends a "com.tencent.mm.opensdk.modelbase.BaseReq" through a
     * "com.tencent.mm.opensdk.openapi.IWXAPI" instance.
     */
    fun sendReq(iWxApi: Any, baseReq: Any): Boolean

    /**
     * Creates a "com.tencent.mm.opensdk.modelpay.PayReq" object from [WeChat] object.
     */
    fun createPayReq(weChat: WeChat): Any

    /**
     * Handles a [Intent] through a "com.tencent.mm.opensdk.openapi.IWXAPIEventHandler" instance.
     */
    fun handleIntent(weChatApi: Any, intent: Intent?, iWXAPIEventHandler: Any): Boolean

    /**
     * Gets the errorCode from a "com.tencent.mm.opensdk.modelpay.PayResp" object.
     */
    fun getRespErrorCode(payResp: Any): Int

    /**
     * Creates a proxy object for "com.tencent.mm.opensdk.openapi.IWXAPIEventHandler".
     */
    fun createIWXAPIEventHandler(handler: InvocationHandler): Any

    companion object {
        // version number needs to match
        const val WECHAT_PAY_GRADLE_DEP =
            "com.tencent.mm.opensdk:wechat-sdk-android-without-mta:6.7.0"
    }
}
