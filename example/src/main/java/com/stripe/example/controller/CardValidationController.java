package com.stripe.example.controller;

import android.support.annotation.NonNull;
import android.widget.TextView;

import com.stripe.android.model.Card;
import com.stripe.example.R;
import com.stripe.example.view.CreditCardView;

/**
 * Created by simonkenny on 08/12/2016.
 */

public class CardValidationController {

    private CreditCardView mCreditCardView;
    private TextView mValidationErrorTextView;
    private MessageDialogHandler mMessageDialogHandler;

    public CardValidationController(
            @NonNull CreditCardView creditCardView,
            @NonNull TextView validationErrorTextView,
            @NonNull MessageDialogHandler messageDialogHandler) {
        mCreditCardView = creditCardView;
        mValidationErrorTextView = validationErrorTextView;
        mMessageDialogHandler = messageDialogHandler;

        creditCardView.setCallback(new CreditCardView.Callback() {
            @Override
            public void onValidated(Card card) {
                mMessageDialogHandler.showMessage(R.string.validationSuccess, mCreditCardView.getContext().getString(R.string.cardValidatedMessage));
                // NOTE : could automatically save, or focus on other field, etc., at this point
                //          as we have a validated Card object
            }

            @Override
            public void onError(int errorCode) {
                showValidationError(errorCode);
            }

            @Override
            public void onClearError() {
                showValidationError(CreditCardView.ERROR_NONE);
            }
        });
    }

    private void showValidationError(int errorCode) {
        if (errorCode == CreditCardView.ERROR_NUMBER) {
            mValidationErrorTextView.setText(R.string.cardErrorNumber);
        } else if (errorCode == CreditCardView.ERROR_EXPIRY_MONTH
                || errorCode == CreditCardView.ERROR_EXPIRY_YEAR) {
            mValidationErrorTextView.setText(R.string.cardErrorExpDate);
        } else if (errorCode == CreditCardView.ERROR_CVC) {
            mValidationErrorTextView.setText(R.string.cardErrorCvc);
        } else if (errorCode == CreditCardView.ERROR_UNKNOWN) {
            mValidationErrorTextView.setText(R.string.cardErrorUnknown);
        } else {
            mValidationErrorTextView.setText("");
        }
    }
}
