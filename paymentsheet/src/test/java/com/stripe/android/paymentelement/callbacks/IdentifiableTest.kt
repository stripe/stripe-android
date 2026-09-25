package com.stripe.android.paymentelement.callbacks

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.Identifiable
import com.stripe.android.utils.PaymentElementCallbackTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IdentifiableTest {
    @get:Rule
    val callbackTestRule = PaymentElementCallbackTestRule()

    @Test
    fun `Parceled identifier retains its identity for callback lookup`() {
        val id = createTestIdentifier("test")
        val callbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences[createTestIdentifier("fallback")] = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences[id] = callbacks

        val parcel = Parcel.obtain()
        val restoredId = try {
            parcel.writeSerializable(id)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            parcel.readSerializable() as Identifiable
        } finally {
            parcel.recycle()
        }

        assertThat(restoredId).isEqualTo(id)
        assertThat(restoredId.hashCode()).isEqualTo(id.hashCode())
        assertThat(restoredId).isNotSameInstanceAs(id)
        assertThat(PaymentElementCallbackReferences[restoredId]).isSameInstanceAs(callbacks)
    }
}
