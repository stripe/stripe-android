package com.stripe.android;

import android.app.Activity;
import android.content.Intent;

import com.stripe.android.model.PaymentIntentFixtures;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthenticationControllerTest {

    private PaymentAuthenticationController mController;

    @Mock private Activity mActivity;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mController = new PaymentAuthenticationController();
    }

    @Test
    public void handleNextAction_whenAuthRequired() {
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_ACTION);
        verify(mActivity).startActivityForResult(any(Intent.class),
                eq(PaymentAuthenticationController.REQUEST_CODE));
    }

    @Test
    public void shouldHandleResult_withInvalidResultCode() {
        assertFalse(mController.shouldHandleResult(500, Activity.RESULT_OK, new Intent()));
    }
}