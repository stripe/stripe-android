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
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.R;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import java.util.List;

import static com.stripe.android.CustomerSession.EVENT_SHIPPING_INFO_SAVED;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_CONFIG;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_DATA_KEY;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;

/**
 * Activity containing a two-part payment flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
public class PaymentFlowActivity extends StripeActivity {

    public static final String EXTRA_DEFAULT_SHIPPING_METHOD = "default_shipping_method";
    public static final String EXTRA_IS_SHIPPING_INFO_VALID = "shipping_is_shipping_info_valid";
    public static final String EXTRA_SHIPPING_INFO_DATA = "shipping_info_data";
    public static final String EXTRA_SHIPPING_INFO_ERROR = "shipping_info_error";
    public static final String EVENT_SHIPPING_INFO_PROCESSED = "shipping_info_processed";
    public static final String EVENT_SHIPPING_INFO_SUBMITTED = "shipping_info_submitted";
    public static final String EXTRA_VALID_SHIPPING_METHODS = "valid_shipping_methods";

    static final String TOKEN_PAYMENT_FLOW_ACTIVITY = "PaymentFlowActivity";

    private BroadcastReceiver mShippingInfoSavedBroadcastReceiver;
    private BroadcastReceiver mShippingInfoSubmittedBroadcastReceiver;
    private PaymentFlowPagerAdapter mPaymentFlowPagerAdapter;
    private ViewPager mViewPager;
    private PaymentSessionData mPaymentSessionData;
    private ShippingInformation mShippingInformationSubmitted;
    private List<ShippingMethod> mValidShippingMethods;
    private ShippingMethod mDefaultShippingMethod;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_FLOW_ACTIVITY);
        mViewStub.setLayoutResource(R.layout.activity_shipping_flow);
        mViewStub.inflate();
        mViewPager = findViewById(R.id.shipping_flow_viewpager);
        final PaymentSessionConfig paymentSessionConfig =
                getIntent().getParcelableExtra(PAYMENT_SESSION_CONFIG);
        mPaymentSessionData = getIntent().getParcelableExtra(PAYMENT_SESSION_DATA_KEY);

        if (mPaymentSessionData == null) {
            throw new IllegalArgumentException(
                    "PaymentFlowActivity launched without PaymentSessionData");
        }

        mPaymentFlowPagerAdapter = new PaymentFlowPagerAdapter(this, paymentSessionConfig);
        mViewPager.setAdapter(mPaymentFlowPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                setTitle(mViewPager.getAdapter().getPageTitle(i));
                if (mPaymentFlowPagerAdapter.getPageAt(i) == PaymentFlowPagerEnum.SHIPPING_INFO) {
                    mPaymentFlowPagerAdapter.hideShippingPage();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        mShippingInfoSubmittedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isShippingInfoValid = intent.getBooleanExtra(
                        EXTRA_IS_SHIPPING_INFO_VALID,
                        false);
                if (isShippingInfoValid) {
                    onShippingInfoValidated();
                    mValidShippingMethods =
                            intent.getParcelableArrayListExtra(EXTRA_VALID_SHIPPING_METHODS);
                    mDefaultShippingMethod =
                            intent.getParcelableExtra(EXTRA_DEFAULT_SHIPPING_METHOD);
                } else {
                    setCommunicatingProgress(false);
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
        mShippingInfoSavedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onShippingMethodsReady(mValidShippingMethods, mDefaultShippingMethod);
                mPaymentSessionData.setShippingInformation(mShippingInformationSubmitted);
            }
        };
        setTitle(mPaymentFlowPagerAdapter.getPageTitle(mViewPager.getCurrentItem()));
    }

    @Override
    protected void onActionSave() {
        if (mPaymentFlowPagerAdapter.getPageAt(
                mViewPager.getCurrentItem()).equals(PaymentFlowPagerEnum.SHIPPING_INFO)) {
            onShippingInfoSubmitted();
        } else {
            onShippingMethodSave();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mShippingInfoSubmittedBroadcastReceiver);
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mShippingInfoSavedBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mShippingInfoSubmittedBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_PROCESSED));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mShippingInfoSavedBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_SAVED));
    }

    private void onShippingInfoValidated() {
        CustomerSession.getInstance().setCustomerShippingInformation(
                this,
                mShippingInformationSubmitted);
    }

    private void onShippingMethodsReady(
            @NonNull List<ShippingMethod> validShippingMethods,
            @Nullable ShippingMethod defaultShippingMethod) {
        setCommunicatingProgress(false);
        mPaymentFlowPagerAdapter.setShippingMethods(validShippingMethods, defaultShippingMethod);
        mPaymentFlowPagerAdapter.setShippingInfoSaved(true);
        if (hasNextPage()) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        } else {
            mPaymentSessionData.setShippingInformation(mShippingInformationSubmitted);
            Intent intent = new Intent();
            intent.putExtra(PAYMENT_SESSION_DATA_KEY, mPaymentSessionData);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void onShippingInfoSubmitted() {
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
        SelectShippingMethodWidget selectShippingMethodWidget =
                findViewById(R.id.select_shipping_method_widget);
        ShippingMethod shippingMethod = selectShippingMethodWidget.getSelectedShippingMethod();
        mPaymentSessionData.setShippingMethod(shippingMethod);
        Intent intent = new Intent();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, mPaymentSessionData);
        setResult(RESULT_OK, intent);
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
