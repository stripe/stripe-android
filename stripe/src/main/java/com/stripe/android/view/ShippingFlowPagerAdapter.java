package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;

import java.util.ArrayList;
import java.util.List;

class ShippingFlowPagerAdapter extends PagerAdapter {

    @NonNull private Context mContext;
    @NonNull private ShippingFlowConfig mShippingFlowConfig;
    @NonNull private List<ShippingFlowPagerEnum> mPages;
    private boolean mAddressSaved;

    ShippingFlowPagerAdapter(@NonNull Context context, @NonNull ShippingFlowConfig shippingFlowConfig) {
        mContext = context;
        mShippingFlowConfig = shippingFlowConfig;
        mPages = new ArrayList<>();
        if (!mShippingFlowConfig.isHideAddressScreen()) {
            mPages.add(ShippingFlowPagerEnum.ADDRESS);
        }
        if (!shouldHideShippingScreen()) {
            mPages.add(ShippingFlowPagerEnum.SHIPPING_METHOD);
        }
    }

    private boolean shouldHideShippingScreen() {
        return mShippingFlowConfig.isHideShippingScreen() || (!mShippingFlowConfig.isHideAddressScreen() && !mAddressSaved);
    }

    void setAddressSaved(boolean addressSaved) {
        mAddressSaved = addressSaved;
        if (!shouldHideShippingScreen()) {
            mPages.add(ShippingFlowPagerEnum.SHIPPING_METHOD);
        }
        notifyDataSetChanged();
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        ShippingFlowPagerEnum shippingFlowPagerEnum = mPages.get(position);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(shippingFlowPagerEnum.getLayoutResId(), collection, false);
        if (shippingFlowPagerEnum.equals(ShippingFlowPagerEnum.SHIPPING_METHOD)) {
            SelectShippingMethodWidget selectShippingMethodWidget = layout.findViewById(R.id.select_shipping_method_widget);
            selectShippingMethodWidget.setShippingMethods(PaymentConfiguration.getInstance().getShippingMethods());
        }
        if (shippingFlowPagerEnum.equals(ShippingFlowPagerEnum.ADDRESS)) {
            ShippingInfoWidget shippingInfoWidget = layout.findViewById(R.id.set_shipping_info_widget);
            shippingInfoWidget.setHiddenFields(mShippingFlowConfig.getHiddenAddressFields());
            shippingInfoWidget.setOptionalFields(mShippingFlowConfig.getOptionalAddressFields());
            shippingInfoWidget.populateShippingInfo(mShippingFlowConfig.getPrepopulatedShippingInfo());
        }

        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Nullable ShippingFlowPagerEnum getPageAt(int position) {
        if (position < mPages.size()) {
            return mPages.get(position);
        }
        return null;
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == o;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getString(mPages.get(position).getTitleResId());
    }
}
