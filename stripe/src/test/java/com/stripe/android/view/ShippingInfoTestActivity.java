package com.stripe.android.view;

import android.os.Bundle;
import android.widget.LinearLayout;

import com.stripe.android.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity used to test UI components
 */
public class ShippingInfoTestActivity extends AppCompatActivity {

    private ShippingInfoWidget mShippingInfoWidget;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.StripeDefaultTheme);
        mShippingInfoWidget = new ShippingInfoWidget(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(mShippingInfoWidget);
        setContentView(linearLayout);
    }

    public ShippingInfoWidget getShippingInfoWidget() {
        return mShippingInfoWidget;
    }
}
