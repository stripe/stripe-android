package com.stripe.android

import android.app.Activity
import android.content.Intent
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.AuthActivityStarter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewStarterTest {

    @Mock
    private lateinit var activity: Activity

    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>
    private lateinit var requestCodeCaptor: KArgumentCaptor<Int>

    private lateinit var host: AuthActivityStarter.Host

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        intentArgumentCaptor = argumentCaptor()
        requestCodeCaptor = argumentCaptor()
        host = AuthActivityStarter.Host.create(activity)
    }

    @Test
    fun start_startsWithCorrectIntentAndRequestCode() {
        PaymentAuthWebViewStarter(host, 50000).start(DATA)
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(),
            requestCodeCaptor.capture())

        val args: PaymentAuthWebViewStarter.Args = requireNotNull(
            intentArgumentCaptor.firstValue.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        )
        assertNull(args.toolbarCustomization)
        assertEquals(DATA.clientSecret, args.clientSecret)
    }

    @Test
    fun start_startsWithCorrectIntentAndRequestCodeAndCustomization() {
        PaymentAuthWebViewStarter(host, 50000).start(DATA.copy(
            toolbarCustomization = StripeToolbarCustomization()
        ))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(),
            requestCodeCaptor.capture())

        val args: PaymentAuthWebViewStarter.Args = requireNotNull(
            intentArgumentCaptor.firstValue.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        )
        assertNotNull(args.toolbarCustomization)
        assertEquals(DATA.clientSecret, args.clientSecret)
    }

    private companion object {
        private val DATA = PaymentAuthWebViewStarter.Args(
            clientSecret = "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k",
            url = "https://hooks.stripe.com/",
            returnUrl = "stripe://payment-auth"
        )
    }
}
