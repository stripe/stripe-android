package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.R;
import com.stripe.android.model.Address;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that can take accept an address. Uses {@link AddAddressWidget}
 */
public class AddAddressActivity extends StripeActivity {

    static final String EXTRA_NEW_ADDRESS = "new_adddress";
    static final String EXTRA_OPTIONAL_FIELDS = "optional_fields";
    static final String EXTRA_HIDDEN_FIELDS = "hidden_fields";
    static final String EXTRA_PREPOPULATED_ADDRESS = "prepopulate_address";

    private AddAddressWidget mAddAddressWidget;

    public static Intent newIntent(@NonNull Context context) {
        return newIntent(context, null, null, null);
    }

    public static Intent newIntent(
            @NonNull Context context,
            @Nullable ArrayList<String> optionalFields,
            @Nullable ArrayList<String> hiddenFields,
            @Nullable Address prepopulatedAddress) {
        Intent intent = new Intent(context, AddAddressActivity.class);
        intent.putStringArrayListExtra(EXTRA_OPTIONAL_FIELDS, optionalFields);
        intent.putStringArrayListExtra(EXTRA_HIDDEN_FIELDS, hiddenFields);
        intent.putExtra(EXTRA_PREPOPULATED_ADDRESS, prepopulatedAddress);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_add_address);
        mViewStub.inflate();
        mAddAddressWidget = findViewById(R.id.add_address_widget);
        List<String> optionalFields = getIntent().getStringArrayListExtra(EXTRA_OPTIONAL_FIELDS);
        mAddAddressWidget.setOptionalFields(optionalFields);
        List<String> hiddenFields = getIntent().getStringArrayListExtra(EXTRA_HIDDEN_FIELDS);
        mAddAddressWidget.setHiddenFields(hiddenFields);
        Address address = getIntent().getParcelableExtra(EXTRA_PREPOPULATED_ADDRESS);
        mAddAddressWidget.populateAddress(address);
        setTitle(R.string.title_add_an_address);
    }

    @Override
    protected void onActionSave() {
        Address address = mAddAddressWidget.getAddress();
        if (address == null) {
            // In this case, the error will be displayed in the address widget
            return;
        }
        setCommunicatingProgress(false);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NEW_ADDRESS, address);
        setResult(RESULT_OK, intent);
        finish();
    }
}

