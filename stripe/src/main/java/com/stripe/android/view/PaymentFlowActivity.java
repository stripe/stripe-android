package com.stripe.android.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.R;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import java.util.List;

import static com.stripe.android.PaymentSession.PAYMENT_SESSION_DATA_KEY;

/**
 * Activity containing a two-part payment flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
public class PaymentFlowActivity extends StripeActivity {

    public static final String EXTRA_DEFAULT_SHIPPING_METHOD = "default_shipping_method";
    public static final String EXTRA_IS_SHIPPING_INFO_VALID = "shipping_is_shipping_info_valid";
    public static final String EXTRA_PAYMENT_FLOW_CONFIG = "payment_flow_config";
    public static final String EXTRA_SHIPPING_INFO_DATA = "shipping_info_data";
    public static final String EXTRA_SHIPPING_INFO_ERROR = "shipping_info_error";
    public static final String EVENT_SHIPPING_INFO_PROCESSED = "shipping_info_processed";
    public static final String EVENT_SHIPPING_INFO_SUBMITTED = "shipping_info_submitted";
    public static final String EXTRA_VALID_SHIPPING_METHODS = "valid_shipping_methods";

    private BroadcastReceiver mAlertBroadcastReceiver;
    private BroadcastReceiver mBroadcastReceiver;
    private PaymentFlowPagerAdapter mPaymentFlowPagerAdapter;
    private ViewPager mViewPager;
    private PaymentSessionData mPaymentSessionData;
    private ShippingInformation mShippingInformationSubmitted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_shipping_flow);
        mViewStub.inflate();
        mViewPager = findViewById(R.id.shipping_flow_viewpager);
        final PaymentFlowConfig paymentFlowConfig = getIntent().getParcelableExtra(EXTRA_PAYMENT_FLOW_CONFIG);
        mPaymentSessionData = getIntent().getParcelableExtra(PAYMENT_SESSION_DATA_KEY);
        mPaymentFlowPagerAdapter = new PaymentFlowPagerAdapter(this, paymentFlowConfig);
        mViewPager.setAdapter(mPaymentFlowPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                setTitle(mViewPager.getAdapter().getPageTitle(i));
                if (mPaymentFlowPagerAdapter.getPageAt(i) == PaymentFlowPagerEnum.ADDRESS) {
                    mPaymentFlowPagerAdapter.hideShippingPage();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }

        });
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setCommunicatingProgress(false);
                boolean isShippingInfoValid = intent.getBooleanExtra(EXTRA_IS_SHIPPING_INFO_VALID, false);
                if (isShippingInfoValid) {
                    List<ShippingMethod> validShippingMethods = intent.getParcelableArrayListExtra(EXTRA_VALID_SHIPPING_METHODS);
                    ShippingMethod shippingMethod = intent.getParcelableExtra(EXTRA_DEFAULT_SHIPPING_METHOD);
                    onShippingMethodsReady(validShippingMethods, shippingMethod);
                    mPaymentSessionData.setShippingInformation(mShippingInformationSubmitted);
                } else {
                    String shippingInfoError = intent.getStringExtra(EXTRA_SHIPPING_INFO_ERROR);
                    if (shippingInfoError != null && !shippingInfoError.isEmpty()) {
                        showError(shippingInfoError);
                    } else {
                        showError(getString(R.string.invalid_shipping_information));
                    }
                    mShippingInformationSubmitted = null;
                }
            }
        };
        setTitle(mPaymentFlowPagerAdapter.getPageTitle(mViewPager.getCurrentItem()));

        mAlertBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                StripeException exception = (StripeException)
                        intent.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
                alertUser(exception);
            }
        };
    }

    @Override
    protected void onActionSave() {
        if (mPaymentFlowPagerAdapter.getPageAt(mViewPager.getCurrentItem()).equals(PaymentFlowPagerEnum.ADDRESS)) {
            onAddressSave();
        } else {
            onShippingMethodSave();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAlertBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mAlertBroadcastReceiver,
                        new IntentFilter(CustomerSession.ACTION_API_EXCEPTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_PROCESSED));
    }

    private void onShippingMethodsReady(
            @NonNull List<ShippingMethod> validShippingMethods,
            @Nullable ShippingMethod defaultShippingMethod) {
        mPaymentFlowPagerAdapter.setShippingMethods(validShippingMethods, defaultShippingMethod);
        mPaymentFlowPagerAdapter.setAddressSaved(true);
        if (hasNextPage()) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        } else {
            finish();
        }
    }

    private void onAddressSave() {
        ShippingInfoWidget shippingInfoWidget = findViewById(R.id.shipping_info_widget);
        ShippingInformation shippingInformation = shippingInfoWidget.getShippingInformation();
        if (shippingInformation !=  null) {
            mShippingInformationSubmitted = shippingInformation;
            setCommunicatingProgress(true);
            broadcastShippingInfoSubmitted(shippingInformation);
        }
    }

    private void broadcastShippingInfoSubmitted(ShippingInformation shippingInformation) {
        Intent intent = new Intent(EVENT_SHIPPING_INFO_SUBMITTED);
        intent.putExtra(EXTRA_SHIPPING_INFO_DATA, shippingInformation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private boolean hasNextPage() {
        return mViewPager.getCurrentItem() + 1 < mPaymentFlowPagerAdapter.getCount();
    }

    private boolean hasPreviousPage() {
        int currentPageIndex = mViewPager.getCurrentItem();
        return currentPageIndex != 0;
    }

    private void onShippingMethodSave() {
        SelectShippingMethodWidget selectShippingMethodWidget = findViewById(R.id.select_shipping_method_widget);
        setCommunicatingProgress(true);
        ShippingMethod shippingMethod = selectShippingMethodWidget.getSelectedShippingMethod();
        mPaymentSessionData.setShippingMethod(shippingMethod);
        Intent intent = new Intent();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, mPaymentSessionData);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void alertUser(@Nullable StripeException exception) {
        if (exception == null) {
            return;
        }

        showError(exception.getLocalizedMessage());
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
