package com.stripe.android.checkout

import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class CheckoutMandateStateTest {
    @Test
    fun `content access survives process death only for the same configuration`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val mandateState = CheckoutMandateState(savedStateHandle)
        mandateState.recordContentAccess("original_configuration")

        val restoredState = CheckoutMandateState(savedStateHandle.restoreFromParcel())

        assertThat(restoredState.hasAccessedContent("original_configuration")).isTrue()
        assertThat(restoredState.hasAccessedContent("new_configuration")).isFalse()
    }

    @Test
    fun `mandate access survives process death only for the same configuration and payment method`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val mandateState = CheckoutMandateState(savedStateHandle)
        val paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
        mandateState.recordMandateTextAccess("original_configuration", paymentMethod)

        val restoredState = CheckoutMandateState(savedStateHandle.restoreFromParcel())

        assertThat(restoredState.hasAccessedMandateText("original_configuration", paymentMethod)).isTrue()
        assertThat(restoredState.hasAccessedMandateText("new_configuration", paymentMethod)).isFalse()
        assertThat(
            restoredState.hasAccessedMandateText("original_configuration", paymentMethod.copy(id = "pm_another_sepa"))
        ).isFalse()
    }

    @Suppress("RestrictedApi")
    private fun SavedStateHandle.restoreFromParcel(): SavedStateHandle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(savedStateProvider().saveState())
            parcel.setDataPosition(0)
            SavedStateHandle.createHandle(parcel.readBundle(CheckoutMandateState::class.java.classLoader), null)
        } finally {
            parcel.recycle()
        }
    }
}
