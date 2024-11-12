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
import com.stripe.android.view.NetbankingBank;
import com.stripe.example.R;
import com.stripe.example.Settings;
import com.stripe.example.databinding.BankSelectorPaymentActivityBinding;

import java.util.Objects;

public class NetbankingListPaymentActivity extends AppCompatActivity {

    private BankSelectorPaymentActivityBinding viewBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding = BankSelectorPaymentActivityBinding.inflate(getLayoutInflater());

        setContentView(viewBinding.getRoot());
        setTitle(R.string.netbanking_payment_example);

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
                        .setPaymentMethodType(PaymentMethod.Type.Netbanking)
                        .build()
                );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final AddPaymentMethodActivityStarter.Result result =
                AddPaymentMethodActivityStarter.Result.fromIntent(data);
        if (result instanceof AddPaymentMethodActivityStarter.Result.Success) {
            final AddPaymentMethodActivityStarter.Result.Success successResult =
                    (AddPaymentMethodActivityStarter.Result.Success) result;
            onPaymentMethodResult(successResult.getPaymentMethod());
        }
    }

    private void onPaymentMethodResult(@NonNull PaymentMethod paymentMethod) {
        final String netbankingCode = Objects.requireNonNull(paymentMethod.netbanking).bank;
        final String resultMessage = "Created Payment Method\n" +
                "\nType: " + paymentMethod.type +
                "\nId: " + paymentMethod.id +
                "\nBank code: " + netbankingCode;
        viewBinding.paymentMethodResult.setText(resultMessage);

        final NetbankingBank netbankingBank = NetbankingBank.get(netbankingCode);
        if (netbankingBank != null) {
            viewBinding.bankInfo.setVisibility(View.VISIBLE);
            if (netbankingBank.getBrandIconResId() != null) {
                viewBinding.bankInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        ContextCompat.getDrawable(this, netbankingBank.getBrandIconResId()),
                        null,
                        null,
                        null
                );
            }
            viewBinding.bankInfo.setText(netbankingBank.getDisplayName());
        } else {
            viewBinding.bankInfo.setVisibility(View.GONE);
        }
    }
}
