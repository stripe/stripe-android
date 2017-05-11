package com.stripe.wrap.pay.activity;

import android.content.Intent;
import android.support.annotation.NonNull;

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
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Token;
import com.stripe.android.net.StripeApiHandler;
import com.stripe.android.net.StripeResponse;
import com.stripe.android.net.TokenParser;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.BuildConfig;
import com.stripe.wrap.pay.testharness.TestAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

import org.json.JSONException;
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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    private static final String RAW_TOKEN = "{\n" +
            "  \"id\": \"tok_189fi32eZvKYlo2Ct0KZvU5Y\",\n" +
            "  \"object\": \"token\",\n" +
            "  \"card\": {\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": null,\n" +
            "    \"address_country\": null,\n" +
            "    \"address_line1\": null,\n" +
            "    \"address_line1_check\": null,\n" +
            "    \"address_line2\": null,\n" +
            "    \"address_state\": null,\n" +
            "    \"address_zip\": null,\n" +
            "    \"address_zip_check\": null,\n" +
            "    \"brand\": \"Visa\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"cvc_check\": null,\n" +
            "    \"dynamic_last4\": null,\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"last4\": \"4242\",\n" +
            "    \"metadata\": {\n" +
            "    },\n" +
            "    \"name\": null,\n" +
            "    \"tokenization_method\": null\n" +
            "  },\n" +
            "  \"client_ip\": null,\n" +
            "  \"created\": 1462905355,\n" +
            "  \"livemode\": false,\n" +
            "  \"type\": \"card\",\n" +
            "  \"used\": false\n" +
            "}";

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
        mCartManager = new CartManager("USD");
        mCartManager.addLineItem("First item", 100L);
        mCartManager.addLineItem("Second item", 200L);

        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.getInstance();
        androidPayConfiguration.setPublicApiKey(FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
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

    @Test
    public void logApiCallOnNewThread_whenPaymentMethodIsToken_logsTokenCreation() {
        mActivityController.create().start();

        StripeApiHandler.LoggingResponseListener listener =
                mock(StripeApiHandler.LoggingResponseListener.class);
        when(listener.shouldLogTest()).thenReturn(true);

        ArgumentCaptor<StripeResponse> responseArgumentCaptor =
                ArgumentCaptor.forClass(StripeResponse.class);
        StripeAndroidPayActivity stripeActivity = mActivityController.get();

        // For testing purposes, we work on the main thread.
        stripeActivity.setExecutor(new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                command.run();
            }
        });

        try {
            Token token = TokenParser.parseToken(RAW_TOKEN);
            stripeActivity.logApiCallOnNewThread(token, listener);

            verify(listener).onLoggingResponse(responseArgumentCaptor.capture());
            StripeResponse response = responseArgumentCaptor.getValue();

            assertEquals(200, response.getResponseCode());
            verify(listener, never()).onStripeException(any(StripeException.class));
        } catch (JSONException unexpected) {
            fail("Test data parsing failure");
        }
    }
}
