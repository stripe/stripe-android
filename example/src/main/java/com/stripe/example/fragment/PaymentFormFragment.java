package com.stripe.example.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import com.stripe.example.PaymentForm;
import com.stripe.example.R;
import com.stripe.example.activity.PaymentActivity;

public class PaymentFormFragment extends Fragment implements PaymentForm {

    Button saveButton;
    EditText cardNumber;
    EditText cvc;
    Spinner monthSpinner;
    Spinner yearSpinner;
    Spinner currencySpinner;
    private static final String CURRENCY_UNSPECIFIED = "Unspecified";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.payment_form_fragment, container, false);

        this.saveButton = (Button) view.findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveForm(view);
            }
        });

        this.cardNumber = (EditText) view.findViewById(R.id.number);
        this.cvc = (EditText) view.findViewById(R.id.cvc);
        this.monthSpinner = (Spinner) view.findViewById(R.id.expMonth);
        this.yearSpinner = (Spinner) view.findViewById(R.id.expYear);
        this.currencySpinner = (Spinner) view.findViewById(R.id.currency);

        return view;
    }

    @Override
    public String getCardNumber() {
        return this.cardNumber.getText().toString();
    }

    @Override
    public String getCvc() {
        return this.cvc.getText().toString();
    }

    @Override
    public Integer getExpMonth() {
        return getInteger(this.monthSpinner);
    }

    @Override
    public Integer getExpYear() {
        return getInteger(this.yearSpinner);
    }

    @Override
    public String getCurrency() {
        if (currencySpinner.getSelectedItemPosition() == 0) return null;
        String selected = (String) currencySpinner.getSelectedItem();
        if (selected.equals(CURRENCY_UNSPECIFIED))
            return null;
        else
            return selected.toLowerCase();
    }

    public void saveForm(View button) {
        ((PaymentActivity)getActivity()).saveCreditCard(this);
    }

    private Integer getInteger(Spinner spinner) {
    	try {
    		return Integer.parseInt(spinner.getSelectedItem().toString());
    	} catch (NumberFormatException e) {
    		return 0;
    	}
    }
}
