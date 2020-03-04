package com.stripe.android.view

import android.view.inputmethod.EditorInfo
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import kotlin.test.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodCardViewTest {

    private val activity: AddPaymentMethodActivity = mock()
    private val addPaymentMethodCardView: AddPaymentMethodCardView = mock()
    private val keyboardController: KeyboardController = mock()

    @Test
    fun softEnterKey_whenDataIsValid_hidesKeyboardAndAttemptsToSave() {
        `when`<PaymentMethodCreateParams>(addPaymentMethodCardView.createParams)
            .thenReturn(PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

        AddPaymentMethodCardView.OnEditorActionListenerImpl(
            activity, addPaymentMethodCardView, keyboardController
        )
            .onEditorAction(null, EditorInfo.IME_ACTION_DONE, null)

        verify(keyboardController).hide()
        verify(activity).onActionSave()
    }
}
