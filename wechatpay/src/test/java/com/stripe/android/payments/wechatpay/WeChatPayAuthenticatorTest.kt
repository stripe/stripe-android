package com.stripe.android.payments.wechatpay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.WeChat
import com.stripe.android.payments.wechatpay.reflection.WeChatPayReflectionHelper
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class WeChatPayAuthenticatorTest {
    private val mockWeChatPayAuthStarter: WeChatPayAuthStarter = mock()
    private val mockReflectionHelper: WeChatPayReflectionHelper = mock()
    private val authenticator = WeChatPayAuthenticator().also {
        it.weChatAuthLauncherFactory = { _, _ -> mockWeChatPayAuthStarter }
        it.reflectionHelper = mockReflectionHelper
    }

    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
    }

    @Before
    fun setUpMocks() {
        whenever(mockReflectionHelper.isWeChatPayAvailable()).thenReturn(true)
    }

    @Test
    fun `authenticate should throw exception when weChatPay dependency is not available`() =
        runTest {
            whenever(mockReflectionHelper.isWeChatPayAvailable()).thenReturn(false)

            assertFailsWithMessage<IllegalArgumentException>(
                "WeChatPay dependency is not found, add " +
                    "${WeChatPayReflectionHelper.WECHAT_PAY_GRADLE_DEP} in app's build.gradle"
            ) {
                authenticator.authenticate(
                    host,
                    PaymentIntentFixtures.PI_REQUIRES_BLIK_AUTHORIZE,
                    REQUEST_OPTIONS
                )
            }
        }

    @Test
    fun `authenticate should throw exception when stripeIntent is not WeChatPay`() =
        runTest {
            assertFailsWithMessage<IllegalArgumentException>(
                "stripeIntent.nextActionData should be WeChatPayRedirect, " +
                    "instead it is BlikAuthorize"
            ) {
                authenticator.authenticate(
                    host,
                    PaymentIntentFixtures.PI_REQUIRES_BLIK_AUTHORIZE,
                    REQUEST_OPTIONS
                )
            }
        }

    @Test
    fun `authenticate should throw exception when stripeIntent#nextActionData is null`() =
        runTest {
            assertFailsWithMessage<IllegalArgumentException>(
                "stripeIntent.nextActionData should be WeChatPayRedirect, " +
                    "instead it is null"
            ) {
                authenticator.authenticate(
                    host,
                    PaymentIntentFixtures.PI_NO_NEXT_ACTION_DATA,
                    REQUEST_OPTIONS
                )
            }
        }

    @Test
    @Ignore("Flaky mockito behavior: https://github.com/mockito/mockito/issues/2026")
    fun `wechatPayAuthStarter should start when stripeIntent is WeChatPay`() =
        runTest {
            authenticator.authenticate(
                host,
                PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE,
                REQUEST_OPTIONS
            )
            verify(mockWeChatPayAuthStarter).start(
                argWhere {
                    it.weChat == WeChat(
                        appId = "wx65997d6307c3827d",
                        nonce = "some_random_string",
                        packageValue = "Sign=WXPay",
                        partnerId = "wx65997d6307c3827d",
                        prepayId = "test_transaction",
                        sign = "8B26124BABC816D7140034DDDC7D3B2F1036CCB2D910E52592687F6A44790D5E",
                        timestamp = "1619638941"
                    ) &&
                        it.clientSecret == "pi_1IlJH7BNJ02ErVOjm37T3OUt_secret_vgMExmjvESdtPqddHOSSSDip2"
                }
            )
        }

    private inline fun <reified T : Throwable> assertFailsWithMessage(
        throwableMsg: String,
        block: () -> Unit
    ) {
        val throwable = assertFailsWith<T> { block() }
        assertEquals(throwableMsg, throwable.message)
    }

    private companion object {
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = "TestKey",
            stripeAccount = "TestAccountId"
        )
    }
}
