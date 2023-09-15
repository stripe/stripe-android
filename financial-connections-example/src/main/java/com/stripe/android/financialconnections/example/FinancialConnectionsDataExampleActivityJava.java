package com.stripe.android.financialconnections.example;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.stripe.android.financialconnections.FinancialConnections;
import com.stripe.android.financialconnections.FinancialConnectionsSheet;
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample;
import com.stripe.android.financialconnections.example.databinding.ActivityFinancialconnectionsExampleBinding;

import kotlin.Unit;

public class FinancialConnectionsDataExampleActivityJava extends AppCompatActivity {

    private FinancialConnectionsExampleViewModel viewModel;
    private FinancialConnectionsSheet financialConnectionsSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).
                get(FinancialConnectionsExampleViewModel.class);

        ActivityFinancialconnectionsExampleBinding binding =
                ActivityFinancialconnectionsExampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews(binding);
        observeViews(binding);
        observeState(binding);
    }

    private void setupViews(ActivityFinancialconnectionsExampleBinding binding) {
        binding.toolbar.setTitle(R.string.collect_bank_account_for_data_title);
        setSupportActionBar(binding.toolbar);
        FinancialConnections.setEventListener(
                event -> {
                    Log.d("FinancialConnections", "Event: " + event);
                    return Unit.INSTANCE;
                }
        );
        financialConnectionsSheet = FinancialConnectionsSheet.create(
                this,
                this.viewModel::onFinancialConnectionsSheetResult
        );
    }

    private void observeViews(ActivityFinancialconnectionsExampleBinding binding) {
        binding.launchConnectionsSheet.setOnClickListener(
                v -> viewModel.startFinancialConnectionsSessionForData());
    }

    private void observeState(ActivityFinancialconnectionsExampleBinding binding) {
        viewModel.getStateLiveData().observe(this, state -> bindState(state, binding));
        viewModel.getViewEffectLiveData().observe(this, (FinancialConnectionsExampleViewEffect effect) -> {
                    if (effect instanceof OpenFinancialConnectionsSheetExample) {
                        financialConnectionsSheet.present(
                                ((OpenFinancialConnectionsSheetExample) effect).getConfiguration()
                        );
                    }
                }
        );
    }

    private void bindState(FinancialConnectionsExampleState financialConnectionsExampleState,
                           ActivityFinancialconnectionsExampleBinding viewBinding) {
        viewBinding.status.setText(financialConnectionsExampleState.getStatus());
        viewBinding.launchConnectionsSheet.setEnabled(!financialConnectionsExampleState.getLoading());
    }
}