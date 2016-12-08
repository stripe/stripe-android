package com.stripe.example.controller;

import android.widget.EditText;
import android.widget.Spinner;

import com.stripe.android.model.Card;
import com.stripe.example.view.CreditCardView;

/**
 * A class that reads the UI.
 */
public class CardInformationReader {

    private static final String CURRENCY_UNSPECIFIED = "Unspecified";

    private CreditCardView mCreditCardView;
    private Spinner mCurrencySpinner;

    public CardInformationReader(
            CreditCardView creditCardView,
            Spinner currencySpinner) {
        mCreditCardView = creditCardView;
        mCurrencySpinner = currencySpinner;
    }

    /**
     * Read the user input and create a {@link Card} from it.\
     *
     * @return a {@link Card} based on the currently displayed user input
     */
    public Card readCardData() {
        String currency = getCurrency();
        Card cardToSave = mCreditCardView.getCard();
        if (cardToSave != null) {
            cardToSave.setCurrency(currency);
            return cardToSave;
        }
        return new Card.Builder("", 1, 0, "").build();
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
