package com.stripe.android.view;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import com.stripe.android.model.PaymentMethodCreateParamsFixtures;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AddPaymentMethodCardViewTest {

    @Mock
    private AddPaymentMethodActivity mActivity;
    @Mock
    private AddPaymentMethodCardView mAddPaymentMethodCardView;
    @Mock
    private InputMethodManager mInputMethodManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void softEnterKey_whenDataIsValid_hidesKeyboardAndAttemptsToSave() {
        when(mAddPaymentMethodCardView.getCreateParams())
                .thenReturn(PaymentMethodCreateParamsFixtures.DEFAULT_CARD);
        new AddPaymentMethodCardView.OnEditorActionListenerImpl(
                mActivity, mAddPaymentMethodCardView, mInputMethodManager)
                .onEditorAction(null, EditorInfo.IME_ACTION_DONE, null);
        verify(mInputMethodManager).hideSoftInputFromWindow(null, 0);
        verify(mActivity).onActionSave();
    }
}
