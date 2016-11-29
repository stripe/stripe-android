package com.stripe.example.activity;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.example.R;
import com.stripe.example.dialog.ErrorDialogFragment;
import com.stripe.example.dialog.ProgressDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PaymentActivity extends FragmentActivity {

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://manage.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY = "pk_test_6pRNASCoBOKtIshFeQd4XMUh";
    private static final String CURRENCY_UNSPECIFIED = "Unspecified";

    private ProgressDialogFragment progressFragment;

    private Button saveButton;
    private EditText cardNumberEditText;
    private EditText cvcEditText;
    private Spinner monthSpinner;
    private Spinner yearSpinner;
    private Spinner currencySpinner;

    private ListView listView;
    private SimpleAdapter simpleAdapter;
    private List<Map<String, String>> cardTokens = new ArrayList<Map<String, String>>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_activity);

        progressFragment = ProgressDialogFragment.newInstance(R.string.progressMessage);

        this.saveButton = (Button) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCreditCard();
            }
        });

        this.cardNumberEditText = (EditText) findViewById(R.id.number);
        this.cvcEditText = (EditText) findViewById(R.id.cvc);
        this.monthSpinner = (Spinner) findViewById(R.id.expMonth);
        this.yearSpinner = (Spinner) findViewById(R.id.expYear);
        this.currencySpinner = (Spinner) findViewById(R.id.currency);
        this.listView = (ListView) findViewById(R.id.listview);
        initListView();
    }

    public void saveCreditCard() {
        String cardNumber = cardNumberEditText.getText().toString();
        String cvc = cvcEditText.getText().toString();

        int expMonth = getIntegerFromSpinner(monthSpinner);
        int expYear = getIntegerFromSpinner(yearSpinner);

        String currency = getCurrency();
        Card cardToSave = new Card(cardNumber, expMonth, expYear, cvc);
        cardToSave.setCurrency(currency);

        if (validateCard(cardToSave)) {
            startProgress();
            new Stripe().createToken(
                    cardToSave,
                    PUBLISHABLE_KEY,
                    new TokenCallback() {
                        public void onSuccess(Token token) {
                            addToList(token);
                            finishProgress();
                        }
                        public void onError(Exception error) {
                            handleError(error.getLocalizedMessage());
                            finishProgress();
                        }
                    });
        }
    }

    private void addToList(Token token) {
        String endingIn = getResources().getString(R.string.endingIn);
        Map<String, String> map = new HashMap<String, String>();
        map.put("last4", endingIn + " " + token.getCard().getLast4());
        map.put("tokenId", token.getId());
        cardTokens.add(map);
        simpleAdapter.notifyDataSetChanged();
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

    private void startProgress() {
        progressFragment.show(getSupportFragmentManager(), "progress");
    }

    private void finishProgress() {
        progressFragment.dismiss();
    }

    public String getCurrency() {
        if (currencySpinner.getSelectedItemPosition() == 0) {
            return null;
        }

        String selected = (String) currencySpinner.getSelectedItem();

        if (selected.equals(CURRENCY_UNSPECIFIED)) {
            return null;
        }

        return selected.toLowerCase();
    }

    private int getIntegerFromSpinner(Spinner spinner) {
        try {
            return Integer.parseInt(spinner.getSelectedItem().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void handleError(String error) {
        DialogFragment fragment = ErrorDialogFragment.newInstance(R.string.validationErrors, error);
        fragment.show(getSupportFragmentManager(), "error");
    }

    private void initListView() {
        simpleAdapter = new SimpleAdapter(
                this,
                cardTokens,
                R.layout.list_item_layout,
                new String[]{"last4", "tokenId"},
                new int[]{R.id.last4, R.id.tokenId});
        listView.setAdapter(simpleAdapter);
    }
}
