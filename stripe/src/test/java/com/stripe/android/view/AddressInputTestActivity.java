package com.stripe.android.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.stripe.android.R;

/**
 * Activity used to test UI components
 */
public class AddressInputTestActivity extends AppCompatActivity {

    private AddAddressWidget mAddAddressWidget;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.StripeDefaultTheme);
        mAddAddressWidget = new AddAddressWidget(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(mAddAddressWidget);
        setContentView(linearLayout);
    }

    public AddAddressWidget getAddAddressWidget() {
        return mAddAddressWidget;
    }
}
