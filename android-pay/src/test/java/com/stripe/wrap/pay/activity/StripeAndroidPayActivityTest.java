package com.stripe.wrap.pay.activity;

import android.content.Intent;

import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.BuildConfig;
import com.stripe.wrap.pay.testharness.TestAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static android.app.Activity.RESULT_OK;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
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
        } catch (CartContentException unexpected) {
            fail("Error setting up tests");
        }
    }

    @Test
    public void onCreate_listenerHitsExpectedMethods() {
        mActivityController.create().start();
        verify(mListener).onBeforeAndroidPayAvailable();
        verify(mListener).onAfterAndroidPayCheckComplete();
        verify(mListener).getWalletEnvironment(WalletConstants.ENVIRONMENT_TEST);
        verify(mListener).getWalletTheme(WalletConstants.THEME_LIGHT);
        verify(mListener).addBuyButtonWalletFragment(any(SupportWalletFragment.class));

        verify(mGoogleApiClient).connect();
    }

    @Test
    public void onAndroidPayAvailable_listenerHitsExpectedMethods() {
        mActivityController.create().start();
        // Actions prior to this point are tested elsewhere
        reset(mListener);

//        MaskedWallet wallet = new MaskedWallet.Builder()
//        Intent dataIntent = new Intent().putExtra(
//                WalletConstants.EXTRA_MASKED_WALLET,
//                MaskedWallet.Builder)
//        mActivityController.get().onActivityResult(
//                StripeAndroidPayActivity.REQUEST_CODE_MASKED_WALLET,
//                RESULT_OK,
//                );

    }
}
