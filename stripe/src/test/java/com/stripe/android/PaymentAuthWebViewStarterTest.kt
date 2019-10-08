package com.stripe.android

import android.app.Activity
import android.content.Intent
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.AuthActivityStarter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
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
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(),
            requestCodeCaptor.capture())

        val intent = intentArgumentCaptor.firstValue
        val extras = requireNotNull(intent.extras)
        assertNull(extras.getParcelable(PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION))
        assertEquals(5, extras.size())
        assertEquals(CLIENT_SECRET,
            extras.getString(PaymentAuthWebViewStarter.EXTRA_CLIENT_SECRET))
    }

    @Test
    fun start_startsWithCorrectIntentAndRequestCodeAndCustomization() {
        PaymentAuthWebViewStarter(host, 50000,
            StripeToolbarCustomization()).start(DATA)
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(),
            requestCodeCaptor.capture())

        val intent = intentArgumentCaptor.firstValue
        val extras = requireNotNull(intent.extras)
        assertNotNull(
            extras.getParcelable<StripeToolbarCustomization>(
                PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION
            )
        )
        assertEquals(5, extras.size())
        assertEquals(CLIENT_SECRET,
            extras.getString(PaymentAuthWebViewStarter.EXTRA_CLIENT_SECRET))
    }

    companion object {
        private const val CLIENT_SECRET = "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k"
        private val DATA = PaymentAuthWebViewStarter.Data(
            CLIENT_SECRET,
            "https://hooks.stripe.com/",
            "stripe://payment-auth"
        )
    }
}
