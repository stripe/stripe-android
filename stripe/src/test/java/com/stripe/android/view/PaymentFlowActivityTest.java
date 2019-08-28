package com.stripe.android.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.ApiKeyFixtures;
import com.stripe.android.CustomerSession;
import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.PaymentConfiguration;
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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.Collections;

import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EVENT_SHIPPING_INFO_SAVED;
import static com.stripe.android.CustomerSession.EXTRA_EXCEPTION;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_DATA_KEY;
import static com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS;
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
public class PaymentFlowActivityTest extends BaseViewTest<PaymentFlowActivity> {

    private ShippingInfoWidget mShippingInfoWidget;
    private LocalBroadcastManager mLocalBroadcastManager;

    @Mock private EphemeralKeyProvider mEphemeralKeyProvider;
    @Mock private BroadcastReceiver mBroadcastReceiver;

    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    public PaymentFlowActivityTest() {
        super(PaymentFlowActivity.class);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mLocalBroadcastManager = LocalBroadcastManager
                .getInstance(ApplicationProvider.getApplicationContext());
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED));
        PaymentConfiguration.init(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);
        CustomerSession.initCustomerSession(ApplicationProvider.getApplicationContext(),
                mEphemeralKeyProvider);
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    @Test
    public void launchPaymentFlowActivity_withHideShippingInfoConfig_hidesShippingInfoView() {
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .setShippingInfoRequired(false)
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );
        assertNull(paymentFlowActivity.findViewById(R.id.shipping_info_widget));
        assertNotNull(paymentFlowActivity.findViewById(R.id.select_shipping_method_widget));
    }

    @Test
    public void onShippingInfoSave_whenShippingNotPopulated_doesNotFinish() {
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );
        mShippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        paymentFlowActivity.onActionSave();
        assertFalse(paymentFlowActivity.isFinishing());
    }

    @Test
    public void onShippingInfoSave_whenShippingInfoNotPopulated_doesNotContinue() {
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );
        mShippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        paymentFlowActivity.onActionSave();
        assertFalse(paymentFlowActivity.isFinishing());
        assertNotNull(paymentFlowActivity.findViewById(R.id.shipping_info_widget));
    }

    @Test
    public void onShippingInfoSave_whenShippingPopulated_sendsCorrectIntent() {
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );
        mShippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        paymentFlowActivity.onActionSave();
        verify(mBroadcastReceiver).onReceive(any(Context.class), mIntentArgumentCaptor.capture());

        final Intent captured = mIntentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertEquals(
                captured.getParcelableExtra(EXTRA_SHIPPING_INFO_DATA),
                getExampleShippingInfo());
    }

    @Test
    public void onErrorBroadcast_displaysAlertDialog() {
        final StripeActivity.AlertMessageListener mockListener =
                mock(StripeActivity.AlertMessageListener.class);
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );
        paymentFlowActivity.setAlertMessageListener(mockListener);

        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION,
                new APIException("Something's wrong", "ID123", 400, null, null));

        final Intent errorIntent = new Intent(ACTION_API_EXCEPTION);
        errorIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(paymentFlowActivity)
                .sendBroadcast(errorIntent);

        verify(mockListener).onAlertMessageDisplayed("Something's wrong");
    }

    @Test
    public void onShippingInfoProcessed_whenInvalidShippingInfoSubmitted_rendersCorrectly() {
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );

        // invalid result
        paymentFlowActivity.onActionSave();
        assertEquals(paymentFlowActivity.getProgressBar().getVisibility(), View.VISIBLE);

        final Intent onShippingInfoProcessedInvalid = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
        onShippingInfoProcessedInvalid.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(onShippingInfoProcessedInvalid);
        assertEquals(paymentFlowActivity.getProgressBar().getVisibility(), View.GONE);
    }

    @Test
    public void onShippingInfoProcessed_whenValidShippingInfoSubmitted_rendersCorrectly() {
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );

        // valid result
        paymentFlowActivity.onActionSave();

        final Intent onShippingInfoProcessedValid = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
        onShippingInfoProcessedValid.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);

        final ArrayList<ShippingMethod> shippingMethods = new ArrayList<>(Collections.singletonList(
                new ShippingMethod("label", "id", 0, "USD")));
        onShippingInfoProcessedValid.putExtra(EXTRA_VALID_SHIPPING_METHODS, shippingMethods);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(onShippingInfoProcessedValid);
        assertEquals(View.VISIBLE, paymentFlowActivity.getProgressBar().getVisibility());

        final Intent shippingInfoSaved = new Intent(EVENT_SHIPPING_INFO_SAVED);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(shippingInfoSaved);
        assertEquals(View.GONE, paymentFlowActivity.getProgressBar().getVisibility());
    }

    @Test
    public void onShippingInfoSaved_whenOnlyShippingInfo_finishWithSuccess() {
        final PaymentFlowActivity paymentFlowActivity = createActivity(
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                                .setShippingMethodsRequired(false)
                                .build())
                        .setPaymentSessionData(new PaymentSessionData())
                        .build()
        );
        final ShadowActivity shadowActivity = shadowOf(paymentFlowActivity);

        // valid result
        paymentFlowActivity.onActionSave();

        final Intent onShippingInfoProcessedValid = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
        onShippingInfoProcessedValid.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(onShippingInfoProcessedValid);

        final Intent shippingInfoSaved = new Intent(EVENT_SHIPPING_INFO_SAVED);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .sendBroadcast(shippingInfoSaved);
        assertTrue(paymentFlowActivity.isFinishing());
        assertEquals(shadowActivity.getResultCode(), Activity.RESULT_OK);

        final Bundle extras = shadowActivity.getResultIntent().getExtras();
        assertNotNull(extras);

        final PaymentSessionData resultSessionData = extras.getParcelable(PAYMENT_SESSION_DATA_KEY);
        assertNotNull(resultSessionData);
        assertEquals(resultSessionData.getShippingInformation(), getExampleShippingInfo());
    }

    @NonNull
    private ShippingInformation getExampleShippingInfo() {
        final Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        return new ShippingInformation(address, "Fake Name", "(555) 555-5555");
    }
}
