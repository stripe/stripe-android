package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    @NonNull final ShippingMethodAdapter mShippingMethodAdapter;

    public SelectShippingMethodWidget(@NonNull Context context) {
        this(context, null);
    }

    public SelectShippingMethodWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectShippingMethodWidget(@NonNull Context context, @Nullable AttributeSet attrs,
                                      int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(getContext(), R.layout.select_shipping_method_widget, this);
        mShippingMethodAdapter = new ShippingMethodAdapter();

        final RecyclerView shippingMethodRecyclerView = findViewById(R.id.rv_shipping_methods_ssmw);
        shippingMethodRecyclerView.setHasFixedSize(true);
        shippingMethodRecyclerView.setAdapter(mShippingMethodAdapter);
        shippingMethodRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * @return The {@link ShippingMethod} selected by the customer or {@code null} if no option is
     *  selected.
     */
    @Nullable
    public ShippingMethod getSelectedShippingMethod() {
        return mShippingMethodAdapter.getSelectedShippingMethod();
    }

    /**
     * Specify the shipping methods to show.
     */
    public void setShippingMethods(@Nullable List<ShippingMethod> shippingMethods,
                                   @Nullable ShippingMethod defaultShippingMethod) {
        mShippingMethodAdapter.setShippingMethods(shippingMethods, defaultShippingMethod);
    }
}
