package com.stripe.wrap.pay.activity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.BuildConfig;
import com.stripe.wrap.pay.testharness.TestAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link StripeAndroidPayActivity}. Note that we have to test against SDK 22
 * because of a <a href="https://github.com/robolectric/robolectric/issues/1932">known issue</a> in
 * Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 22)
public class StripeAndroidPayActivityTest {

    private static final String TEST_KEY = "pk_test_willnotreallywork";

    @Mock TestAndroidPayActivity.AndroidPayAvailabilityChooser mAndroidPayAvailabilityChooser;
    @Mock GoogleApiClient mGoogleApiClient;
    @Mock TestAndroidPayActivity.GoogleApiClientMockBuilder mGoogleApiClientMockBuilder;
    @Mock TestAndroidPayActivity.StripeAndroidPayActivityListener mListener;

    private ActivityController<TestAndroidPayActivity> mActivityController;
    private CartManager mCartManager;
    private Cart mCart;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mGoogleApiClientMockBuilder.getMockGoogleApiClient()).thenReturn(mGoogleApiClient);
        mCartManager = new CartManager();
        mCartManager.addLineItem("First item", 100L);
        mCartManager.addLineItem("Second item", 200L);

        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.getInstance();
        androidPayConfiguration.setPublicApiKey(TEST_KEY);
        when(mAndroidPayAvailabilityChooser.doesAndroidPayCheckSucceed()).thenReturn(
                new BooleanResult(new Status(CommonStatusCodes.SUCCESS), true));

        try {
            mCart = mCartManager.buildCart();
            Intent intent = new Intent(RuntimeEnvironment.application, TestAndroidPayActivity.class)
                    .putExtra(StripeAndroidPayActivity.EXTRA_CART, mCart);
            mActivityController = Robolectric.buildActivity(TestAndroidPayActivity.class, intent);
            mActivityController.get().setStripeAppCompatActivityListener(mListener);
            mActivityController.get().setGoogleApiClientMockBuilder(mGoogleApiClientMockBuilder);
            mActivityController.get()
                    .setAndroidPayAvailabilityChooser(mAndroidPayAvailabilityChooser);
            mActivityController.get().setTheme(
                    android.support.v7.appcompat.R.style.Theme_AppCompat);
        } catch (CartContentException unexpected) {
            fail("Error setting up tests");
        }
    }

    @Test
    public void onCreate_whenGooglePlaySucceedsAndIsAvailable_listenerHitsExpectedMethods() {
        mActivityController.create().start();

        ArgumentCaptor<WalletFragmentOptions> walletFragmentOptionsCaptor =
                ArgumentCaptor.forClass(WalletFragmentOptions.class);

        verify(mListener).onBeforeAndroidPayAvailable();
        verify(mListener).onAfterAndroidPayCheckComplete();
        verify(mListener).getWalletEnvironment(WalletConstants.ENVIRONMENT_TEST);
        verify(mListener).getWalletFragmentOptions(walletFragmentOptionsCaptor.capture());
        verify(mListener).getWalletTheme(WalletConstants.THEME_LIGHT);
        verify(mListener).addBuyButtonWalletFragment(any(SupportWalletFragment.class));
        verify(mListener).verifyAndPrepareAndroidPayControls(any(IsReadyToPayRequest.class));
        verifyNoMoreInteractions(mListener);

        WalletFragmentOptions optionsUsed = walletFragmentOptionsCaptor.getValue();
        assertEquals(WalletFragmentMode.BUY_BUTTON, optionsUsed.getMode());

        verify(mGoogleApiClient).connect();
    }

    @Test
    public void onCreate_whenGooglePlaySucceedsAndIsNotAvailable_listenerHitsExpectedMethods() {
        reset(mAndroidPayAvailabilityChooser);
        when(mAndroidPayAvailabilityChooser.doesAndroidPayCheckSucceed()).thenReturn(
                new BooleanResult(new Status(CommonStatusCodes.SUCCESS), false));
        mActivityController.create().start();

        verify(mListener).onBeforeAndroidPayAvailable();
        verify(mListener).verifyAndPrepareAndroidPayControls(any(IsReadyToPayRequest.class));
        verify(mListener).onAfterAndroidPayCheckComplete();
        verify(mListener).onAndroidPayNotAvailable();
        verifyNoMoreInteractions(mListener);

        verify(mGoogleApiClient).connect();
    }

    @Test
    public void onCreate_whenGooglePlayFails_listenerHitsExpectedMethods() {
        reset(mAndroidPayAvailabilityChooser);
        when(mAndroidPayAvailabilityChooser.doesAndroidPayCheckSucceed()).thenReturn(
                new BooleanResult(new Status(CommonStatusCodes.ERROR), false));
        mActivityController.create().start();

        verify(mListener).onBeforeAndroidPayAvailable();
        verify(mListener).verifyAndPrepareAndroidPayControls(any(IsReadyToPayRequest.class));
        verify(mListener).onAfterAndroidPayCheckComplete();
        verify(mListener).onAndroidPayNotAvailable();
        verifyNoMoreInteractions(mListener);

        verify(mGoogleApiClient).connect();
    }

    @Test
    public void onStop_disconnectsGoogleApiClient() {
        mActivityController.create().start();
        reset(mListener);
        reset(mGoogleApiClient);

        mActivityController.stop();

        verify(mGoogleApiClient).disconnect();
        verifyNoMoreInteractions(mGoogleApiClient);
        verifyZeroInteractions(mListener);
    }

    @Test
    public void onConnected_overrideFromConnectionCallbacks_callsOnAndroidPayAvailable() {
        mActivityController.create().start();
        reset(mListener);

        Bundle testBundle = new Bundle();
        mActivityController.get().onConnected(testBundle);
        verify(mListener).onConnected(testBundle);
        verify(mListener).onAndroidPayAvailable();
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void onConnectionInterrupted_fromConnectionCallbacks_callsOnAndroidPayNotAvailable() {
        mActivityController.create().start();
        reset(mListener);

        mActivityController.get().onConnectionSuspended(
                GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST);
        verify(mListener).onConnectionSuspended(
                GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST);
        verify(mListener).onAndroidPayNotAvailable();
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void getWalletOptions_whenUsingBuyButtonMode_returnsExpectedResult() {
        mActivityController.create().start();
        WalletFragmentOptions options = mActivityController.get()
                        .accessWalletFragmentOptions(WalletFragmentMode.BUY_BUTTON);
        assertEquals(WalletFragmentMode.BUY_BUTTON, options.getMode());
        assertEquals(WalletConstants.ENVIRONMENT_TEST, options.getEnvironment());
        assertEquals(WalletConstants.THEME_LIGHT, options.getTheme());
    }

    @Test
    public void getWalletOptions_whenUsingSettingsMode_returnsExpectedResult() {
        mActivityController.create().start();
        WalletFragmentOptions options = mActivityController.get()
                .accessWalletFragmentOptions(WalletFragmentMode.SELECTION_DETAILS);
        assertEquals(WalletFragmentMode.SELECTION_DETAILS, options.getMode());
        assertEquals(WalletConstants.ENVIRONMENT_TEST, options.getEnvironment());
        assertEquals(WalletConstants.THEME_LIGHT, options.getTheme());
    }
}
