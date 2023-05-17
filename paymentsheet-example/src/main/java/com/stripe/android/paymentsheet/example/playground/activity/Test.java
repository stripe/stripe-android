package com.stripe.android.paymentsheet.example.playground.activity;

import androidx.activity.ComponentActivity;

import com.stripe.android.CreateIntentCallback;
import com.stripe.android.CreateIntentResult;
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi;
import com.stripe.android.LegacyCreateIntentCallback;
import com.stripe.android.paymentsheet.PaymentSheet;

@ExperimentalPaymentSheetDecouplingApi
public class Test {

    private final LegacyCreateIntentCallback callback = (paymentMethod, shouldSavePaymentMethod, resultListener) -> {
        CreateIntentResult result = computeThings();
        resultListener.onResult(result);
    };

    void foo(ComponentActivity activity) {
        PaymentSheet ps = new PaymentSheet(
                activity,
                CreateIntentCallback.forJava(callback),
                paymentSheetResult -> {}
        );
    }

    private CreateIntentResult computeThings() {
        return new CreateIntentResult.Success("bla");
    }
}
