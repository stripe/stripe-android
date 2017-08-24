package com.stripe.android.view;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;

public class SelectShippingMethodActivity extends StripeActivity {

    SelectShippingMethodWidget mSelectShippingMethodWidget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_select_shipping_method);
        mViewStub.inflate();
        setTitle(getString(R.string.title_select_shipping_method));
        mSelectShippingMethodWidget = findViewById(R.id.select_shipping_method_widget);
        mSelectShippingMethodWidget.setShippingMethods(PaymentConfiguration.getInstance().getShippingMethods());
    }

    @Override
    protected void onActionSave() {

    }
}
