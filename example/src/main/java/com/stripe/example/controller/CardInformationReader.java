package com.stripe.example.controller;

import android.widget.EditText;
import android.widget.Spinner;

import com.stripe.android.model.Card;

/**
 * A class that reads the UI.
 */
public class CardInformationReader {

    private static final String CURRENCY_UNSPECIFIED = "Unspecified";

    private EditText mCardNumberEditText;
    private Spinner mMonthSpinner;
    private Spinner mYearSpinner;
    private EditText mCvcEditText;
    private Spinner mCurrencySpinner;

    public CardInformationReader(
            EditText cardNumberEditText,
            Spinner monthSpinner,
            Spinner yearSpinner,
            EditText cvcEditText,
            Spinner currencySpinner) {
        mCardNumberEditText = cardNumberEditText;
        mMonthSpinner = monthSpinner;
        mYearSpinner = yearSpinner;
        mCvcEditText = cvcEditText;
        mCurrencySpinner = currencySpinner;
    }

    /**
     * Read the user input and create a {@link Card} from it.\
     *
     * @return a {@link Card} based on the currently displayed user input
     */
    public Card readCardData() {
        String cardNumber = mCardNumberEditText.getText().toString();
        String cvc = mCvcEditText.getText().toString();

        int expMonth = getIntegerFromSpinner(mMonthSpinner);
        int expYear = getIntegerFromSpinner(mYearSpinner);

        String currency = getCurrency();
        Card cardToSave = new Card(cardNumber, expMonth, expYear, cvc);
        cardToSave.setCurrency(currency);
        return cardToSave;
    }

    private int getIntegerFromSpinner(Spinner spinner) {
        try {
            return Integer.parseInt(spinner.getSelectedItem().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getCurrency() {
        if (mCurrencySpinner.getSelectedItemPosition() == 0) {
            return null;
        }

        String selected = (String) mCurrencySpinner.getSelectedItem();

        if (selected.equals(CURRENCY_UNSPECIFIED)) {
            return null;
        }

        return selected.toLowerCase();
    }
}
