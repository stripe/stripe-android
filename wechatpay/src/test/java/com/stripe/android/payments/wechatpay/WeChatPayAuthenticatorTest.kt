package com.stripe.android.payments.wechatpay

import com.stripe.android.model.WeChat
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class WeChatPayAuthenticatorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val mockWeChatAuthLauncherFactory: (AuthActivityStarterHost, Int) -> WeChatPayAuthStarter =
        mock()
    private val mockWeChatPayAuthStarter: WeChatPayAuthStarter = mock()

    private val authenticator = WeChatPayAuthenticator().also {
        it.weChatAuthLauncherFactory = mockWeChatAuthLauncherFactory
    }

    @Before
    fun setUpMockFactory() {
        whenever(mockWeChatAuthLauncherFactory(any(), any())).thenReturn(
            mockWeChatPayAuthStarter
        )
    }

    @Test
    fun `wechatPayAuthStarter should start when stripeIntent is WeChatPay`() =
        testDispatcher.runBlockingTest {
            authenticator.authenticate(
                mock(),
                PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE,
                "",
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

    @Test
    fun `authenticate should throw exception when stripeIntent is not WeChatPay`() =
        testDispatcher.runBlockingTest {
            assertFailsWith<IllegalArgumentException> {
                authenticator.authenticate(
                    mock(),
                    PaymentIntentFixtures.PI_REQUIRES_BLIK_AUTHORIZE,
                    "",
                    REQUEST_OPTIONS
                )
            }
        }

    private companion object {
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = "TestKey",
            stripeAccount = "TestAccountId"
        )
    }
}
