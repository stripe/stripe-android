package com.stripe.android.view

import android.view.inputmethod.EditorInfo
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodCardViewTest {

    @Mock
    private lateinit var activity: AddPaymentMethodActivity
    @Mock
    private lateinit var addPaymentMethodCardView: AddPaymentMethodCardView
    @Mock
    private lateinit var keyboardController: KeyboardController

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun softEnterKey_whenDataIsValid_hidesKeyboardAndAttemptsToSave() {
        `when`<PaymentMethodCreateParams>(addPaymentMethodCardView.createParams)
            .thenReturn(PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
        AddPaymentMethodCardView.OnEditorActionListenerImpl(
            activity, addPaymentMethodCardView, keyboardController)
            .onEditorAction(null, EditorInfo.IME_ACTION_DONE, null)
        verify(keyboardController).hide()
        verify(activity).onActionSave()
    }
}
