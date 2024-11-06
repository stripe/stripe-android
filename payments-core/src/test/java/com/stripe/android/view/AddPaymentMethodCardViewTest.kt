package com.stripe.android.view

import android.view.inputmethod.EditorInfo
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

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
            activity,
            addPaymentMethodCardView,
            keyboardController
        )
            .onEditorAction(null, EditorInfo.IME_ACTION_DONE, null)

        verify(keyboardController).hide()
        verify(activity).onActionSave()
    }
}
