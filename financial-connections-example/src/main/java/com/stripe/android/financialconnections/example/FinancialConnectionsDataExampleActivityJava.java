package com.stripe.android.financialconnections.example;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.stripe.android.financialconnections.FinancialConnections;
import com.stripe.android.financialconnections.FinancialConnectionsSheet;
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample;

public class FinancialConnectionsDataExampleActivityJava extends AppCompatActivity {

    private FinancialConnectionsExampleViewModel viewModel;
    private FinancialConnectionsSheet financialConnectionsSheet;

    private TextView status;
    private Button launchConnectionsSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financialconnections_example);
        viewModel = new ViewModelProvider(this).
                get(FinancialConnectionsExampleViewModel.class);
        setupViews();
        observeViews();
        observeState();
        FinancialConnections.setEventListener(
                event -> Log.d("FinancialConnections", "Event: " + event.getName())
        );
        financialConnectionsSheet = FinancialConnectionsSheet.create(
                this,
                viewModel::onFinancialConnectionsSheetResult
        );
    }

    private void setupViews() {
        status = findViewById(R.id.status);
        launchConnectionsSheet = findViewById(R.id.launch_connections_sheet);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.collect_bank_account_for_data_title);
        setSupportActionBar(toolbar);
    }

    private void observeViews() {
        findViewById(R.id.launch_connections_sheet).setOnClickListener(
                v -> viewModel.startFinancialConnectionsSessionForData());
    }

    private void observeState() {
        viewModel.getStateLiveData().observe(this, this::bindState);
        viewModel.getViewEffectLiveData().observe(this, (FinancialConnectionsExampleViewEffect effect) -> {
                    if (effect instanceof OpenFinancialConnectionsSheetExample) {
                        financialConnectionsSheet.present(
                                ((OpenFinancialConnectionsSheetExample) effect).getConfiguration()
                        );
                    }
                }
        );
    }

    private void bindState(FinancialConnectionsExampleState state) {
        status.setText(state.getStatus());
        launchConnectionsSheet.setEnabled(!state.getLoading());
    }
}