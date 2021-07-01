package com.stripe.android.payments.wechatpay

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.WeChat
import com.stripe.android.payments.PaymentFlowResult
import com.tencent.mm.opensdk.modelbase.BaseResp.ErrCode.ERR_COMM
import com.tencent.mm.opensdk.modelbase.BaseResp.ErrCode.ERR_OK
import com.tencent.mm.opensdk.modelbase.BaseResp.ErrCode.ERR_USER_CANCEL
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.modelpay.PayResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class WeChatPayAuthActivityTest {
    private val mockWeChatApi = mock<IWXAPI>()
    private val context = ApplicationProvider.getApplicationContext<Context>().also {
        it.setTheme(R.style.Theme_AppCompat)
    }
    private val mockResp = mock<PayResp>()

    @After
    fun clearMockResp() {
        mockResp.errCode = 0
    }

    @Test
    fun `when registration succeeds should sendReq to wechatApi`() {
        whenever(mockWeChatApi.registerApp(any())).thenReturn(true)

        val reqCaptor = argumentCaptor<PayReq>()

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity {
                verify(mockWeChatApi).sendReq(reqCaptor.capture())
                val req = reqCaptor.firstValue
                assertEquals(req.partnerId, PARTNER_ID)
                assertEquals(req.prepayId, PREPAY_ID)
                assertEquals(req.packageValue, PACKAGE_VALUE)
                assertEquals(req.nonceStr, NONCE)
                assertEquals(req.timeStamp, TIME_STAMP)
                assertEquals(req.sign, SIGN)
                assertEquals(req.options.callbackClassName, WeChatPayAuthActivity::class.java.name)
            }
        }
    }

    @Test
    fun `when registration fails should finish with exception`() {
        whenever(mockWeChatApi.registerApp(any())).thenReturn(false)
        testActivityWithIntent(createIntent(isValid = true)) {
            assertResult(
                it.getResult(),
                StripeIntentResult.Outcome.FAILED,
                WeChatPayAuthActivity.REGISTRATION_FAIL
            )
        }
    }

    @Test
    fun `when started with invalid intent should finish with exception`() {
        testActivityWithIntent(
            createIntent(isValid = false)
        ) {
            assertResult(
                it.getResult(),
                StripeIntentResult.Outcome.FAILED,
                WeChatPayAuthActivity.NULL_ARG_MSG
            )
        }
    }

    @Test
    fun `when weChatApi succeeds should finish with success`() {
        whenever(mockWeChatApi.registerApp(any())).thenReturn(true)
        mockResp.errCode = ERR_OK

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity { activity ->
                activity.onResp(mockResp)
            }

            assertResult(
                it.getResult(),
                StripeIntentResult.Outcome.SUCCEEDED,
                exceptionMessage = null,
                hasClientSecret = true
            )
        }
    }

    @Test
    fun `when weChatApi canceled should finish with canceled`() {
        whenever(mockWeChatApi.registerApp(any())).thenReturn(true)
        mockResp.errCode = ERR_USER_CANCEL

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity { activity ->
                activity.onResp(mockResp)
            }

            assertResult(
                it.getResult(),
                StripeIntentResult.Outcome.CANCELED,
                exceptionMessage = null,
                hasClientSecret = true
            )
        }
    }

    @Test
    fun `when weChatApi return other values should finish with failed`() {
        whenever(mockWeChatApi.registerApp(any())).thenReturn(true)
        mockResp.errCode = ERR_COMM // any value other than ERR_OK or ERR_USER_CANCEL

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity { activity ->
                activity.onResp(mockResp)
            }

            assertResult(
                it.getResult(),
                StripeIntentResult.Outcome.FAILED,
                exceptionMessage = "${WeChatPayAuthActivity.FAILS_WITH_CODE} $ERR_COMM",
                hasClientSecret = true
            )
        }
    }

    /**
     * Asserts the [Instrumentation.ActivityResult] when [WeChatPayAuthActivity] finishes.
     * Only assert the result's exception message when [exceptionMessage] is not null.
     * Only assert the result's clientSecret if [hasClientSecret] is true.
     */
    private fun assertResult(
        activityResult: Instrumentation.ActivityResult,
        flowOutcome: Int,
        exceptionMessage: String? = null,
        hasClientSecret: Boolean = false,
    ) {
        assertEquals(Activity.RESULT_OK, activityResult.resultCode)
        val flowResult = PaymentFlowResult.Unvalidated.fromIntent(activityResult.resultData)
        assertEquals(flowOutcome, flowResult.flowOutcome)
        if (hasClientSecret) {
            assertEquals(flowResult.clientSecret, CLIENT_SECRET)
        }
        exceptionMessage?.let {
            assertEquals(flowResult.exception!!.message, it)
        } ?: run {
            assertNull(flowResult.exception)
        }
    }

    private fun testActivityWithIntent(
        intent: Intent?,
        testBody: (InjectableActivityScenario<WeChatPayAuthActivity>) -> Unit
    ) = injectableActivityScenario<WeChatPayAuthActivity> {
        injectActivity {
            weChatApiProvider = { mockWeChatApi }
        }
    }.launch(intent).use {
        testBody(it)
    }

    private fun createIntent(isValid: Boolean = false) =
        if (isValid) {
            WeChatPayAuthContract().createIntent(
                context,
                WeChatPayAuthContract.Args(
                    WeChat(
                        appId = APP_ID,
                        partnerId = PARTNER_ID,
                        prepayId = PREPAY_ID,
                        packageValue = PACKAGE_VALUE,
                        nonce = NONCE,
                        timestamp = TIME_STAMP,
                        sign = SIGN
                    ),
                    CLIENT_SECRET
                )
            )
        } else {
            Intent(
                context,
                WeChatPayAuthActivity::class.java
            )
        }

    private companion object {
        const val APP_ID = "appId"
        const val PARTNER_ID = "partnerId"
        const val PREPAY_ID = "prepayId"
        const val PACKAGE_VALUE = "packageValue"
        const val NONCE = "nonceStr"
        const val TIME_STAMP = "timeStamp"
        const val SIGN = "sign"
        const val CLIENT_SECRET = "clientSecret"
    }
}
