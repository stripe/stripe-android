package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ListView;

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.R;
import com.stripe.example.module.DependencyHandler;

import rx.functions.Action1;

public class PaymentActivity extends AppCompatActivity {

    private DependencyHandler mDependencyHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_activity);

        mDependencyHandler = new DependencyHandler(
                this,
                (CardInputWidget) findViewById(R.id.card_input_widget),
                (ListView) findViewById(R.id.listview));

        Button saveButton = (Button) findViewById(R.id.save);
        mDependencyHandler.attachAsyncTaskTokenController(saveButton);

        Button saveRxButton = (Button) findViewById(R.id.saverx);
        mDependencyHandler.attachRxTokenController(saveRxButton);

        Button saveIntentServiceButton = (Button) findViewById(R.id.saveWithService);
        mDependencyHandler.attachIntentServiceTokenController(this, saveIntentServiceButton);

        RxView.clicks(findViewById(R.id.three_d_secure)).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                Intent intent = new Intent(PaymentActivity.this, PollingActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDependencyHandler.clearReferences();
    }
}
