package com.stripe.android.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.stripe.android.R;
import com.stripe.android.model.ShippingMethod;

import java.util.List;

/**
 * A widget that allows the user to select a shipping method.
 */
public class SelectShippingMethodWidget extends FrameLayout {

    RecyclerView mShippingMethodRecyclerView;
    ShippingMethodAdapter mShippingMethodAdapter;

    public SelectShippingMethodWidget(Context context) {
        super(context);
        initView();
    }

    public SelectShippingMethodWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public SelectShippingMethodWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * @return The {@link ShippingMethod} selected by the customer or {@code null} if no option is
     *  selected.
     */
    public ShippingMethod getSelectedShippingMethod() {
        return mShippingMethodAdapter.getSelectedShippingMethod();
    }

    /**
     * Specify the shipping methods to show.
     */
    public void setShippingMethods(List<ShippingMethod> shippingMethods,
                                   ShippingMethod defaultShippingMethod) {
        mShippingMethodAdapter.setShippingMethods(shippingMethods, defaultShippingMethod);
    }

    private void initView() {
        inflate(getContext(), R.layout.select_shipping_method_widget, this);
        mShippingMethodRecyclerView = findViewById(R.id.rv_shipping_methods_ssmw);
        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mShippingMethodAdapter = new ShippingMethodAdapter();
        mShippingMethodRecyclerView.setHasFixedSize(true);
        mShippingMethodRecyclerView.setAdapter(mShippingMethodAdapter);
        mShippingMethodRecyclerView.setLayoutManager(linearLayoutManager);
    }

}
