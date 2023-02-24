package com.stripe.android.payments.core.authentication

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.view.AuthActivityStarterHost
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SourceAuthenticatorTest {
    private val activity: ComponentActivity = mock {
        on { lifecycle } doReturn LifecycleRegistry(mock).apply {
            currentState = Lifecycle.State.RESUMED
        }
    }
    private val host = AuthActivityStarterHost.create(activity)
    private val paymentBrowserAuthStarterFactory =
        mock<(AuthActivityStarterHost) -> PaymentBrowserAuthStarter>()
    private val paymentRelayStarterFactory =
        { host: AuthActivityStarterHost ->
            PaymentRelayStarter.Legacy(host)
        }
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )
    private val testDispatcher = UnconfinedTestDispatcher()

    private val authenticator = SourceAuthenticator(
        paymentBrowserAuthStarterFactory,
        paymentRelayStarterFactory,
        analyticsRequestExecutor,
        analyticsRequestFactory,
        false,
        testDispatcher,
        { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        false
    )

    @Test
    fun authenticate_withNoneFlowSource_shouldBypassAuth() =
        runTest {
            authenticator.authenticate(
                host = host,
                authenticatable = SourceFixtures.SOURCE_WITH_SOURCE_ORDER.copy(
                    flow = Source.Flow.None
                ),
                requestOptions = mock()
            )
            verify(activity).startActivityForResult(
                argWhere {
                    it.component?.className == PaymentRelayActivity::class.java.name
                },
                eq(StripePaymentController.SOURCE_REQUEST_CODE)
            )
        }
}
