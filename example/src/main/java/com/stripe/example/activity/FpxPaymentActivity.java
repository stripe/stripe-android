package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.view.AddPaymentMethodActivityStarter;
import com.stripe.android.view.FpxBank;
import com.stripe.example.R;
import com.stripe.example.Settings;
import com.stripe.example.databinding.FpxPaymentActivityBinding;

import java.util.Objects;

public class FpxPaymentActivity extends AppCompatActivity {

    private FpxPaymentActivityBinding viewBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding = FpxPaymentActivityBinding.inflate(getLayoutInflater());

        setContentView(viewBinding.getRoot());
        setTitle(R.string.fpx_payment_example);

        PaymentConfiguration.init(
                this,
                new Settings(this).getPublishableKey()
        );

        viewBinding.selectPaymentMethodButton
                .setOnClickListener(view -> launchAddPaymentMethod());
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
        final String fpxBankCode = Objects.requireNonNull(paymentMethod.fpx).bank;
        final String resultMessage = "Created Payment Method\n" +
                "\nType: " + paymentMethod.type +
                "\nId: " + paymentMethod.id +
                "\nBank code: " + fpxBankCode;
        viewBinding.paymentMethodResult.setText(resultMessage);

        final FpxBank fpxBank = FpxBank.get(fpxBankCode);
        if (fpxBank != null) {
            viewBinding.fpxBankInfo.setVisibility(View.VISIBLE);
            viewBinding.fpxBankInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(this, fpxBank.getBrandIconResId()),
                    null,
                    null,
                    null
            );
            viewBinding.fpxBankInfo.setText(fpxBank.getDisplayName());
        } else {
            viewBinding.fpxBankInfo.setVisibility(View.GONE);
        }
    }
}
