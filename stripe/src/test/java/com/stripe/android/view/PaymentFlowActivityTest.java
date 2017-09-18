package com.stripe.android.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.stripe.android.BuildConfig;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.exception.APIException;
import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.List;

import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EXTRA_EXCEPTION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PaymentFlowActivityTest {

    private ActivityController<PaymentFlowActivity> mActivityController;
    private ShadowActivity mShadowActivity;
    private ShippingInfoWidget mShippingInfoWidget;

    @Test
    public void launchPaymentFlowActivity_withHideShippingInfoConfig_hidesShippingInfoView() {
        initializePaymentConfig();
        Intent intent = new Intent();
        PaymentFlowConfig paymentFlowConfig = new PaymentFlowConfig.Builder()
                .setHideShippingInfoScreen(true)
                .build();
        intent.putExtra(PaymentFlowActivity.EXTRA_PAYMENT_FLOW_CONFIG, paymentFlowConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        assertNull(mActivityController.get().findViewById(R.id.shipping_info_widget));
        assertNotNull(mActivityController.get().findViewById(R.id.select_shipping_method_widget));
    }

    @Test
    public void onShippingInfoSave_whenShippingNotPopulated_doesNotFinish() {
        Intent intent = new Intent();
        PaymentFlowConfig paymentFlowConfig = new PaymentFlowConfig.Builder()
                .build();
        intent.putExtra(PaymentFlowActivity.EXTRA_PAYMENT_FLOW_CONFIG, paymentFlowConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        PaymentFlowActivity paymentFlowActivity = mActivityController.get();
        paymentFlowActivity.onActionSave();
        assertFalse(mShadowActivity.isFinishing());
    }

    @Test
    public void onShippingInfoSave_whenShippingInfoNotPopulated_doesNotContinue() {
        Intent intent = new Intent();
        PaymentFlowConfig paymentFlowConfig = new PaymentFlowConfig.Builder()
                .build();
        intent.putExtra(PaymentFlowActivity.EXTRA_PAYMENT_FLOW_CONFIG, paymentFlowConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        PaymentFlowActivity paymentFlowActivity = mActivityController.get();
        paymentFlowActivity.onActionSave();
        assertFalse(mShadowActivity.isFinishing());
        assertNotNull(mActivityController.get().findViewById(R.id.shipping_info_widget));
    }

    @Test
    public void onErrorBroadcast_displaysAlertDialog() {
        Intent intent = new Intent();
        PaymentFlowConfig paymentFlowConfig = new PaymentFlowConfig.Builder()
                .build();
        intent.putExtra(PaymentFlowActivity.EXTRA_PAYMENT_FLOW_CONFIG, paymentFlowConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();

        PaymentFlowActivity.AlertMessageListener alertMessageListener =
                mock(PaymentFlowActivity.AlertMessageListener.class);
        mActivityController.get().setAlertMessageListener(alertMessageListener);

        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, new APIException("Something's wrong", "ID123", 400, null));
        Intent errorIntent = new Intent(ACTION_API_EXCEPTION);
        errorIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(mActivityController.get())
                .sendBroadcast(errorIntent);

        verify(alertMessageListener).onUserAlert("Something's wrong");
    }

    private void initializePaymentConfig() {
        List<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("UPS Ground", "ups-ground", "Arrives in 3-5 days", 0, "USD"));
        shippingMethods.add(new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD"));
        PaymentConfiguration.init("FAKE PUBLISHABLE KEY");
        PaymentConfiguration.getInstance().setShippingMethods(shippingMethods);
    }

    private ShippingInformation getExampleShippingInfo() {
        Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        return new ShippingInformation(address, "Fake Name", "6504604645");
    }
}
