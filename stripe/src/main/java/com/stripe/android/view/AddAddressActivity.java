package com.stripe.android.view;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.stripe.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that can take accept an address. Uses {@link AddAddressWidget}
 */
public class AddAddressActivity extends StripeActivity {

    private AddAddressWidget mAddAddressWidget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_add_address);
        mViewStub.inflate();
        mAddAddressWidget = findViewById(R.id.add_address_widget);
        List<String> optionalFields = new ArrayList<>();
        optionalFields.add(AddAddressWidget.ADDRESS_LINE_TWO_FIELD);
        optionalFields.add(AddAddressWidget.POSTAL_CODE_FIELD);
        mAddAddressWidget.setOptionalFields(optionalFields);
        List<String> hiddenFields = new ArrayList<>();
        hiddenFields.add(AddAddressWidget.NAME_FIELD);
        mAddAddressWidget.setHiddenFields(hiddenFields);
        setTitle(R.string.title_add_an_address);
    }

    @Override
    protected void onActionSave() {
        mAddAddressWidget.validateAllFields();
    }
}

