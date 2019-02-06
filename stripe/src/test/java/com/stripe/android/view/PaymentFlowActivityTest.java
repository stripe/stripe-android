package com.stripe.android.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.CustomerSession;
import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.R;
import com.stripe.android.exception.APIException;
import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;

import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EVENT_SHIPPING_INFO_SAVED;
import static com.stripe.android.CustomerSession.EXTRA_EXCEPTION;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_CONFIG;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_DATA_KEY;
import static com.stripe.android.view.PaymentFlowActivity.EVENT_SHIPPING_INFO_PROCESSED;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_IS_SHIPPING_INFO_VALID;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_SHIPPING_INFO_DATA;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_VALID_SHIPPING_METHODS;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class PaymentFlowActivityTest {

    private ShippingInfoWidget mShippingInfoWidget;
    @Mock private EphemeralKeyProvider mEphemeralKeyProvider;
    @Mock private BroadcastReceiver mBroadcastReceiver;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        LocalBroadcastManager localBroadcastManager =
                LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext());
        localBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(PaymentFlowActivity.EVENT_SHIPPING_INFO_SUBMITTED));
        CustomerSession.initCustomerSession(mEphemeralKeyProvider);
    }

    @After
    public void tearDown() {
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .unregisterReceiver(mBroadcastReceiver);
    }

    @Test
    public void launchPaymentFlowActivity_withHideShippingInfoConfig_hidesShippingInfoView() {
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .build();
        Intent intent = new Intent()
                .putExtra(PAYMENT_SESSION_DATA_KEY, new PaymentSessionData())
                .putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);
        assertNull(paymentFlowActivity.findViewById(R.id.shipping_info_widget));
        assertNotNull(paymentFlowActivity.findViewById(R.id.select_shipping_method_widget));
    }

    @Test
    public void onShippingInfoSave_whenShippingNotPopulated_doesNotFinish() {
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .build();
        Intent intent = new Intent()
                .putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig)
                .putExtra(PAYMENT_SESSION_DATA_KEY, new PaymentSessionData());
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);
        mShippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        paymentFlowActivity.onActionSave();
        assertFalse(paymentFlowActivity.isFinishing());
    }

    @Test
    public void onShippingInfoSave_whenShippingInfoNotPopulated_doesNotContinue() {
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentSessionData paymentSessionData = new PaymentSessionData();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, paymentSessionData);
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);
        mShippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        paymentFlowActivity.onActionSave();
        assertFalse(paymentFlowActivity.isFinishing());
        assertNotNull(paymentFlowActivity.findViewById(R.id.shipping_info_widget));
    }

    @Test
    public void onShippingInfoSave_whenShippingPopulated_sendsCorrectIntent() {
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentSessionData paymentSessionData = new PaymentSessionData();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, paymentSessionData);
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);
        mShippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        paymentFlowActivity.onActionSave();
        ArgumentCaptor<Intent> intentArgumentCaptor =
                ArgumentCaptor.forClass(Intent.class);
        verify(mBroadcastReceiver).onReceive(any(Context.class), intentArgumentCaptor.capture());
        Intent captured = intentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertEquals(
                captured.getParcelableExtra(EXTRA_SHIPPING_INFO_DATA),
                getExampleShippingInfo());
    }

    @Test
    public void onErrorBroadcast_displaysAlertDialog() {
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentSessionData paymentSessionData = new PaymentSessionData();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, paymentSessionData);

        StripeActivity.AlertMessageListener mockListener =
                mock(StripeActivity.AlertMessageListener.class);
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);
        paymentFlowActivity.setAlertMessageListener(mockListener);

        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION,
                new APIException("Something's wrong", "ID123", 400, null, null));
        Intent errorIntent = new Intent(ACTION_API_EXCEPTION);
        errorIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(paymentFlowActivity)
                .sendBroadcast(errorIntent);

        verify(mockListener).onAlertMessageDisplayed("Something's wrong");
    }

    @Test
    public void onShippingInfoProcessed_whenInvalidShippingInfoSubmitted_rendersCorrectly() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                PaymentFlowActivity.class);
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentSessionData paymentSessionData = new PaymentSessionData();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, paymentSessionData);
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);

        // invalid result
        paymentFlowActivity.onActionSave();
        assertEquals(paymentFlowActivity.mProgressBar.getVisibility(), View.VISIBLE);
        Intent onShippingInfoProcessedInvalid = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
        onShippingInfoProcessedInvalid.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(onShippingInfoProcessedInvalid);
        assertEquals(paymentFlowActivity.mProgressBar.getVisibility(), View.GONE);
    }

    @Test
    public void onShippingInfoProcessed_whenValidShippingInfoSubmitted_rendersCorrectly() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                PaymentFlowActivity.class);
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentSessionData paymentSessionData = new PaymentSessionData();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, paymentSessionData);
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);

        // valid result
        paymentFlowActivity.onActionSave();
        Intent onShippingInfoProcessedValid = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
        onShippingInfoProcessedValid.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
        ArrayList<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("label", "id", 0, "USD"));
        onShippingInfoProcessedValid.putExtra(EXTRA_VALID_SHIPPING_METHODS, shippingMethods);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(onShippingInfoProcessedValid);
        assertEquals(View.VISIBLE, paymentFlowActivity.mProgressBar.getVisibility());
        Intent shippingInfoSaved = new Intent(EVENT_SHIPPING_INFO_SAVED);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(shippingInfoSaved);
        assertEquals(View.GONE, paymentFlowActivity.mProgressBar.getVisibility());
    }

    @Test
    public void onShippingInfoSaved_whenOnlyShippingInfo_finishWithSuccess() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                PaymentFlowActivity.class);
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                .setShippingMethodsRequired(false)
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentSessionData paymentSessionData = new PaymentSessionData();
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, paymentSessionData);
        PaymentFlowActivity paymentFlowActivity = createActivity(intent);
        final ShadowActivity shadowActivity = shadowOf(paymentFlowActivity);

        // valid result
        paymentFlowActivity.onActionSave();
        Intent onShippingInfoProcessedValid = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
        onShippingInfoProcessedValid.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(onShippingInfoProcessedValid);
        Intent shippingInfoSaved = new Intent(EVENT_SHIPPING_INFO_SAVED);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(shippingInfoSaved);
        assertTrue(paymentFlowActivity.isFinishing());
        assertEquals(shadowActivity.getResultCode(), Activity.RESULT_OK);
        PaymentSessionData resultSessionData = shadowActivity.getResultIntent().getExtras()
                .getParcelable(PAYMENT_SESSION_DATA_KEY);
        assertEquals(resultSessionData.getShippingInformation(), getExampleShippingInfo());
    }

    @NonNull
    private ShippingInformation getExampleShippingInfo() {
        Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        return new ShippingInformation(address, "Fake Name", "(555) 555-5555");
    }

    @NonNull
    private PaymentFlowActivity createActivity(@NonNull Intent intent) {
        return Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create()
                .start()
                .resume()
                .visible()
                .get();
    }
}
