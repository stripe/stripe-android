package com.stripe.android.view;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.stripe.android.R;

public class AddAddressActivity extends StripeActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_add_address);
        mViewStub.inflate();
        setTitle(R.string.title_add_an_address);
    }

    @Override
    protected void onActionSave() {

    }
}

