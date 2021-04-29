package com.stripe.android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.AuthActivityStarter
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PaymentBrowserAuthStarterTest {

    private val activity: Activity = mock()

    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val requestCodeCaptor: KArgumentCaptor<Int> = argumentCaptor()

    private val defaultReturnUrl = DefaultReturnUrl.create(
        ApplicationProvider.getApplicationContext()
    )

    private val legacyStarter = PaymentBrowserAuthStarter.Legacy(
        AuthActivityStarter.Host.create(activity),
        isCustomTabsSupported = true,
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
    fun `intent extras should include statusBarColor when available`() {
        val legacyStarter = PaymentBrowserAuthStarter.Legacy(
            AuthActivityStarter.Host(
                activity,
                fragment = null,
                statusBarColor = Color.RED
            ),
            isCustomTabsSupported = true,
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
            returnUrl = "stripe://payment-auth"
        )
    }
}
