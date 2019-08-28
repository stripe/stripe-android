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
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import java.util.List;

import static com.stripe.android.CustomerSession.EVENT_SHIPPING_INFO_SAVED;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_DATA_KEY;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;

/**
 * Activity containing a two-part payment flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
public class PaymentFlowActivity extends StripeActivity {
    public static final String TOKEN_PAYMENT_FLOW_ACTIVITY = "PaymentFlowActivity";
    public static final String TOKEN_SHIPPING_INFO_SCREEN = "ShippingInfoScreen";
    public static final String TOKEN_SHIPPING_METHOD_SCREEN = "ShippingMethodScreen";

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

        final PaymentFlowActivityStarter.Args args =
                PaymentFlowActivityStarter.Args.create(getIntent());

        CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_FLOW_ACTIVITY);
        getViewStub().setLayoutResource(R.layout.activity_shipping_flow);
        getViewStub().inflate();
        mViewPager = findViewById(R.id.shipping_flow_viewpager);
        mPaymentSessionData = args.paymentSessionData;

        if (mPaymentSessionData == null) {
            throw new IllegalArgumentException(
                    "PaymentFlowActivity launched without PaymentSessionData");
        }

        mPaymentFlowPagerAdapter =
                new PaymentFlowPagerAdapter(this, args.paymentSessionConfig);
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
                final boolean isShippingInfoValid = intent.getBooleanExtra(
                        PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID,
                        false);
                if (isShippingInfoValid) {
                    onShippingInfoValidated();
                    mValidShippingMethods = intent.getParcelableArrayListExtra(
                            PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS);
                    mDefaultShippingMethod = intent
                            .getParcelableExtra(PaymentFlowExtras.EXTRA_DEFAULT_SHIPPING_METHOD);
                } else {
                    setCommunicatingProgress(false);
                    final String shippingInfoError = intent
                            .getStringExtra(PaymentFlowExtras.EXTRA_SHIPPING_INFO_ERROR);
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
        if (PaymentFlowPagerEnum.SHIPPING_INFO
                .equals(mPaymentFlowPagerAdapter.getPageAt(mViewPager.getCurrentItem()))) {
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
                new IntentFilter(PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mShippingInfoSavedBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_SAVED));
    }

    private void onShippingInfoValidated() {
        CustomerSession.getInstance().setCustomerShippingInformation(
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
            setResult(RESULT_OK,
                    new Intent().putExtra(PAYMENT_SESSION_DATA_KEY, mPaymentSessionData));
            finish();
        }
    }

    private void onShippingInfoSubmitted() {
        final ShippingInfoWidget shippingInfoWidget = findViewById(R.id.shipping_info_widget);
        final ShippingInformation shippingInformation = shippingInfoWidget.getShippingInformation();
        if (shippingInformation != null) {
            mShippingInformationSubmitted = shippingInformation;
            setCommunicatingProgress(true);
            broadcastShippingInfoSubmitted(shippingInformation);
        }
    }

    private void broadcastShippingInfoSubmitted(ShippingInformation shippingInformation) {
        LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(
                        new Intent(PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED)
                                .putExtra(PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA,
                                        shippingInformation));
    }

    private boolean hasNextPage() {
        return mViewPager.getCurrentItem() + 1 < mPaymentFlowPagerAdapter.getCount();
    }

    private boolean hasPreviousPage() {
        int currentPageIndex = mViewPager.getCurrentItem();
        return currentPageIndex != 0;
    }

    private void onShippingMethodSave() {
        final SelectShippingMethodWidget selectShippingMethodWidget =
                findViewById(R.id.select_shipping_method_widget);
        final ShippingMethod shippingMethod = selectShippingMethodWidget
                .getSelectedShippingMethod();
        mPaymentSessionData.setShippingMethod(shippingMethod);
        setResult(RESULT_OK,
                new Intent().putExtra(PAYMENT_SESSION_DATA_KEY, mPaymentSessionData));
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
