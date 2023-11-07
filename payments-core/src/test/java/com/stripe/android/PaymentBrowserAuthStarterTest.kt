package com.stripe.android

import android.content.Intent
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.AuthActivityStarterHost
import com.stripe.android.view.PaymentAuthWebViewActivity
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PaymentBrowserAuthStarterTest {
    private val activity: ComponentActivity = mock()

    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val requestCodeCaptor: KArgumentCaptor<Int> = argumentCaptor()

    private val defaultReturnUrl = DefaultReturnUrl.create(
        ApplicationProvider.getApplicationContext()
    )

    private val legacyStarter = PaymentBrowserAuthStarter.Legacy(
        AuthActivityStarterHost.create(activity),
        defaultReturnUrl
    )

    @Test
    fun start_startsWithCorrectIntentAndRequestCode() {
        legacyStarter.start(DATA)
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            requestCodeCaptor.capture()
        )

        val args = requireNotNull(
            PaymentBrowserAuthContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertNull(args.toolbarCustomization)
        assertEquals(DATA.clientSecret, args.clientSecret)
    }

    @Test
    fun start_startsWithCorrectIntentAndRequestCodeAndCustomization() {
        legacyStarter.start(
            DATA.copy(
                toolbarCustomization = StripeToolbarCustomization()
            )
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            requestCodeCaptor.capture()
        )

        val args = requireNotNull(
            PaymentBrowserAuthContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertNotNull(args.toolbarCustomization)
        assertEquals(DATA.clientSecret, args.clientSecret)
    }

    @Test
    fun `start with isInstantApp false and defaultReturnUrl will start PaymentAuthWebViewActivity`() {
        val legacyStarter = PaymentBrowserAuthStarter.Legacy(
            AuthActivityStarterHost.ActivityHost(
                activity,
                statusBarColor = Color.RED
            ),
            defaultReturnUrl
        )
        legacyStarter.start(
            DATA.copy(
                isInstantApp = false
            )
        )
        verify(activity).startActivityForResult(
            argWhere { intent ->
                intent.component?.className == PaymentAuthWebViewActivity::class.java.name
            },
            any()
        )
    }

    @Test
    fun `start with isInstantApp true and defaultReturnUrl will start StripeBrowserLauncherActivity`() {
        val legacyStarter = PaymentBrowserAuthStarter.Legacy(
            AuthActivityStarterHost.ActivityHost(
                activity,
                statusBarColor = Color.RED
            ),
            defaultReturnUrl
        )
        legacyStarter.start(
            DATA.copy(
                isInstantApp = true
            )
        )
        verify(activity).startActivityForResult(
            argWhere { intent ->
                intent.component?.className == StripeBrowserLauncherActivity::class.java.name
            },
            any()
        )
    }

    @Test
    fun `intent extras should include statusBarColor when available`() {
        val legacyStarter = PaymentBrowserAuthStarter.Legacy(
            AuthActivityStarterHost.ActivityHost(
                activity,
                statusBarColor = Color.RED
            ),
            defaultReturnUrl
        )
        legacyStarter.start(DATA)
        verify(activity).startActivityForResult(
            argWhere { intent ->
                val args = requireNotNull(
                    intent.getParcelableExtra<PaymentBrowserAuthContract.Args>("extra_args")
                )
                args.statusBarColor == Color.RED
            },
            any()
        )
    }

    private companion object {
        private val DATA = PaymentBrowserAuthContract.Args(
            objectId = "pi_1EceMnCRMbs6FrXfCXdF8dnx",
            requestCode = 50000,
            clientSecret = "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k",
            url = "https://hooks.stripe.com/",
            returnUrl = "stripe://payment-auth",
            statusBarColor = Color.RED,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            isInstantApp = false
        )
    }
}
