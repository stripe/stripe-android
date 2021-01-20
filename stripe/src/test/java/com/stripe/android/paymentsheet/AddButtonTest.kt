package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.PaymentOptionViewState
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddButtonTest {
    private val addButton = AddButton(ApplicationProvider.getApplicationContext())

    @Test
    fun `onCompletedState() should update view`() {
        addButton.onCompletedState(
            PaymentOptionViewState.Completed(PaymentSelection.GooglePay)
        )
        assertThat(
            addButton.viewBinding.lockIcon.isShown
        ).isFalse()
    }
}
