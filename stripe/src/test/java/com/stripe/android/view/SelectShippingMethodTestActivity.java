package com.stripe.android.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.stripe.android.R;

/**
 * Activity used to test UI components related to selecting a shipping method
 */
public class SelectShippingMethodTestActivity extends AppCompatActivity {

    private SelectShippingMethodWidget mSelectShippingMethodWidget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.StripeDefaultTheme);
        mSelectShippingMethodWidget = new SelectShippingMethodWidget(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(mSelectShippingMethodWidget);
        setContentView(linearLayout);
    }

    public SelectShippingMethodWidget getSelectShippingMethodWidget() {
        return mSelectShippingMethodWidget;
    }
}
