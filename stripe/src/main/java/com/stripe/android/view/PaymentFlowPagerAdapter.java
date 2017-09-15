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

class PaymentFlowPagerAdapter extends PagerAdapter {

    @NonNull private Context mContext;
    @NonNull private PaymentFlowConfig mPaymentFlowConfig;
    @NonNull private List<PaymentFlowPagerEnum> mPages;
    private boolean mAddressSaved;

    PaymentFlowPagerAdapter(@NonNull Context context, @NonNull PaymentFlowConfig paymentFlowConfig) {
        mContext = context;
        mPaymentFlowConfig = paymentFlowConfig;
        mPages = new ArrayList<>();
        if (!mPaymentFlowConfig.isHideShippingInfoScreen()) {
            mPages.add(PaymentFlowPagerEnum.ADDRESS);
        }
        if (!shouldHideShippingScreen()) {
            mPages.add(PaymentFlowPagerEnum.SHIPPING_METHOD);
        }
    }

    private boolean shouldHideShippingScreen() {
        return mPaymentFlowConfig.isHideShippingMethodsScreen() || (!mPaymentFlowConfig.isHideShippingInfoScreen() && !mAddressSaved);
    }

    void setAddressSaved(boolean addressSaved) {
        mAddressSaved = addressSaved;
        if (!shouldHideShippingScreen()) {
            mPages.add(PaymentFlowPagerEnum.SHIPPING_METHOD);
        }
        notifyDataSetChanged();
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        PaymentFlowPagerEnum paymentFlowPagerEnum = mPages.get(position);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(paymentFlowPagerEnum.getLayoutResId(), collection, false);
        if (paymentFlowPagerEnum.equals(PaymentFlowPagerEnum.SHIPPING_METHOD)) {
            SelectShippingMethodWidget selectShippingMethodWidget = layout.findViewById(R.id.select_shipping_method_widget);
            selectShippingMethodWidget.setShippingMethods(PaymentConfiguration.getInstance().getShippingMethods());
        }
        if (paymentFlowPagerEnum.equals(PaymentFlowPagerEnum.ADDRESS)) {
            ShippingInfoWidget shippingInfoWidget = layout.findViewById(R.id.shipping_info_widget);
            shippingInfoWidget.setHiddenFields(mPaymentFlowConfig.getHiddenShippingInfoFields());
            shippingInfoWidget.setOptionalFields(mPaymentFlowConfig.getOptionalShippingInfoFields());
            shippingInfoWidget.populateShippingInfo(mPaymentFlowConfig.getPrepopulatedShippingInfo());
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

    @Nullable
    PaymentFlowPagerEnum getPageAt(int position) {
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
