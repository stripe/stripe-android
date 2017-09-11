package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;

import com.stripe.android.R;
import com.stripe.android.model.Address;
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

    static final String EXTRA_SHIPPING_FLOW_CONFIG = "shipping_flow_config";

    public static class IntentBuilder {
        private List mHiddenAddressFields;
        private List mOptionalAddressFields;
        private Address mPrepopulatedAddress;
        private boolean mHideAddressScreen;
        private boolean mHideShippingScreen;

        public IntentBuilder() {
            mHiddenAddressFields = new ArrayList();
            mOptionalAddressFields = new ArrayList();
            mPrepopulatedAddress = new Address.Builder().build();
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
         * @param prepopulatedAddress set an address to be prepopulated into the add address input
         *                            fields.
         */
        public IntentBuilder setPrepopulatedAddress(@NonNull Address prepopulatedAddress) {
            mPrepopulatedAddress = prepopulatedAddress;
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
                            mPrepopulatedAddress,
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
    }


    @Override
    protected void onActionSave() {
        if (mShippingFlowPagerAdapter.getPageAt(mViewPager.getCurrentItem()).equals(ShippingFlowPagerEnum.ADDRESS)) {
            onAddressSave();
        } else {
            onShippingMethodSave();
        }
    }

    private void onAddressSave() {
        AddAddressWidget addAddressWidget = findViewById(R.id.add_address_widget);
        Address address = addAddressWidget.getAddress();
        if (address !=  null) {
            setCommunicatingProgress(true);
            // TODO: Call into payment context
            setCommunicatingProgress(false);
            mShippingFlowPagerAdapter.setAddressSaved(true);
            if (hasNextPage()) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
            } else {
                finish();
            }
        }
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
        // TODO: Call into payment context and save shipping method
        finish();
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
