package com.stripe.android.payments.wechatpay

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.WeChat
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.wechatpay.WeChatPayAuthActivity.Companion.WECHAT_PAY_ERR_OK
import com.stripe.android.payments.wechatpay.WeChatPayAuthActivity.Companion.WECHAT_PAY_ERR_USER_CANCEL
import com.stripe.android.payments.wechatpay.reflection.WeChatPayReflectionHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Method
import kotlin.test.assertEquals
import kotlin.test.assertNull
import androidx.appcompat.R as AppCompatR

@RunWith(RobolectricTestRunner::class)
class WeChatPayAuthActivityTest {
    private val mockWeChatApi = mock<Any>()
    private val mockReflectionHelper: WeChatPayReflectionHelper = mock()
    private val context = ApplicationProvider.getApplicationContext<Context>().also {
        it.setTheme(AppCompatR.style.Theme_AppCompat)
    }
    private val mockOnRespMethod = mock<Method>()
    private val responseArg = Object()

    @Before
    fun setupMocks() {
        whenever(mockReflectionHelper.registerApp(any(), any())).thenReturn(true)
        whenever(mockReflectionHelper.createWXAPI(any())).thenReturn(mockWeChatApi)
        whenever(mockOnRespMethod.name).thenReturn("onResp")
    }

    @Test
    fun `when registration succeeds should sendReq to wechatApi`() {
        val mockBaseReq = mock<Any>()
        whenever(mockReflectionHelper.createPayReq(any())).thenReturn(mockBaseReq)
        val weChatCaptor = argumentCaptor<WeChat>()

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity {
                verify(mockReflectionHelper).createPayReq(weChatCaptor.capture())

                val req = weChatCaptor.firstValue

                assertEquals(req.partnerId, PARTNER_ID)
                assertEquals(req.prepayId, PREPAY_ID)
                assertEquals(req.packageValue, PACKAGE_VALUE)
                assertEquals(req.nonce, NONCE)
                assertEquals(req.timestamp, TIME_STAMP)
                assertEquals(req.sign, SIGN)

                verify(mockReflectionHelper).sendReq(same(mockWeChatApi), same(mockBaseReq))
            }
        }
    }

    @Test
    fun `when registration fails should finish with exception`() {
        whenever(mockReflectionHelper.registerApp(any(), any())).thenReturn(false)
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
        whenever(mockReflectionHelper.getRespErrorCode(eq(responseArg))).thenReturn(
            WECHAT_PAY_ERR_OK
        )

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity { activity ->
                activity.invoke(mock(), mockOnRespMethod, arrayOf(responseArg))
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
        whenever(mockReflectionHelper.getRespErrorCode(eq(responseArg))).thenReturn(
            WECHAT_PAY_ERR_USER_CANCEL
        )

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity { activity ->
                activity.invoke(mock(), mockOnRespMethod, arrayOf(responseArg))
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
    fun `when weChatApi returns unexpected code should finish with failed`() {
        val unexpectedErrorCode =
            12345 // any value other than WECHAT_PAY_ERR_OK or WECHAT_PAY_ERR_USER_CANCEL

        whenever(mockReflectionHelper.getRespErrorCode(eq(responseArg))).thenReturn(
            unexpectedErrorCode
        )

        testActivityWithIntent(createIntent(isValid = true)) {
            it.onActivity { activity ->
                activity.invoke(mock(), mockOnRespMethod, arrayOf(responseArg))
            }

            assertResult(
                it.getResult(),
                StripeIntentResult.Outcome.FAILED,
                exceptionMessage = "${WeChatPayAuthActivity.FAILS_WITH_CODE} $unexpectedErrorCode",
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
        hasClientSecret: Boolean = false
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
            reflectionHelperProvider = { mockReflectionHelper }
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
