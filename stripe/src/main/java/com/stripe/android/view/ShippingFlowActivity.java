package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;

import com.stripe.android.CustomerSession;
import com.stripe.android.R;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity containing a two-part shipping flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
public class ShippingFlowActivity extends StripeActivity {

    private ViewPager mViewPager;
    private ShippingFlowPagerAdapter mShippingFlowPagerAdapter;
    public static final String SHIPPING_INFO_SUBMITTED_EVENT = "shipping_info_submitted_event";
    public static final String SHIPPING_INFO_DATA = "shipping_info_data";


    static final String EXTRA_SHIPPING_FLOW_CONFIG = "shipping_flow_config";

    public static class IntentBuilder {
        private List mHiddenAddressFields;
        private List mOptionalAddressFields;
        private ShippingInformation mPrepopulatedShippingInfo;
        private boolean mHideAddressScreen;
        private boolean mHideShippingScreen;

        public IntentBuilder() {
            mHiddenAddressFields = new ArrayList();
            mOptionalAddressFields = new ArrayList();
            mPrepopulatedShippingInfo = new ShippingInformation();
        }

        /**
         * @param hiddenAddressFields sets address fields that should be hidden on the address
         *                            screen. Hidden fields are automatically optional.
         */
        public IntentBuilder setHiddenAddressFields(@NonNull List hiddenAddressFields) {
            mHiddenAddressFields = hiddenAddressFields;
            return this;
        }

        /**
         * @param optionalAddressFields sets address fields that should be optional.
         */
        public IntentBuilder setOptionalAddressFields(@NonNull List optionalAddressFields) {
            mOptionalAddressFields = optionalAddressFields;
            return this;
        }

        /**
         *
         * @param prepopulatedShippingInfo set an address to be prepopulated into the add address input
         *                            fields.
         */
        public IntentBuilder setPrepopulatedShippingInfo(@NonNull ShippingInformation prepopulatedShippingInfo) {
            mPrepopulatedShippingInfo = prepopulatedShippingInfo;
            return this;
        }

        /**
         * Sets the add shipping address screen to be skipped.
         */
        public IntentBuilder setHideAddressScreen(boolean isHideAddressScreen) {
            mHideAddressScreen = isHideAddressScreen;
            return this;
        }

        /**
         * Sets the select shipping method screen to be skipped.
         */
        public IntentBuilder setHideShippingScreen(boolean isHideShippingScreen) {
            mHideShippingScreen = isHideShippingScreen;
            return this;
        }

        public Intent build(Context context) {
            Intent intent = new Intent(context, ShippingFlowActivity.class);
            ShippingFlowConfig shippingFlowConfig =
                    new ShippingFlowConfig(
                            mHiddenAddressFields,
                            mOptionalAddressFields,
                            mPrepopulatedShippingInfo,
                            mHideAddressScreen,
                            mHideShippingScreen);
            intent.putExtra(EXTRA_SHIPPING_FLOW_CONFIG, shippingFlowConfig);
            return intent;
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_shipping_flow);
        mViewStub.inflate();
        mViewPager = findViewById(R.id.shipping_flow_viewpager);
        ShippingFlowConfig shippingFlowConfig = getIntent().getParcelableExtra(EXTRA_SHIPPING_FLOW_CONFIG);
        mShippingFlowPagerAdapter = new ShippingFlowPagerAdapter(this, shippingFlowConfig);
        mViewPager.setAdapter(mShippingFlowPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                setTitle(mViewPager.getAdapter().getPageTitle(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        setTitle(mShippingFlowPagerAdapter.getPageTitle(mViewPager.getCurrentItem()));
        CustomerSession.getInstance().setShippingFlowUpdateListener(new CustomerSession.ShippingFlowUpdateListener() {
            @Override
            public void onError(@NonNull int errorCode, @Nullable String errorMessage) {
                if (!StringUtils.isNullOrEmpty(errorMessage)) {
                    showError(errorMessage);
                }
            }
        });
    }


    @Override
    protected void onActionSave() {
        if (mShippingFlowPagerAdapter.getPageAt(mViewPager.getCurrentItem()).equals(ShippingFlowPagerEnum.ADDRESS)) {
            onShippingInfoSubmitted();
        } else {
            onShippingMethodSave();
        }
    }

    private void onShippingInfoSubmitted() {
        setCommunicatingProgress(true);
        ShippingInfoWidget shippingInfoWidget = findViewById(R.id.set_shipping_info_widget);
        ShippingInformation shippingInformation = shippingInfoWidget.getShippingInformation();
        Intent intent = new Intent(SHIPPING_INFO_SUBMITTED_EVENT);
        intent.putExtra(SHIPPING_INFO_DATA, shippingInformation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private boolean hasNextPage() {
        return mViewPager.getCurrentItem() + 1 < mShippingFlowPagerAdapter.getCount();
    }

    private boolean hasPreviousPage() {
        int currentPageIndex = mViewPager.getCurrentItem();
        return currentPageIndex != 0 && currentPageIndex >= mShippingFlowPagerAdapter.getCount();
    }


    private void onShippingMethodSave() {
        SelectShippingMethodWidget selectShippingMethodWidget = findViewById(R.id.select_shipping_method_widget);
        setCommunicatingProgress(true);
        ShippingMethod shippingMethod = selectShippingMethodWidget.getSelectedShippingMethod();
        finish();
    }

    private void onShippingInfoSaved() {
        setCommunicatingProgress(false);
        mShippingFlowPagerAdapter.setAddressSaved(true);
        if (hasNextPage()) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (hasPreviousPage()) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            return;
        }
        super.onBackPressed();
    }
}
