package com.stripe.android.paymentsheet.model

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MobileSessionParcelerTest {
    @Test
    fun `elements session preserves availability and log messages through parceling`() {
        val mobileSession = MobileSession.Session(
            paymentMethodAvailability = MobileSession.PaymentMethodAvailability(
                paymentMethodTypes = listOf("card", "instant_debits", "external_paypal", "cpmt_custom"),
                linkEnabled = true,
            ),
            logMessages = listOf("Payment method unavailable: café"),
        )

        assertThat(roundTrip(mobileSession).mobileSession).isEqualTo(mobileSession)
    }

    @Test
    fun `elements session preserves an empty sidecar through parceling`() {
        val mobileSession = MobileSession.Session(paymentMethodAvailability = null, logMessages = null)

        assertThat(roundTrip(mobileSession).mobileSession).isEqualTo(mobileSession)
    }

    @Test
    fun `elements session preserves an absent sidecar through parceling`() {
        assertThat(roundTrip(null).mobileSession).isNull()
    }

    private fun roundTrip(mobileSession: MobileSession.Session?): ElementsSession {
        val session = ElementsSession.createFromFallback(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            sessionsError = null,
        ).copy(mobileSession = mobileSession)
        val parcel = Parcel.obtain()
        return try {
            parcel.writeParcelable(session, 0)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readParcelable(ElementsSession::class.java.classLoader))
        } finally {
            parcel.recycle()
        }
    }
}
