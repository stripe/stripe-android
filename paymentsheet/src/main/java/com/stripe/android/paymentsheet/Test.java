package com.stripe.android.paymentsheet;

import androidx.activity.ComponentActivity;

import com.stripe.android.CreateIntentCallback;
import com.stripe.android.CreateIntentCallbackForServerSideConfirmation;
import com.stripe.android.LegacyCreateIntentCallback;
import com.stripe.android.LegacyCreateIntentCallbackForServerSideConfirmation;

public class Test {

    private LegacyCreateIntentCallback callback = (paymentMethodId, resultListener) -> {
        IllegalStateException e = new IllegalStateException("some error");
        resultListener.onResult(new CreateIntentCallback.Result.Failure(e));
    };

    private LegacyCreateIntentCallbackForServerSideConfirmation serverSideCallback = (
            paymentMethodId,
            shouldSavePaymentMethod,
            resultListener
    ) -> {
        IllegalStateException e = new IllegalStateException("some error");
        resultListener.onResult(new CreateIntentCallback.Result.Failure(e));
    };

    void foo(ComponentActivity componentActivity) {
        PaymentSheet ps = new PaymentSheet(
                componentActivity,
                paymentSheetResult -> {},
                CreateIntentCallback.forJava(callback)
        );

        PaymentSheet ps2 = new PaymentSheet(
                componentActivity,
                paymentSheetResult -> {},
                CreateIntentCallbackForServerSideConfirmation.forJava(serverSideCallback)
        );
    }
}
