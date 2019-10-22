package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.view.AddPaymentMethodActivityStarter;
import com.stripe.example.R;
import com.stripe.example.Settings;

import java.util.Objects;

public class FpxPaymentActivity extends AppCompatActivity {

    private TextView resultView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fpx_payment);
        setTitle(R.string.fpx_payment_example);

        PaymentConfiguration.init(this, Settings.PUBLISHABLE_KEY);

        findViewById(R.id.btn_select_payment_method)
                .setOnClickListener(view -> launchAddPaymentMethod());
        resultView = findViewById(R.id.payment_method_result);
    }

    private void launchAddPaymentMethod() {
        new AddPaymentMethodActivityStarter(this)
                .startForResult(new AddPaymentMethodActivityStarter.Args.Builder()
                        .setPaymentMethodType(PaymentMethod.Type.Fpx)
                        .build()
                );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final AddPaymentMethodActivityStarter.Result result =
                AddPaymentMethodActivityStarter.Result.fromIntent(data);
        if (result != null) {
            onPaymentMethodResult(result.getPaymentMethod());
        }
    }

    private void onPaymentMethodResult(@NonNull PaymentMethod paymentMethod) {
        final String resultMessage = "Created Payment Method\n" +
                "\nType: " + paymentMethod.type +
                "\nId: " + paymentMethod.id +
                "\nBank name: " + Objects.requireNonNull(paymentMethod.fpx).bank;
        resultView.setText(resultMessage);
    }
}
