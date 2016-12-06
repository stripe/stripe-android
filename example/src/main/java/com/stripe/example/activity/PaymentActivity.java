package com.stripe.example.activity;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.stripe.android.model.Card;
import com.stripe.example.R;
import com.stripe.example.controller.AsyncTaskTokenController;
import com.stripe.example.controller.IntentServiceTokenController;
import com.stripe.example.controller.RxTokenController;
import com.stripe.example.dialog.ErrorDialogFragment;
import com.stripe.example.module.DependencyHandler;

public class PaymentActivity extends AppCompatActivity {

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://manage.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY = "pk_test_6pRNASCoBOKtIshFeQd4XMUh";

    private DependencyHandler mDependencyHandler;

    private AsyncTaskTokenController mAsyncTaskTokenController;
    private IntentServiceTokenController mIntentServiceTokenController;
    private RxTokenController mRxTokenController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_activity);

        mDependencyHandler = new DependencyHandler(
                this,
                (EditText) findViewById(R.id.number),
                (Spinner) findViewById(R.id.expMonth),
                (Spinner) findViewById(R.id.expYear),
                (EditText) findViewById(R.id.cvc),
                (Spinner) findViewById(R.id.currency),
                (ListView) findViewById(R.id.listview));

        Button saveButton = (Button) findViewById(R.id.save);
        mAsyncTaskTokenController = mDependencyHandler.getAsyncTaskTokenController(saveButton);

        Button saveRxButton = (Button) findViewById(R.id.saverx);
        mRxTokenController = mDependencyHandler.getRxTokenController(saveRxButton);

        Button saveIntentServiceButton = (Button) findViewById(R.id.saveWithService);
        mIntentServiceTokenController = mDependencyHandler.getIntentServiceTokenController(
                this, saveIntentServiceButton);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRxTokenController != null) {
            mRxTokenController.detach();
        }

        if (mIntentServiceTokenController != null) {
            mIntentServiceTokenController.detach();
        }
    }

    private boolean validateCard(Card card) {
        boolean valid = true;
        if (!card.validateNumber()) {
            handleError("The card number that you entered is invalid");
            valid = false;
        } else if (!card.validateExpiryDate()) {
            handleError("The expiration date that you entered is invalid");
            valid = false;
        } else if (!card.validateCVC()) {
            handleError("The CVC code that you entered is invalid");
            valid = false;
        } else if (!card.validateCard()){
            handleError("The card details that you entered are invalid");
            valid = false;
        }

        return valid;
    }

    private void handleError(String error) {
        DialogFragment fragment = ErrorDialogFragment.newInstance(R.string.validationErrors, error);
        fragment.show(getSupportFragmentManager(), "error");
    }

}
